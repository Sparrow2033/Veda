package com.veda.app.ui.homework;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.veda.app.R;
import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.data.repo.VedaRepository;
import com.veda.app.databinding.ActivityHomeworkEditBinding;
import com.veda.app.ui.nav.BottomNav;
import com.veda.app.ui.nav.NavNoAnim;
import com.veda.app.ui.nav.VedaTopBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public final class HomeworkEditActivity extends AppCompatActivity {

    public static final String EXTRA_HOMEWORK_ID = "homeworkId";

    private ActivityHomeworkEditBinding binding;
    private HomeworkEditViewModel vm;

    private final List<SubjectEntity> subjects = new ArrayList<>();
    private ArrayAdapter<String> subjectAdapter;

    private long selectedDueDate = 0L;
    private HomeworkEntity bound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeworkEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        VedaTopBar.setup(this, binding.toolbar, false, false);
        BottomNav.bind(this, binding.bottomNav, R.id.nav_homework);

        vm = new ViewModelProvider(this).get(HomeworkEditViewModel.class);

        long homeworkId = getIntent().getLongExtra(EXTRA_HOMEWORK_ID, 0L);
        binding.toolbar.setTitle(homeworkId > 0 ? "Редактирование ДЗ" : "Новое задание");

        setupSpinners();
        setupButtons();
        observeSubjects();

        if (homeworkId > 0) {
            vm.setHomeworkId(homeworkId);
            vm.homework().observe(this, this::renderHomework);
        } else {
            binding.btnDelete.setVisibility(android.view.View.GONE);
            setDueDateText(0L);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.homework_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save_homework) {
            onSave();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSpinners() {
        subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSubject.setAdapter(subjectAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Нужно сделать", "В процессе", "Готово", "Отменено"}
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerStatus.setAdapter(statusAdapter);

        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Низкий", "Средний", "Высокий"}
        );
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPriority.setAdapter(priorityAdapter);
    }

    private void setupButtons() {
        binding.btnPickDueDate.setOnClickListener(v -> openDatePicker());
        binding.btnClearDueDate.setOnClickListener(v -> {
            selectedDueDate = 0L;
            setDueDateText(0L);
        });
        binding.btnDelete.setOnClickListener(v -> onDelete());
    }

    private void observeSubjects() {
        VedaRepository.get(this).observeSubjects().observe(this, list -> {
            subjects.clear();
            if (list != null) subjects.addAll(list);

            List<String> names = new ArrayList<>();
            for (SubjectEntity s : subjects) names.add(s.name);

            subjectAdapter.clear();
            subjectAdapter.addAll(names);
            subjectAdapter.notifyDataSetChanged();

            if (bound != null) setSubjectSelection(bound.subjectId);
        });
    }

    private void renderHomework(@Nullable HomeworkEntity entity) {
        if (entity == null) return;

        bound = entity;

        binding.inputTitle.setText(entity.title);
        binding.inputDescription.setText(entity.description);

        selectedDueDate = entity.dueDate;
        setDueDateText(entity.dueDate);

        binding.spinnerStatus.setSelection(statusToPos(entity.status));
        binding.spinnerPriority.setSelection(priorityToPos(entity.priority));

        binding.btnDelete.setVisibility(android.view.View.VISIBLE);
        setSubjectSelection(entity.subjectId);
    }

    private void setSubjectSelection(long subjectId) {
        int idx = -1;
        for (int i = 0; i < subjects.size(); i++) {
            if (subjects.get(i).id == subjectId) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) binding.spinnerSubject.setSelection(idx);
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();
        if (selectedDueDate > 0) c.setTimeInMillis(selectedDueDate);

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar out = Calendar.getInstance();
                    out.set(Calendar.YEAR, y);
                    out.set(Calendar.MONTH, m);
                    out.set(Calendar.DAY_OF_MONTH, d);
                    out.set(Calendar.HOUR_OF_DAY, 0);
                    out.set(Calendar.MINUTE, 0);
                    out.set(Calendar.SECOND, 0);
                    out.set(Calendar.MILLISECOND, 0);
                    selectedDueDate = out.getTimeInMillis();
                    setDueDateText(selectedDueDate);
                },
                year,
                month,
                day
        );
        dlg.show();
    }

    private void setDueDateText(long dueDate) {
        if (dueDate <= 0) {
            binding.txtDueDate.setText("Без дедлайна");
            return;
        }
        String s = DateFormat.getDateFormat(this).format(new Date(dueDate));
        binding.txtDueDate.setText(s);
    }

    private void onSave() {
        if (subjects.isEmpty()) {
            Toast.makeText(this, "Сначала добавь предмет", Toast.LENGTH_SHORT).show();
            return;
        }

        int subjectPos = binding.spinnerSubject.getSelectedItemPosition();
        if (subjectPos < 0 || subjectPos >= subjects.size()) {
            Toast.makeText(this, "Выбери предмет", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = safe(binding.inputTitle.getText() == null ? null : binding.inputTitle.getText().toString()).trim();
        String description = safe(binding.inputDescription.getText() == null ? null : binding.inputDescription.getText().toString()).trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Тема не должна быть пустой", Toast.LENGTH_SHORT).show();
            return;
        }

        long subjectId = subjects.get(subjectPos).id;

        int status = posToStatus(binding.spinnerStatus.getSelectedItemPosition());
        int priority = posToPriority(binding.spinnerPriority.getSelectedItemPosition());

        if (bound != null && bound.id > 0) {
            vm.update(
                    bound.id,
                    subjectId,
                    title,
                    description,
                    selectedDueDate,
                    status,
                    priority,
                    bound.createdAt
            );
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
            NavNoAnim.finish(this);
            return;
        }

        vm.insert(
                subjectId,
                title,
                description,
                selectedDueDate,
                status,
                priority,
                id -> {
                    Toast.makeText(this, "Создано", Toast.LENGTH_SHORT).show();
                    NavNoAnim.finish(this);
                }
        );
    }

    private void onDelete() {
        if (bound == null || bound.id <= 0) return;
        vm.deleteById(bound.id);
        Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show();
        NavNoAnim.finish(this);
    }

    private static String safe(@Nullable String s) {
        return s == null ? "" : s;
    }

    private static int statusToPos(int status) {
        switch (status) {
            case HomeworkEntity.STATUS_IN_PROGRESS:
                return 1;
            case HomeworkEntity.STATUS_DONE:
                return 2;
            case HomeworkEntity.STATUS_CANCELED:
                return 3;
            case HomeworkEntity.STATUS_TODO:
            default:
                return 0;
        }
    }

    private static int priorityToPos(int priority) {
        switch (priority) {
            case HomeworkEntity.PRIORITY_LOW:
                return 0;
            case HomeworkEntity.PRIORITY_HIGH:
                return 2;
            case HomeworkEntity.PRIORITY_MEDIUM:
            default:
                return 1;
        }
    }

    private static int posToStatus(int pos) {
        switch (pos) {
            case 1:
                return HomeworkEntity.STATUS_IN_PROGRESS;
            case 2:
                return HomeworkEntity.STATUS_DONE;
            case 3:
                return HomeworkEntity.STATUS_CANCELED;
            case 0:
            default:
                return HomeworkEntity.STATUS_TODO;
        }
    }

    private static int posToPriority(int pos) {
        switch (pos) {
            case 0:
                return HomeworkEntity.PRIORITY_LOW;
            case 2:
                return HomeworkEntity.PRIORITY_HIGH;
            case 1:
            default:
                return HomeworkEntity.PRIORITY_MEDIUM;
        }
    }
}