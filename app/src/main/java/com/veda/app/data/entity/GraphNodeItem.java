package com.veda.app.data.entity;

public class GraphNodeItem {
    public long id;
    public String title;

    public Long subjectId;        // nullable
    public String subjectName;    // nullable
    public Integer subjectColor;  // nullable ARGB

    public String tags;           // raw comma-separated tags from note
    public long createdAt;
    public long updatedAt;
}
