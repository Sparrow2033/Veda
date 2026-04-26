package com.veda.app.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.veda.app.R;
import com.veda.app.data.db.AppDatabase;
import com.veda.app.data.entity.NoteEntity;
import com.veda.app.data.entity.NoteListItem;
import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.ui.edit.EditNoteActivity;
import com.veda.app.ui.home.HomeActivity;
import com.veda.app.ui.homework.HomeworkEditActivity;
import com.veda.app.ui.homework.HomeworkListActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SmokeInstrumentationTest {

    @Test
    public void navigation_homeToHomework_startsHomeworkScreen() {
        Intents.init();
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(HomeActivity.class)) {
            onView(withText("ДЗ")).perform(click());
            intended(hasComponent(HomeworkListActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }

    @Test
    public void noteCreateAndEdit_flowLoadsSavedNote() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppDatabase.get(context).subjectDao().insert(new SubjectEntity("Smoke Subject", 0xFF448AFF, 999));

        try (ActivityScenario<EditNoteActivity> createScenario = ActivityScenario.launch(EditNoteActivity.class)) {
            onView(withId(R.id.titleInput)).perform(replaceText("Smoke note"), closeSoftKeyboard());
            onView(withId(R.id.contentInput)).perform(replaceText("Body"), closeSoftKeyboard());
            onView(withId(R.id.action_save)).perform(click());
        }

        long createdId = waitForNoteIdByTitle("Smoke note");
        assertTrue(createdId > 0L);

        Intent editIntent = new Intent(context, EditNoteActivity.class)
                .putExtra(EditNoteActivity.EXTRA_NOTE_ID, createdId);
        try (ActivityScenario<EditNoteActivity> editScenario = ActivityScenario.launch(editIntent)) {
            onView(withId(R.id.titleInput)).check(matches(withText("Smoke note")));
            onView(withId(R.id.contentInput)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void homeworkCreate_flowSavesAndClosesScreen() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppDatabase.get(context).subjectDao().insert(new SubjectEntity("Smoke HW Subject", 0xFF00A8C8, 1000));

        try (ActivityScenario<HomeworkEditActivity> scenario = ActivityScenario.launch(HomeworkEditActivity.class)) {
            onView(withId(R.id.inputTitle)).perform(replaceText("Smoke homework"), closeSoftKeyboard());
            onView(withText("Сохранить")).perform(click());

            long timeoutAt = System.currentTimeMillis() + 5_000L;
            while (scenario.getState() != Lifecycle.State.DESTROYED && System.currentTimeMillis() < timeoutAt) {
                Thread.sleep(100L);
            }
            assertTrue(scenario.getState() == Lifecycle.State.DESTROYED);
        }
    }

    private long waitForNoteIdByTitle(String title) throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        long timeoutAt = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < timeoutAt) {
            List<NoteListItem> notes = AppDatabase.get(context).noteDao().getPickListSync(-1L);
            for (NoteListItem note : notes) {
                if (title.equals(note.title)) {
                    return note.id;
                }
            }
            Thread.sleep(100L);
        }
        return -1L;
    }
}
