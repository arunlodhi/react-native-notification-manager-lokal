package io.lokal.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class LocalNotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val title = intent.getStringExtra("title") ?: ""
        val body = intent.getStringExtra("body") ?: ""
        val data = intent.getBundleExtra("data")

        // Create notification channel if needed
        createNotificationChannel(context)

        // Build notification
        val builder = NotificationCompat.Builder(context, "LocalNotifications")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(getNotificationIcon(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Show notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "LocalNotifications",
                "Local Notifications",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Local scheduled notifications"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotificationIcon(context: Context): Int {
        return try {
            context.applicationInfo.icon
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }
    }
}
