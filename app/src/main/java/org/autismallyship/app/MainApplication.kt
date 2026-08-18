package org.autismallyship.app

import android.app.Application

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppSettings(this).applyThemePreference()
    }
}
