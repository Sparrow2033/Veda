package com.veda.app.ui.nav;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public final class NavNoAnim {

    private NavNoAnim() {
    }

    public static void start(@NonNull AppCompatActivity activity, @NonNull Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static void startAndFinish(@NonNull AppCompatActivity activity, @NonNull Intent intent) {
        start(activity, intent);
        activity.finish();
    }

    public static void finish(@NonNull AppCompatActivity activity) {
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }
}