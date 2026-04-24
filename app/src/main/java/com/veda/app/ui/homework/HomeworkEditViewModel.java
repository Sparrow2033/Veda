package com.veda.app.ui.homework;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.repo.VedaRepository;

public final class HomeworkEditViewModel extends AndroidViewModel {

    private final VedaRepository repo;

    private final MutableLiveData<Long> homeworkId = new MutableLiveData<>(0L);
    private final LiveData<HomeworkEntity> homework;

    public HomeworkEditViewModel(@NonNull Application application) {
        super(application);
        repo = VedaRepository.get(application);

        homework = Transformations.switchMap(homeworkId, id -> {
            if (id == null || id <= 0) return new MutableLiveData<>(null);
            return repo.observeHomework(id);
        });
    }

    public LiveData<HomeworkEntity> homework() {
        return homework;
    }

    public void setHomeworkId(long id) {
        homeworkId.setValue(id);
    }

    public void insert(long subjectId,
                       @NonNull String title,
                       @NonNull String description,
                       long dueDate,
                       int status,
                       int priority,
                       VedaRepository.IdCallback callback) {
        long now = System.currentTimeMillis();
        HomeworkEntity entity = HomeworkEntity.newItem(
                subjectId,
                title,
                description,
                dueDate,
                status,
                priority,
                now
        );
        repo.insertHomework(entity, callback);
    }

    public void update(long id,
                       long subjectId,
                       @NonNull String title,
                       @NonNull String description,
                       long dueDate,
                       int status,
                       int priority,
                       long createdAt) {
        long now = System.currentTimeMillis();
        HomeworkEntity entity = new HomeworkEntity(
                id,
                subjectId,
                title,
                description,
                dueDate,
                status,
                priority,
                createdAt,
                now
        );
        repo.updateHomework(entity);
    }

    public void deleteById(long id) {
        repo.deleteHomeworkById(id);
    }
}