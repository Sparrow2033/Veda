package com.veda.app.data.repo;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.veda.app.data.dao.HomeworkDao;
import com.veda.app.data.dao.LinkDao;
import com.veda.app.data.dao.NoteDao;
import com.veda.app.data.dao.SubjectDao;
import com.veda.app.data.db.AppDatabase;
import com.veda.app.data.db.DbExecutors;
import com.veda.app.data.entity.GraphNodeItem;
import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.entity.LinkEntity;
import com.veda.app.data.entity.NoteDetailItem;
import com.veda.app.data.entity.NoteEntity;
import com.veda.app.data.entity.NoteListItem;
import com.veda.app.data.entity.SubjectEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class VedaRepository {

    public interface ResultCallback<T> {
        void onResult(T result);
    }

    public static final class GraphData {
        public final List<GraphNodeItem> nodes;
        public final List<LinkEntity> edges;

        public GraphData(List<GraphNodeItem> nodes, List<LinkEntity> edges) {
            this.nodes = nodes;
            this.edges = edges;
        }
    }

    private static volatile VedaRepository INSTANCE;

    private final AppDatabase db;
    private final SubjectDao subjectDao;
    private final NoteDao noteDao;
    private final LinkDao linkDao;
    private final HomeworkDao homeworkDao;

    private VedaRepository(Context context) {
        db = AppDatabase.get(context);
        subjectDao = db.subjectDao();
        noteDao = db.noteDao();
        linkDao = db.linkDao();
        homeworkDao = db.homeworkDao();
    }

    public static VedaRepository get(Context context) {
        if (INSTANCE == null) {
            synchronized (VedaRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new VedaRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<SubjectEntity>> observeSubjects() {
        return subjectDao.observeAll();
    }

    public void insertSubject(SubjectEntity subject, @Nullable ResultCallback<Long> callback) {
        runDisk(() -> subjectDao.insert(subject), callback);
    }

    public void updateSubject(SubjectEntity subject) {
        runDisk(() -> {
            subjectDao.update(subject);
            return null;
        }, null);
    }

    public void deleteSubject(SubjectEntity subject) {
        runDisk(() -> {
            subjectDao.delete(subject);
            return null;
        }, null);
    }

    public void ensureDefaultSubjects() {
        runDisk(() -> {
            if (subjectDao.countSync() > 0) return null;

            List<SubjectEntity> seed = new ArrayList<>();
            seed.add(new SubjectEntity("Математика", 0xFF2166F3, 1));
            seed.add(new SubjectEntity("Физика", 0xFF7149F7, 2));
            seed.add(new SubjectEntity("Химия", 0xFF00A8C8, 3));
            seed.add(new SubjectEntity("Биология", 0xFF4CC73D, 4));
            seed.add(new SubjectEntity("История", 0xFFCF5B1F, 5));
            seed.add(new SubjectEntity("Литература", 0xFFCD46B3, 6));
            seed.add(new SubjectEntity("Английский", 0xFF448AFF, 7));
            seed.add(new SubjectEntity("Информатика", 0xFF263238, 8));

            subjectDao.insertAll(seed);
            return null;
        }, null);
    }

    public LiveData<List<NoteListItem>> observeNotesList() {
        return noteDao.observeList();
    }

    public LiveData<NoteEntity> observeNote(long noteId) {
        return noteDao.observeById(noteId);
    }

    public LiveData<NoteDetailItem> observeNoteDetail(long noteId) {
        return noteDao.observeDetail(noteId);
    }

    public void deleteNoteById(long noteId) {
        runDisk(() -> {
            noteDao.deleteById(noteId);
            return null;
        }, null);
    }

    public void loadNotesForPicker(long excludeId, @Nullable ResultCallback<List<NoteListItem>> callback) {
        runDisk(() -> noteDao.getPickListSync(excludeId), callback);
    }

    public void insertNoteWithLinks(NoteEntity note, List<Long> toNoteIds, @Nullable ResultCallback<Long> callback) {
        runDisk(() -> {
            final long[] out = new long[]{-1};
            db.runInTransaction(() -> {
                long newId = noteDao.insert(note);
                out[0] = newId;
                replaceOutgoingLinksTx(newId, toNoteIds);
            });
            return out[0];
        }, callback);
    }

    public void updateNoteWithLinks(NoteEntity note, List<Long> toNoteIds) {
        runDisk(() -> {
            db.runInTransaction(() -> {
                noteDao.update(note);
                replaceOutgoingLinksTx(note.id, toNoteIds);
            });
            return null;
        }, null);
    }

    private void replaceOutgoingLinksTx(long fromNoteId, List<Long> toNoteIds) {
        linkDao.deleteByFromNoteId(fromNoteId);
        if (toNoteIds == null || toNoteIds.isEmpty()) return;

        Set<Long> uniq = new LinkedHashSet<>();
        for (Long id : toNoteIds) {
            if (id == null) continue;
            if (id <= 0) continue;
            if (id == fromNoteId) continue;
            uniq.add(id);
        }
        if (uniq.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<LinkEntity> links = new ArrayList<>();
        for (Long toId : uniq) links.add(new LinkEntity(fromNoteId, toId, now));
        linkDao.insertAll(links);
    }

    public void loadGraph(@Nullable ResultCallback<GraphData> callback) {
        runDisk(() -> {
            List<GraphNodeItem> nodes = noteDao.getGraphNodesSync();
            List<LinkEntity> edges = linkDao.getAllSync();
            return new GraphData(nodes, edges);
        }, callback);
    }

    public LiveData<List<HomeworkEntity>> observeHomeworkAll() {
        return homeworkDao.observeAll();
    }

    public LiveData<HomeworkEntity> observeHomework(long id) {
        return homeworkDao.observeById(id);
    }

    public LiveData<List<HomeworkEntity>> observeHomeworkByDueDateRange(long startInclusive, long endExclusive) {
        return homeworkDao.observeByDueDateRange(startInclusive, endExclusive);
    }

    public LiveData<List<HomeworkEntity>> observeHomeworkOverdue(long now) {
        return homeworkDao.observeOverdue(now);
    }

    public LiveData<List<HomeworkEntity>> observeHomeworkNextUpcoming(long now, int limit) {
        return homeworkDao.observeNextUpcoming(now, limit);
    }

    public void insertHomework(HomeworkEntity homework, @Nullable ResultCallback<Long> callback) {
        runDisk(() -> homeworkDao.insert(homework), callback);
    }

    public void updateHomework(HomeworkEntity homework) {
        runDisk(() -> {
            homeworkDao.update(homework);
            return null;
        }, null);
    }

    public void deleteHomework(HomeworkEntity homework) {
        runDisk(() -> {
            homeworkDao.delete(homework);
            return null;
        }, null);
    }

    public void deleteHomeworkById(long id) {
        runDisk(() -> {
            homeworkDao.deleteById(id);
            return null;
        }, null);
    }

    private <T> void runDisk(RepoCommand<T> command, @Nullable ResultCallback<T> callback) {
        DbExecutors.get().diskIO().execute(() -> {
            T result = command.execute();
            deliver(callback, result);
        });
    }

    private <T> void deliver(@Nullable ResultCallback<T> callback, T result) {
        if (callback == null) {
            return;
        }
        DbExecutors.get().mainThread().execute(() -> callback.onResult(result));
    }

    private interface RepoCommand<T> {
        T execute();
    }
}
