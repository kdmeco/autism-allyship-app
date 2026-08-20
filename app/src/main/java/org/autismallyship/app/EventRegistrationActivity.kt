package org.autismallyship.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.databinding.ActivityEventRegisterBinding
import java.util.regex.Pattern

// Free events only. A ticketed event's register button never opens this screen, and the Worker
// refuses one anyway, see DECISIONS.md and TODO-APP.md section 6 on the paid path.
class EventRegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.registerRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.registerEventTitle.text = intent.getStringExtra(EXTRA_EVENT_TITLE).orEmpty()

        binding.submitButton.setOnClickListener { submit() }
    }

    private fun submit() {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val quantityText = binding.quantityInput.text.toString().trim()

        if (name.isBlank()) {
            showError(getString(R.string.register_name_required))
            return
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(getString(R.string.register_email_invalid))
            return
        }
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
            showError(getString(R.string.register_quantity_invalid))
            return
        }

        showLoading()
        TicketRegistrationApi.register(
            eventId,
            name,
            email,
            quantity,
            onSuccess = { token ->
                startActivity(TicketDetailActivity.newIntent(this, token))
                finish()
            },
            onError = { message -> showError(message ?: getString(R.string.register_generic_error)) }
        )
    }

    private fun showLoading() {
        binding.registerError.isVisible = false
        binding.submitButton.isEnabled = false
        binding.loadingRow.isVisible = true
        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so
        // only the "booking your place" text stays up in that mode.
        binding.loadingSpinner.isVisible = !AppSettings(this).isSensoryMode()
    }

    private fun showError(message: String) {
        binding.loadingRow.isVisible = false
        binding.submitButton.isEnabled = true
        binding.registerError.text = message
        binding.registerError.isVisible = true
    }

    companion object {
        private const val EXTRA_EVENT_ID = "event_id"
        private const val EXTRA_EVENT_TITLE = "event_title"

        // Section 6's own validation, not RULES-WEBSITE.md's token rules: the same limit the
        // Worker enforces server-side, so a person is told about it before they submit rather
        // than after. See MAX_QUANTITY in the api repo's src/index.ts.
        private const val MAX_QUANTITY = 10
        private val EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun newIntent(context: Context, eventId: String, eventTitle: String): Intent {
            return Intent(context, EventRegistrationActivity::class.java)
                .putExtra(EXTRA_EVENT_ID, eventId)
                .putExtra(EXTRA_EVENT_TITLE, eventTitle)
        }
    }
}
