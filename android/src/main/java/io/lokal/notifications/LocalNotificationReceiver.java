package io.lokal.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;

public class LocalNotificationReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Extract notification data from intent
            int notificationId = intent.getIntExtra("notification_id", 0);
            String title = intent.getStringExtra("title");
            String body = intent.getStringExtra("body");
            long scheduledTime = intent.getLongExtra("scheduled_time", 0);
            Bundle data = intent.getBundleExtra("data");
            
            // Create and show the notification
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "LocalNotifications")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(getNotificationIcon(context))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setWhen(scheduledTime);
            
            // Add any extra data to the notification
            if (data != null) {
                for (String key : data.keySet()) {
                    Object value = data.get(key);
                    if (value instanceof String) {
                        builder.getExtras().putString(key, (String) value);
                    } else if (value instanceof Double) {
                        builder.getExtras().putDouble(key, (Double) value);
                    } else if (value instanceof Boolean) {
                        builder.getExtras().putBoolean(key, (Boolean) value);
                    }
                }
            }
            
            notificationManager.notify(notificationId, builder.build());
            
        } catch (Exception e) {
            android.util.Log.e("LocalNotificationReceiver", "Failed to show scheduled notification", e);
        }
    }
    
    private int getNotificationIcon(Context context) {
        try {
            return context.getApplicationInfo().icon;
        } catch (Exception e) {
            return android.R.drawable.ic_dialog_info;
        }
    }
}
