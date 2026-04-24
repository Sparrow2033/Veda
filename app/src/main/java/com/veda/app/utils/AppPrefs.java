package com.veda.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AppPrefs {

    private static final String PREFS = "veda_prefs";

    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_CONFIRM_DELETE = "confirm_delete";

    private static final String KEY_NOTES_SORT = "notes_sort";
    private static final String KEY_HOMEWORK_SORT = "homework_sort";
    private static final String KEY_HOMEWORK_SAVE_FILTERS = "homework_save_filters";
    private static final String KEY_HOMEWORK_LAST_FILTER = "homework_last_filter";

    private static final String KEY_OPEN_LAST_SECTION = "open_last_section";
    private static final String KEY_LAST_SECTION_ID = "last_section_id";

    public enum ThemeMode { SYSTEM, LIGHT, DARK }
    public enum TextSize { NORMAL, LARGE }
    public enum NotesSort { NEW_FIRST, OLD_FIRST, ALPHABETICAL }
    public enum HomeworkSort { DUE_DATE, PRIORITY, STATUS }

    private AppPrefs() {
    }

    private static SharedPreferences sp(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public static ThemeMode getThemeMode(@NonNull Context context) {
        String v = sp(context).getString(KEY_THEME_MODE, ThemeMode.DARK.name());
        return safeEnum(ThemeMode.class, v, ThemeMode.DARK);
    }

    public static void setThemeMode(@NonNull Context context, @NonNull ThemeMode value) {
        sp(context).edit().putString(KEY_THEME_MODE, value.name()).apply();
    }

    @NonNull
    public static TextSize getTextSize(@NonNull Context context) {
        String v = sp(context).getString(KEY_TEXT_SIZE, TextSize.NORMAL.name());
        return safeEnum(TextSize.class, v, TextSize.NORMAL);
    }

    public static void setTextSize(@NonNull Context context, @NonNull TextSize value) {
        sp(context).edit().putString(KEY_TEXT_SIZE, value.name()).apply();
    }

    public static boolean isConfirmDeleteEnabled(@NonNull Context context) {
        return sp(context).getBoolean(KEY_CONFIRM_DELETE, true);
    }

    public static void setConfirmDeleteEnabled(@NonNull Context context, boolean enabled) {
        sp(context).edit().putBoolean(KEY_CONFIRM_DELETE, enabled).apply();
    }

    @NonNull
    public static NotesSort getNotesSort(@NonNull Context context) {
        String v = sp(context).getString(KEY_NOTES_SORT, NotesSort.NEW_FIRST.name());
        return safeEnum(NotesSort.class, v, NotesSort.NEW_FIRST);
    }

    public static void setNotesSort(@NonNull Context context, @NonNull NotesSort sort) {
        sp(context).edit().putString(KEY_NOTES_SORT, sort.name()).apply();
    }

    @NonNull
    public static HomeworkSort getHomeworkSort(@NonNull Context context) {
        String v = sp(context).getString(KEY_HOMEWORK_SORT, HomeworkSort.DUE_DATE.name());
        return safeEnum(HomeworkSort.class, v, HomeworkSort.DUE_DATE);
    }

    public static void setHomeworkSort(@NonNull Context context, @NonNull HomeworkSort sort) {
        sp(context).edit().putString(KEY_HOMEWORK_SORT, sort.name()).apply();
    }

    public static boolean isHomeworkSaveFiltersEnabled(@NonNull Context context) {
        return sp(context).getBoolean(KEY_HOMEWORK_SAVE_FILTERS, true);
    }

    public static void setHomeworkSaveFiltersEnabled(@NonNull Context context, boolean enabled) {
        sp(context).edit().putBoolean(KEY_HOMEWORK_SAVE_FILTERS, enabled).apply();
    }

    @NonNull
    public static String getHomeworkLastFilter(@NonNull Context context, @NonNull String defaultValue) {
        String v = sp(context).getString(KEY_HOMEWORK_LAST_FILTER, defaultValue);
        return v == null ? defaultValue : v;
    }

    public static void setHomeworkLastFilter(@NonNull Context context, @NonNull String value) {
        sp(context).edit().putString(KEY_HOMEWORK_LAST_FILTER, value).apply();
    }

    public static boolean isOpenLastSectionEnabled(@NonNull Context context) {
        return sp(context).getBoolean(KEY_OPEN_LAST_SECTION, false);
    }

    public static void setOpenLastSectionEnabled(@NonNull Context context, boolean enabled) {
        sp(context).edit().putBoolean(KEY_OPEN_LAST_SECTION, enabled).apply();
    }

    public static int getLastSectionId(@NonNull Context context, int defaultId) {
        return sp(context).getInt(KEY_LAST_SECTION_ID, defaultId);
    }

    public static void setLastSectionId(@NonNull Context context, int sectionId) {
        sp(context).edit().putInt(KEY_LAST_SECTION_ID, sectionId).apply();
    }

    @NonNull
    private static <T extends Enum<T>> T safeEnum(@NonNull Class<T> cls, String raw, @NonNull T fallback) {
        if (raw == null) return fallback;
        try {
            return Enum.valueOf(cls, raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}