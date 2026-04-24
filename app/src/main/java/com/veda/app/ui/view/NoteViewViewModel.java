package com.veda.app.ui.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.veda.app.data.entity.NoteDetailItem;
import com.veda.app.data.repo.VedaRepository;

public final class NoteViewViewModel extends AndroidViewModel {

    private final VedaRepository repo;
    private final MutableLiveData<Long> noteId = new MutableLiveData<>(0L);

    public final LiveData<NoteDetailItem> item;

    public NoteViewViewModel(@NonNull Application application) {
        super(application);
        repo = VedaRepository.get(application);

        item = Transformations.switchMap(noteId, id -> {
            if (id == null || id <= 0) return new MutableLiveData<>(null);
            return repo.observeNoteDetail(id);
        });
    }

    public void load(long id) {
        noteId.setValue(id);
    }

    public long getNoteId() {
        Long v = noteId.getValue();
        return v == null ? 0L : v;
    }

    public void delete() {
        long id = getNoteId();
        if (id <= 0) return;
        repo.deleteNoteById(id);
    }
}