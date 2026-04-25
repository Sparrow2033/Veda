package com.veda.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veda.app.R;
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
        vm.notes().observe(this, items -> {
            if (!firstDataLoaded) {
                if (items == null) return;
                firstDataLoaded = true;
            }
            adapter.submitList(items);
        });
        vm.screenState().observe(this, this::renderScreenState);

        binding.fab.setOnClickListener(v ->
                NavNoAnim.start(this, new Intent(this, EditNoteActivity.class))
        );
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

    private void renderScreenState(NotesListViewModel.ScreenState state) {
        if (state == null) return;

        switch (state) {
            case LOADING:
                binding.empty.setVisibility(View.VISIBLE);
                binding.recycler.setVisibility(View.GONE);
                break;
            case CONTENT:
                binding.empty.setVisibility(View.GONE);
                binding.recycler.setVisibility(View.VISIBLE);
                break;
            case EMPTY:
                binding.empty.setVisibility(View.VISIBLE);
                binding.recycler.setVisibility(View.GONE);
                break;
            case ERROR:
            default:
                binding.empty.setVisibility(View.VISIBLE);
                binding.recycler.setVisibility(View.GONE);
                Toast.makeText(this, R.string.notes_error_generic, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
