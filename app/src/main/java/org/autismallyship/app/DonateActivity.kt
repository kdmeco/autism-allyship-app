package org.autismallyship.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.autismallyship.app.databinding.ActivityDonateBinding

// Donations are made by EFT straight into the foundation's bank account. There is no payment
// gateway behind this screen and nothing here touches money, so it is a static set of details
// plus a way to copy them. Mirrors the EFT block on donate.html.
//
// The details are held as constants rather than read from Firestore on purpose. Bank details
// that arrive over the network are bank details an attacker can change; these ship with the
// build, so altering them takes a release. The same reasoning is why the warning below is on
// the screen at all.
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

        binding.bankAccountName.text = ACCOUNT_NAME
        binding.bankName.text = BANK_NAME
        binding.bankAccountNumber.text = ACCOUNT_NUMBER
        binding.bankBranchCode.text = BRANCH_CODE

        binding.copyDetailsButton.setOnClickListener { copyDetails() }

        // The website form is where a donor can optionally leave their details so the
        // foundation can match the transfer. Not rebuilt natively: it is one form, used once,
        // and the WebView already carries the site's own validation and privacy wording.
        binding.websiteDonateButton.setOnClickListener {
            startActivity(SiteWebViewActivity.newIntent(this, R.string.donate_title, "donate.html"))
        }
    }

    private fun copyDetails() {
        val details = getString(
            R.string.donate_copy_payload,
            ACCOUNT_NAME,
            BANK_NAME,
            ACCOUNT_NUMBER,
            BRANCH_CODE,
            getString(R.string.donate_bank_reference_format),
        )
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.donate_title), details))

        // Android 13 and up shows its own copy confirmation, so a toast there would say the
        // same thing twice.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.donate_details_copied, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        // Confirmed against the foundation's own banking details, 2 September 2026.
        // These are duplicated in donate.html and shop.html; change all three together.
        private const val ACCOUNT_NAME = "Active Autism Neuro-Diversity"
        private const val BANK_NAME = "FNB Gold Business Account"
        private const val ACCOUNT_NUMBER = "630 736 70 119"
        private const val BRANCH_CODE = "210 835"
    }
}
