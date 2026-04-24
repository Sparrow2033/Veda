package com.veda.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.veda.app.data.entity.SubjectEntity;

import java.util.List;

@Dao
public interface SubjectDao {

    @Query("SELECT COUNT(*) FROM subjects")
    int countSync();

    @Query("SELECT * FROM subjects ORDER BY sortOrder ASC, name ASC")
    LiveData<List<SubjectEntity>> observeAll();

    @Query("SELECT * FROM subjects ORDER BY sortOrder ASC, name ASC")
    List<SubjectEntity> getAllSync();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(SubjectEntity subject);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<SubjectEntity> subjects);

    @Update
    void update(SubjectEntity subject);

    @Delete
    void delete(SubjectEntity subject);
}