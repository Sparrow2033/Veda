package com.veda.app.ui.schedule;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.veda.app.R;
import com.veda.app.databinding.ActivityScheduleBinding;
import com.veda.app.ui.nav.BottomNav;
import com.veda.app.ui.nav.ScrollToTop;
import com.veda.app.ui.nav.VedaTopBar;
import com.veda.app.utils.SchedulePrefs;

public class ScheduleActivity extends AppCompatActivity implements ScrollToTop {

    private ActivityScheduleBinding binding;
    private String currentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        VedaTopBar.setup(this, binding.toolbar, false, false);
        BottomNav.bind(this, binding.bottomNav, R.id.nav_schedule);

        setupWebView(binding.web);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.web.getVisibility() == View.VISIBLE && binding.web.canGoBack()) {
                    binding.web.goBack();
                } else {
                    finish();
                }
            }
        });

        binding.btnSetUrl.setOnClickListener(v -> showUrlDialog());

        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void setupWebView(WebView webView) {
        webView.setBackgroundColor(Color.TRANSPARENT);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!request.isForMainFrame()) return;

                int code = error.getErrorCode();

                if (code == -10) {
                    if (!TextUtils.isEmpty(currentUrl)) {
                        view.post(() -> {
                            view.stopLoading();
                            view.clearCache(true);
                            view.loadUrl(currentUrl);
                        });
                    }
                    return;
                }

                Toast.makeText(
                        ScheduleActivity.this,
                        getString(R.string.schedule_error_loading, code),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void render() {
        currentUrl = SchedulePrefs.getUrl(this);
        boolean hasUrl = !TextUtils.isEmpty(currentUrl);

        binding.empty.setVisibility(hasUrl ? View.GONE : View.VISIBLE);
        binding.web.setVisibility(hasUrl ? View.VISIBLE : View.GONE);

        if (hasUrl) {
            binding.web.stopLoading();
            binding.web.clearCache(true);
            binding.web.loadUrl(currentUrl);
        }
    }

    private void showUrlDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_schedule_url, null, false);
        TextInputEditText input = v.findViewById(R.id.urlInput);

        String current = SchedulePrefs.getUrl(this);
        if (current != null) input.setText(current);

        new AlertDialog.Builder(this)
                .setTitle(R.string.schedule_dialog_title)
                .setView(v)
                .setPositiveButton(R.string.schedule_dialog_save, (d, w) -> {
                    String raw = input.getText() == null ? "" : input.getText().toString().trim();
                    String normalized = normalizeUrl(raw);
                    if (normalized == null) {
                        Toast.makeText(this, R.string.schedule_invalid_url, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SchedulePrefs.setUrl(this, normalized);
                    render();
                })
                .setNegativeButton(R.string.schedule_dialog_cancel, null)
                .setNeutralButton(R.string.schedule_dialog_open_browser, (d, w) -> {
                    String raw = input.getText() == null ? "" : input.getText().toString().trim();
                    String normalized = normalizeUrl(raw);
                    if (normalized == null) {
                        Toast.makeText(this, R.string.schedule_invalid_url, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(normalized)));
                })
                .show();
    }

    private String normalizeUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s;
        }

        try {
            Uri uri = Uri.parse(s);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return null;
            if (uri.getHost() == null || uri.getHost().isEmpty()) return null;
            return uri.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.schedule_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit_url) {
            showUrlDialog();
            return true;
        }

        if (id == R.id.action_reload) {
            if (!TextUtils.isEmpty(currentUrl)) {
                binding.web.stopLoading();
                binding.web.clearCache(true);
                binding.web.loadUrl(currentUrl);
            }
            return true;
        }

        if (id == R.id.action_open_browser) {
            if (!TextUtils.isEmpty(currentUrl)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void scrollToTop() {
        if (binding == null) return;
        binding.web.scrollTo(0, 0);
    }
}