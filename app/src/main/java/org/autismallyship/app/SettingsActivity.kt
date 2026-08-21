package org.autismallyship.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import org.autismallyship.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

    private val languageLabels by lazy {
        listOf(
            getString(R.string.language_english),
            getString(R.string.language_afrikaans),
            getString(R.string.language_sesotho)
        )
    }

    private val languageTags = listOf(
        AppSettings.LANGUAGE_EN,
        AppSettings.LANGUAGE_AF,
        AppSettings.LANGUAGE_ST
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = AppSettings(this)
        applyAppTheme(settings)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        setUpLanguage()
        setUpTheme()
        setUpSwitches()
        setUpActions()
        showVersion()
        showStaffSignOutIfNeeded()
    }

    private fun setUpLanguage() {
        val field = binding.languageInput
        field.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, languageLabels))
        val index = languageTags.indexOf(settings.languageTag()).coerceAtLeast(0)
        field.setText(languageLabels[index], false)
        field.setOnItemClickListener { _, _, position, _ ->
            settings.saveLanguageTag(languageTags[position])
            recreate()
        }
    }

    private fun setUpTheme() {
        when (settings.themePreference()) {
            AppSettings.THEME_LIGHT -> binding.themeLight.isChecked = true
            AppSettings.THEME_DARK -> binding.themeDark.isChecked = true
            else -> binding.themeSystem.isChecked = true
        }
        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.theme_light -> AppSettings.THEME_LIGHT
                R.id.theme_dark -> AppSettings.THEME_DARK
                else -> AppSettings.THEME_SYSTEM
            }
            settings.saveThemePreference(theme)
        }
    }

    private fun setUpSwitches() {
        binding.sensoryModeSwitch.isChecked = settings.isSensoryMode()
        binding.sensoryModeSwitch.setOnCheckedChangeListener { _, checked ->
            settings.setSensoryMode(checked)
            recreate()
        }

        binding.hapticsSwitch.isChecked = settings.isHapticsPreferenceOn()
        binding.hapticsSwitch.setOnCheckedChangeListener { _, checked ->
            settings.setHaptics(checked)
        }
    }

    private fun setUpActions() {
        binding.notificationsButton.setOnClickListener { openNotificationSettings() }
        binding.fontSizeButton.setOnClickListener { openDisplaySettings() }
        binding.clearCacheButton.setOnClickListener {
            settings.clearCachedData {
                Toast.makeText(this, R.string.settings_cache_cleared, Toast.LENGTH_SHORT).show()
            }
        }
        binding.legalButton.setOnClickListener {
            startActivity(Intent(this, LegalActivity::class.java))
        }
        binding.staffSignOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            binding.staffSignOutButton.isVisible = false
            Toast.makeText(this, R.string.settings_signed_out, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openDisplaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showVersion() {
        val versionName = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
        binding.appVersion.text = getString(R.string.settings_version, versionName)
    }

    private fun showStaffSignOutIfNeeded() {
        binding.staffSignOutButton.isVisible = FirebaseAuth.getInstance().currentUser != null
    }
}
