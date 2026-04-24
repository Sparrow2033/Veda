package com.veda.app.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.veda.app.R;
import com.veda.app.databinding.ActivitySettingsBinding;
import com.veda.app.demo.DemoDeleteResult;
import com.veda.app.demo.DemoImportResult;
import com.veda.app.demo.DemoLibraryCleaner;
import com.veda.app.demo.DemoLibraryImporter;
import com.veda.app.ui.nav.BottomNav;
import com.veda.app.ui.nav.ScrollToTop;
import com.veda.app.ui.nav.VedaTopBar;
import com.veda.app.utils.AppPrefs;
import com.veda.app.utils.UiTextSize;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SettingsActivity extends AppCompatActivity implements ScrollToTop {

    private ActivitySettingsBinding binding;
    private boolean initializing = true;

    private final ExecutorService demoExecutor = Executors.newSingleThreadExecutor();
    private boolean demoOperationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UiTextSize.apply(binding.getRoot(), AppPrefs.getTextSize(this));

        VedaTopBar.setup(this, binding.toolbar);
        BottomNav.bind(this, binding.bottomNav, R.id.nav_settings);

        setupSpinners();
        bindInitialValues();
        bindListeners();
        bindDemoActions();

        initializing = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        demoExecutor.shutdown();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> themeMode = ArrayAdapter.createFromResource(
                this,
                R.array.pref_theme_mode_entries,
                android.R.layout.simple_spinner_item
        );
        themeMode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerThemeMode.setAdapter(themeMode);

        ArrayAdapter<CharSequence> notesSort = ArrayAdapter.createFromResource(
                this,
                R.array.pref_notes_sort_entries,
                android.R.layout.simple_spinner_item
        );
        notesSort.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerNotesSort.setAdapter(notesSort);

        ArrayAdapter<CharSequence> hwSort = ArrayAdapter.createFromResource(
                this,
                R.array.pref_homework_sort_entries,
                android.R.layout.simple_spinner_item
        );
        hwSort.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerHomeworkSort.setAdapter(hwSort);
    }

    private void bindInitialValues() {
        AppPrefs.ThemeMode themeMode = AppPrefs.getThemeMode(this);
        binding.spinnerThemeMode.setSelection(mapThemeModeToPos(themeMode));

        AppPrefs.TextSize textSize = AppPrefs.getTextSize(this);
        if (textSize == AppPrefs.TextSize.LARGE) {
            binding.radioTextLarge.setChecked(true);
        } else {
            binding.radioTextNormal.setChecked(true);
        }

        binding.switchConfirmDelete.setChecked(AppPrefs.isConfirmDeleteEnabled(this));

        AppPrefs.NotesSort notesSort = AppPrefs.getNotesSort(this);
        binding.spinnerNotesSort.setSelection(mapNotesSortToPos(notesSort));

        AppPrefs.HomeworkSort hwSort = AppPrefs.getHomeworkSort(this);
        binding.spinnerHomeworkSort.setSelection(mapHomeworkSortToPos(hwSort));

        binding.switchSaveHomeworkFilters.setChecked(AppPrefs.isHomeworkSaveFiltersEnabled(this));
        binding.switchOpenLastSection.setChecked(AppPrefs.isOpenLastSectionEnabled(this));
    }

    private void bindListeners() {
        binding.spinnerThemeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (initializing) return;

                AppPrefs.ThemeMode newMode = mapPosToThemeMode(position);
                AppPrefs.ThemeMode currentMode = AppPrefs.getThemeMode(SettingsActivity.this);
                if (newMode == currentMode) return;

                AppPrefs.setThemeMode(SettingsActivity.this, newMode);
                AppCompatDelegate.setDefaultNightMode(toNightMode(newMode));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.radioGroupTextSize.setOnCheckedChangeListener((group, checkedId) -> {
            if (initializing) return;

            if (checkedId == binding.radioTextLarge.getId()) {
                AppPrefs.setTextSize(this, AppPrefs.TextSize.LARGE);
            } else if (checkedId == binding.radioTextNormal.getId()) {
                AppPrefs.setTextSize(this, AppPrefs.TextSize.NORMAL);
            }
        });

        binding.switchConfirmDelete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (initializing) return;
            AppPrefs.setConfirmDeleteEnabled(this, isChecked);
        });

        binding.spinnerNotesSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (initializing) return;
                AppPrefs.setNotesSort(SettingsActivity.this, mapPosToNotesSort(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.spinnerHomeworkSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (initializing) return;
                AppPrefs.setHomeworkSort(SettingsActivity.this, mapPosToHomeworkSort(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.switchSaveHomeworkFilters.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (initializing) return;
            AppPrefs.setHomeworkSaveFiltersEnabled(this, isChecked);
        });

        binding.switchOpenLastSection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (initializing) return;
            AppPrefs.setOpenLastSectionEnabled(this, isChecked);
        });
    }

    private void bindDemoActions() {
        binding.btnLoadDemoLibrary.setOnClickListener(v -> onLoadDemoLibraryClick());
        binding.btnDeleteDemoLibrary.setOnClickListener(v -> onDeleteDemoLibraryClick());
    }

    private void onLoadDemoLibraryClick() {
        if (demoOperationInProgress) return;
        showLoadDemoConfirmDialog();
    }

    private void onDeleteDemoLibraryClick() {
        if (demoOperationInProgress) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_delete_dialog_title)
                .setMessage(R.string.demo_delete_dialog_message)
                .setPositiveButton(R.string.demo_delete_action, (d, w) -> executeDeleteDemoLibrary())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showLoadDemoConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_load_dialog_title)
                .setMessage(R.string.demo_load_dialog_message)
                .setPositiveButton(R.string.demo_load_action, (d, w) -> executeImportDemoLibrary())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void executeImportDemoLibrary() {
        setDemoUiBusy(true);

        demoExecutor.execute(() -> {
            try {
                DemoImportResult result = new DemoLibraryImporter(this).importLibrary();

                runOnUiThread(() -> {
                    setDemoUiBusy(false);
                    showImportResult(result);
                });
            } catch (Throwable throwable) {
                runOnUiThread(() -> {
                    setDemoUiBusy(false);
                    showError(throwable);
                });
            }
        });
    }

    private void executeDeleteDemoLibrary() {
        setDemoUiBusy(true);

        demoExecutor.execute(() -> {
            try {
                DemoDeleteResult result = new DemoLibraryCleaner(this).deleteDemoLibrary();

                runOnUiThread(() -> {
                    setDemoUiBusy(false);
                    showDeleteResult(result);
                });
            } catch (Throwable throwable) {
                runOnUiThread(() -> {
                    setDemoUiBusy(false);
                    showError(throwable);
                });
            }
        });
    }

    private void setDemoUiBusy(boolean busy) {
        demoOperationInProgress = busy;

        binding.btnLoadDemoLibrary.setEnabled(!busy);
        binding.btnDeleteDemoLibrary.setEnabled(!busy);
        binding.demoProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void showImportResult(@NonNull DemoImportResult result) {
        String message = getString(
                R.string.demo_import_result_message,
                result.notesInserted,
                result.subjectsCreated,
                result.linksResolved
        );

        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_import_result_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showDeleteResult(@NonNull DemoDeleteResult result) {
        String message = getString(
                R.string.demo_delete_result_message,
                result.demoNotesDeleted
        );

        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_delete_result_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showError(@NonNull Throwable throwable) {
        Toast.makeText(
                this,
                getString(R.string.demo_operation_error, safeMessage(throwable)),
                Toast.LENGTH_LONG
        ).show();
    }

    @NonNull
    private static String safeMessage(@NonNull Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.trim().isEmpty()
                ? "неизвестная ошибка"
                : message;
    }

    private static int mapThemeModeToPos(@NonNull AppPrefs.ThemeMode mode) {
        switch (mode) {
            case LIGHT:
                return 1;
            case DARK:
                return 2;
            case SYSTEM:
            default:
                return 0;
        }
    }

    @NonNull
    private static AppPrefs.ThemeMode mapPosToThemeMode(int pos) {
        if (pos == 1) return AppPrefs.ThemeMode.LIGHT;
        if (pos == 2) return AppPrefs.ThemeMode.DARK;
        return AppPrefs.ThemeMode.SYSTEM;
    }

    private static int toNightMode(@NonNull AppPrefs.ThemeMode mode) {
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

    private static int mapNotesSortToPos(@NonNull AppPrefs.NotesSort sort) {
        switch (sort) {
            case OLD_FIRST:
                return 1;
            case ALPHABETICAL:
                return 2;
            case NEW_FIRST:
            default:
                return 0;
        }
    }

    @NonNull
    private static AppPrefs.NotesSort mapPosToNotesSort(int pos) {
        if (pos == 1) return AppPrefs.NotesSort.OLD_FIRST;
        if (pos == 2) return AppPrefs.NotesSort.ALPHABETICAL;
        return AppPrefs.NotesSort.NEW_FIRST;
    }

    private static int mapHomeworkSortToPos(@NonNull AppPrefs.HomeworkSort sort) {
        switch (sort) {
            case PRIORITY:
                return 1;
            case STATUS:
                return 2;
            case DUE_DATE:
            default:
                return 0;
        }
    }

    @NonNull
    private static AppPrefs.HomeworkSort mapPosToHomeworkSort(int pos) {
        if (pos == 1) return AppPrefs.HomeworkSort.PRIORITY;
        if (pos == 2) return AppPrefs.HomeworkSort.STATUS;
        return AppPrefs.HomeworkSort.DUE_DATE;
    }

    @Override
    public void scrollToTop() {
        if (binding == null) return;
        binding.scroll.smoothScrollTo(0, 0);
    }
}