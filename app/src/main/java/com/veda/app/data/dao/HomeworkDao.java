package com.veda.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.veda.app.data.entity.HomeworkEntity;

import java.util.List;

@Dao
public interface HomeworkDao {

    @Insert
    long insert(HomeworkEntity homework);

    @Update
    void update(HomeworkEntity homework);

    @Delete
    void delete(HomeworkEntity homework);

    @Query("DELETE FROM homework WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM homework WHERE id = :id LIMIT 1")
    LiveData<HomeworkEntity> observeById(long id);

    @Query("SELECT * FROM homework WHERE id = :id LIMIT 1")
    HomeworkEntity getByIdSync(long id);

    @Query("SELECT * FROM homework ORDER BY dueDate ASC, updatedAt DESC")
    LiveData<List<HomeworkEntity>> observeAll();

    @Query(
            "SELECT * FROM homework " +
                    "WHERE dueDate > 0 AND dueDate >= :startInclusive AND dueDate < :endExclusive " +
                    "ORDER BY dueDate ASC, updatedAt DESC"
    )
    LiveData<List<HomeworkEntity>> observeByDueDateRange(long startInclusive, long endExclusive);

    @Query(
            "SELECT * FROM homework " +
                    "WHERE dueDate > 0 AND dueDate < :now AND status IN (0, 1) " +
                    "ORDER BY dueDate ASC, updatedAt DESC"
    )
    LiveData<List<HomeworkEntity>> observeOverdue(long now);

    @Query(
            "SELECT * FROM homework " +
                    "WHERE dueDate > 0 AND dueDate >= :now AND status IN (0, 1) " +
                    "ORDER BY dueDate ASC, updatedAt DESC " +
                    "LIMIT :limit"
    )
    LiveData<List<HomeworkEntity>> observeNextUpcoming(long now, int limit);
}