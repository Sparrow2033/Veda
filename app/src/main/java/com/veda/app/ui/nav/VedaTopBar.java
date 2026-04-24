package com.veda.app.ui.nav;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.veda.app.R;
import com.veda.app.ui.home.HomeActivity;

public final class VedaTopBar {

    private VedaTopBar() {
    }

    public static void setup(@NonNull AppCompatActivity activity, @NonNull Toolbar toolbar) {
        activity.setSupportActionBar(toolbar);

        toolbar.setTitle("Veda");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setBackgroundColor(ContextCompat.getColor(activity, R.color.veda_color_surface_elevated));
        toolbar.setElevation(0f);
        toolbar.setContentInsetsRelative(
                dp(activity, 8),
                dp(activity, 8)
        );

        @ColorInt int iconColor = ContextCompat.getColor(activity, R.color.veda_color_text_primary);
        toolbar.setTitleTextColor(iconColor);
        toolbar.setSubtitleTextColor(ContextCompat.getColor(activity, R.color.veda_color_text_secondary));

        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
            activity.getSupportActionBar().setTitle("Veda");
        }

        applyToolbarTint(toolbar, iconColor);
        scheduleRetint(toolbar, iconColor);

        toolbar.setNavigationOnClickListener(v ->
                NavNoAnim.start(activity, new Intent(activity, HomeActivity.class))
        );
    }

    public static void setup(@NonNull AppCompatActivity activity,
                             @NonNull Toolbar toolbar,
                             boolean showPlus,
                             boolean plusCreatesHomework) {
        setup(activity, toolbar);
    }

    private static void scheduleRetint(@NonNull Toolbar toolbar, @ColorInt int color) {
        toolbar.post(() -> applyToolbarTint(toolbar, color));
        toolbar.postDelayed(() -> applyToolbarTint(toolbar, color), 16L);
        toolbar.postDelayed(() -> applyToolbarTint(toolbar, color), 64L);

        toolbar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                applyToolbarTint(toolbar, color)
        );

        ViewTreeObserver observer = toolbar.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(() -> applyToolbarTint(toolbar, color));
    }

    private static void applyToolbarTint(@NonNull Toolbar toolbar, @ColorInt int color) {
        tintNavigationIcon(toolbar, color);
        tintOverflowIcon(toolbar, color);
        tintMenuIcons(toolbar.getMenu(), color);
    }

    private static void tintNavigationIcon(@NonNull Toolbar toolbar, @ColorInt int color) {
        Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon == null) return;

        Drawable wrapped = DrawableCompat.wrap(navigationIcon.mutate());
        DrawableCompat.setTint(wrapped, color);
        toolbar.setNavigationIcon(wrapped);
    }

    private static void tintOverflowIcon(@NonNull Toolbar toolbar, @ColorInt int color) {
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon == null) return;

        Drawable wrapped = DrawableCompat.wrap(overflowIcon.mutate());
        DrawableCompat.setTint(wrapped, color);
        toolbar.setOverflowIcon(wrapped);
    }

    private static void tintMenuIcons(Menu menu, @ColorInt int color) {
        if (menu == null) return;

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Drawable icon = item.getIcon();
            if (icon != null) {
                Drawable wrapped = DrawableCompat.wrap(icon.mutate());
                DrawableCompat.setTint(wrapped, color);
                item.setIcon(wrapped);
            }

            if (item.hasSubMenu() && item.getSubMenu() != null) {
                tintMenuIcons(item.getSubMenu(), color);
            }
        }
    }

    private static int dp(@NonNull AppCompatActivity activity, int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}