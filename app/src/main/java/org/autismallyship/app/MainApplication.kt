package org.autismallyship.app

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val settings = AppSettings(this)
        settings.applyThemePreference()
        settings.applyLanguagePreference()

        Firebase.firestore.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {})
        }
    }
}
