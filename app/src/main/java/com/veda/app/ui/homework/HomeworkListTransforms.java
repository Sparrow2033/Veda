package com.veda.app.ui.homework;

import androidx.annotation.NonNull;

import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.utils.AppPrefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

final class HomeworkListTransforms {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long WEEK_MS = 7L * DAY_MS;

    private HomeworkListTransforms() {
    }

    static void applySort(@NonNull List<HomeworkEntity> list, @NonNull AppPrefs.HomeworkSort sort) {
        if (sort == AppPrefs.HomeworkSort.PRIORITY) {
            list.sort((a, b) -> Integer.compare(b.priority, a.priority));
            return;
        }
        if (sort == AppPrefs.HomeworkSort.STATUS) {
            list.sort((a, b) -> Integer.compare(a.status, b.status));
            return;
        }

        list.sort((a, b) -> {
            long da = a.dueDate <= 0 ? Long.MAX_VALUE : a.dueDate;
            long db = b.dueDate <= 0 ? Long.MAX_VALUE : b.dueDate;
            return Long.compare(da, db);
        });
    }

    @NonNull
    static List<HomeworkEntity> applyFilter(List<HomeworkEntity> sourceList,
                                            @NonNull HomeworkListViewModel.Filter filter,
                                            long now) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

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
                    if (!isDone) out.add(item);
                    break;
                case TODAY:
                    if (!isDone && due >= todayStart && due < tomorrowStart) out.add(item);
                    break;
                case WEEK:
                    if (!isDone && due > 0L && due >= todayStart && due < weekEnd) out.add(item);
                    break;
                case OVERDUE:
                    if (!isDone && due > 0L && due < todayStart) out.add(item);
                    break;
                case UPCOMING:
                    if (!isDone && due >= tomorrowStart) out.add(item);
                    break;
                case DONE:
                    if (isDone && item.updatedAt >= doneCutoff) out.add(item);
                    break;
            }
        }

        return out;
    }

    static long startOfDay(long timeMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
