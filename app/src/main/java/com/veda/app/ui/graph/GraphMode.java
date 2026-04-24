package com.veda.app.ui.graph;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum GraphMode {
    GLOBAL,
    LOCAL;

    @NonNull
    public static GraphMode fromRaw(@Nullable String raw) {
        if (raw == null) return GLOBAL;
        try {
            return GraphMode.valueOf(raw);
        } catch (Exception ignored) {
            return GLOBAL;
        }
    }
}
