package com.example.makeithappen.activities.main;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.makeithappen.R;
import com.example.makeithappen.activities.UpdateTaskActivity;
import com.example.makeithappen.databinding.TaskRowBinding;
import com.example.makeithappen.models.Task;
import com.example.makeithappen.utils.DBHelper;
import com.example.makeithappen.utils.NotificationScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Task> tasks;
    private boolean showCompletedTask;
    private int notificationOption;
    private TextView noTasksTextView;
    private final int[] notificationOptions = new int[] {300000, 900000, 1800000, 3600000, 86400000};
    private final NotificationScheduler notificationScheduler;

    public TaskAdapter(Context context, ArrayList<Task> tasks) {
        this.context = context;
        this.tasks = tasks;
        notificationScheduler = new NotificationScheduler(context);
    }

    void deleteAllTasks() {
        int size = getItemCount();
        tasks.clear();
        notifyItemRangeRemoved(0, size);
    }

    void setFilteredTasks(ArrayList<Task> filteredTasks) {
        tasks = filteredTasks;
        notifyDataSetChanged();
    }

    void setNoTasksTextView(TextView textView) {
        this.noTasksTextView = textView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TaskRowBinding binding = TaskRowBinding.inflate(LayoutInflater.from(context), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) holder.binding.taskCardView.getLayoutParams();
        if(position == tasks.size() - 1) {
            layoutParams.bottomMargin = 250;
            layoutParams.topMargin = 50;
            holder.binding.taskCardView.setLayoutParams(layoutParams);
        }
        else {
            layoutParams.topMargin = 50;
            layoutParams.bottomMargin = 0;
            holder.binding.taskCardView.setLayoutParams(layoutParams);
        }

        holder.binding.taskTitleText.setText(tasks.get(position).getTitle());
        holder.binding.taskDescriptionText.setText(tasks.get(position).getDescription());
        Date taskCompletionDate = tasks.get(position).getCompletionDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat(context.getString(R.string.displayedDateFormat), Locale.getDefault());
        holder.binding.completionTimeText.setText(dateFormat.format(taskCompletionDate));

        if(tasks.get(position).getNotification()!= 0)
            holder.binding.notificationImageView.setVisibility(View.VISIBLE);
        else
            holder.binding.notificationImageView.setVisibility(View.GONE);
        if(tasks.get(position).getAttachment() != null)
            holder.binding.attachmentImageView.setVisibility(View.VISIBLE);
        else
            holder.binding.attachmentImageView.setVisibility(View.GONE);

        holder.binding.statusCheckBox.setChecked(tasks.get(position).isStatus());
        holder.binding.taskCardView.setOnClickListener(getTaskCardOnClickListener(holder));
        holder.binding.statusCheckBox.setOnClickListener(getStatusCheckBoxOnClickListener(holder));
    }

    @NonNull
    private View.OnClickListener getTaskCardOnClickListener(@NonNull ViewHolder holder) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, UpdateTaskActivity.class);
                intent.putExtra("task", tasks.get(holder.getBindingAdapterPosition()));
                intent.putExtra("time", notificationOption);
                context.startActivity(intent);
            }
        };
    }

    @NonNull
    private View.OnClickListener getStatusCheckBoxOnClickListener(@NonNull ViewHolder holder) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked = ((CheckBox) view).isChecked();

                DBHelper db = new DBHelper(context);
                Task task = tasks.get(holder.getBindingAdapterPosition());
                task.updateStatus(isChecked);
                Date currentDate = new Date();

                if (isChecked) {
                    if (!showCompletedTask) {
                        tasks.remove(task);
                        notifyItemRemoved(holder.getBindingAdapterPosition());
                        if (tasks.isEmpty())
                            noTasksTextView.setVisibility(View.VISIBLE);
                    }
                    int notificationID = task.getNotification();
                    if (notificationID != 0)
                        notificationScheduler.cancelNotification(task);
                } else {
                    if (task.getNotification() != 0 && task.getCompletionDate().compareTo(currentDate) > 0) {
                        notificationScheduler.scheduleNotification(task, notificationOptions[notificationOption]);
                    }
                }

                db.updateTask(task);
            }
        };
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TaskRowBinding binding;

        public ViewHolder(TaskRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public void setShowCompletedTask(boolean showCompletedTask) {
        this.showCompletedTask = showCompletedTask;
    }

    public void setNotificationOption(int notificationOption) {
        this.notificationOption = notificationOption;
    }

}
