package com.veda.app.ui.homework;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veda.app.R;
import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.repo.VedaRepository;
import com.veda.app.databinding.ActivityHomeworkListBinding;
import com.veda.app.ui.nav.BottomNav;
import com.veda.app.ui.nav.NavNoAnim;
import com.veda.app.ui.nav.ScrollToTop;
import com.veda.app.ui.nav.VedaTopBar;
import com.veda.app.utils.AppPrefs;
import com.veda.app.utils.UiTextSize;

import java.util.Collections;
import java.util.List;

public final class HomeworkListActivity extends AppCompatActivity implements ScrollToTop {

    private ActivityHomeworkListBinding binding;
    private HomeworkListViewModel vm;
    private HomeworkAdapter adapter;

    private boolean firstDataLoaded = false;
    private HomeworkListViewModel.Filter currentFilter = HomeworkListViewModel.Filter.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeworkListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UiTextSize.apply(binding.getRoot(), AppPrefs.getTextSize(this));

        VedaTopBar.setup(this, binding.toolbar);
        BottomNav.bind(this, binding.bottomNav, R.id.nav_homework);

        vm = new ViewModelProvider(this).get(HomeworkListViewModel.class);

        adapter = new HomeworkAdapter(new HomeworkAdapter.Listener() {
            @Override
            public void onItemClick(@NonNull HomeworkEntity item) {
                openEditor(item.id);
            }

            @Override
            public void onItemLongClick(@NonNull HomeworkEntity item) {
                vm.toggleDone(item);
            }
        });

        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        VedaRepository.get(this).observeSubjects().observe(this, adapter::setSubjects);

        binding.emptyText.setVisibility(android.view.View.GONE);

        restoreFilterIfNeeded();
        updateFilterUi(currentFilter);

        binding.btnAll.setOnClickListener(v -> selectFilter(HomeworkListViewModel.Filter.ALL));
        binding.btnToday.setOnClickListener(v -> selectFilter(HomeworkListViewModel.Filter.TODAY));
        binding.btnWeek.setOnClickListener(v -> selectFilter(HomeworkListViewModel.Filter.WEEK));
        binding.btnOverdue.setOnClickListener(v -> selectFilter(HomeworkListViewModel.Filter.OVERDUE));
        binding.btnUpcoming.setOnClickListener(v -> selectFilter(HomeworkListViewModel.Filter.UPCOMING));
        binding.btnDone.setOnClickListener(v -> selectFilter(HomeworkListViewModel.Filter.DONE));

        binding.fabAdd.setOnClickListener(v ->
                NavNoAnim.start(this, new Intent(this, HomeworkEditActivity.class))
        );

        vm.rows().observe(this, this::renderRows);
        vm.screenState().observe(this, this::renderState);
        vm.operationMessageRes().observe(this, messageRes -> {
            if (messageRes == null || messageRes == 0) return;
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
            vm.consumeOperationMessage();
        });
    }

    private void restoreFilterIfNeeded() {
        HomeworkListViewModel.Filter f = HomeworkListViewModel.Filter.ALL;

        if (AppPrefs.isHomeworkSaveFiltersEnabled(this)) {
            String raw = AppPrefs.getHomeworkLastFilter(this, HomeworkListViewModel.Filter.ALL.name());
            try {
                f = HomeworkListViewModel.Filter.valueOf(raw);
            } catch (Exception ignored) {
                f = HomeworkListViewModel.Filter.ALL;
            }
        }

        currentFilter = f;
        vm.setFilter(f);
    }

    private void selectFilter(@NonNull HomeworkListViewModel.Filter filter) {
        currentFilter = filter;
        vm.setFilter(filter);
        updateFilterUi(filter);
        persistFilterIfNeeded(filter);
    }

    private void persistFilterIfNeeded(@NonNull HomeworkListViewModel.Filter f) {
        if (!AppPrefs.isHomeworkSaveFiltersEnabled(this)) return;
        AppPrefs.setHomeworkLastFilter(this, f.name());
    }

    private void renderRows(List<HomeworkRowItem> rows) {
        if (!firstDataLoaded) {
            if (rows == null) return;
            firstDataLoaded = true;
        }

        List<HomeworkRowItem> safe = rows == null ? Collections.emptyList() : rows;

        binding.emptyText.setVisibility(safe.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        adapter.submit(safe);
    }

    private void renderState(HomeworkListViewModel.ScreenState state) {
        if (state == null) return;
        switch (state) {
            case LOADING:
                binding.emptyText.setVisibility(android.view.View.VISIBLE);
                binding.emptyText.setText(R.string.state_loading);
                binding.recycler.setVisibility(android.view.View.GONE);
                break;
            case CONTENT:
                binding.recycler.setVisibility(android.view.View.VISIBLE);
                break;
            case EMPTY:
                binding.emptyText.setVisibility(android.view.View.VISIBLE);
                binding.emptyText.setText(R.string.homework_empty);
                binding.recycler.setVisibility(android.view.View.VISIBLE);
                break;
            case ERROR:
            default:
                binding.emptyText.setVisibility(android.view.View.VISIBLE);
                binding.emptyText.setText(R.string.homework_error_load);
                binding.recycler.setVisibility(android.view.View.GONE);
                break;
        }
    }


    private void openEditor(long homeworkId) {
        Intent i = new Intent(this, HomeworkEditActivity.class);
        i.putExtra(HomeworkEditActivity.EXTRA_HOMEWORK_ID, homeworkId);
        NavNoAnim.start(this, i);
    }

    private void updateFilterUi(@NonNull HomeworkListViewModel.Filter filter) {
        setFilterButtonState(binding.btnAll, filter == HomeworkListViewModel.Filter.ALL);
        setFilterButtonState(binding.btnToday, filter == HomeworkListViewModel.Filter.TODAY);
        setFilterButtonState(binding.btnWeek, filter == HomeworkListViewModel.Filter.WEEK);
        setFilterButtonState(binding.btnOverdue, filter == HomeworkListViewModel.Filter.OVERDUE);
        setFilterButtonState(binding.btnUpcoming, filter == HomeworkListViewModel.Filter.UPCOMING);
        setFilterButtonState(binding.btnDone, filter == HomeworkListViewModel.Filter.DONE);
    }

    private void setFilterButtonState(@NonNull android.widget.Button button, boolean active) {
        button.setSelected(active);
        button.setTextColor(ContextCompat.getColor(this, active
                ? R.color.veda_color_on_accent
                : R.color.veda_color_text_primary));
        button.setBackgroundResource(active
                ? R.drawable.bg_homework_filter_active
                : R.drawable.bg_homework_filter_inactive);
    }



    @Override
    public void scrollToTop() {
        if (binding == null) return;
        binding.recycler.smoothScrollToPosition(0);
    }
}
