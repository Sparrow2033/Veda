package com.veda.app.ui.homework;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.veda.app.R;
import com.veda.app.data.entity.HomeworkEntity;
import com.veda.app.data.entity.SubjectEntity;
import com.veda.app.databinding.ItemHomeworkBinding;
import com.veda.app.databinding.ItemHomeworkHeaderBinding;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public final class HomeworkAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onItemClick(@NonNull HomeworkEntity item);
        void onItemLongClick(@NonNull HomeworkEntity item);
    }

    private static final DiffUtil.ItemCallback<HomeworkRowItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull HomeworkRowItem oldItem, @NonNull HomeworkRowItem newItem) {
            return oldItem.type == newItem.type && oldItem.stableId == newItem.stableId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull HomeworkRowItem oldItem, @NonNull HomeworkRowItem newItem) {
            if (oldItem.type != newItem.type) return false;

            if (oldItem.type == HomeworkRowItem.TYPE_HEADER) {
                return Objects.equals(oldItem.headerText, newItem.headerText);
            }

            return taskEquals(oldItem.task, newItem.task);
        }

        private boolean taskEquals(@Nullable HomeworkEntity oldTask, @Nullable HomeworkEntity newTask) {
            if (oldTask == newTask) return true;
            if (oldTask == null || newTask == null) return false;

            return oldTask.id == newTask.id
                    && oldTask.subjectId == newTask.subjectId
                    && oldTask.dueDate == newTask.dueDate
                    && oldTask.status == newTask.status
                    && oldTask.priority == newTask.priority
                    && Objects.equals(oldTask.title, newTask.title)
                    && Objects.equals(oldTask.description, newTask.description);
        }
    };

    private final Listener listener;
    private final SubjectIndex subjectIndex = new SubjectIndex();
    private final AsyncListDiffer<HomeworkRowItem> differ = new AsyncListDiffer<>(this, DIFF);

    public HomeworkAdapter(@NonNull Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(@NonNull List<HomeworkRowItem> newItems) {
        differ.submitList(newItems);
    }

    public void setSubjects(@Nullable List<SubjectEntity> subjects) {
        subjectIndex.setSubjects(subjects);
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public int getItemViewType(int position) {
        return differ.getCurrentList().get(position).type;
    }

    @Override
    public long getItemId(int position) {
        return differ.getCurrentList().get(position).stableId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == HomeworkRowItem.TYPE_HEADER) {
            ItemHomeworkHeaderBinding binding = ItemHomeworkHeaderBinding.inflate(inf, parent, false);
            return new HeaderVH(binding);
        }
        ItemHomeworkBinding binding = ItemHomeworkBinding.inflate(inf, parent, false);
        return new TaskVH(binding, listener, subjectIndex);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HomeworkRowItem row = differ.getCurrentList().get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind(row);
        } else if (holder instanceof TaskVH) {
            ((TaskVH) holder).bind(row.task);
        }
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    static final class HeaderVH extends RecyclerView.ViewHolder {

        private final ItemHomeworkHeaderBinding binding;

        HeaderVH(@NonNull ItemHomeworkHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull HomeworkRowItem row) {
            binding.txtHeader.setText(row.headerText == null ? "" : row.headerText);
        }
    }

    static final class TaskVH extends RecyclerView.ViewHolder {

        private final ItemHomeworkBinding binding;
        private final Listener listener;
        private final SubjectIndex subjectIndex;

        private HomeworkEntity bound;

        TaskVH(@NonNull ItemHomeworkBinding binding,
               @NonNull Listener listener,
               @NonNull SubjectIndex subjectIndex) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.subjectIndex = subjectIndex;

            binding.getRoot().setOnClickListener(v -> {
                if (bound != null) this.listener.onItemClick(bound);
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (bound != null) this.listener.onItemLongClick(bound);
                return true;
            });
        }

        void bind(@Nullable HomeworkEntity item) {
            bound = item;
            if (item == null) {
                clear();
                return;
            }

            Context context = itemView.getContext();

            String titleText = safe(item.title);
            binding.txtTitle.setText(titleText);
            binding.txtTitle.setTypeface(binding.txtTitle.getTypeface(),
                    item.status == HomeworkEntity.STATUS_DONE ? Typeface.NORMAL : Typeface.BOLD);

            if (item.status == HomeworkEntity.STATUS_DONE) {
                binding.txtTitle.setPaintFlags(binding.txtTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.txtTitle.setAlpha(0.82f);
            } else {
                binding.txtTitle.setPaintFlags(binding.txtTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                binding.txtTitle.setAlpha(1f);
            }

            String description = safe(item.description).trim();
            boolean hasDescription = !description.isEmpty();
            binding.txtDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
            binding.txtDescription.setText(description);

            String subjectName = subjectIndex.name(item.subjectId);
            boolean hasSubject = !subjectName.isEmpty();
            binding.subjectGroup.setVisibility(hasSubject ? View.VISIBLE : View.GONE);
            binding.txtSubject.setText(subjectName);
            binding.subjectDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(subjectIndex.color(item.subjectId)));

            binding.chipStatus.setText(statusLabel(context, item.status));
            binding.chipStatus.setBackgroundResource(statusBackground(item.status));

            binding.chipPriority.setText(priorityLabel(context, item.priority));
            binding.chipPriority.setBackgroundResource(priorityBackground(item.priority));

            if (item.dueDate > 0) {
                String due = DateFormat.getDateFormat(context).format(new Date(item.dueDate));
                binding.txtDue.setText(context.getString(R.string.homework_due_date, due));
            } else {
                binding.txtDue.setText(R.string.homework_no_deadline);
            }
        }

        private void clear() {
            binding.txtTitle.setText("");
            binding.txtDescription.setText("");
            binding.txtDue.setText("");
            binding.txtSubject.setText("");
            binding.subjectGroup.setVisibility(View.GONE);
            binding.txtTitle.setPaintFlags(binding.txtTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.txtTitle.setAlpha(1f);
        }

        @DrawableRes
        private static int statusBackground(int status) {
            switch (status) {
                case HomeworkEntity.STATUS_IN_PROGRESS:
                    return R.drawable.bg_homework_chip_status_progress;
                case HomeworkEntity.STATUS_DONE:
                    return R.drawable.bg_homework_chip_status_done;
                case HomeworkEntity.STATUS_CANCELED:
                    return R.drawable.bg_homework_chip_status_canceled;
                case HomeworkEntity.STATUS_TODO:
                default:
                    return R.drawable.bg_homework_chip_status_todo;
            }
        }

        @DrawableRes
        private static int priorityBackground(int priority) {
            switch (priority) {
                case HomeworkEntity.PRIORITY_LOW:
                    return R.drawable.bg_homework_chip_priority_low;
                case HomeworkEntity.PRIORITY_HIGH:
                    return R.drawable.bg_homework_chip_priority_high;
                case HomeworkEntity.PRIORITY_MEDIUM:
                default:
                    return R.drawable.bg_homework_chip_priority_medium;
            }
        }

        @NonNull
        private static String statusLabel(@NonNull Context context, int status) {
            switch (status) {
                case HomeworkEntity.STATUS_IN_PROGRESS:
                    return context.getString(R.string.homework_status_progress);
                case HomeworkEntity.STATUS_DONE:
                    return context.getString(R.string.homework_status_done);
                case HomeworkEntity.STATUS_CANCELED:
                    return context.getString(R.string.homework_status_canceled);
                case HomeworkEntity.STATUS_TODO:
                default:
                    return context.getString(R.string.homework_status_todo);
            }
        }

        @NonNull
        private static String priorityLabel(@NonNull Context context, int priority) {
            switch (priority) {
                case HomeworkEntity.PRIORITY_LOW:
                    return context.getString(R.string.homework_priority_low);
                case HomeworkEntity.PRIORITY_HIGH:
                    return context.getString(R.string.homework_priority_high);
                case HomeworkEntity.PRIORITY_MEDIUM:
                default:
                    return context.getString(R.string.homework_priority_medium);
            }
        }

        @NonNull
        private static String safe(@Nullable String s) {
            return s == null ? "" : s;
        }
    }
}
