package com.veda.app.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.veda.app.data.entity.NoteListItem;
import com.veda.app.data.repo.VedaRepository;
import com.veda.app.utils.AppPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotesListViewModel extends AndroidViewModel {

    private final MediatorLiveData<List<NoteListItem>> notes = new MediatorLiveData<>();

    public NotesListViewModel(@NonNull Application application) {
        super(application);

        VedaRepository repo = VedaRepository.get(application);
        notes.addSource(repo.observeNotesList(), items -> notes.setValue(sortNotes(items)));
    }

    public LiveData<List<NoteListItem>> notes() {
        return notes;
    }

    private List<NoteListItem> sortNotes(List<NoteListItem> items) {
        if (items == null) return Collections.emptyList();

        List<NoteListItem> list = new ArrayList<>(items);
        AppPrefs.NotesSort sort = AppPrefs.getNotesSort(getApplication());

        if (sort == AppPrefs.NotesSort.ALPHABETICAL) {
            list.sort((a, b) -> {
                String ta = a.title == null ? "" : a.title.trim();
                String tb = b.title == null ? "" : b.title.trim();
                return ta.compareToIgnoreCase(tb);
            });
            return list;
        }

        Comparator<NoteListItem> byUpdated = (a, b) -> Long.compare(a.updatedAt, b.updatedAt);
        list.sort(byUpdated);

        if (sort == AppPrefs.NotesSort.NEW_FIRST) Collections.reverse(list);
        return list;
    }
}
