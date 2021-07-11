package dev.koffein.shoppingreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dev.koffein.shoppingreminder.utils.ReminderNotificationManager

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    init {
        Log.d(TAG, "receiver invoked")
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "I got $intent in $context")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val error = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, error)
            return
        }
        if (geofencingEvent.geofenceTransition != Geofence.GEOFENCE_TRANSITION_DWELL) {
            Log.e(
                TAG,
                "Unexpected type of transition notification: ${geofencingEvent.geofenceTransition}"
            )
            return
        }

        val name = intent.getStringExtra("name")!!
        val description = intent.getStringExtra("description")!!
        val id = intent.getStringExtra("id")!!
        val notification =
            ReminderNotificationManager.createNotification(context, name, description)
        Log.d(TAG, "and created notification $notification")
        val notificationId = id.hashCode()
        ReminderNotificationManager.notify(context, notificationId, notification)
    }

    companion object {
        const val TAG = "GeofenceReceiver"
    }
}