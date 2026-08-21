package org.autismallyship.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.Timestamp
import org.autismallyship.app.data.Repository
import org.autismallyship.app.data.Submission
import org.autismallyship.app.databinding.ActivityContactBinding
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class ContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactBinding

    // Display labels mapped to the exact website option values stored in Firestore.
    private val categories by lazy {
        listOf(
            CategoryOption(getString(R.string.contact_category_general), "General"),
            CategoryOption(getString(R.string.contact_category_volunteer), "volunteer"),
            CategoryOption(getString(R.string.contact_category_partnership), "partnership"),
            CategoryOption(getString(R.string.contact_category_media), "media"),
            CategoryOption(
                getString(R.string.contact_category_resource_suggestion),
                "resource suggestion"
            ),
            CategoryOption(
                getString(R.string.contact_category_accessibility),
                "accessibility"
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.contactRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        val labels = categories.map { it.label }
        binding.categoryInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        )

        binding.whatsappButton.setOnClickListener { openWhatsApp() }
        binding.submitButton.setOnClickListener { submit() }
        binding.successDoneButton.setOnClickListener { finish() }
    }

    private fun submit() {
        val categoryLabel = binding.categoryInput.text.toString().trim()
        val categoryValue = categories.firstOrNull { it.label == categoryLabel }?.value
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val message = binding.messageInput.text.toString().trim()

        if (categoryValue == null) {
            showError(getString(R.string.contact_category_required))
            return
        }
        if (name.isBlank()) {
            showError(getString(R.string.contact_name_required))
            return
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(getString(R.string.contact_email_invalid))
            return
        }
        if (phone.isNotBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
            showError(getString(R.string.contact_phone_invalid))
            return
        }
        if (message.isBlank()) {
            showError(getString(R.string.contact_message_required))
            return
        }
        if (!binding.popiaCheckbox.isChecked) {
            showError(getString(R.string.contact_popia_required))
            return
        }

        showLoading()
        val submission = Submission(
            name = name,
            email = email,
            phone = phone,
            category = categoryValue,
            message = message,
            handled = false,
            createdAt = Timestamp.now()
        )

        Repository.sendSubmission(
            submission,
            onSuccess = {
                notifyFoundation(name, email, phone, categoryValue, message)
                showSuccess()
            },
            onError = {
                showError(getString(R.string.contact_generic_error))
            }
        )
    }

    // Same payload shape as contact.js: name, email, phone, category, message. Failure here must
    // not hide a successful Firestore write.
    private fun notifyFoundation(
        name: String,
        email: String,
        phone: String,
        category: String,
        message: String
    ) {
        Thread {
            try {
                val body = JSONObject()
                    .put("name", name)
                    .put("email", email)
                    .put("phone", phone)
                    .put("category", category)
                    .put("message", message)
                    .toString()

                val connection =
                    URL(SiteUrls.CONTACT_NOTIFY_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(connection.outputStream).use { it.write(body) }
                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun showLoading() {
        binding.contactError.isVisible = false
        binding.submitButton.isEnabled = false
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.loadingRow.isVisible = true
    }

    private fun showSuccess() {
        binding.loadingRow.isVisible = false
        binding.formGroup.isVisible = false
        binding.successGroup.isVisible = true
    }

    private fun showError(message: String) {
        binding.loadingRow.isVisible = false
        binding.submitButton.isEnabled = true
        binding.contactError.text = message
        binding.contactError.isVisible = true
    }

    private fun openWhatsApp() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SiteUrls.WHATSAPP_DIRECT)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }

    private data class CategoryOption(val label: String, val value: String)

    companion object {
        private val EMAIL_PATTERN = Pattern.compile("^[^@ ]+@[^@ ]+[.][^@ ]+$")
        private val PHONE_PATTERN = Pattern.compile("^[0-9 +()-]+$")
    }
}
