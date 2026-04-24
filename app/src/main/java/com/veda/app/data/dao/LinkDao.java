package com.veda.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veda.app.data.entity.LinkEntity;

import java.util.List;

@Dao
public interface LinkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<LinkEntity> links);

    @Query("DELETE FROM links WHERE fromNoteId = :fromNoteId")
    void deleteByFromNoteId(long fromNoteId);

    @Query("SELECT * FROM links")
    LiveData<List<LinkEntity>> observeAll();

    @Query("SELECT * FROM links")
    List<LinkEntity> getAllSync();
}