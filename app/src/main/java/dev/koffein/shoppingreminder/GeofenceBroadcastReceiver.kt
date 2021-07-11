package dev.koffein.shoppingreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "I got $intent")
    }

    companion object {
        const val TAG = "GeofenceReceiver"

        const val INTENT_REQUEST_CODE = 29378
    }
}