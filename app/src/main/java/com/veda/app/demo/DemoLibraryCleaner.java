package com.veda.app.demo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.veda.app.data.repo.VedaRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class DemoLibraryCleaner {

    private static final long AWAIT_TIMEOUT_SECONDS = 15L;

    private final Context appContext;
    private final VedaRepository repo;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DemoLibraryCleaner(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.repo = VedaRepository.get(appContext);
    }

    @NonNull
    public DemoDeleteResult deleteDemoLibrary() throws Exception {
        List<Long> demoIds = findDemoNoteIdsByServiceTag();
        int found = demoIds.size();

        int deleted = 0;
        for (Long id : demoIds) {
            if (id == null || id <= 0L) continue;
            deleteNoteById(id);
            deleted++;
        }

        return new DemoDeleteResult(found, deleted);
    }

    public boolean hasDemoLibrary() throws Exception {
        return !findDemoNoteIdsByServiceTag().isEmpty();
    }

    @NonNull
    private List<Long> findDemoNoteIdsByServiceTag() throws Exception {
        Method notesMethod = findNotesLiveDataMethod();
        if (notesMethod == null) {
            throw new IllegalStateException("Не найден метод репозитория для чтения списка заметок");
        }

        @SuppressWarnings("unchecked")
        LiveData<?> liveData = (LiveData<?>) notesMethod.invoke(repo);

        Object rawValue = awaitValue(liveData);
        if (!(rawValue instanceof List)) {
            throw new IllegalStateException("Репозиторий вернул не список заметок");
        }

        List<?> list = (List<?>) rawValue;
        List<Long> out = new ArrayList<>();

        for (Object item : list) {
            if (item == null) continue;

            String tags = readStringField(item, "tags");
            Long id = readLongField(item, "id");

            if (id == null || id <= 0L) continue;
            if (containsDemoTag(tags)) {
                out.add(id);
            }
        }

        return out;
    }

    private void deleteNoteById(long id) throws Exception {
        Method method = findMethod(repo.getClass(), "deleteNoteById", 1);
        if (method == null) {
            throw new IllegalStateException("Не найден метод deleteNoteById(...) в VedaRepository");
        }

        method.invoke(repo, id);
    }

    private static boolean containsDemoTag(String tags) {
        if (tags == null || tags.trim().isEmpty()) return false;
        return tags.contains(DemoLibrarySource.DEMO_SERVICE_TAG);
    }

    private Method findNotesLiveDataMethod() {
        List<Method> candidates = new ArrayList<>();

        for (Method method : repo.getClass().getMethods()) {
            if (!LiveData.class.isAssignableFrom(method.getReturnType())) continue;
            if (method.getParameterTypes().length != 0) continue;

            String name = method.getName();
            if (name.startsWith("observe") || name.startsWith("notes") || name.startsWith("allNotes")) {
                candidates.add(method);
            }
        }

        for (Method method : candidates) {
            try {
                @SuppressWarnings("unchecked")
                LiveData<?> liveData = (LiveData<?>) method.invoke(repo);
                Object value = awaitValue(liveData);

                if (!(value instanceof List)) continue;
                List<?> list = (List<?>) value;
                if (list.isEmpty()) {
                    continue;
                }

                Object first = list.get(0);
                if (first == null) continue;

                boolean hasId = hasField(first.getClass(), "id");
                boolean hasTags = hasField(first.getClass(), "tags");

                if (hasId && hasTags) {
                    return method;
                }
            } catch (Exception ignored) {
            }
        }

        for (Method method : candidates) {
            return method;
        }

        return null;
    }

    private static boolean hasField(@NonNull Class<?> cls, @NonNull String fieldName) {
        return findField(cls, fieldName) != null;
    }

    private static String readStringField(@NonNull Object target, @NonNull String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) return null;
            Object value = field.get(target);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long readLongField(@NonNull Object target, @NonNull String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) return null;

            Object value = field.get(target);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Field findField(@NonNull Class<?> cls, @NonNull String fieldName) {
        Class<?> current = cls;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(@NonNull Class<?> cls,
                                     @NonNull String name,
                                     int paramCount) {
        for (Method method : cls.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == paramCount) {
                return method;
            }
        }

        for (Method method : cls.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == paramCount) {
                method.setAccessible(true);
                return method;
            }
        }

        return null;
    }

    @NonNull
    private Object awaitValue(@NonNull LiveData<?> liveData) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Object[] box = new Object[1];
        Observer<Object>[] holder = new Observer[1];

        Observer<Object> observer = new Observer<Object>() {
            @Override
            public void onChanged(Object value) {
                box[0] = value;
                Observer<Object> current = holder[0];
                if (current != null) {
                    mainHandler.post(() -> {
                        @SuppressWarnings("unchecked")
                        LiveData<Object> typed = (LiveData<Object>) liveData;
                        typed.removeObserver(current);
                    });
                }
                latch.countDown();
            }
        };
        holder[0] = observer;

        CountDownLatch observeStarted = new CountDownLatch(1);
        mainHandler.post(() -> {
            @SuppressWarnings("unchecked")
            LiveData<Object> typed = (LiveData<Object>) liveData;
            typed.observeForever(observer);
            observeStarted.countDown();
        });

        boolean observeOk = observeStarted.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!observeOk) {
            throw new IllegalStateException("Не удалось начать наблюдение списка заметок");
        }

        boolean ok = latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!ok) {
            mainHandler.post(() -> {
                @SuppressWarnings("unchecked")
                LiveData<Object> typed = (LiveData<Object>) liveData;
                typed.removeObserver(observer);
            });
            throw new IllegalStateException("Таймаут ожидания списка заметок");
        }

        return box[0];
    }
}