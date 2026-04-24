package com.veda.app.demo;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.ui.edit.EditNoteViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class DemoLibraryImporter {

    private static final long AWAIT_TIMEOUT_SECONDS = 15L;

    private final Context appContext;
    private final EditNoteViewModel notesVm;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DemoLibraryImporter(@NonNull Context context) {
        this.appContext = context.getApplicationContext();

        if (!(this.appContext instanceof Application)) {
            throw new IllegalStateException("Application context is required for DemoLibraryImporter");
        }

        this.notesVm = new EditNoteViewModel((Application) this.appContext);
    }

    @NonNull
    public DemoImportResult importLibrary() throws Exception {
        DemoLibrarySeed seed = DemoLibrarySource.load(appContext);
        return importLibrary(seed);
    }

    @NonNull
    public DemoImportResult importLibrary(@NonNull DemoLibrarySeed seed) throws Exception {
        Map<String, DemoSubjectSeed> subjectBySlug = indexSubjects(seed.subjects);
        Map<String, DemoNoteSeed> noteBySlug = indexNotes(seed.notes);

        int subjectsCreated = ensureSubjects(seed.subjects);

        List<SubjectEntity> currentSubjects = awaitValue(notesVm.subjects());
        Map<String, Long> subjectIdsBySlug = resolveSubjectIds(subjectBySlug, currentSubjects);

        Map<String, Long> noteIdsBySlug = new LinkedHashMap<>();
        Map<String, Long> createdAtBySlug = new HashMap<>();

        int notesInserted = 0;
        for (DemoNoteSeed note : seed.notes) {
            Long subjectId = subjectIdsBySlug.get(note.subjectSlug);
            if (subjectId == null || subjectId <= 0L) {
                throw new IllegalStateException("Не удалось определить subjectId для " + note.slug);
            }

            String initialHtml = DemoLibraryHtml.buildInitialContentHtml(note);
            String tags = joinTags(note.tags);

            long createdAt = System.currentTimeMillis();
            long noteId = insertNote(subjectId, note.title, initialHtml, tags, new ArrayList<>());

            noteIdsBySlug.put(note.slug, noteId);
            createdAtBySlug.put(note.slug, createdAt);
            notesInserted++;
        }

        int notesUpdated = 0;
        int linksResolved = 0;

        for (DemoNoteSeed note : seed.notes) {
            Long noteId = noteIdsBySlug.get(note.slug);
            Long subjectId = subjectIdsBySlug.get(note.subjectSlug);
            Long createdAt = createdAtBySlug.get(note.slug);

            if (noteId == null || subjectId == null || createdAt == null) {
                throw new IllegalStateException("Не удалось подготовить update для " + note.slug);
            }

            List<Long> resolvedLinks = resolveLinkIds(note.linkSlugs, noteIdsBySlug);
            String finalHtml = DemoLibraryHtml.buildFinalContentHtml(note, noteBySlug, noteIdsBySlug);

            notesVm.saveUpdateWithLinks(
                    noteId,
                    subjectId,
                    note.title,
                    finalHtml,
                    joinTags(note.tags),
                    createdAt,
                    resolvedLinks
            );

            notesUpdated++;
            linksResolved += resolvedLinks.size();
        }

        return new DemoImportResult(
                subjectsCreated,
                notesInserted,
                notesUpdated,
                linksResolved
        );
    }

    private int ensureSubjects(@NonNull List<DemoSubjectSeed> subjects) throws Exception {
        List<SubjectEntity> existing = awaitValue(notesVm.subjects());
        Map<String, SubjectEntity> byName = new HashMap<>();

        if (existing != null) {
            for (SubjectEntity subject : existing) {
                if (subject == null) continue;
                byName.put(normalize(subject.name), subject);
            }
        }

        int created = 0;
        int sortOrder = existing == null ? 1 : existing.size() + 1;

        for (DemoSubjectSeed seed : subjects) {
            if (byName.containsKey(normalize(seed.name))) continue;

            long newId = insertSubject(seed.name, parseColor(seed.colorHex), sortOrder++);
            if (newId > 0L) {
                created++;
                byName.put(normalize(seed.name), null);
            }
        }

        return created;
    }

    @NonNull
    private Map<String, Long> resolveSubjectIds(@NonNull Map<String, DemoSubjectSeed> subjectBySlug,
                                                List<SubjectEntity> currentSubjects) {
        Map<String, Long> result = new HashMap<>();
        Map<String, SubjectEntity> byName = new HashMap<>();

        if (currentSubjects != null) {
            for (SubjectEntity subject : currentSubjects) {
                if (subject == null) continue;
                byName.put(normalize(subject.name), subject);
            }
        }

        for (Map.Entry<String, DemoSubjectSeed> entry : subjectBySlug.entrySet()) {
            SubjectEntity subject = byName.get(normalize(entry.getValue().name));
            if (subject == null) {
                throw new IllegalStateException("Subject не найден после ensureSubjects: " + entry.getValue().name);
            }
            result.put(entry.getKey(), subject.id);
        }

        return result;
    }

    @NonNull
    private List<Long> resolveLinkIds(@NonNull List<String> linkSlugs,
                                      @NonNull Map<String, Long> noteIdsBySlug) {
        List<Long> ids = new ArrayList<>();

        for (String slug : linkSlugs) {
            Long id = noteIdsBySlug.get(slug);
            if (id != null && id > 0L) {
                ids.add(id);
            }
        }

        return ids;
    }

    private long insertSubject(@NonNull String name, int color, int sortOrder) throws Exception {
        long[] outId = {0L};
        CountDownLatch latch = new CountDownLatch(1);

        notesVm.insertSubject(name, color, sortOrder, id -> {
            outId[0] = id;
            latch.countDown();
        });

        awaitLatch(latch, "insertSubject");
        return outId[0];
    }

    private long insertNote(long subjectId,
                            @NonNull String title,
                            @NonNull String contentHtml,
                            @NonNull String tags,
                            @NonNull List<Long> linkedIds) throws Exception {
        long[] outId = {0L};
        CountDownLatch latch = new CountDownLatch(1);

        notesVm.saveNewWithLinks(subjectId, title, contentHtml, tags, linkedIds, id -> {
            outId[0] = id;
            latch.countDown();
        });

        awaitLatch(latch, "saveNewWithLinks");
        return outId[0];
    }

    @NonNull
    private static Map<String, DemoSubjectSeed> indexSubjects(@NonNull List<DemoSubjectSeed> subjects) {
        Map<String, DemoSubjectSeed> out = new LinkedHashMap<>();
        for (DemoSubjectSeed item : subjects) {
            out.put(item.slug, item);
        }
        return out;
    }

    @NonNull
    private static Map<String, DemoNoteSeed> indexNotes(@NonNull List<DemoNoteSeed> notes) {
        Map<String, DemoNoteSeed> out = new LinkedHashMap<>();
        for (DemoNoteSeed item : notes) {
            out.put(item.slug, item);
        }
        return out;
    }

    @NonNull
    private static String joinTags(@NonNull List<String> tags) {
        if (tags.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(tags.get(i));
        }

        return sb.toString();
    }

    private static int parseColor(@NonNull String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.parseColor("#7A8694");
        }
    }

    @NonNull
    private static String normalize(@NonNull String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private static void awaitLatch(@NonNull CountDownLatch latch, @NonNull String operation) throws Exception {
        boolean ok = latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!ok) {
            throw new IllegalStateException("Таймаут ожидания callback для " + operation);
        }
    }

    @NonNull
    private <T> T awaitValue(@NonNull LiveData<T> liveData) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Object[] box = new Object[1];
        Observer<T>[] holder = new Observer[1];

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T value) {
                box[0] = value;
                Observer<T> current = holder[0];
                if (current != null) {
                    mainHandler.post(() -> liveData.removeObserver(current));
                }
                latch.countDown();
            }
        };
        holder[0] = observer;

        CountDownLatch observeStarted = new CountDownLatch(1);
        mainHandler.post(() -> {
            liveData.observeForever(observer);
            observeStarted.countDown();
        });

        boolean observeOk = observeStarted.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!observeOk) {
            throw new IllegalStateException("Не удалось начать наблюдение LiveData");
        }

        boolean ok = latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!ok) {
            mainHandler.post(() -> liveData.removeObserver(observer));
            throw new IllegalStateException("Таймаут ожидания LiveData");
        }

        @SuppressWarnings("unchecked")
        T value = (T) box[0];
        return value;
    }
}