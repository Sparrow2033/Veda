package com.veda.app.ui.homework;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.veda.app.R;
import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.repo.VedaRepository;
import com.veda.app.utils.AppPrefs;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class HomeworkListViewModel extends AndroidViewModel {

    public enum Filter {
        ALL,
        TODAY,
        WEEK,
        OVERDUE,
        UPCOMING,
        DONE
    }

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long WEEK_MS = 7L * DAY_MS;

    private final VedaRepository repo;
    private final LiveData<List<HomeworkEntity>> source;
    private final MediatorLiveData<List<HomeworkRowItem>> rows = new MediatorLiveData<>();

    private Filter currentFilter = Filter.ALL;

    public HomeworkListViewModel(@NonNull Application application) {
        super(application);

        repo = VedaRepository.get(application);
        source = repo.observeHomeworkAll();

        rows.addSource(source, list -> rows.setValue(buildRows(list)));
    }

    @NonNull
    public LiveData<List<HomeworkRowItem>> rows() {
        return rows;
    }

    public void setFilter(@NonNull Filter filter) {
        currentFilter = filter;
        rows.setValue(buildRows(source.getValue()));
    }

    public void toggleDone(@NonNull HomeworkEntity item) {
        int newStatus = item.status == HomeworkEntity.STATUS_DONE
                ? HomeworkEntity.STATUS_TODO
                : HomeworkEntity.STATUS_DONE;

        HomeworkEntity updated = new HomeworkEntity(
                item.id,
                item.subjectId,
                item.title == null ? "" : item.title,
                item.description == null ? "" : item.description,
                item.dueDate,
                newStatus,
                item.priority,
                item.createdAt,
                System.currentTimeMillis()
        );

        repo.updateHomework(updated);
    }

    public int toggledStateToastRes(@NonNull HomeworkEntity item) {
        boolean goesDone = item.status != HomeworkEntity.STATUS_DONE;
        return goesDone ? R.string.homework_toast_done : R.string.homework_toast_todo;
    }

    private List<HomeworkRowItem> buildRows(List<HomeworkEntity> sourceList) {
        List<HomeworkEntity> filtered = applyFilter(sourceList, currentFilter);
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        applySort(filtered);

        List<HomeworkEntity> withDue = new ArrayList<>();
        List<HomeworkEntity> noDue = new ArrayList<>();

        for (HomeworkEntity e : filtered) {
            if (e == null) continue;
            if (e.dueDate > 0) withDue.add(e);
            else noDue.add(e);
        }

        noDue.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));

        List<HomeworkRowItem> out = new ArrayList<>();
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplication());

        long prevDay = Long.MIN_VALUE;
        for (HomeworkEntity e : withDue) {
            long day = startOfDay(e.dueDate);
            if (day != prevDay) {
                prevDay = day;
                String header = dateFormat.format(new Date(day));
                out.add(HomeworkRowItem.header(header, -day));
            }
            out.add(HomeworkRowItem.task(e));
        }

        if (!noDue.isEmpty()) {
            out.add(HomeworkRowItem.header(getApplication().getString(R.string.homework_header_no_deadline), -1L));
            for (HomeworkEntity e : noDue) out.add(HomeworkRowItem.task(e));
        }

        return out;
    }

    private void applySort(@NonNull List<HomeworkEntity> list) {
        AppPrefs.HomeworkSort sort = AppPrefs.getHomeworkSort(getApplication());

        if (sort == AppPrefs.HomeworkSort.PRIORITY) {
            list.sort((a, b) -> Integer.compare(b.priority, a.priority));
            return;
        }
        if (sort == AppPrefs.HomeworkSort.STATUS) {
            list.sort((a, b) -> Integer.compare(a.status, b.status));
            return;
        }

        list.sort((a, b) -> {
            long da = a.dueDate <= 0 ? Long.MAX_VALUE : a.dueDate;
            long db = b.dueDate <= 0 ? Long.MAX_VALUE : b.dueDate;
            return Long.compare(da, db);
        });
    }

    @NonNull
    private List<HomeworkEntity> applyFilter(List<HomeworkEntity> sourceList, @NonNull Filter filter) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        long todayStart = startOfDay(now);
        long tomorrowStart = todayStart + DAY_MS;
        long weekEnd = todayStart + WEEK_MS;
        long doneCutoff = now - WEEK_MS;

        List<HomeworkEntity> out = new ArrayList<>();

        for (HomeworkEntity item : sourceList) {
            if (item == null) continue;

            boolean isDone = item.status == HomeworkEntity.STATUS_DONE;
            long due = item.dueDate;

            switch (filter) {
                case ALL:
                    if (!isDone) out.add(item);
                    break;
                case TODAY:
                    if (!isDone && due >= todayStart && due < tomorrowStart) out.add(item);
                    break;
                case WEEK:
                    if (!isDone && due > 0L && due >= todayStart && due < weekEnd) out.add(item);
                    break;
                case OVERDUE:
                    if (!isDone && due > 0L && due < todayStart) out.add(item);
                    break;
                case UPCOMING:
                    if (!isDone && due >= tomorrowStart) out.add(item);
                    break;
                case DONE:
                    if (isDone && item.updatedAt >= doneCutoff) out.add(item);
                    break;
            }
        }

        return out;
    }

    private static long startOfDay(long timeMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
