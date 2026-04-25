// File: app/src/main/java/com/veda/app/ui/subjects/SubjectsAdapter.java
package com.veda.app.ui.subjects;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.databinding.ItemSubjectBinding;

public class SubjectsAdapter extends ListAdapter<SubjectEntity, SubjectsAdapter.VH> {

    public interface Listener {
        void onClick(SubjectEntity subject);
        void onLongClick(SubjectEntity subject);
    }

    private final Listener listener;

    public SubjectsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    private static final DiffUtil.ItemCallback<SubjectEntity> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull SubjectEntity oldItem, @NonNull SubjectEntity newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull SubjectEntity oldItem, @NonNull SubjectEntity newItem) {
            return safeEq(oldItem.name, newItem.name)
                    && oldItem.color == newItem.color;
        }

        private boolean safeEq(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSubjectBinding b = ItemSubjectBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SubjectEntity s = getItem(position);

        holder.b.name.setText(s.name);
        holder.b.dot.setBackgroundTintList(ColorStateList.valueOf(s.color));

        // Клик по карточке (как было)
        holder.b.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });

        // Долгий тап по карточке (как было)
        holder.b.getRoot().setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(s);
            return true;
        });

        // ✅ НОВОЕ: клик по иконке "редактировать"
        // Используем тот же обработчик, что и long-click (у тебя это открывает редактирование)
        holder.b.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onLongClick(s);
        });
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemSubjectBinding b;

        VH(ItemSubjectBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
