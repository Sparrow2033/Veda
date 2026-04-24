package com.veda.app.ui.main;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veda.app.data.entity.NoteListItem;
import com.veda.app.databinding.ItemNoteBinding;

import java.text.DateFormat;
import java.util.Date;

public class NotesAdapter extends ListAdapter<NoteListItem, NotesAdapter.VH> {

    public interface Listener {
        void onNoteClick(long noteId);
    }

    private final Listener listener;

    public NotesAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<NoteListItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull NoteListItem oldItem, @NonNull NoteListItem newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull NoteListItem oldItem, @NonNull NoteListItem newItem) {
            return safeEq(oldItem.title, newItem.title)
                    && oldItem.updatedAt == newItem.updatedAt
                    && safeEq(oldItem.subjectName, newItem.subjectName)
                    && safeEq(oldItem.subjectColor, newItem.subjectColor);
        }

        private boolean safeEq(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding b = ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NoteListItem item = getItem(position);

        holder.b.title.setText(item.title == null || item.title.isBlank() ? "(без названия)" : item.title);

        String subject = item.subjectName == null ? "Без предмета" : item.subjectName;
        holder.b.subject.setText(subject);

        int color = item.subjectColor == null ? 0xFF9E9E9E : item.subjectColor;
        holder.b.dot.setBackgroundTintList(ColorStateList.valueOf(color));

        String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(item.updatedAt));
        holder.b.updatedAt.setText(date);

        holder.b.getRoot().setOnClickListener(v -> listener.onNoteClick(item.id));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemNoteBinding b;

        VH(ItemNoteBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}