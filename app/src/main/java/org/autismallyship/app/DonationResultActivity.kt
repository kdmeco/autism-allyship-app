package org.autismallyship.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.databinding.ActivityDonationResultBinding

// Mirrors donate-result.js: this screen always calls DonationApi.verify itself, and shows exactly
// what the Worker says Paystack confirmed. Nothing from DonationCheckoutActivity is trusted here.
class DonationResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonationResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDonationResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.resultRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finishToMain() }

        verify()
    }

    private fun verify() {
        val reference = intent.getStringExtra(EXTRA_REFERENCE)
        if (reference.isNullOrBlank()) {
            showUnknown()
            return
        }

        showLoading()
        DonationApi.verify(
            reference,
            onSuccess = { result ->
                when (result.status) {
                    "success" -> showSuccess(result)
                    "failed" -> showFailed(result)
                    else -> showUnknown()
                }
            },
            onError = { showUnknown() }
        )
    }

    private fun showLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingRow.isVisible = !sensoryMode
        binding.resultGroup.isVisible = sensoryMode
        if (sensoryMode) {
            binding.resultHeading.text = getString(R.string.donate_result_loading)
            binding.resultBody.isVisible = false
            binding.resultAmount.isVisible = false
            binding.resultReference.isVisible = false
            binding.resultReason.isVisible = false
            binding.resultEftNote.isVisible = false
            binding.actionButton.isVisible = false
        }
    }

    private fun showSuccess(result: DonationResult) {
        binding.loadingRow.isVisible = false
        binding.resultGroup.isVisible = true

        binding.resultHeading.text = getString(R.string.donate_success_heading)
        binding.resultBody.text = if (result.donorName.isNotBlank()) {
            getString(R.string.donate_success_thanks_named, result.donorName)
        } else {
            getString(R.string.donate_success_thanks)
        }
        binding.resultBody.isVisible = true

        binding.resultAmount.text = getString(R.string.donate_amount_donated, formatRands(result.amount))
        binding.resultAmount.isVisible = true

        binding.resultReference.text = getString(R.string.donate_reference_label, result.reference)
        binding.resultReference.isVisible = true

        binding.resultReason.isVisible = false
        binding.resultEftNote.isVisible = false

        binding.actionButton.text = getString(R.string.donate_done)
        binding.actionButton.setOnClickListener { finishToMain() }
        binding.actionButton.isVisible = true
    }

    private fun showFailed(result: DonationResult) {
        binding.loadingRow.isVisible = false
        binding.resultGroup.isVisible = true

        binding.resultHeading.text = getString(R.string.donate_failed_heading)
        binding.resultBody.text = getString(R.string.donate_failed_text)
        binding.resultBody.isVisible = true

        binding.resultAmount.isVisible = false

        binding.resultReference.text = getString(R.string.donate_reference_label, result.reference)
        binding.resultReference.isVisible = true

        if (result.gatewayResponse != null) {
            binding.resultReason.text = getString(R.string.donate_failed_reason, result.gatewayResponse)
            binding.resultReason.isVisible = true
        } else {
            binding.resultReason.isVisible = false
        }

        binding.resultEftNote.isVisible = true

        binding.actionButton.text = getString(R.string.donate_try_again)
        binding.actionButton.setOnClickListener { startFreshDonation() }
        binding.actionButton.isVisible = true
    }

    // Covers both a network failure calling verify, and a status the Worker has not resolved to
    // success or failed yet. Re-checking with the same reference is correct in both cases: the
    // request itself may just not have landed, and verify resolves a pending Paystack status
    // itself when it does land, see DECISIONS.md and the Worker's handleDonationVerify.
    private fun showUnknown() {
        binding.loadingRow.isVisible = false
        binding.resultGroup.isVisible = true

        binding.resultHeading.text = getString(R.string.donate_unknown_heading)
        binding.resultBody.text = getString(R.string.donate_unknown_text)
        binding.resultBody.isVisible = true

        binding.resultAmount.isVisible = false
        binding.resultReference.isVisible = false
        binding.resultReason.isVisible = false
        binding.resultEftNote.isVisible = false

        binding.actionButton.text = getString(R.string.donate_check_again)
        binding.actionButton.setOnClickListener { verify() }
        binding.actionButton.isVisible = true
    }

    private fun startFreshDonation() {
        startActivity(Intent(this, DonateActivity::class.java))
        finish()
    }

    private fun finishToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val EXTRA_REFERENCE = "reference"

        fun newIntent(context: Context, reference: String): Intent {
            return Intent(context, DonationResultActivity::class.java)
                .putExtra(EXTRA_REFERENCE, reference)
        }
    }
}
