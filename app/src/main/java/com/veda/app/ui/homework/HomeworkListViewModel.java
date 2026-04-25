package com.veda.app.ui.homework;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.repo.VedaRepository;

import java.util.ArrayList;
import java.util.Collections;
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

    private final MediatorLiveData<List<HomeworkEntity>> items = new MediatorLiveData<>();
    private final LiveData<List<HomeworkEntity>> source;

    private Filter currentFilter = Filter.ALL;

    public HomeworkListViewModel(@NonNull Application application) {
        super(application);

        VedaRepository repo = VedaRepository.get(application);
        source = repo.observeHomeworkAll();

        items.addSource(source, list -> items.setValue(applyFilter(list, currentFilter)));
    }

    @NonNull
    public LiveData<List<HomeworkEntity>> items() {
        return items;
    }

    public void setFilter(@NonNull Filter filter) {
        currentFilter = filter;
        items.setValue(applyFilter(source.getValue(), currentFilter));
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
                    if (!isDone) {
                        out.add(item);
                    }
                    break;

                case TODAY:
                    if (!isDone && due >= todayStart && due < tomorrowStart) {
                        out.add(item);
                    }
                    break;

                case WEEK:
                    if (!isDone && due > 0L && due >= todayStart && due < weekEnd) {
                        out.add(item);
                    }
                    break;

                case OVERDUE:
                    if (!isDone && due > 0L && due < todayStart) {
                        out.add(item);
                    }
                    break;

                case UPCOMING:
                    if (!isDone && due >= tomorrowStart) {
                        out.add(item);
                    }
                    break;

                case DONE:
                    if (isDone && item.updatedAt >= doneCutoff) {
                        out.add(item);
                    }
                    break;
            }
        }

        return out;
    }

    private static long startOfDay(long timeMillis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
