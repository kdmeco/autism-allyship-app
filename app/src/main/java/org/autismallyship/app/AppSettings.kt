package org.autismallyship.app

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun themePreference(): String = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun saveThemePreference(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        applyThemePreference(theme)
    }

    fun applyThemePreference() {
        applyThemePreference(themePreference())
    }

    private fun applyThemePreference(theme: String) {
        val mode = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun isSensoryMode(): Boolean = prefs.getString(KEY_SENSORY, OFF) == ON

    fun setSensoryMode(on: Boolean) {
        prefs.edit().putString(KEY_SENSORY, if (on) ON else OFF).apply()
    }

    fun hapticsAllowed(): Boolean = !isSensoryMode() && prefs.getString(KEY_HAPTICS, ON) == ON

    fun setHaptics(on: Boolean) {
        prefs.edit().putString(KEY_HAPTICS, if (on) ON else OFF).apply()
    }

    // The pop-it's sound effect only. Deliberately not tied to sensory mode the way haptics are:
    // the calming sounds tool exists to play sound in exactly that state, so a blanket mute would
    // break the one tool someone in sensory mode is most likely to want.
    fun popSoundAllowed(): Boolean = prefs.getString(KEY_POP_SOUND, ON) == ON

    fun setPopSound(on: Boolean) {
        prefs.edit().putString(KEY_POP_SOUND, if (on) ON else OFF).apply()
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        private const val PREFS_NAME = "aaf-settings"
        private const val ON = "on"
        private const val OFF = "off"

        private const val KEY_THEME = "aaf-theme"
        private const val KEY_SENSORY = "aaf-sensory-mode"
        private const val KEY_HAPTICS = "aaf-haptics"
        private const val KEY_POP_SOUND = "aaf-pop-sound"
    }
}

fun AppCompatActivity.applyAppTheme(settings: AppSettings) {
    if (settings.isSensoryMode()) {
        setTheme(R.style.Theme_AutismAllyship_Sensory)
    } else {
        setTheme(R.style.Theme_AutismAllyship)
    }
}
