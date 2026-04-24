package com.veda.app.ui.graph;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.veda.app.databinding.ActivityGraphBinding;
import com.veda.app.ui.homework.HomeworkListActivity;
import com.veda.app.ui.main.MainActivity;
import com.veda.app.ui.nav.BottomNav;
import com.veda.app.ui.nav.ScrollToTop;
import com.veda.app.ui.schedule.ScheduleActivity;
import com.veda.app.ui.settings.SettingsActivity;
import com.veda.app.ui.view.NoteViewActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class GraphActivity extends AppCompatActivity implements ScrollToTop {

    private static final String GRAPH_URL = "file:///android_asset/graph/graph.html";
    private static final long HOLD_TAP_SUPPRESS_MS = 1600L;
    private static final long NOTE_OPEN_GUARD_MS = 700L;

    private static final String STATE_SELECTED_SUBJECTS = "graph_selected_subjects";
    private static final String STATE_HIDE_ISOLATED = "graph_hide_isolated";
    private static final String STATE_SHOW_LABELS = "graph_show_labels";
    private static final String STATE_SCALE_NODES = "graph_scale_nodes";
    private static final String STATE_GROUP_COLORS = "graph_group_colors";
    private static final String STATE_FADE_EDGES = "graph_fade_edges";
    private static final String STATE_LAST_OPEN_NOTE_ID = "graph_last_open_note_id";
    private static final String STATE_LAST_OPEN_NOTE_AT = "graph_last_open_note_at";
    private static final String STATE_MODE = "graph_mode";
    private static final String STATE_DEPTH = "graph_depth";
    private static final String STATE_FOCUS_NOTE_ID = "graph_focus_note_id";
    private static final String STATE_SETTINGS_PANEL_VISIBLE = "graph_settings_panel_visible";

    private ActivityGraphBinding binding;
    private GraphViewModel viewModel;

    private boolean rendererReady = false;
    private boolean payloadReady = false;
    private boolean modelLoading = true;
    private boolean isActivityAlive = false;
    private String pendingPayloadJson = "";

    private long currentFocusNoteId = -1L;
    private int currentDepth = 2;
    private GraphMode currentMode = GraphMode.GLOBAL;

    private final Set<String> selectedSubjects = new LinkedHashSet<>();
    private boolean hideIsolated = false;

    private boolean showLabels = true;
    private boolean scaleNodesByDegree = true;
    private boolean useGroupColors = true;
    private boolean fadeEdges = false;

    private long suppressedNodeTapId = -1L;
    private long suppressTapUntilMs = 0L;

    private long lastOpenedNoteId = -1L;
    private long lastOpenedNoteAtMs = 0L;

    private boolean hasPendingRestoredGraphState = false;
    private GraphMode restoredMode = GraphMode.GLOBAL;
    private int restoredDepth = 2;
    private long restoredFocusNoteId = -1L;
    private boolean settingsPanelVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isActivityAlive = true;

        binding = ActivityGraphBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        restoreUiState(savedInstanceState);

        setupToolbar();
        setupBottomNavigation();
        setupControls();
        setupWebView();

        viewModel = new ViewModelProvider(this).get(GraphViewModel.class);
        observeViewModel();

        applyRestoredControlStates();
        updateSettingsPanelVisibility();
        applyPendingRestoredGraphStateIfNeeded();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void restoreUiState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        ArrayList<String> restoredSubjects = savedInstanceState.getStringArrayList(STATE_SELECTED_SUBJECTS);
        selectedSubjects.clear();
        if (restoredSubjects != null) {
            selectedSubjects.addAll(restoredSubjects);
        }

        hideIsolated = savedInstanceState.getBoolean(STATE_HIDE_ISOLATED, false);
        showLabels = savedInstanceState.getBoolean(STATE_SHOW_LABELS, true);
        scaleNodesByDegree = savedInstanceState.getBoolean(STATE_SCALE_NODES, true);
        useGroupColors = savedInstanceState.getBoolean(STATE_GROUP_COLORS, true);
        fadeEdges = savedInstanceState.getBoolean(STATE_FADE_EDGES, false);

        lastOpenedNoteId = savedInstanceState.getLong(STATE_LAST_OPEN_NOTE_ID, -1L);
        lastOpenedNoteAtMs = savedInstanceState.getLong(STATE_LAST_OPEN_NOTE_AT, 0L);

        String modeName = savedInstanceState.getString(STATE_MODE, GraphMode.GLOBAL.name());
        try {
            restoredMode = GraphMode.valueOf(modeName);
        } catch (Throwable ignored) {
            restoredMode = GraphMode.GLOBAL;
        }

        restoredDepth = savedInstanceState.getInt(STATE_DEPTH, 2);
        if (restoredDepth < 1) restoredDepth = 1;
        if (restoredDepth > 3) restoredDepth = 3;

        restoredFocusNoteId = savedInstanceState.getLong(STATE_FOCUS_NOTE_ID, -1L);
        settingsPanelVisible = savedInstanceState.getBoolean(STATE_SETTINGS_PANEL_VISIBLE, false);
        hasPendingRestoredGraphState = true;
    }

    private void applyRestoredControlStates() {
        binding.checkHideIsolated.setChecked(hideIsolated);
        binding.checkShowLabels.setChecked(showLabels);
        binding.checkScaleNodes.setChecked(scaleNodesByDegree);
        binding.checkGroupColors.setChecked(useGroupColors);
        binding.checkFadeEdges.setChecked(fadeEdges);
    }

    private void applyPendingRestoredGraphStateIfNeeded() {
        if (!hasPendingRestoredGraphState || viewModel == null) {
            return;
        }

        hasPendingRestoredGraphState = false;

        viewModel.setLocalDepth(restoredDepth);

        if (restoredMode == GraphMode.LOCAL && restoredFocusNoteId > 0L) {
            viewModel.showLocalGraph(restoredFocusNoteId);
        } else {
            viewModel.showGlobalGraph();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(STATE_SELECTED_SUBJECTS, new ArrayList<>(selectedSubjects));
        outState.putBoolean(STATE_HIDE_ISOLATED, hideIsolated);
        outState.putBoolean(STATE_SHOW_LABELS, showLabels);
        outState.putBoolean(STATE_SCALE_NODES, scaleNodesByDegree);
        outState.putBoolean(STATE_GROUP_COLORS, useGroupColors);
        outState.putBoolean(STATE_FADE_EDGES, fadeEdges);
        outState.putLong(STATE_LAST_OPEN_NOTE_ID, lastOpenedNoteId);
        outState.putLong(STATE_LAST_OPEN_NOTE_AT, lastOpenedNoteAtMs);
        outState.putString(STATE_MODE, currentMode == null ? GraphMode.GLOBAL.name() : currentMode.name());
        outState.putInt(STATE_DEPTH, currentDepth);
        outState.putLong(STATE_FOCUS_NOTE_ID, currentFocusNoteId);
        outState.putBoolean(STATE_SETTINGS_PANEL_VISIBLE, settingsPanelVisible);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        binding.tvToolbarTitle.setText("Veda");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupBottomNavigation() {
        if (!bindBottomNavViaHelper()) {
            binding.bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);
            selectGraphItemIfPossible();
        }
    }

    private boolean bindBottomNavViaHelper() {
        try {
            int selectedId = resolveGraphMenuItemId();

            for (Method method : BottomNav.class.getDeclaredMethods()) {
                if (!"bind".equals(method.getName())) {
                    continue;
                }

                method.setAccessible(true);
                Class<?>[] params = method.getParameterTypes();

                if (params.length == 4
                        && AppCompatActivity.class.isAssignableFrom(params[0])
                        && BottomNavigationView.class.isAssignableFrom(params[1])
                        && int.class == params[2]
                        && ScrollToTop.class.isAssignableFrom(params[3])) {
                    method.invoke(null, this, binding.bottomNav, selectedId, this);
                    return true;
                }

                if (params.length == 3
                        && AppCompatActivity.class.isAssignableFrom(params[0])
                        && BottomNavigationView.class.isAssignableFrom(params[1])
                        && int.class == params[2]) {
                    method.invoke(null, this, binding.bottomNav, selectedId);
                    return true;
                }

                if (params.length == 3
                        && AppCompatActivity.class.isAssignableFrom(params[0])
                        && BottomNavigationView.class.isAssignableFrom(params[1])
                        && ScrollToTop.class.isAssignableFrom(params[2])) {
                    method.invoke(null, this, binding.bottomNav, this);
                    selectGraphItemIfPossible();
                    return true;
                }

                if (params.length == 2
                        && AppCompatActivity.class.isAssignableFrom(params[0])
                        && BottomNavigationView.class.isAssignableFrom(params[1])) {
                    method.invoke(null, this, binding.bottomNav);
                    selectGraphItemIfPossible();
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == resolveGraphMenuItemId()) {
            return true;
        }

        CharSequence title = item.getTitle();
        String normalized = title == null ? "" : title.toString().trim().toLowerCase(Locale.ROOT);

        if (normalized.contains("глав")) {
            openSection(MainActivity.class);
            return true;
        }

        if (normalized.contains("дз") || normalized.contains("дом")) {
            openSection(HomeworkListActivity.class);
            return true;
        }

        if (normalized.contains("расп")) {
            openSection(ScheduleActivity.class);
            return true;
        }

        if (normalized.contains("настр")) {
            openSection(SettingsActivity.class);
            return true;
        }

        return false;
    }

    private void selectGraphItemIfPossible() {
        int graphItemId = resolveGraphMenuItemId();
        if (graphItemId != View.NO_ID) {
            binding.bottomNav.setSelectedItemId(graphItemId);
        }
    }

    private int resolveGraphMenuItemId() {
        for (int i = 0; i < binding.bottomNav.getMenu().size(); i++) {
            MenuItem item = binding.bottomNav.getMenu().getItem(i);
            CharSequence title = item.getTitle();
            if (title != null && title.toString().trim().toLowerCase(Locale.ROOT).contains("граф")) {
                return item.getItemId();
            }
        }
        return View.NO_ID;
    }

    private void setupControls() {
        binding.btnToggleControls.setOnClickListener(v -> toggleSettingsPanel());

        binding.btnModeGlobal.setOnClickListener(v -> {
            if (currentMode != GraphMode.GLOBAL) {
                viewModel.showGlobalGraph();
            }
        });

        binding.btnModeLocal.setOnClickListener(v -> {
            if (currentFocusNoteId > 0L) {
                if (currentMode != GraphMode.LOCAL) {
                    viewModel.showLocalGraph(currentFocusNoteId);
                }
            } else {
                Toast.makeText(
                        this,
                        "Сначала удержи узел, чтобы открыть локальный граф",
                        Toast.LENGTH_SHORT
                ).show();
                syncToggleState();
            }
        });

        binding.btnDepth1.setOnClickListener(v -> {
            if (currentDepth != 1) {
                viewModel.setLocalDepth(1);
            }
        });
        binding.btnDepth2.setOnClickListener(v -> {
            if (currentDepth != 2) {
                viewModel.setLocalDepth(2);
            }
        });
        binding.btnDepth3.setOnClickListener(v -> {
            if (currentDepth != 3) {
                viewModel.setLocalDepth(3);
            }
        });

        binding.btnReset.setOnClickListener(v -> resetGraphView());

        binding.checkHideIsolated.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hideIsolated = isChecked;
            pushFiltersIfPossible();
        });

        binding.checkShowLabels.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showLabels = isChecked;
            pushDisplaySettingsIfPossible();
        });

        binding.checkScaleNodes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            scaleNodesByDegree = isChecked;
            pushDisplaySettingsIfPossible();
        });

        binding.checkGroupColors.setOnCheckedChangeListener((buttonView, isChecked) -> {
            useGroupColors = isChecked;
            pushDisplaySettingsIfPossible();
        });

        binding.checkFadeEdges.setOnCheckedChangeListener((buttonView, isChecked) -> {
            fadeEdges = isChecked;
            pushDisplaySettingsIfPossible();
        });
    }

    private void toggleSettingsPanel() {
        settingsPanelVisible = !settingsPanelVisible;
        updateSettingsPanelVisibility();
    }

    private void updateSettingsPanelVisibility() {
        if (binding == null) {
            return;
        }

        binding.cardControls.setVisibility(settingsPanelVisible ? View.VISIBLE : View.GONE);
        binding.btnToggleControls.setText(settingsPanelVisible ? "Скрыть настройки" : "Настройки");
        binding.btnToggleControls.setContentDescription(
                settingsPanelVisible ? "Скрыть настройки графа" : "Показать настройки графа"
        );
        binding.btnToggleControls.setChecked(settingsPanelVisible);
    }

    private void setupWebView() {
        WebSettings settings = binding.webGraph.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        binding.webGraph.setBackgroundColor(Color.TRANSPARENT);
        binding.webGraph.setFocusable(true);
        binding.webGraph.setFocusableInTouchMode(true);
        binding.webGraph.setClickable(true);
        binding.webGraph.setLongClickable(true);
        binding.webGraph.setHapticFeedbackEnabled(false);

        binding.webGraph.addJavascriptInterface(new GraphBridge(), "AndroidBridge");

        binding.webGraph.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return super.onConsoleMessage(consoleMessage);
            }
        });

        binding.webGraph.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isActivityAlive || isFinishing() || isDestroyed() || binding == null) {
                    return;
                }

                rendererReady = true;
                pushThemeIfPossible();
                pushPayloadIfPossible();
                pushFiltersIfPossible();
                pushDisplaySettingsIfPossible();
                updateLoadingOverlay();
                super.onPageFinished(view, url);
            }
        });

        binding.webGraph.loadUrl(GRAPH_URL);
    }

    private void observeViewModel() {
        viewModel.getPayloadJson().observe(this, json -> {
            pendingPayloadJson = json != null ? json : "";
            payloadReady = !TextUtils.isEmpty(pendingPayloadJson);
            rebuildSubjectFiltersFromPayload();
            pushPayloadIfPossible();
            pushFiltersIfPossible();
            pushDisplaySettingsIfPossible();
            updateLoadingOverlay();
        });

        viewModel.getLoading().observe(this, loading -> {
            modelLoading = loading != null && loading;
            updateLoadingOverlay();
        });

        viewModel.getStatusText().observe(this, text -> {
            if (binding != null) {
                binding.tvStatus.setText(text != null ? text : "");
            }
        });

        viewModel.getGraphMode().observe(this, mode -> {
            currentMode = mode != null ? mode : GraphMode.GLOBAL;
            updateDepthControlsVisibility();
            syncToggleState();
        });

        viewModel.getLocalDepth().observe(this, depth -> {
            currentDepth = depth != null ? depth : 2;
            syncToggleState();
        });

        viewModel.getFocusNoteId().observe(this, noteId -> {
            currentFocusNoteId = noteId != null ? noteId : -1L;
        });
    }

    private void rebuildSubjectFiltersFromPayload() {
        if (binding == null) {
            return;
        }

        binding.filtersContainer.removeAllViews();

        addFilterButton("Все", selectedSubjects.isEmpty(), v -> {
            selectedSubjects.clear();
            rebuildSubjectFiltersFromPayload();
            pushFiltersIfPossible();
        });

        try {
            JSONObject payload = new JSONObject(pendingPayloadJson);
            JSONObject filters = payload.optJSONObject("filters");
            JSONArray subjects = filters != null ? filters.optJSONArray("subjects") : null;

            if (subjects == null) {
                return;
            }

            Set<String> availableSubjects = new LinkedHashSet<>();
            for (int i = 0; i < subjects.length(); i++) {
                String subject = subjects.optString(i, "").trim();
                if (!subject.isEmpty()) {
                    availableSubjects.add(subject);
                }
            }

            selectedSubjects.retainAll(availableSubjects);

            for (String subject : availableSubjects) {
                final String finalSubject = subject;
                boolean selected = selectedSubjects.contains(subject);

                addFilterButton(subject, selected, v -> {
                    if (selectedSubjects.contains(finalSubject)) {
                        selectedSubjects.remove(finalSubject);
                    } else {
                        selectedSubjects.add(finalSubject);
                    }
                    rebuildSubjectFiltersFromPayload();
                    pushFiltersIfPossible();
                });
            }
        } catch (Throwable ignored) {
        }
    }

    private void addFilterButton(String text, boolean selected, View.OnClickListener listener) {
        if (binding == null) {
            return;
        }

        MaterialButton button = new MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
        );

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(dp(8));
        button.setLayoutParams(params);
        button.setText(text);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setSingleLine(true);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setCornerRadius(dp(18));
        button.setStrokeWidth(dp(1));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        button.setTextColor(Color.WHITE);

        if (selected) {
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#6EA8FF")));
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#334C8BF5")));
            button.setAlpha(1f);
        } else {
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#55FFFFFF")));
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")));
            button.setAlpha(0.92f);
        }

        button.setOnClickListener(listener);
        binding.filtersContainer.addView(button);
    }

    private void pushPayloadIfPossible() {
        if (!rendererReady || !payloadReady || TextUtils.isEmpty(pendingPayloadJson) || !canUseWebView()) {
            return;
        }

        String encoded = Base64.encodeToString(
                pendingPayloadJson.getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP
        );

        String script = "window.VedaGraph && window.VedaGraph.setPayloadBase64('" + encoded + "');";
        binding.webGraph.evaluateJavascript(script, value -> {
            if (!canUseWebView()) {
                return;
            }
            pushFiltersIfPossible();
            pushDisplaySettingsIfPossible();
        });
    }

    private void pushFiltersIfPossible() {
        if (!rendererReady || !canUseWebView()) {
            return;
        }

        try {
            JSONArray subjectsArray = new JSONArray();
            for (String subject : selectedSubjects) {
                subjectsArray.put(subject);
            }

            JSONObject filters = new JSONObject();
            filters.put("subjects", subjectsArray);
            filters.put("hideIsolated", hideIsolated);

            String encoded = Base64.encodeToString(
                    filters.toString().getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
            );

            String script = "window.VedaGraph && window.VedaGraph.applyFiltersBase64('" + encoded + "');";
            binding.webGraph.evaluateJavascript(script, null);
        } catch (Throwable ignored) {
        }
    }

    private void pushDisplaySettingsIfPossible() {
        if (!rendererReady || !canUseWebView()) {
            return;
        }

        try {
            JSONObject display = new JSONObject();
            display.put("showLabels", showLabels);
            display.put("scaleNodesByDegree", scaleNodesByDegree);
            display.put("useGroupColors", useGroupColors);
            display.put("fadeEdges", fadeEdges);

            String encoded = Base64.encodeToString(
                    display.toString().getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
            );

            String script = "window.VedaGraph && window.VedaGraph.applyDisplaySettingsBase64('" + encoded + "');";
            binding.webGraph.evaluateJavascript(script, null);
        } catch (Throwable ignored) {
        }
    }

    private void pushThemeIfPossible() {
        if (!rendererReady || !canUseWebView()) {
            return;
        }

        String theme = isDarkTheme() ? "dark" : "light";
        binding.webGraph.evaluateJavascript(
                "window.VedaGraph && window.VedaGraph.setTheme('" + theme + "');",
                null
        );
    }

    private void resetGraphView() {
        if (!canUseWebView()) {
            return;
        }

        binding.webGraph.evaluateJavascript(
                "window.VedaGraph && window.VedaGraph.resetView();",
                null
        );
    }

    private boolean canUseWebView() {
        return isActivityAlive
                && !isFinishing()
                && !isDestroyed()
                && binding != null
                && binding.webGraph != null;
    }

    @Override
    public void scrollToTop() {
        resetGraphView();
    }

    private void updateDepthControlsVisibility() {
        if (binding == null) {
            return;
        }
        binding.depthGroup.setVisibility(currentMode == GraphMode.LOCAL ? View.VISIBLE : View.GONE);
    }

    private void syncToggleState() {
        if (binding == null) {
            return;
        }

        int targetModeButton = currentMode == GraphMode.LOCAL
                ? binding.btnModeLocal.getId()
                : binding.btnModeGlobal.getId();

        if (binding.modeGroup.getCheckedButtonId() != targetModeButton) {
            binding.modeGroup.check(targetModeButton);
        }

        int targetDepthButton;
        if (currentDepth == 1) {
            targetDepthButton = binding.btnDepth1.getId();
        } else if (currentDepth == 3) {
            targetDepthButton = binding.btnDepth3.getId();
        } else {
            targetDepthButton = binding.btnDepth2.getId();
        }

        if (binding.depthGroup.getCheckedButtonId() != targetDepthButton) {
            binding.depthGroup.check(targetDepthButton);
        }
    }

    private void updateLoadingOverlay() {
        if (binding == null) {
            return;
        }
        boolean show = modelLoading || !rendererReady || !payloadReady;
        binding.loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private boolean isDarkTheme() {
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void openNote(long noteId) {
        if (noteId <= 0L) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (lastOpenedNoteId == noteId && (now - lastOpenedNoteAtMs) < NOTE_OPEN_GUARD_MS) {
            return;
        }

        lastOpenedNoteId = noteId;
        lastOpenedNoteAtMs = now;

        Intent intent = new Intent(this, NoteViewActivity.class);
        intent.putExtra("noteId", noteId);
        intent.putExtra("note_id", noteId);
        intent.putExtra("NOTE_ID", noteId);
        intent.putExtra("EXTRA_NOTE_ID", noteId);
        intent.putExtra("id", noteId);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void openSection(Class<?> target) {
        if (target == null || target == GraphActivity.class) {
            return;
        }

        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private long parseNodeId(String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return -1L;
        }

        try {
            return Long.parseLong(rawValue);
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityAlive = true;
        pushThemeIfPossible();
        pushFiltersIfPossible();
        pushDisplaySettingsIfPossible();
    }

    @Override
    protected void onPause() {
        super.onPause();
        suppressedNodeTapId = -1L;
        suppressTapUntilMs = 0L;
    }

    @Override
    protected void onDestroy() {
        isActivityAlive = false;

        if (binding != null && binding.webGraph != null) {
            try {
                binding.webGraph.removeJavascriptInterface("AndroidBridge");
                binding.webGraph.stopLoading();
                binding.webGraph.setWebChromeClient(null);
                binding.webGraph.setWebViewClient(null);
                binding.webGraph.destroy();
            } catch (Throwable ignored) {
            }
        }

        binding = null;
        super.onDestroy();
    }

    private class GraphBridge {

        @JavascriptInterface
        public void onRendererReady() {
            runOnUiThread(() -> {
                if (!canUseWebView()) {
                    return;
                }
                rendererReady = true;
                pushThemeIfPossible();
                pushPayloadIfPossible();
                pushFiltersIfPossible();
                pushDisplaySettingsIfPossible();
                updateLoadingOverlay();
            });
        }

        @JavascriptInterface
        public void onRendererError(String message) {
            runOnUiThread(() -> {
                if (!canUseWebView()) {
                    return;
                }
                if (!TextUtils.isEmpty(message)) {
                    Toast.makeText(GraphActivity.this, message, Toast.LENGTH_SHORT).show();
                }
                updateLoadingOverlay();
            });
        }

        @JavascriptInterface
        public void onNodeTap(String rawNodeId) {
            long noteId = parseNodeId(rawNodeId);
            long now = SystemClock.elapsedRealtime();

            if (noteId <= 0L) {
                return;
            }

            if (suppressedNodeTapId == noteId && now < suppressTapUntilMs) {
                return;
            }

            runOnUiThread(() -> {
                if (!canUseWebView()) {
                    return;
                }
                openNote(noteId);
            });
        }

        @JavascriptInterface
        public void onNodeHold(String rawNodeId) {
            long noteId = parseNodeId(rawNodeId);
            if (noteId <= 0L) {
                return;
            }

            suppressedNodeTapId = noteId;
            suppressTapUntilMs = SystemClock.elapsedRealtime() + HOLD_TAP_SUPPRESS_MS;

            runOnUiThread(() -> {
                if (!canUseWebView()) {
                    return;
                }
                viewModel.showLocalGraph(noteId);
            });
        }
    }
}