package com.veda.app.ui.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;

import com.veda.app.R;
import com.veda.app.data.entity.NoteDetailItem;
import com.veda.app.databinding.ActivityNoteViewBinding;
import com.veda.app.ui.edit.EditNoteActivity;
import com.veda.app.ui.nav.NavNoAnim;
import com.veda.app.ui.nav.VedaTopBar;

import java.util.Date;

public class NoteViewActivity extends AppCompatActivity {

    private static final String EXTRA_NOTE_ID = "noteId";

    private ActivityNoteViewBinding binding;
    private NoteViewViewModel vm;
    private long noteId;

    public static Intent intent(@NonNull Context context, long noteId) {
        Intent i = new Intent(context, NoteViewActivity.class);
        i.putExtra(EXTRA_NOTE_ID, noteId);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNoteViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        VedaTopBar.setup(this, binding.toolbar);

        noteId = getIntent().getLongExtra(EXTRA_NOTE_ID, 0L);

        vm = new ViewModelProvider(this).get(NoteViewViewModel.class);
        vm.load(noteId);

        binding.content.setMovementMethod(LinkMovementMethod.getInstance());
        binding.content.setLinkTextColor(ContextCompat.getColor(this, R.color.veda_color_accent));
        binding.content.setHighlightColor(ContextCompat.getColor(this, R.color.veda_color_accent_container));

        vm.item.observe(this, this::render);
    }

    private void render(NoteDetailItem item) {
        if (item == null) return;

        binding.title.setText(item.title == null ? "" : item.title);
        binding.subject.setText(item.subjectName == null || item.subjectName.trim().isEmpty()
                ? getString(R.string.note_without_subject)
                : item.subjectName);

        String dateText = android.text.format.DateFormat.getDateFormat(this).format(new Date(item.updatedAt));
        binding.date.setText(getString(R.string.note_updated_at, dateText));

        CharSequence spanned = HtmlCompat.fromHtml(
                item.contentHtml == null ? "" : item.contentHtml,
                HtmlCompat.FROM_HTML_MODE_LEGACY
        );

        SpannableStringBuilder ssb = new SpannableStringBuilder(spanned);
        URLSpan[] spans = ssb.getSpans(0, ssb.length(), URLSpan.class);

        for (URLSpan span : spans) {
            String url = span.getURL();
            int start = ssb.getSpanStart(span);
            int end = ssb.getSpanEnd(span);
            int flags = ssb.getSpanFlags(span);

            ssb.removeSpan(span);

            if (url != null && url.startsWith("veda://note/")) {
                long targetId = parseNoteId(url);
                if (targetId > 0) {
                    ssb.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            NavNoAnim.start(
                                    NoteViewActivity.this,
                                    NoteViewActivity.intent(NoteViewActivity.this, targetId)
                            );
                        }

                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            ds.setColor(ContextCompat.getColor(NoteViewActivity.this, R.color.veda_color_accent));
                            ds.setUnderlineText(false);
                            ds.setFakeBoldText(true);
                        }
                    }, start, end, flags);
                    continue;
                }
            }

            final String externalUrl = url;
            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (externalUrl == null || externalUrl.trim().isEmpty()) return;
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl)));
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(ContextCompat.getColor(NoteViewActivity.this, R.color.veda_color_accent));
                    ds.setUnderlineText(false);
                }
            }, start, end, flags);
        }

        binding.content.setText(ssb);
    }

    private static long parseNoteId(@NonNull String url) {
        try {
            Uri u = Uri.parse(url);
            String last = u.getLastPathSegment();
            if (last == null) return -1;
            return Long.parseLong(last);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_view_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            Intent i = new Intent(this, EditNoteActivity.class);
            i.putExtra(EXTRA_NOTE_ID, noteId);
            NavNoAnim.start(this, i);
            return true;
        }

        if (id == R.id.action_delete) {
            vm.delete();
            NavNoAnim.finish(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}