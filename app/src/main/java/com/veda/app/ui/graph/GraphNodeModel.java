package com.veda.app.ui.graph;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GraphNodeModel {
    public long id;

    @NonNull
    public String label;

    @NonNull
    public String fullLabel;

    @Nullable
    public Long subjectId;

    @NonNull
    public String subjectName;

    @NonNull
    public String color;

    @NonNull
    public String fontColor;

    @NonNull
    public List<String> tags = new ArrayList<>();

    public int degree;
    public int value;
    public boolean orphan;

    public long createdAt;
    public long updatedAt;

    public GraphNodeModel(long id,
                          @NonNull String label,
                          @NonNull String fullLabel,
                          @Nullable Long subjectId,
                          @NonNull String subjectName,
                          @NonNull String color,
                          @NonNull String fontColor,
                          int degree,
                          int value,
                          boolean orphan,
                          long createdAt,
                          long updatedAt) {
        this.id = id;
        this.label = label;
        this.fullLabel = fullLabel;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.color = color;
        this.fontColor = fontColor;
        this.degree = degree;
        this.value = value;
        this.orphan = orphan;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
