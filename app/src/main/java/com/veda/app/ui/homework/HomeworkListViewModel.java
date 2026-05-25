package com.veda.app.ui.homework;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.veda.app.R;
import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.repo.VedaRepository;
import com.veda.app.utils.AppPrefs;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class HomeworkListViewModel extends AndroidViewModel {
    public enum ScreenState {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    public enum Filter {
        ALL,
        TODAY,
        WEEK,
        OVERDUE,
        UPCOMING,
        DONE
    }

    private final VedaRepository repo;
    private final LiveData<List<HomeworkEntity>> source;
    private final MediatorLiveData<List<HomeworkRowItem>> rows = new MediatorLiveData<>();
    private final MutableLiveData<ScreenState> screenState = new MutableLiveData<>(ScreenState.LOADING);
    private final MutableLiveData<Integer> operationMessageRes = new MutableLiveData<>(0);

    private Filter currentFilter = Filter.ALL;

    public HomeworkListViewModel(@NonNull Application application) {
        super(application);

        repo = VedaRepository.get(application);
        source = repo.observeHomeworkAll();

        rows.addSource(source, list -> {
            try {
                List<HomeworkRowItem> builtRows = buildRows(list);
                rows.setValue(builtRows);
                screenState.setValue(builtRows.isEmpty() ? ScreenState.EMPTY : ScreenState.CONTENT);
            } catch (Throwable ignored) {
                rows.setValue(Collections.emptyList());
                screenState.setValue(ScreenState.ERROR);
            }
        });
    }

    @NonNull
    public LiveData<List<HomeworkRowItem>> rows() {
        return rows;
    }

    public void setFilter(@NonNull Filter filter) {
        currentFilter = filter;
        try {
            List<HomeworkRowItem> builtRows = buildRows(source.getValue());
            rows.setValue(builtRows);
            screenState.setValue(builtRows.isEmpty() ? ScreenState.EMPTY : ScreenState.CONTENT);
        } catch (Throwable ignored) {
            rows.setValue(Collections.emptyList());
            screenState.setValue(ScreenState.ERROR);
        }
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

        repo.updateHomework(updated, success -> {
            boolean goesDone = item.status != HomeworkEntity.STATUS_DONE;
            if (success != null && success) {
                operationMessageRes.setValue(goesDone ? R.string.homework_toast_done : R.string.homework_toast_todo);
            } else {
                operationMessageRes.setValue(R.string.homework_error_update);
            }
        });
    }

    @NonNull
    public LiveData<ScreenState> screenState() {
        return screenState;
    }

    @NonNull
    public LiveData<Integer> operationMessageRes() {
        return operationMessageRes;
    }

    public void consumeOperationMessage() {
        operationMessageRes.setValue(0);
    }

    private List<HomeworkRowItem> buildRows(List<HomeworkEntity> sourceList) {
        List<HomeworkEntity> filtered = HomeworkListTransforms.applyFilter(
                sourceList,
                currentFilter,
                System.currentTimeMillis()
        );
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        HomeworkListTransforms.applySort(filtered, AppPrefs.getHomeworkSort(getApplication()));

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
            long day = HomeworkListTransforms.startOfDay(e.dueDate);
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

}
