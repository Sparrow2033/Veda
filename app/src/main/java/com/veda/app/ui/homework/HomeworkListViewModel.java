package com.veda.app.ui.homework;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.repo.VedaRepository;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class HomeworkListViewModel extends AndroidViewModel {

    public enum Filter {
        ALL,
        TODAY,
        WEEK,
        OVERDUE,
        UPCOMING,
        DONE
    }

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long WEEK_MS = 7L * DAY_MS;

    private final MediatorLiveData<List<HomeworkEntity>> items = new MediatorLiveData<>();
    private final LiveData<List<HomeworkEntity>> source;

    private Filter currentFilter = Filter.ALL;

    public HomeworkListViewModel(@NonNull Application application) {
        super(application);

        VedaRepository repo = VedaRepository.get(application);
        source = resolveHomeworkListSource(repo, application);

        items.addSource(source, list -> items.setValue(applyFilter(list, currentFilter)));
    }

    @NonNull
    public LiveData<List<HomeworkEntity>> items() {
        return items;
    }

    public void setFilter(@NonNull Filter filter) {
        currentFilter = filter;
        items.setValue(applyFilter(source.getValue(), currentFilter));
    }

    @NonNull
    private List<HomeworkEntity> applyFilter(List<HomeworkEntity> sourceList, @NonNull Filter filter) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        long todayStart = startOfDay(now);
        long tomorrowStart = todayStart + DAY_MS;
        long weekEnd = todayStart + WEEK_MS;
        long doneCutoff = now - WEEK_MS;

        List<HomeworkEntity> out = new ArrayList<>();

        for (HomeworkEntity item : sourceList) {
            if (item == null) continue;

            boolean isDone = item.status == HomeworkEntity.STATUS_DONE;
            long due = item.dueDate;

            switch (filter) {
                case ALL:
                    if (!isDone) {
                        out.add(item);
                    }
                    break;

                case TODAY:
                    if (!isDone && due >= todayStart && due < tomorrowStart) {
                        out.add(item);
                    }
                    break;

                case WEEK:
                    if (!isDone && due > 0L && due >= todayStart && due < weekEnd) {
                        out.add(item);
                    }
                    break;

                case OVERDUE:
                    if (!isDone && due > 0L && due < todayStart) {
                        out.add(item);
                    }
                    break;

                case UPCOMING:
                    if (!isDone && due >= tomorrowStart) {
                        out.add(item);
                    }
                    break;

                case DONE:
                    if (isDone && item.updatedAt >= doneCutoff) {
                        out.add(item);
                    }
                    break;
            }
        }

        return out;
    }

    private static long startOfDay(long timeMillis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private static LiveData<List<HomeworkEntity>> resolveHomeworkListSource(@NonNull VedaRepository repo,
                                                                            @NonNull Application application) {
        Method exact = findHomeworkListMethod(repo.getClass());
        if (exact == null) {
            throw new IllegalStateException("Не найден метод репозитория, возвращающий LiveData<List<HomeworkEntity>>");
        }

        try {
            Object result = invokeRepoMethod(repo, exact, application);
            if (!(result instanceof LiveData)) {
                throw new IllegalStateException("Метод списка ДЗ не вернул LiveData");
            }
            return (LiveData<List<HomeworkEntity>>) result;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось подключить источник списка ДЗ: " + e.getMessage(), e);
        }
    }

    private static Method findHomeworkListMethod(@NonNull Class<?> repoClass) {
        Method fallback = null;

        for (Method method : repoClass.getMethods()) {
            if (!LiveData.class.isAssignableFrom(method.getReturnType())) continue;

            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("homework")) continue;

            String generic = genericName(method.getGenericReturnType());
            boolean isHomeworkList = generic.contains("java.util.list")
                    && generic.contains("homeworkentity");
            if (!isHomeworkList) continue;

            if (supportsInvocation(method)) {
                if (name.contains("observe") || name.contains("all") || name.contains("list")) {
                    return method;
                }
                if (fallback == null) {
                    fallback = method;
                }
            }
        }

        return fallback;
    }

    private static boolean supportsInvocation(@NonNull Method method) {
        Class<?>[] params = method.getParameterTypes();

        if (params.length == 0) return true;

        if (params.length == 1) {
            Class<?> p = params[0];
            return p == Application.class
                    || Context.class.isAssignableFrom(p);
        }

        return false;
    }

    private static Object invokeRepoMethod(@NonNull VedaRepository repo,
                                           @NonNull Method method,
                                           @NonNull Application application) throws Exception {
        Class<?>[] params = method.getParameterTypes();

        if (params.length == 0) {
            return method.invoke(repo);
        }

        if (params.length == 1) {
            Class<?> p = params[0];
            if (p == Application.class || Context.class.isAssignableFrom(p)) {
                return method.invoke(repo, application);
            }
        }

        throw new IllegalStateException("Неподдерживаемая сигнатура метода: " + method.getName());
    }

    @NonNull
    private static String genericName(Type type) {
        return type == null ? "" : String.valueOf(type).toLowerCase(Locale.ROOT);
    }
}