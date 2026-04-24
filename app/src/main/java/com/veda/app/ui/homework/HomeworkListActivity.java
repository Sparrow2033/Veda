package com.veda.app.ui.homework;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class HomeworkListActivity extends AppCompatActivity implements ScrollToTop {

    private ActivityHomeworkListBinding binding;
    private HomeworkListViewModel vm;
    private HomeworkAdapter adapter;
    private VedaRepository repo;

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

        repo = VedaRepository.get(this);
        vm = new ViewModelProvider(this).get(HomeworkListViewModel.class);

        adapter = new HomeworkAdapter(new HomeworkAdapter.Listener() {
            @Override
            public void onItemClick(@NonNull HomeworkEntity item) {
                openEditor(item.id);
            }

            @Override
            public void onItemLongClick(@NonNull HomeworkEntity item) {
                toggleDone(item);
            }
        });

        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        repo.observeSubjects().observe(this, adapter::setSubjects);

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

        vm.items().observe(this, this::render);
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

    private void render(List<HomeworkEntity> list) {
        if (!firstDataLoaded) {
            if (list == null) return;
            firstDataLoaded = true;
        }

        List<HomeworkEntity> safe = list == null ? Collections.emptyList() : new ArrayList<>(list);

        binding.emptyText.setVisibility(safe.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);

        applyHomeworkSort(safe);

        List<HomeworkRowItem> rows = buildRows(safe);
        adapter.submit(rows);
    }

    private void applyHomeworkSort(@NonNull List<HomeworkEntity> list) {
        AppPrefs.HomeworkSort sort = AppPrefs.getHomeworkSort(this);

        if (sort == AppPrefs.HomeworkSort.PRIORITY) {
            list.sort((a, b) -> Integer.compare(b.priority, a.priority));
            return;
        }
        if (sort == AppPrefs.HomeworkSort.STATUS) {
            list.sort((a, b) -> Integer.compare(a.status, b.status));
            return;
        }

        list.sort((a, b) -> {
            long da = a.dueDate <= 0 ? Long.MAX_VALUE : a.dueDate;
            long db = b.dueDate <= 0 ? Long.MAX_VALUE : b.dueDate;
            return Long.compare(da, db);
        });
    }

    private void openEditor(long homeworkId) {
        Intent i = new Intent(this, HomeworkEditActivity.class);
        i.putExtra(HomeworkEditActivity.EXTRA_HOMEWORK_ID, homeworkId);
        NavNoAnim.start(this, i);
    }

    private void toggleDone(@NonNull HomeworkEntity item) {
        int newStatus = item.status == HomeworkEntity.STATUS_DONE
                ? HomeworkEntity.STATUS_TODO
                : HomeworkEntity.STATUS_DONE;

        long now = System.currentTimeMillis();

        HomeworkEntity updated = new HomeworkEntity(
                item.id,
                item.subjectId,
                item.title == null ? "" : item.title,
                item.description == null ? "" : item.description,
                item.dueDate,
                newStatus,
                item.priority,
                item.createdAt,
                now
        );

        repo.updateHomework(updated);

        Toast.makeText(
                this,
                newStatus == HomeworkEntity.STATUS_DONE
                        ? getString(R.string.homework_toast_done)
                        : getString(R.string.homework_toast_todo),
                Toast.LENGTH_SHORT
        ).show();
    }

    @NonNull
    private List<HomeworkRowItem> buildRows(@NonNull List<HomeworkEntity> source) {
        if (source.isEmpty()) return Collections.emptyList();

        List<HomeworkEntity> withDue = new ArrayList<>();
        List<HomeworkEntity> noDue = new ArrayList<>();

        for (HomeworkEntity e : source) {
            if (e == null) continue;
            if (e.dueDate > 0) withDue.add(e);
            else noDue.add(e);
        }

        noDue.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));

        List<HomeworkRowItem> out = new ArrayList<>();

        long prevDay = Long.MIN_VALUE;
        for (HomeworkEntity e : withDue) {
            long day = startOfDay(e.dueDate);
            if (day != prevDay) {
                prevDay = day;
                String header = DateFormat.getDateFormat(this).format(new Date(day));
                out.add(HomeworkRowItem.header(header, -day));
            }
            out.add(HomeworkRowItem.task(e));
        }

        if (!noDue.isEmpty()) {
            out.add(HomeworkRowItem.header(getString(R.string.homework_header_no_deadline), -1L));
            for (HomeworkEntity e : noDue) out.add(HomeworkRowItem.task(e));
        }

        return out;
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
        button.setTextColor(getColor(active ? android.R.color.white : android.R.color.black));
        button.setBackgroundResource(active
                ? R.drawable.bg_homework_filter_active
                : R.drawable.bg_homework_filter_inactive);
    }

    private static long startOfDay(long timeMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Override
    public void scrollToTop() {
        if (binding == null) return;
        binding.recycler.smoothScrollToPosition(0);
    }
}