package org.autismallyship.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.autismallyship.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

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

        binding.darkModeSwitch.isChecked = settings.themePreference() == AppSettings.THEME_DARK
        binding.darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            val theme = if (checked) AppSettings.THEME_DARK else AppSettings.THEME_LIGHT
            settings.saveThemePreference(theme)
        }

        binding.sensoryModeSwitch.isChecked = settings.isSensoryMode()
        binding.sensoryModeSwitch.setOnCheckedChangeListener { _, checked ->
            settings.setSensoryMode(checked)
        }

        binding.hapticsSwitch.isChecked = settings.hapticsAllowed()
        binding.hapticsSwitch.setOnCheckedChangeListener { _, checked ->
            settings.setHaptics(checked)
        }
    }
}
