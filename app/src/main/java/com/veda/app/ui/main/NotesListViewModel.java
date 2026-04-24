package com.veda.app.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.veda.app.data.entity.NoteListItem;
import com.veda.app.data.repo.VedaRepository;

import java.util.List;

public class NotesListViewModel extends AndroidViewModel {

    private final VedaRepository repo;
    public final LiveData<List<NoteListItem>> notes;

    public NotesListViewModel(@NonNull Application application) {
        super(application);
        repo = VedaRepository.get(application);
        notes = repo.observeNotesList();
    }
}