package org.autismallyship.app

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.autismallyship.app.databinding.ActivityFirstRunBinding

class FirstRunActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirstRunBinding
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
        binding = ActivityFirstRunBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.firstRunRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setUpLanguage()
        setUpTheme()
        binding.sensoryModeSwitch.isChecked = settings.isSensoryMode()

        binding.skipButton.setOnClickListener { finishSetup() }
        binding.continueButton.setOnClickListener {
            applyChoices()
            finishSetup()
        }
    }

    private fun setUpLanguage() {
        val field = binding.languageInput
        field.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, languageLabels))
        val index = languageTags.indexOf(settings.languageTag()).coerceAtLeast(0)
        field.setText(languageLabels[index], false)
    }

    private fun setUpTheme() {
        when (settings.themePreference()) {
            AppSettings.THEME_LIGHT -> binding.themeLight.isChecked = true
            AppSettings.THEME_DARK -> binding.themeDark.isChecked = true
            else -> binding.themeSystem.isChecked = true
        }
    }

    private fun applyChoices() {
        val label = binding.languageInput.text.toString()
        val languageIndex = languageLabels.indexOf(label).coerceAtLeast(0)
        settings.saveLanguageTag(languageTags[languageIndex])

        val theme = when (binding.themeGroup.checkedRadioButtonId) {
            R.id.theme_light -> AppSettings.THEME_LIGHT
            R.id.theme_dark -> AppSettings.THEME_DARK
            else -> AppSettings.THEME_SYSTEM
        }
        settings.saveThemePreference(theme)
        settings.setSensoryMode(binding.sensoryModeSwitch.isChecked)
    }

    private fun finishSetup() {
        settings.setFirstRunCompleted()
        startActivity(Intent(this, MainActivity::class.java))
        if (settings.isSensoryMode()) {
            overridePendingTransition(0, 0)
        }
        finish()
    }
}
