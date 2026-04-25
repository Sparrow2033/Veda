package com.veda.app.ui.homework;

import static org.junit.Assert.assertEquals;

import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.utils.AppPrefs;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HomeworkListTransformsTest {

    @Test
    public void applyFilter_coversCriticalFiltersAndBoundaries() {
        long now = 1_800_000_000_000L;
        long todayStart = HomeworkListTransforms.startOfDay(now);
        long tomorrowStart = todayStart + 24L * 60L * 60L * 1000L;
        long weekEnd = todayStart + 7L * 24L * 60L * 60L * 1000L;

        List<HomeworkEntity> source = List.of(
                hw(1, todayStart + 1_000L, HomeworkEntity.STATUS_TODO, HomeworkEntity.PRIORITY_LOW, now),
                hw(2, todayStart - 1_000L, HomeworkEntity.STATUS_TODO, HomeworkEntity.PRIORITY_LOW, now),
                hw(3, tomorrowStart + 1_000L, HomeworkEntity.STATUS_TODO, HomeworkEntity.PRIORITY_LOW, now),
                hw(4, weekEnd + 1_000L, HomeworkEntity.STATUS_TODO, HomeworkEntity.PRIORITY_LOW, now),
                hw(5, 0L, HomeworkEntity.STATUS_TODO, HomeworkEntity.PRIORITY_LOW, now),
                hw(6, todayStart + 2_000L, HomeworkEntity.STATUS_DONE, HomeworkEntity.PRIORITY_LOW, now),
                hw(7, todayStart + 2_000L, HomeworkEntity.STATUS_DONE, HomeworkEntity.PRIORITY_LOW, now - 8L * 24L * 60L * 60L * 1000L)
        );

        assertIds(
                HomeworkListTransforms.applyFilter(source, HomeworkListViewModel.Filter.ALL, now),
                List.of(1L, 2L, 3L, 4L, 5L)
        );
        assertIds(
                HomeworkListTransforms.applyFilter(source, HomeworkListViewModel.Filter.TODAY, now),
                List.of(1L)
        );
        assertIds(
                HomeworkListTransforms.applyFilter(source, HomeworkListViewModel.Filter.WEEK, now),
                List.of(1L, 3L)
        );
        assertIds(
                HomeworkListTransforms.applyFilter(source, HomeworkListViewModel.Filter.OVERDUE, now),
                List.of(2L)
        );
        assertIds(
                HomeworkListTransforms.applyFilter(source, HomeworkListViewModel.Filter.UPCOMING, now),
                List.of(3L, 4L)
        );
        assertIds(
                HomeworkListTransforms.applyFilter(source, HomeworkListViewModel.Filter.DONE, now),
                List.of(6L)
        );
    }

    @Test
    public void applySort_sortsByDueDatePriorityAndStatus() {
        long now = 1_800_000_000_000L;
        HomeworkEntity low = hw(1, now + 5_000, HomeworkEntity.STATUS_IN_PROGRESS, HomeworkEntity.PRIORITY_LOW, now);
        HomeworkEntity high = hw(2, now + 3_000, HomeworkEntity.STATUS_DONE, HomeworkEntity.PRIORITY_HIGH, now);
        HomeworkEntity noDue = hw(3, 0L, HomeworkEntity.STATUS_TODO, HomeworkEntity.PRIORITY_MEDIUM, now);

        List<HomeworkEntity> dueDate = new ArrayList<>(List.of(low, high, noDue));
        HomeworkListTransforms.applySort(dueDate, AppPrefs.HomeworkSort.DUE_DATE);
        assertIds(dueDate, List.of(2L, 1L, 3L));

        List<HomeworkEntity> priority = new ArrayList<>(List.of(low, high, noDue));
        HomeworkListTransforms.applySort(priority, AppPrefs.HomeworkSort.PRIORITY);
        assertIds(priority, List.of(2L, 3L, 1L));

        List<HomeworkEntity> status = new ArrayList<>(List.of(low, high, noDue));
        HomeworkListTransforms.applySort(status, AppPrefs.HomeworkSort.STATUS);
        assertIds(status, List.of(3L, 1L, 2L));
    }

    @Test
    public void startOfDay_returnsMidnightForGivenTimestamp() {
        long ts = 1_800_012_345_678L;
        long sod = HomeworkListTransforms.startOfDay(ts);
        assertEquals(0L, (sod / 1000L) % (24L * 60L * 60L));
        assertEquals(sod, HomeworkListTransforms.startOfDay(sod + 777L));
    }

    private static HomeworkEntity hw(long id, long dueDate, int status, int priority, long updatedAt) {
        return new HomeworkEntity(
                id,
                1L,
                "HW " + id,
                "",
                dueDate,
                status,
                priority,
                updatedAt - 100L,
                updatedAt
        );
    }

    private static void assertIds(List<HomeworkEntity> input, List<Long> expectedIds) {
        List<Long> actual = new ArrayList<>();
        for (HomeworkEntity entity : input) {
            actual.add(entity.id);
        }
        assertEquals(expectedIds, actual);
    }
}
