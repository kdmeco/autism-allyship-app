package org.autismallyship.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.databinding.ActivityEventWebviewBinding

// Opens the website event page inside a WebView so registration behaves exactly
// as it does in the browser. After a successful booking
// the site navigates to ticket.html?token=…; that URL is intercepted here and
// handed to TicketDetailActivity, the same screen My Tickets and email links use.
class EventWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventWebviewBinding
    private var popupDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.eventWebviewRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.toolbar.setNavigationOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.eventWebView.canGoBack()) {
                        binding.eventWebView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        setUpWebView()
        binding.retryButton.setOnClickListener { loadEvent() }
        loadEvent()
    }

    override fun onDestroy() {
        popupDialog?.dismiss()
        popupDialog = null
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        // The site needs JavaScript and DOM storage. setSupportMultipleWindows is
        // for the popups event attachments open with target=_blank; without it
        // the popup never opens inside a WebView.
        val settings = binding.eventWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)

        binding.eventWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleNavigation(request.url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (handleNavigation(Uri.parse(url))) {
                    return
                }
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

        binding.eventWebView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val popup = WebView(view.context)
                popup.settings.javaScriptEnabled = true
                popup.settings.domStorageEnabled = true
                popup.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        popupView: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        if (handleNavigation(request.url)) {
                            popupDialog?.dismiss()
                            return true
                        }
                        return false
                    }
                }

                popupDialog?.dismiss()
                popupDialog = AlertDialog.Builder(this@EventWebViewActivity)
                    .setView(popup)
                    .setNegativeButton(R.string.cd_close) { dialog, _ -> dialog.dismiss() }
                    .create()
                    .also { it.show() }

                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = popup
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView) {
                popupDialog?.dismiss()
                popupDialog = null
            }
        }
    }

    private fun handleNavigation(uri: Uri): Boolean {
        if (!isTicketUrl(uri)) {
            return false
        }
        val token = uri.getQueryParameter(TOKEN_PARAMETER).orEmpty()
        if (token.isBlank()) {
            return false
        }
        startActivity(TicketDetailActivity.newIntent(this, token))
        finish()
        return true
    }

    private fun isTicketUrl(uri: Uri): Boolean {
        if (uri.scheme != "https") {
            return false
        }
        val host = uri.host.orEmpty()
        val onSite =
            host == SITE_HOST || host.endsWith(".$SITE_HOST")
        return onSite && uri.path == TICKET_PATH
    }

    private fun loadEvent() {
        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            showFailure()
        } else {
            showLoading()
            binding.eventWebView.loadUrl(url)
        }
    }

    private fun showLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.loadingMessage.isVisible = sensoryMode
        binding.failureGroup.isVisible = false
        binding.eventWebView.isVisible = false
    }

    private fun showLoaded() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.failureGroup.isVisible = false
        binding.eventWebView.isVisible = true
    }

    private fun showFailure() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.eventWebView.isVisible = false
        binding.failureGroup.isVisible = true
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_URL = "url"
        private const val SITE_HOST = "autism-allyship.pages.dev"
        private const val SITE_URL = "https://autism-allyship.pages.dev"
        private const val TICKET_PATH = "/ticket.html"
        private const val TOKEN_PARAMETER = "token"

        fun newIntent(context: Context, eventId: String, title: String): Intent {
            return Intent(context, EventWebViewActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_URL, eventUrl(context, eventId))
        }

        private fun eventUrl(context: Context, eventId: String): String {
            val nightMode =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val theme =
                if (nightMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
            val sensory = if (AppSettings(context).isSensoryMode()) "on" else "off"

            return Uri.parse(SITE_URL).buildUpon()
                .appendPath("event.html")
                .appendQueryParameter("id", eventId)
                .appendQueryParameter("app", "1")
                .appendQueryParameter("theme", theme)
                .appendQueryParameter("sensory", sensory)
                .build()
                .toString()
        }
    }
}
