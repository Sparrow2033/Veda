package com.veda.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.veda.app.data.entity.GraphNodeItem;
import com.veda.app.data.entity.NoteDetailItem;
import com.veda.app.data.entity.NoteEntity;
import com.veda.app.data.entity.NoteListItem;

import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    long insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteById(long noteId);

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    LiveData<NoteEntity> observeById(long noteId);

    @Query(
            "SELECT " +
                    "n.id AS id, " +
                    "n.title AS title, " +
                    "n.updatedAt AS updatedAt, " +
                    "s.name AS subjectName, " +
                    "s.color AS subjectColor " +
                    "FROM notes n " +
                    "LEFT JOIN subjects s ON n.subjectId = s.id " +
                    "ORDER BY n.updatedAt DESC"
    )
    LiveData<List<NoteListItem>> observeList();

    @Query(
            "SELECT " +
                    "n.id AS id, " +
                    "n.subjectId AS subjectId, " +
                    "n.title AS title, " +
                    "n.contentHtml AS contentHtml, " +
                    "n.createdAt AS createdAt, " +
                    "n.updatedAt AS updatedAt, " +
                    "s.name AS subjectName, " +
                    "s.color AS subjectColor " +
                    "FROM notes n " +
                    "LEFT JOIN subjects s ON n.subjectId = s.id " +
                    "WHERE n.id = :noteId " +
                    "LIMIT 1"
    )
    LiveData<NoteDetailItem> observeDetail(long noteId);

    @Query(
            "SELECT " +
                    "n.id AS id, " +
                    "n.title AS title, " +
                    "n.updatedAt AS updatedAt, " +
                    "s.name AS subjectName, " +
                    "s.color AS subjectColor " +
                    "FROM notes n " +
                    "LEFT JOIN subjects s ON n.subjectId = s.id " +
                    "WHERE n.id != :excludeId " +
                    "ORDER BY n.updatedAt DESC"
    )
    List<NoteListItem> getPickListSync(long excludeId);

    // Данные для графа (узлы)
    @Query(
            "SELECT " +
                    "n.id AS id, " +
                    "n.title AS title, " +
                    "n.subjectId AS subjectId, " +
                    "s.name AS subjectName, " +
                    "s.color AS subjectColor, " +
                    "n.tags AS tags, " +
                    "n.createdAt AS createdAt, " +
                    "n.updatedAt AS updatedAt " +
                    "FROM notes n " +
                    "LEFT JOIN subjects s ON n.subjectId = s.id " +
                    "ORDER BY n.updatedAt DESC"
    )
    List<GraphNodeItem> getGraphNodesSync();
}
