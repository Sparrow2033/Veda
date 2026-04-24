package com.veda.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notes",
        indices = {@Index("subjectId"), @Index("updatedAt")},
        foreignKeys = {
                @ForeignKey(
                        entity = SubjectEntity.class,
                        parentColumns = "id",
                        childColumns = "subjectId",
                        onDelete = ForeignKey.SET_NULL
                )
        }
)
public class NoteEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public Long subjectId;

    @NonNull
    public String title;

    @NonNull
    public String contentHtml;

    @NonNull
    public String tags;

    public long createdAt;
    public long updatedAt;

    public NoteEntity(Long subjectId,
                      @NonNull String title,
                      @NonNull String contentHtml,
                      @NonNull String tags,
                      long createdAt,
                      long updatedAt) {
        this.subjectId = subjectId;
        this.title = title;
        this.contentHtml = contentHtml;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}