package com.veda.app.ui.subjects;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veda.app.R;
import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.databinding.ActivitySubjectsBinding;
import com.veda.app.utils.SubjectPalette;

import java.util.ArrayList;
import java.util.List;

public class SubjectsActivity extends AppCompatActivity implements SubjectsAdapter.Listener {

    private ActivitySubjectsBinding binding;
    private SubjectsViewModel vm;
    private SubjectsAdapter adapter;

    private List<SubjectEntity> current = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubjectsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setTitle(R.string.notes_subjects);

        vm = new ViewModelProvider(this).get(SubjectsViewModel.class);

        adapter = new SubjectsAdapter(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        vm.subjects().observe(this, list -> {
            current = list == null ? new ArrayList<>() : list;
            adapter.submitList(current);
        });
        vm.screenState().observe(this, this::renderState);
        vm.operationMessageRes().observe(this, messageRes -> {
            if (messageRes == null || messageRes == 0) return;
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
            vm.consumeOperationMessage();
        });

        binding.fab.setOnClickListener(v -> showCreateDialog());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showCreateDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.subject_name_hint);
        input.setSingleLine(true);
        input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);

        int padH = dp(16); // левый/правый отступ
        int padV = dp(12); // верх/низ
        input.setPaddingRelative(padH, padV, padH, padV);

        new AlertDialog.Builder(this)
                .setTitle(R.string.subject_add_title)
                .setView(input)
                .setPositiveButton(R.string.action_next, (d, w) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (name.isBlank()) {
                        Toast.makeText(this, R.string.subject_name_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pickColorAndCreate(name);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void pickColorAndCreate(String name) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.subject_color_title)
                .setItems(SubjectPalette.COLOR_NAMES, (d, which) -> {
                    int color = SubjectPalette.clampColorByIndex(which);
                    int sortOrder = current.size() + 1;

                    vm.addSubject(name, color, sortOrder, id -> { });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onClick(SubjectEntity subject) {
        showEditDialog(subject);
    }

    @Override
    public void onLongClick(SubjectEntity subject) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.subject_delete_title)
                .setMessage(R.string.subject_delete_message)
                .setPositiveButton(R.string.action_delete, (d, w) -> vm.delete(subject))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showEditSubjectDialog(SubjectEntity subject) {
        android.view.View v = getLayoutInflater().inflate(R.layout.dialog_edit_subject, null, false);

        com.google.android.material.textfield.TextInputEditText nameInput =
                v.findViewById(R.id.nameInput);
        com.google.android.material.button.MaterialButton colorBtn =
                v.findViewById(R.id.colorBtn);

        nameInput.setText(subject.name);

        final int[] pickedColor = new int[]{subject.color};

        colorBtn.setOnClickListener(btn -> {
            // Если у тебя есть SubjectPalette, используй её.
            // Иначе — простая палитра как в заметках:
            String[] names = {"Синий", "Фиолетовый", "Бирюзовый", "Зелёный", "Оранжевый", "Розовый", "Голубой", "Графит"};
            int[] colors = {
                    0xFF2166F3, 0xFF7149F7, 0xFF00A8C8, 0xFF4CC73D,
                    0xFFCF5B1F, 0xFFCD46B3, 0xFF448AFF, 0xFF263238
            };

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.subject_color_title)
                    .setItems(names, (d, which) -> {
                        pickedColor[0] = colors[Math.max(0, Math.min(which, colors.length - 1))];
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.subject_edit_title)
                .setView(v)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String newName = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                    if (newName.isEmpty()) newName = subject.name;

                    subject.name = newName;
                    subject.color = pickedColor[0];

                    // важно: это вызывает repo.updateSubject(...)
                    vm.update(subject);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showEditDialog(SubjectEntity subject) {
        showEditSubjectDialog(subject);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void renderState(SubjectsViewModel.ScreenState state) {
        if (state == null) return;
        switch (state) {
            case LOADING:
                binding.empty.setVisibility(android.view.View.VISIBLE);
                binding.emptyTitle.setText(R.string.state_loading);
                binding.emptySubtitle.setText(R.string.subjects_loading_subtitle);
                binding.recycler.setVisibility(android.view.View.GONE);
                break;
            case CONTENT:
                binding.empty.setVisibility(android.view.View.GONE);
                binding.recycler.setVisibility(android.view.View.VISIBLE);
                break;
            case EMPTY:
                binding.empty.setVisibility(android.view.View.VISIBLE);
                binding.emptyTitle.setText(R.string.subjects_empty_title);
                binding.emptySubtitle.setText(R.string.subjects_empty_subtitle);
                binding.recycler.setVisibility(android.view.View.GONE);
                break;
            case ERROR:
            default:
                binding.empty.setVisibility(android.view.View.VISIBLE);
                binding.emptyTitle.setText(R.string.subjects_error_title);
                binding.emptySubtitle.setText(R.string.subjects_error_subtitle);
                binding.recycler.setVisibility(android.view.View.GONE);
                break;
        }
    }
}
