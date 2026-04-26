package com.veda.app.ui.nav;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.veda.app.R;
import com.veda.app.ui.graph.GraphActivity;
import com.veda.app.ui.homework.HomeworkListActivity;
import com.veda.app.ui.main.MainActivity;
import com.veda.app.ui.schedule.ScheduleActivity;
import com.veda.app.ui.settings.SettingsActivity;
import com.veda.app.utils.AppPrefs;

public final class BottomNav {

    private BottomNav() {
    }

    public static void bind(@NonNull AppCompatActivity activity,
                            @NonNull BottomNavigationView nav,
                            int selectedItemId) {

        nav.getMenu().setGroupCheckable(0, true, true);
        nav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        nav.setItemHorizontalTranslationEnabled(false);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;

            Class<?> target = targetActivity(id);
            if (target == null) return false;

            AppPrefs.setLastSectionId(activity, id);
            NavNoAnim.startAndFinish(activity, new Intent(activity, target));
            return true;
        });

        nav.setOnItemReselectedListener(item -> {
            if (item.getItemId() != selectedItemId) return;
            if (activity instanceof ScrollToTop) {
                ((ScrollToTop) activity).scrollToTop();
            }
        });

        if (nav.getMenu().findItem(selectedItemId) != null) {
            nav.getMenu().findItem(selectedItemId).setChecked(true);
        }
    }

    private static Class<?> targetActivity(int menuItemId) {
        if (menuItemId == R.id.nav_homework) return HomeworkListActivity.class;
        if (menuItemId == R.id.nav_schedule) return ScheduleActivity.class;
        if (menuItemId == R.id.nav_notes) return MainActivity.class;
        if (menuItemId == R.id.nav_graph) return GraphActivity.class;
        if (menuItemId == R.id.nav_settings) return SettingsActivity.class;
        return null;
    }
}