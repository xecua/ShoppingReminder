package dev.koffein.shoppingreminder

import android.app.Application
import com.google.firebase.FirebaseApp

class CustomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}