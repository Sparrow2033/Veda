// File: app/src/main/java/com/veda/app/ui/edit/EditNoteViewModel.java
package com.veda.app.ui.edit;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.veda.app.data.entity.NoteEntity;
import com.veda.app.data.entity.NoteListItem;
import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.data.repo.VedaRepository;

import java.util.List;

public class EditNoteViewModel extends AndroidViewModel {

    private final VedaRepository repo;

    public EditNoteViewModel(@NonNull Application application) {
        super(application);
        repo = VedaRepository.get(application);
    }

    public LiveData<List<SubjectEntity>> subjects() {
        return repo.observeSubjects();
    }

    public LiveData<NoteEntity> note(long noteId) {
        return repo.observeNote(noteId);
    }

    public void loadNotesForPicker(long excludeId, VedaRepository.NotesCallback callback) {
        repo.loadNotesForPicker(excludeId, callback);
    }

    public void insertSubject(String name, int color, int sortOrder, VedaRepository.IdCallback callback) {
        repo.insertSubject(new SubjectEntity(name, color, sortOrder), callback);
    }

    public void saveNewWithLinks(
            Long subjectId,
            String title,
            String contentHtml,
            String tags,
            List<Long> toNoteIds,
            VedaRepository.IdCallback callback
    ) {
        long now = System.currentTimeMillis();
        NoteEntity note = new NoteEntity(subjectId, title, contentHtml, tags, now, now);
        repo.insertNoteWithLinks(note, toNoteIds, callback);
    }

    public void saveUpdateWithLinks(
            long noteId,
            Long subjectId,
            String title,
            String contentHtml,
            String tags,
            long createdAt,
            List<Long> toNoteIds
    ) {
        long now = System.currentTimeMillis();
        NoteEntity note = new NoteEntity(subjectId, title, contentHtml, tags, createdAt, now);
        note.id = noteId;
        repo.updateNoteWithLinks(note, toNoteIds);
    }

    public void deleteById(long noteId) {
        repo.deleteNoteById(noteId);
    }
}