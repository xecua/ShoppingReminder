package dev.koffein.shoppingreminder

import android.app.Application
import com.google.firebase.FirebaseApp
import dev.koffein.shoppingreminder.utils.ReminderNotificationManager

class CustomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        ReminderNotificationManager.createChannel(this)
    }
}