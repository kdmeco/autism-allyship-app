package org.autismallyship.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.databinding.ActivityDonationCheckoutBinding

// Loads Paystack's own hosted checkout page for the access code the Worker returned, the standard
// checkout.paystack.com/{access_code} address, which is exactly what Paystack's own
// "authorization_url" resolves to. See the group's note in DECISIONS.md on why this is a WebView
// rather than the Paystack Android SDK.
//
// There is no reliable way for this screen to know payment finished: that needs a callback URL
// configured in the Paystack dashboard, which is a console step, not something this code can do.
// So the screen relies on the person closing it once Paystack shows it is done, the same way
// closing any other WebView here works. That is safe because the next screen, DonationResultActivity,
// never trusts what happened in this WebView: it always asks the Worker to confirm the payment with
// Paystack directly, matching donate-result.js on the website.
class DonationCheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonationCheckoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDonationCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.checkoutRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { goToResult() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.checkoutWebView.canGoBack()) {
                        binding.checkoutWebView.goBack()
                    } else {
                        goToResult()
                    }
                }
            }
        )

        setUpWebView()
        binding.retryButton.setOnClickListener { loadCheckout() }
        loadCheckout()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        // Paystack's own checkout page needs JavaScript to run at all, and DOM storage because its
        // script reads from localStorage during setup. Without it, Android's WebView leaves
        // window.localStorage null, and the page throws before it can render: "Cannot read
        // properties of null (reading 'getItem')" from checkout.paystack.com's own JS, confirmed
        // in logcat.
        binding.checkoutWebView.settings.javaScriptEnabled = true
        binding.checkoutWebView.settings.domStorageEnabled = true
        binding.checkoutWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                showLoaded()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    showFailure()
                }
            }
        }
    }

    private fun loadCheckout() {
        val accessCode = intent.getStringExtra(EXTRA_ACCESS_CODE)
        if (accessCode.isNullOrBlank()) {
            showFailure()
        } else {
            showLoading()
            binding.checkoutWebView.loadUrl("$PAYSTACK_CHECKOUT_URL$accessCode")
        }
    }

    private fun goToResult() {
        val reference = intent.getStringExtra(EXTRA_REFERENCE).orEmpty()
        startActivity(DonationResultActivity.newIntent(this, reference))
        finish()
    }

    private fun showLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.loadingMessage.isVisible = sensoryMode
        binding.failureGroup.isVisible = false
        binding.checkoutWebView.isVisible = false
    }

    private fun showLoaded() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.failureGroup.isVisible = false
        binding.checkoutWebView.isVisible = true
    }

    private fun showFailure() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.checkoutWebView.isVisible = false
        binding.failureGroup.isVisible = true
    }

    companion object {
        private const val PAYSTACK_CHECKOUT_URL = "https://checkout.paystack.com/"
        private const val EXTRA_ACCESS_CODE = "access_code"
        private const val EXTRA_REFERENCE = "reference"

        fun newIntent(context: Context, accessCode: String, reference: String): Intent {
            return Intent(context, DonationCheckoutActivity::class.java)
                .putExtra(EXTRA_ACCESS_CODE, accessCode)
                .putExtra(EXTRA_REFERENCE, reference)
        }
    }
}
