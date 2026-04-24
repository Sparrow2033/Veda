package com.veda.app.utils;

public final class SubjectPalette {

    private SubjectPalette() {}

    public static final String[] COLOR_NAMES = {
            "Синий", "Фиолетовый", "Бирюзовый", "Зелёный",
            "Оранжевый", "Розовый", "Голубой", "Графит"
    };

    // ARGB
    public static final int[] COLORS = {
            0xFF2166F3,
            0xFF7149F7,
            0xFF00A8C8,
            0xFF4CC73D,
            0xFFCF5B1F,
            0xFFCD46B3,
            0xFF448AFF,
            0xFF263238
    };

    public static int clampColorByIndex(int index) {
        int i = Math.max(0, Math.min(index, COLORS.length - 1));
        return COLORS[i];
    }
}