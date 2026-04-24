package com.veda.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class SchedulePrefs {
    private static final String PREFS = "veda_prefs";
    private static final String KEY_URL = "schedule_url";

    private SchedulePrefs() {}

    public static String getUrl(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_URL, null);
    }

    public static void setUrl(Context context, String url) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_URL, url).apply();
    }

    public static void clear(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_URL).apply();
    }
}