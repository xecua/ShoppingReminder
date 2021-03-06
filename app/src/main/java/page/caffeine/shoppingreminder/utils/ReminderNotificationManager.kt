package page.caffeine.shoppingreminder.utils

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import page.caffeine.shoppingreminder.R

object ReminderNotificationManager {
    const val TAG = "NotificationManager"
    private const val CHANNEL_ID = "page.caffeine.shoppingreminder.notification.reminder"

    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("Reminder")
            .setDescription(context.getString(R.string.reminder_notification_channel_description))
            .build()

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
        Log.d(TAG, "channel $channel created")
    }

    fun createNotification(context: Context, name: String, description: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(name)
            .setContentText(description)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    fun notify(context: Context, notificationId: Int, notification: Notification) {
        Log.d(TAG, "notifying $notification")
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}