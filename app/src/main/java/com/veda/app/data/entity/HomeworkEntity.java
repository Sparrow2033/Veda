package com.veda.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "homework",
        foreignKeys = {
                @ForeignKey(
                        entity = SubjectEntity.class,
                        parentColumns = "id",
                        childColumns = "subjectId",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.NO_ACTION
                )
        },
        indices = {
                @Index("subjectId"),
                @Index("dueDate"),
                @Index("status")
        }
)
public class HomeworkEntity {

    public static final int STATUS_TODO = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_DONE = 2;
    public static final int STATUS_CANCELED = 3;

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "subjectId")
    public long subjectId;

    @NonNull
    @ColumnInfo(name = "title")
    public String title;

    @NonNull
    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "dueDate")
    public long dueDate;

    @ColumnInfo(name = "status")
    public int status;

    @ColumnInfo(name = "priority")
    public int priority;

    @ColumnInfo(name = "createdAt")
    public long createdAt;

    @ColumnInfo(name = "updatedAt")
    public long updatedAt;

    public HomeworkEntity(long id,
                          long subjectId,
                          @NonNull String title,
                          @NonNull String description,
                          long dueDate,
                          int status,
                          int priority,
                          long createdAt,
                          long updatedAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public static HomeworkEntity newItem(long subjectId,
                                         @NonNull String title,
                                         @NonNull String description,
                                         long dueDate,
                                         int status,
                                         int priority,
                                         long now) {
        return new HomeworkEntity(
                0L,
                subjectId,
                title,
                description,
                dueDate,
                status,
                priority,
                now,
                now
        );
    }
}