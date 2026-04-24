package com.veda.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.veda.app.utils.AppPrefs;

public final class App extends Application {

    @Override
    public void onCreate() {
        applySavedTheme();
        super.onCreate();
    }

    private void applySavedTheme() {
        AppCompatDelegate.setDefaultNightMode(
                toNightMode(AppPrefs.getThemeMode(this))
        );
    }

    private static int toNightMode(AppPrefs.ThemeMode mode) {
        switch (mode) {
            case LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}