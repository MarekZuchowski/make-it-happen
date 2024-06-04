package com.example.makeithappen.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.makeithappen.models.Task;

public class NotificationScheduler {
    private final Context context;
    private final AlarmManager alarmManager;

    public NotificationScheduler(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleNotification(Task task, int notificationTime) {
        Intent intent = new Intent(context, TaskNotification.class);
        String title = "Upcoming task";
        String message = task.getTitle();
        intent.putExtra(TaskNotification.titleExtra, title);
        intent.putExtra(TaskNotification.messageExtra, message);
        intent.putExtra("task", task);
        intent.putExtra(TaskNotification.notificationID, task.getNotification());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.getNotification(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        long time = task.getCompletionDate().getTime() - notificationTime;
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }

    public void cancelNotification(Task task) {
        Intent intent = new Intent(context, TaskNotification.class);
        String title = "Upcoming task";
        String message = task.getTitle();
        intent.putExtra(TaskNotification.titleExtra, title);
        intent.putExtra(TaskNotification.messageExtra, message);
        intent.putExtra(TaskNotification.notificationID, task.getNotification());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.getNotification(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if(pendingIntent != null)
            alarmManager.cancel(pendingIntent);
    }

}
