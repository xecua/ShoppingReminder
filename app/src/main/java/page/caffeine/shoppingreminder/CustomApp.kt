package page.caffeine.shoppingreminder

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import page.caffeine.shoppingreminder.utils.ReminderNotificationManager

@HiltAndroidApp
class CustomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        ReminderNotificationManager.createChannel(this)
    }
}