package com.veda.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veda.app.R;
import com.veda.app.data.entity.NoteListItem;
import com.veda.app.databinding.ActivityMainBinding;
import com.veda.app.ui.edit.EditNoteActivity;
import com.veda.app.ui.nav.BottomNav;
import com.veda.app.ui.nav.NavNoAnim;
import com.veda.app.ui.nav.ScrollToTop;
import com.veda.app.ui.nav.VedaTopBar;
import com.veda.app.ui.subjects.SubjectsActivity;
import com.veda.app.ui.view.NoteViewActivity;
import com.veda.app.utils.AppPrefs;
import com.veda.app.utils.UiTextSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesAdapter.Listener, ScrollToTop {

    private ActivityMainBinding binding;
    private NotesAdapter adapter;

    private boolean firstDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UiTextSize.apply(binding.getRoot(), AppPrefs.getTextSize(this));

        VedaTopBar.setup(this, binding.toolbar);
        BottomNav.bind(this, binding.bottomNav, R.id.nav_notes);

        adapter = new NotesAdapter(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        binding.empty.setVisibility(View.GONE);
        binding.recycler.setVisibility(View.VISIBLE);

        NotesListViewModel vm = new ViewModelProvider(this).get(NotesListViewModel.class);
        vm.notes.observe(this, items -> {
            if (!firstDataLoaded) {
                if (items == null) return;
                firstDataLoaded = true;
            }

            List<NoteListItem> sorted = sortNotes(items);
            adapter.submitList(sorted);

            boolean empty = sorted == null || sorted.isEmpty();
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        binding.fab.setOnClickListener(v ->
                NavNoAnim.start(this, new Intent(this, EditNoteActivity.class))
        );
    }

    private List<NoteListItem> sortNotes(List<NoteListItem> items) {
        if (items == null) return Collections.emptyList();

        List<NoteListItem> list = new ArrayList<>(items);
        AppPrefs.NotesSort sort = AppPrefs.getNotesSort(this);

        if (sort == AppPrefs.NotesSort.ALPHABETICAL) {
            list.sort((a, b) -> {
                String ta = a.title == null ? "" : a.title.trim();
                String tb = b.title == null ? "" : b.title.trim();
                return ta.compareToIgnoreCase(tb);
            });
            return list;
        }

        Comparator<NoteListItem> byUpdated = (a, b) -> Long.compare(a.updatedAt, b.updatedAt);
        list.sort(byUpdated);

        if (sort == AppPrefs.NotesSort.NEW_FIRST) Collections.reverse(list);
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_subjects) {
            NavNoAnim.start(this, new Intent(this, SubjectsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNoteClick(long noteId) {
        NavNoAnim.start(this, NoteViewActivity.intent(this, noteId));
    }

    @Override
    public void scrollToTop() {
        if (binding == null) return;
        binding.recycler.smoothScrollToPosition(0);
    }
}