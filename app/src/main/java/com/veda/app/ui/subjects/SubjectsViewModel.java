package com.veda.app.ui.subjects;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.data.repo.VedaRepository;

import java.util.List;

public class SubjectsViewModel extends AndroidViewModel {

    private final VedaRepository repo;

    public SubjectsViewModel(@NonNull Application application) {
        super(application);
        repo = VedaRepository.get(application);
    }

    public LiveData<List<SubjectEntity>> subjects() {
        return repo.observeSubjects();
    }

    public void addSubject(String name, int color, int sortOrder, VedaRepository.ResultCallback<Long> cb) {
        repo.insertSubject(new SubjectEntity(name, color, sortOrder), cb);
    }

    public void update(SubjectEntity subject) {
        repo.updateSubject(subject);
    }

    public void delete(SubjectEntity subject) {
        repo.deleteSubject(subject);
    }
}
