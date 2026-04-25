package com.veda.app.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.veda.app.data.entity.NoteListItem;
import com.veda.app.data.repo.VedaRepository;
import com.veda.app.utils.AppPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotesListViewModel extends AndroidViewModel {

    public enum ScreenState {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    private final MediatorLiveData<List<NoteListItem>> notes = new MediatorLiveData<>();
    private final MutableLiveData<ScreenState> screenState = new MutableLiveData<>(ScreenState.LOADING);

    public NotesListViewModel(@NonNull Application application) {
        super(application);

        VedaRepository repo = VedaRepository.get(application);
        notes.addSource(repo.observeNotesList(), items -> {
            try {
                List<NoteListItem> sorted = sortNotes(items);
                notes.setValue(sorted);
                screenState.setValue(sorted.isEmpty() ? ScreenState.EMPTY : ScreenState.CONTENT);
            } catch (Throwable ignored) {
                notes.setValue(Collections.emptyList());
                screenState.setValue(ScreenState.ERROR);
            }
        });
    }

    public LiveData<List<NoteListItem>> notes() {
        return notes;
    }

    public LiveData<ScreenState> screenState() {
        return screenState;
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
