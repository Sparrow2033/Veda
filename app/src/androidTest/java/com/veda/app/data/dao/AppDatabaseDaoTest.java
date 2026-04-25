package com.veda.app.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.veda.app.data.db.AppDatabase;
import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.entity.LinkEntity;
import com.veda.app.data.entity.NoteEntity;
import com.veda.app.data.entity.SubjectEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseDaoTest {

    private AppDatabase db;
    private SubjectDao subjectDao;
    private NoteDao noteDao;
    private LinkDao linkDao;
    private HomeworkDao homeworkDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        subjectDao = db.subjectDao();
        noteDao = db.noteDao();
        linkDao = db.linkDao();
        homeworkDao = db.homeworkDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void noteCrud_andSubjectSetNullOnDelete() {
        long now = System.currentTimeMillis();
        long subjectId = subjectDao.insert(new SubjectEntity("Math", 0xFF0000FF, 1));

        NoteEntity note = new NoteEntity(subjectId, "Limits", "<p>a</p>", "analysis", now, now);
        long noteId = noteDao.insert(note);

        NoteEntity loaded = noteDao.getByIdSync(noteId);
        assertNotNull(loaded);
        assertEquals(Long.valueOf(subjectId), loaded.subjectId);

        loaded.title = "Limits updated";
        loaded.updatedAt = now + 1;
        noteDao.update(loaded);

        NoteEntity updated = noteDao.getByIdSync(noteId);
        assertNotNull(updated);
        assertEquals("Limits updated", updated.title);

        SubjectEntity subjectToDelete = new SubjectEntity("Math", 0xFF0000FF, 1);
        subjectToDelete.id = subjectId;
        subjectDao.delete(subjectToDelete);

        NoteEntity orphaned = noteDao.getByIdSync(noteId);
        assertNotNull(orphaned);
        assertNull(orphaned.subjectId);

        noteDao.deleteById(noteId);
        assertNull(noteDao.getByIdSync(noteId));
    }

    @Test
    public void linkCrud_uniqueAndCascadeOnNoteDelete() {
        long now = System.currentTimeMillis();
        long subjectId = subjectDao.insert(new SubjectEntity("Physics", 0xFF00FF00, 2));

        long fromId = noteDao.insert(new NoteEntity(subjectId, "Force", "<p>F=ma</p>", "mech", now, now));
        long toId = noteDao.insert(new NoteEntity(subjectId, "Mass", "<p>m</p>", "mech", now, now));

        List<LinkEntity> duplicates = Arrays.asList(
                new LinkEntity(fromId, toId, now),
                new LinkEntity(fromId, toId, now + 1)
        );
        linkDao.insertAll(duplicates);

        List<LinkEntity> links = linkDao.getAllSync();
        assertEquals(1, links.size());

        linkDao.deleteByFromNoteId(fromId);
        assertTrue(linkDao.getAllSync().isEmpty());

        linkDao.insertAll(Collections.singletonList(new LinkEntity(fromId, toId, now + 2)));
        assertEquals(1, linkDao.getAllSync().size());

        noteDao.deleteById(toId);
        assertTrue(linkDao.getAllSync().isEmpty());
    }

    @Test
    public void homeworkCrud_rangeAndOverdueEdgeCases() {
        long now = 1_700_000_000_000L;
        long subjectId = subjectDao.insert(new SubjectEntity("History", 0xFFFF0000, 3));

        HomeworkEntity noDeadline = HomeworkEntity.newItem(
                subjectId,
                "Read chapter",
                "No strict due date",
                0,
                HomeworkEntity.STATUS_TODO,
                HomeworkEntity.PRIORITY_LOW,
                now
        );
        long noDeadlineId = homeworkDao.insert(noDeadline);

        HomeworkEntity overdue = HomeworkEntity.newItem(
                subjectId,
                "Essay",
                "Submit",
                now - 10_000,
                HomeworkEntity.STATUS_IN_PROGRESS,
                HomeworkEntity.PRIORITY_HIGH,
                now
        );
        long overdueId = homeworkDao.insert(overdue);

        HomeworkEntity upcoming = HomeworkEntity.newItem(
                subjectId,
                "Presentation",
                "Prepare slides",
                now + 10_000,
                HomeworkEntity.STATUS_TODO,
                HomeworkEntity.PRIORITY_MEDIUM,
                now
        );
        long upcomingId = homeworkDao.insert(upcoming);

        assertNotNull(homeworkDao.getByIdSync(noDeadlineId));

        List<HomeworkEntity> overdueList = homeworkDao.getOverdueSync(now);
        assertEquals(1, overdueList.size());
        assertEquals(overdueId, overdueList.get(0).id);

        List<HomeworkEntity> range = homeworkDao.getByDueDateRangeSync(now, now + 20_000);
        assertEquals(1, range.size());
        assertEquals(upcomingId, range.get(0).id);

        List<HomeworkEntity> upcomingList = homeworkDao.getNextUpcomingSync(now, 10);
        assertEquals(1, upcomingList.size());
        assertEquals(upcomingId, upcomingList.get(0).id);

        homeworkDao.deleteById(upcomingId);
        assertNull(homeworkDao.getByIdSync(upcomingId));

        SubjectEntity subjectToDelete = new SubjectEntity("History", 0xFFFF0000, 3);
        subjectToDelete.id = subjectId;
        subjectDao.delete(subjectToDelete);

        assertNull(homeworkDao.getByIdSync(noDeadlineId));
        assertNull(homeworkDao.getByIdSync(overdueId));
    }

    @Test
    public void homeworkQueriesReturnEmptyLists_whenNoRowsMatch() {
        long now = 1_700_000_000_000L;

        assertTrue(homeworkDao.getOverdueSync(now).isEmpty());
        assertTrue(homeworkDao.getByDueDateRangeSync(now, now + 1_000).isEmpty());
        assertTrue(homeworkDao.getNextUpcomingSync(now, 5).isEmpty());
    }
}
