package com.veda.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UtilsTransformTest {

    @Test
    public void extractLinkedNoteIds_parsesDeduplicatesAndSkipsInvalid() {
        String html = "<a href='veda://note/10'>A</a>"
                + "<a href='veda://note/10'>dup</a>"
                + "<a href='veda://note/0'>bad</a>"
                + "<a href='veda://note/42'>B</a>";

        assertEquals(List.of(10L, 42L), NoteHtml.extractLinkedNoteIds(html));
        assertTrue(NoteHtml.extractLinkedNoteIds("").isEmpty());
    }

    @Test
    public void appPrefs_readsAndWritesSortAndFallbackSafely() {
        Context context = ApplicationProvider.getApplicationContext();
        AppPrefs.setHomeworkSort(context, AppPrefs.HomeworkSort.STATUS);
        assertEquals(AppPrefs.HomeworkSort.STATUS, AppPrefs.getHomeworkSort(context));

        context.getSharedPreferences("veda_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("homework_sort", "NOT_EXISTING")
                .apply();

        assertEquals(AppPrefs.HomeworkSort.DUE_DATE, AppPrefs.getHomeworkSort(context));
    }
}
