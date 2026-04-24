package com.veda.app.ui.edit;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;

import com.veda.app.R;
import com.veda.app.data.entity.NoteEntity;
import com.veda.app.data.entity.NoteListItem;
import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.databinding.ActivityEditNoteBinding;
import com.veda.app.ui.nav.NavNoAnim;
import com.veda.app.ui.nav.VedaTopBar;
import com.veda.app.utils.NoteHtml;

import java.util.ArrayList;
import java.util.List;

public class EditNoteActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "note_id";
    private static final String ADD_SUBJECT = "+ Добавить предмет…";

    private ActivityEditNoteBinding binding;
    private EditNoteViewModel vm;

    private long editingNoteId = -1;
    private long editingCreatedAt = 0;

    private List<SubjectEntity> subjects = new ArrayList<>();
    private Long selectedSubjectId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vm = new ViewModelProvider(this).get(EditNoteViewModel.class);

        VedaTopBar.setup(this, binding.toolbar);

        editingNoteId = getIntent().getLongExtra(EXTRA_NOTE_ID, -1);

        binding.btnBold.setOnClickListener(v -> toggleStyle(Typeface.BOLD));
        binding.btnItalic.setOnClickListener(v -> toggleStyle(Typeface.ITALIC));

        setupSubjectsDropdown();

        if (editingNoteId == -1) {
            binding.toolbar.setTitle("Новая заметка");
        } else {
            binding.toolbar.setTitle("Редактирование");
            vm.note(editingNoteId).observe(this, this::bindNote);
        }
    }

    private void setupSubjectsDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        );
        binding.subjectDropdown.setAdapter(adapter);

        vm.subjects().observe(this, list -> {
            subjects = (list == null) ? new ArrayList<>() : list;

            List<String> names = new ArrayList<>();
            for (SubjectEntity s : subjects) names.add(s.name);
            names.add(ADD_SUBJECT);

            adapter.clear();
            adapter.addAll(names);
            adapter.notifyDataSetChanged();

            if (selectedSubjectId != null) {
                SubjectEntity s = findSubjectById(selectedSubjectId);
                if (s != null) binding.subjectDropdown.setText(s.name, false);
            }
        });

        binding.subjectDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String picked = adapter.getItem(position);
            if (picked == null) return;

            if (ADD_SUBJECT.equals(picked)) {
                binding.subjectDropdown.setText("", false);
                showAddSubjectDialog();
                return;
            }

            SubjectEntity s = findSubjectByName(picked);
            selectedSubjectId = (s == null) ? null : s.id;
        });
    }

    private void bindNote(NoteEntity note) {
        if (note == null) return;

        editingCreatedAt = note.createdAt;
        selectedSubjectId = note.subjectId;

        binding.titleInput.setText(note.title);

        CharSequence spanned = HtmlCompat.fromHtml(
                note.contentHtml == null ? "" : note.contentHtml,
                HtmlCompat.FROM_HTML_MODE_COMPACT
        );
        binding.contentInput.setText(spanned);

        binding.tagsInput.setText(note.tags == null ? "" : note.tags);

        if (selectedSubjectId != null) {
            SubjectEntity s = findSubjectById(selectedSubjectId);
            if (s != null) binding.subjectDropdown.setText(s.name, false);
        }
    }

    private SubjectEntity findSubjectById(long id) {
        for (SubjectEntity s : subjects) if (s.id == id) return s;
        return null;
    }

    private SubjectEntity findSubjectByName(String name) {
        for (SubjectEntity s : subjects) if (s.name.equals(name)) return s;
        return null;
    }

    private void toggleStyle(int style) {
        EditText edit = binding.contentInput;
        int start = edit.getSelectionStart();
        int end = edit.getSelectionEnd();

        if (start < 0 || end <= start) {
            Toast.makeText(this, "Выдели текст и нажми кнопку форматирования", Toast.LENGTH_SHORT).show();
            return;
        }

        Editable text = edit.getText();
        if (text == null) return;

        StyleSpan[] spans = text.getSpans(start, end, StyleSpan.class);
        boolean removed = false;

        for (StyleSpan span : spans) {
            if (span.getStyle() == style) {
                int spanStart = text.getSpanStart(span);
                int spanEnd = text.getSpanEnd(span);
                if (spanStart <= start && spanEnd >= end) {
                    text.removeSpan(span);
                    removed = true;
                }
            }
        }

        if (!removed) {
            text.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_note_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            NavNoAnim.finish(this);
            return true;
        }

        if (id == R.id.action_link) {
            linkSelection();
            return true;
        }

        if (id == R.id.action_save) {
            save();
            return true;
        }

        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void linkSelection() {
        int start = binding.contentInput.getSelectionStart();
        int end = binding.contentInput.getSelectionEnd();

        if (start < 0 || end <= start) {
            Toast.makeText(this, "Сначала выдели слово/фразу в тексте", Toast.LENGTH_SHORT).show();
            return;
        }

        long excludeId = (editingNoteId == -1) ? -1 : editingNoteId;
        vm.loadNotesForPicker(excludeId, notes -> showLinkPickerDialog(start, end, notes));
    }

    private void showLinkPickerDialog(int selStart, int selEnd, List<NoteListItem> notes) {
        if (notes == null || notes.isEmpty()) {
            Toast.makeText(this, "Нет других заметок для связи", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[notes.size()];
        for (int i = 0; i < notes.size(); i++) {
            NoteListItem n = notes.get(i);
            String title = (n.title == null || n.title.isBlank()) ? "(без названия)" : n.title;
            String subj = (n.subjectName == null) ? "Без предмета" : n.subjectName;
            items[i] = title + "  •  " + subj;
        }

        new AlertDialog.Builder(this)
                .setTitle("Связать с заметкой")
                .setItems(items, (d, which) -> {
                    NoteListItem picked = notes.get(which);
                    applyNoteLink(selStart, selEnd, picked.id);
                    Toast.makeText(this, "Ссылка добавлена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void applyNoteLink(int start, int end, long toNoteId) {
        Editable text = binding.contentInput.getText();
        if (text == null) return;

        URLSpan[] spans = text.getSpans(start, end, URLSpan.class);
        for (URLSpan s : spans) text.removeSpan(s);

        String url = "veda://note/" + toNoteId;
        text.setSpan(new URLSpan(url), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void save() {
        String title = safe(binding.titleInput.getText());
        if (title.isBlank()) title = "(без названия)";

        String tags = safe(binding.tagsInput.getText()).trim();

        Editable content = binding.contentInput.getText();

        String contentHtml = HtmlCompat.toHtml(
                content == null ? new SpannableString("") : content,
                HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
        );

        List<Long> toNoteIds = NoteHtml.extractLinkedNoteIds(contentHtml);

        if (editingNoteId == -1) {
            vm.saveNewWithLinks(selectedSubjectId, title, contentHtml, tags, toNoteIds, newId -> {
                Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
                NavNoAnim.finish(this);
            });
        } else {
            vm.saveUpdateWithLinks(editingNoteId, selectedSubjectId, title, contentHtml, tags, editingCreatedAt, toNoteIds);
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
            NavNoAnim.finish(this);
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить заметку?")
                .setMessage("Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (d, w) -> {
                    vm.deleteById(editingNoteId);
                    Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show();
                    NavNoAnim.finish(this);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAddSubjectDialog() {
        final EditText input = new EditText(this);
        input.setHint("Например: Геометрия");

        new AlertDialog.Builder(this)
                .setTitle("Новый предмет")
                .setView(input)
                .setPositiveButton("Далее", (d, w) -> {
                    String name = safe(input.getText()).trim();
                    if (name.isBlank()) {
                        Toast.makeText(this, "Название не должно быть пустым", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showColorPicker(name);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showColorPicker(String subjectName) {
        String[] names = {"Синий", "Фиолетовый", "Бирюзовый", "Зелёный", "Оранжевый", "Розовый", "Голубой", "Графит"};
        int[] colors = {
                0xFF2166F3, 0xFF7149F7, 0xFF00A8C8, 0xFF4CC73D,
                0xFFCF5B1F, 0xFFCD46B3, 0xFF448AFF, 0xFF263238
        };

        new AlertDialog.Builder(this)
                .setTitle("Цвет предмета")
                .setItems(names, (d, which) -> {
                    int color = colors[Math.max(0, Math.min(which, colors.length - 1))];
                    int sortOrder = subjects.size() + 1;

                    vm.insertSubject(subjectName, color, sortOrder, newId -> {
                        selectedSubjectId = newId;
                        binding.subjectDropdown.setText(subjectName, false);
                        Toast.makeText(this, "Предмет добавлен", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }
}