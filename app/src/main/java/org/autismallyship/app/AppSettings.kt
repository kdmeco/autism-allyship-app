package org.autismallyship.app

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.io.File

class AppSettings(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    fun hapticsAllowed(): Boolean = !isSensoryMode() && isHapticsPreferenceOn()

    // Stored preference only, ignoring sensory mode. Settings shows this so the switch still
    // reflects the choice when sensory mode is temporarily overriding the effect.
    fun isHapticsPreferenceOn(): Boolean = prefs.getString(KEY_HAPTICS, ON) == ON

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

    fun languageTag(): String = prefs.getString(KEY_LANGUAGE, LANGUAGE_EN) ?: LANGUAGE_EN

    fun saveLanguageTag(tag: String) {
        prefs.edit().putString(KEY_LANGUAGE, tag).apply()
        applyLanguagePreference(tag)
    }

    fun applyLanguagePreference() {
        applyLanguagePreference(languageTag())
    }

    private fun applyLanguagePreference(tag: String) {
        val locales = when (tag) {
            LANGUAGE_AF, LANGUAGE_ST -> LocaleListCompat.forLanguageTags(tag)
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun isFirstRunCompleted(): Boolean = prefs.getString(KEY_FIRST_RUN, "") == FIRST_RUN_DONE

    fun setFirstRunCompleted() {
        prefs.edit().putString(KEY_FIRST_RUN, FIRST_RUN_DONE).apply()
    }

    // Clears the default cache directory and any Glide disk cache. TicketStore and these settings
    // prefs are left alone on purpose.
    fun clearCachedData(onDone: () -> Unit) {
        Thread {
            deleteRecursivelyQuietly(appContext.cacheDir)
            appContext.externalCacheDir?.let { deleteRecursivelyQuietly(it) }
            try {
                com.bumptech.glide.Glide.get(appContext).clearDiskCache()
            } catch (_: Exception) {
                // Glide may not be initialised yet; cache dir clear above still ran.
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post(onDone)
        }.start()
    }

    private fun deleteRecursivelyQuietly(file: File) {
        try {
            file.listFiles()?.forEach { child ->
                if (child.isDirectory) deleteRecursivelyQuietly(child) else child.delete()
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        const val LANGUAGE_EN = "en"
        const val LANGUAGE_AF = "af"
        const val LANGUAGE_ST = "st"

        private const val PREFS_NAME = "aaf-settings"
        private const val ON = "on"
        private const val OFF = "off"
        private const val FIRST_RUN_DONE = "done"

        private const val KEY_THEME = "aaf-theme"
        private const val KEY_SENSORY = "aaf-sensory-mode"
        private const val KEY_HAPTICS = "aaf-haptics"
        private const val KEY_POP_SOUND = "aaf-pop-sound"
        private const val KEY_LANGUAGE = "aaf-language"
        private const val KEY_FIRST_RUN = "aaf-first-run"
    }
}

fun AppCompatActivity.applyAppTheme(settings: AppSettings) {
    if (settings.isSensoryMode()) {
        setTheme(R.style.Theme_AutismAllyship_Sensory)
    } else {
        setTheme(R.style.Theme_AutismAllyship)
    }
}
