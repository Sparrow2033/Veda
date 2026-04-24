package com.veda.app.ui.homework;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veda.app.data.entity.HomeworkEntity;

public final class HomeworkRowItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TASK = 1;

    public final int type;
    public final long stableId;

    @Nullable
    public final String headerText;

    @Nullable
    public final HomeworkEntity task;

    private HomeworkRowItem(int type, long stableId, @Nullable String headerText, @Nullable HomeworkEntity task) {
        this.type = type;
        this.stableId = stableId;
        this.headerText = headerText;
        this.task = task;
    }

    @NonNull
    public static HomeworkRowItem header(@NonNull String text, long stableId) {
        return new HomeworkRowItem(TYPE_HEADER, stableId, text, null);
    }

    @NonNull
    public static HomeworkRowItem task(@NonNull HomeworkEntity entity) {
        return new HomeworkRowItem(TYPE_TASK, entity.id, null, entity);
    }
}
