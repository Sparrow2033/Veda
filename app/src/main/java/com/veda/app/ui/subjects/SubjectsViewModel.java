package com.veda.app.ui.subjects;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.data.repo.VedaRepository;

import java.util.List;

public class SubjectsViewModel extends AndroidViewModel {
    public enum ScreenState {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    private final VedaRepository repo;
    private final MediatorLiveData<List<SubjectEntity>> subjects = new MediatorLiveData<>();
    private final MutableLiveData<ScreenState> screenState = new MutableLiveData<>(ScreenState.LOADING);
    private final MutableLiveData<Integer> operationMessageRes = new MutableLiveData<>(0);

    public SubjectsViewModel(@NonNull Application application) {
        super(application);
        repo = VedaRepository.get(application);
        subjects.addSource(repo.observeSubjects(), list -> {
            try {
                subjects.setValue(list);
                boolean isEmpty = list == null || list.isEmpty();
                screenState.setValue(isEmpty ? ScreenState.EMPTY : ScreenState.CONTENT);
            } catch (Throwable ignored) {
                subjects.setValue(null);
                screenState.setValue(ScreenState.ERROR);
            }
        });
    }

    public LiveData<List<SubjectEntity>> subjects() {
        return subjects;
    }

    public LiveData<ScreenState> screenState() {
        return screenState;
    }

    public LiveData<Integer> operationMessageRes() {
        return operationMessageRes;
    }

    public void addSubject(String name, int color, int sortOrder, VedaRepository.ResultCallback<Long> cb) {
        repo.insertSubject(new SubjectEntity(name, color, sortOrder), id -> {
            if (id != null && id > 0L) {
                operationMessageRes.setValue(com.veda.app.R.string.subject_saved);
            } else {
                operationMessageRes.setValue(com.veda.app.R.string.subject_save_error);
            }
            if (cb != null) {
                cb.onResult(id);
            }
        });
    }

    public void update(SubjectEntity subject) {
        repo.updateSubject(subject, success -> operationMessageRes.setValue(
                (success != null && success)
                        ? com.veda.app.R.string.subject_updated
                        : com.veda.app.R.string.subject_update_error
        ));
    }

    public void delete(SubjectEntity subject) {
        repo.deleteSubject(subject, success -> operationMessageRes.setValue(
                (success != null && success)
                        ? com.veda.app.R.string.subject_deleted
                        : com.veda.app.R.string.subject_delete_error
        ));
    }

    public void consumeOperationMessage() {
        operationMessageRes.setValue(0);
    }
}
