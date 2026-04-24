package com.veda.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veda.app.R;
import com.veda.app.databinding.ActivityHomeBinding;
import com.veda.app.ui.edit.EditNoteActivity;
import com.veda.app.ui.graph.GraphActivity;
import com.veda.app.ui.homework.HomeworkListActivity;
import com.veda.app.ui.main.MainActivity;
import com.veda.app.ui.nav.NavNoAnim;
import com.veda.app.ui.schedule.ScheduleActivity;
import com.veda.app.ui.settings.SettingsActivity;
import com.veda.app.utils.AppPrefs;
import com.veda.app.utils.UiTextSize;

public final class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UiTextSize.apply(binding.getRoot(), AppPrefs.getTextSize(this));

        binding.txtWelcome.setText(R.string.home_welcome_title);
        binding.txtSubtitle.setText(R.string.home_welcome_subtitle);

        if (AppPrefs.isOpenLastSectionEnabled(this)) {
            int last = AppPrefs.getLastSectionId(this, R.id.nav_notes);
            Intent i = toSectionIntent(last);
            if (i != null) {
                NavNoAnim.startAndFinish(this, i);
                return;
            }
        }

        binding.fabCreateNote.setOnClickListener(v ->
                NavNoAnim.start(this, new Intent(this, EditNoteActivity.class))
        );

        binding.bottomNav.getMenu().setGroupCheckable(0, true, false);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            openTab(item.getItemId());
            clearBottomSelection();
            return true;
        });

        binding.bottomNav.setOnItemReselectedListener(item -> {
            openTab(item.getItemId());
            clearBottomSelection();
        });

        binding.bottomNav.post(this::positionFabOverNotesItem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearBottomSelection();
        binding.bottomNav.post(this::positionFabOverNotesItem);
    }

    private void openTab(int id) {
        Intent i = toSectionIntent(id);
        if (i != null) {
            NavNoAnim.start(this, i);
        }
    }

    @Nullable
    private Intent toSectionIntent(int id) {
        if (id == R.id.nav_homework) return new Intent(this, HomeworkListActivity.class);
        if (id == R.id.nav_schedule) return new Intent(this, ScheduleActivity.class);
        if (id == R.id.nav_notes) return new Intent(this, MainActivity.class);
        if (id == R.id.nav_graph) return new Intent(this, GraphActivity.class);
        if (id == R.id.nav_settings) return new Intent(this, SettingsActivity.class);
        return null;
    }

    private void clearBottomSelection() {
        if (binding == null) return;
        for (int i = 0; i < binding.bottomNav.getMenu().size(); i++) {
            binding.bottomNav.getMenu().getItem(i).setChecked(false);
        }
    }

    private void positionFabOverNotesItem() {
        if (binding == null) return;

        binding.fabCreateNote.post(() -> {
            ViewGroup menuView = null;
            if (binding.bottomNav.getChildCount() > 0 && binding.bottomNav.getChildAt(0) instanceof ViewGroup) {
                menuView = (ViewGroup) binding.bottomNav.getChildAt(0);
            }
            if (menuView == null) return;

            int notesIndex = 2;
            if (menuView.getChildCount() <= notesIndex) return;

            View itemView = menuView.getChildAt(notesIndex);
            if (itemView == null || itemView.getWidth() == 0 || binding.fabCreateNote.getWidth() == 0) return;

            float itemCenterX = itemView.getX() + (itemView.getWidth() / 2f);
            float fabX = itemCenterX - (binding.fabCreateNote.getWidth() / 2f);

            binding.fabCreateNote.setX(fabX);
        });
    }
}