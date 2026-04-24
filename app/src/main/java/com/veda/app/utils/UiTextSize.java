package com.veda.app.utils;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UiTextSize {

    private UiTextSize() {}

    public static void apply(@Nullable View root, @NonNull AppPrefs.TextSize size) {
        if (root == null) return;
        float mul = size == AppPrefs.TextSize.LARGE ? 1.12f : 1.0f;
        applyRecursive(root, mul);
    }

    private static void applyRecursive(@NonNull View v, float mul) {
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            float px = tv.getTextSize();
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, px * mul);
        }

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View child = g.getChildAt(i);
                if (child != null) applyRecursive(child, mul);
            }
        }
    }
}