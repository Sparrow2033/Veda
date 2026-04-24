package com.veda.app.ui.subjects;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.databinding.ActivitySubjectsBinding;
import com.veda.app.utils.SubjectPalette;

import java.util.ArrayList;
import java.util.List;
import com.veda.app.R;

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
        binding.toolbar.setTitle("Предметы");

        vm = new ViewModelProvider(this).get(SubjectsViewModel.class);

        adapter = new SubjectsAdapter(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        vm.subjects().observe(this, list -> {
            current = list == null ? new ArrayList<>() : list;
            adapter.submitList(current);
            binding.empty.setVisibility(current.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
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
        input.setHint("Название предмета");
        input.setSingleLine(true);
        input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);

        int padH = dp(16); // левый/правый отступ
        int padV = dp(12); // верх/низ
        input.setPaddingRelative(padH, padV, padH, padV);

        new AlertDialog.Builder(this)
                .setTitle("Новый предмет")
                .setView(input)
                .setPositiveButton("Далее", (d, w) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (name.isBlank()) {
                        Toast.makeText(this, "Название не должно быть пустым", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pickColorAndCreate(name);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void pickColorAndCreate(String name) {
        new AlertDialog.Builder(this)
                .setTitle("Цвет предмета")
                .setItems(SubjectPalette.COLOR_NAMES, (d, which) -> {
                    int color = SubjectPalette.clampColorByIndex(which);
                    int sortOrder = current.size() + 1;

                    vm.addSubject(name, color, sortOrder, id ->
                            Toast.makeText(this, "Добавлено", Toast.LENGTH_SHORT).show()
                    );
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onClick(SubjectEntity subject) {
        showEditDialog(subject);
    }

    @Override
    public void onLongClick(SubjectEntity subject) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить предмет?")
                .setMessage("Заметки с этим предметом станут 'Без предмета'.")
                .setPositiveButton("Удалить", (d, w) -> vm.delete(subject))
                .setNegativeButton("Отмена", null)
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
                    .setTitle("Цвет")
                    .setItems(names, (d, which) -> {
                        pickedColor[0] = colors[Math.max(0, Math.min(which, colors.length - 1))];
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Редактировать предмет")
                .setView(v)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String newName = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                    if (newName.isEmpty()) newName = subject.name;

                    subject.name = newName;
                    subject.color = pickedColor[0];

                    // важно: это вызывает repo.updateSubject(...)
                    vm.update(subject);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void pickColorAndUpdate(SubjectEntity subject, String newName) {
        new AlertDialog.Builder(this)
                .setTitle("Цвет предмета")
                .setItems(SubjectPalette.COLOR_NAMES, (d, which) -> {
                    subject.name = newName;
                    subject.color = SubjectPalette.clampColorByIndex(which);
                    vm.update(subject);
                    Toast.makeText(this, "Обновлено", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditDialog(SubjectEntity subject) {
        showEditSubjectDialog(subject);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
