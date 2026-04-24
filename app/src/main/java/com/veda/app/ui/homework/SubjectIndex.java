package com.veda.app.ui.homework;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veda.app.data.entity.SubjectEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SubjectIndex {

    private static final int DEFAULT_COLOR = 0xFF9E9E9E;

    private final Map<Long, SubjectEntity> byId = new HashMap<>();

    public void setSubjects(@Nullable List<SubjectEntity> subjects) {
        byId.clear();
        if (subjects == null) return;
        for (SubjectEntity s : subjects) {
            if (s == null) continue;
            byId.put(s.id, s);
        }
    }

    @NonNull
    public String name(long subjectId) {
        SubjectEntity s = byId.get(subjectId);
        if (s == null || s.name == null) return "";
        return s.name;
    }

    @ColorInt
    public int color(long subjectId) {
        SubjectEntity s = byId.get(subjectId);
        if (s == null) return DEFAULT_COLOR;
        return s.color;
    }
}