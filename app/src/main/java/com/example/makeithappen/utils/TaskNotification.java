package com.example.makeithappen.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.makeithappen.R;
import com.example.makeithappen.activities.main.MainActivity;
import com.example.makeithappen.models.Task;
import com.example.makeithappen.activities.UpdateTaskActivity;


public class TaskNotification extends BroadcastReceiver {
    public static final String notificationID = "notificationID";
    public static final String channelID = "channel1";
    public static final String titleExtra = "titleExtra";
    public static final String messageExtra = "messageExtra";


    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(notificationID, 1);
        Intent openIntent = new Intent(context, UpdateTaskActivity.class);
        Task task = intent.getParcelableExtra("task");
        openIntent.putExtra("task", task);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context).addNextIntentWithParentStack(openIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(intent.getStringExtra(titleExtra))
                .setContentText(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingIntent)
                .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

}
