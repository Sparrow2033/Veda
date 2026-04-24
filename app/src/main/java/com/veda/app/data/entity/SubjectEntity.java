package com.veda.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "subjects",
        indices = {@Index(value = {"name"}, unique = true)}
)
public class SubjectEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    public int color;     // ARGB
    public int sortOrder;

    public SubjectEntity(@NonNull String name, int color, int sortOrder) {
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
    }
}