package com.jesunez.recetairo

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RecetairoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        try {
            val firebaseApp = FirebaseApp.getInstance()
            Timber.d("FirebaseApp initialized: ${firebaseApp.options.projectId}")
        } catch (e: IllegalStateException) {
            Timber.e(e, "FirebaseApp failed to initialize")
        }
    }
}
