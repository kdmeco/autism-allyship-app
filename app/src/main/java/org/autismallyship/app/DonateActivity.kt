package org.autismallyship.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.databinding.ActivityDonateBinding
import java.util.Locale
import java.util.regex.Pattern

// Amounts and copy here follow donate.html and donate.js on the website: the same three preset
// amounts, and a checked POPIA consent box before the request can go anywhere. The preset chips
// are a shortcut that fills the amount field, not a separate choice, same as the website's radio
// buttons: the field itself is what gets validated and sent.
class DonateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDonateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.donateRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.chipAmount50.setOnClickListener { binding.amountInput.setText("50") }
        binding.chipAmount150.setOnClickListener { binding.amountInput.setText("150") }
        binding.chipAmount300.setOnClickListener { binding.amountInput.setText("300") }

        binding.privacyLinkButton.setOnClickListener { openPrivacyPolicy() }
        binding.submitButton.setOnClickListener { submit() }
    }

    // The legal pages are one of the three things RULES-APP.md allows a WebView for, but the
    // in-app legal screen that would host them is section 10 work, not built yet. Opening the
    // website's own page externally, the same way ResourceDetailActivity opens any other outside
    // link, is the honest stand-in until that screen exists.
    private fun openPrivacyPolicy() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$SITE_URL/privacy.html")))
        } catch (error: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }

    private fun submit() {
        val amount = binding.amountInput.text.toString().trim().toDoubleOrNull()
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val message = binding.messageInput.text.toString().trim()

        if (amount == null || amount <= 0.0) {
            showError(getString(R.string.donate_amount_required))
            return
        }
        if (name.isBlank()) {
            showError(getString(R.string.donate_name_required))
            return
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(getString(R.string.donate_email_invalid))
            return
        }
        if (!binding.popiaCheckbox.isChecked) {
            showError(getString(R.string.donate_popia_required))
            return
        }

        showLoading()
        DonationApi.initialize(
            amount,
            name,
            email,
            message,
            onSuccess = { accessCode, reference ->
                startActivity(DonationCheckoutActivity.newIntent(this, accessCode, reference))
                finish()
            },
            onError = { message2 -> showError(message2 ?: getString(R.string.donate_generic_error)) }
        )
    }

    private fun showLoading() {
        binding.donateError.isVisible = false
        binding.submitButton.isEnabled = false
        binding.loadingRow.isVisible = true
        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so
        // only the "starting your donation" text stays up in that mode.
        binding.loadingSpinner.isVisible = !AppSettings(this).isSensoryMode()
    }

    private fun showError(message: String) {
        binding.loadingRow.isVisible = false
        binding.submitButton.isEnabled = true
        binding.donateError.text = message
        binding.donateError.isVisible = true
    }

    companion object {
        private const val SITE_URL = "https://autism-allyship.pages.dev"
        private val EMAIL_PATTERN: Pattern = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

fun formatRands(amount: Double): String = String.format(Locale.US, "%.2f", amount)
