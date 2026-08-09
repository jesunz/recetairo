package com.jesunez.recetairo

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RecetairoApplication : Application() {
    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Firebase and App Check BEFORE super.onCreate()
        // This ensures that when Hilt initializes singletons (like GenerativeModel),
        // App Check is already configured.
        try {
            FirebaseApp.initializeApp(this)
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                Timber.d("Installing AppCheck DebugProvider. Check Logcat for debug token to register in Firebase Console.")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                Timber.d("Installing AppCheck PlayIntegrityProvider")
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
            firebaseAppCheck.setTokenAutoRefreshEnabled(true)
            Timber.d("FirebaseApp and AppCheck initialized successfully")

            // Initialize Anonymous Auth to satisfy Vertex AI token requirements
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener { Timber.d("Anonymous sign-in successful") }
                    .addOnFailureListener { e -> Timber.e(e, "Anonymous sign-in failed") }
            } else {
                Timber.d("User already signed in: ${auth.currentUser?.uid}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Firebase initialization failed")
        }

        super.onCreate()
    }
}
