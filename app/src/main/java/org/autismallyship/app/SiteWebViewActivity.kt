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
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.databinding.ActivitySiteWebviewBinding

// Reusable WebView for legal pages and other fixed site paths. Same loading, sensory and back
// behaviour as BlogArticleActivity.
class SiteWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySiteWebviewBinding
    private var pagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySiteWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.siteWebviewRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val titleRes = intent.getIntExtra(EXTRA_TITLE_RES, 0)
        binding.toolbar.title = if (titleRes != 0) {
            getString(titleRes)
        } else {
            intent.getStringExtra(EXTRA_TITLE).orEmpty()
        }
        pagePath = intent.getStringExtra(EXTRA_PATH).orEmpty()

        binding.toolbar.setNavigationOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.siteWebView.canGoBack()) {
                        binding.siteWebView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        setUpWebView()
        binding.retryButton.setOnClickListener { loadPage() }
        loadPage()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        binding.siteWebView.settings.javaScriptEnabled = true
        binding.siteWebView.webViewClient = object : WebViewClient() {
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

    private fun loadPage() {
        if (pagePath.isBlank()) {
            showFailure()
            return
        }
        showLoading()
        binding.siteWebView.loadUrl(SiteUrls.page(this, pagePath))
    }

    private fun showLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.loadingMessage.isVisible = sensoryMode
        binding.failureGroup.isVisible = false
        binding.siteWebView.isVisible = false
    }

    private fun showLoaded() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.failureGroup.isVisible = false
        binding.siteWebView.isVisible = true
    }

    private fun showFailure() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.siteWebView.isVisible = false
        binding.failureGroup.isVisible = true
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TITLE_RES = "titleRes"
        private const val EXTRA_PATH = "path"

        fun newIntent(context: Context, @StringRes titleRes: Int, path: String): Intent {
            return Intent(context, SiteWebViewActivity::class.java)
                .putExtra(EXTRA_TITLE_RES, titleRes)
                .putExtra(EXTRA_PATH, path)
        }

        fun newIntent(context: Context, title: String, path: String): Intent {
            return Intent(context, SiteWebViewActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_PATH, path)
        }
    }
}
