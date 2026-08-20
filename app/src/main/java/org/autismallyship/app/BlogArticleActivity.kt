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
import org.autismallyship.app.databinding.ActivityBlogArticleBinding

// The URL is passed in rather than built from a post ID here, because the website's convention
// for hiding its header and footer inside a WebView is not settled yet, see DECISIONS.md. This
// screen only knows how to show a URL, not how to construct one.
class BlogArticleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlogArticleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBlogArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.articleRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.title = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        // The toolbar control always leaves the article, no matter how far the WebView has
        // navigated inside the page. The hardware back button below is different on purpose: it
        // steps back through the page's own history first, and only leaves once that runs out.
        binding.toolbar.setNavigationOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.articleWebView.canGoBack()) {
                        binding.articleWebView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        setUpWebView()
        binding.retryButton.setOnClickListener { loadArticle() }
        loadArticle()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        // The site relies on JavaScript for translations and dark mode, so it is left on. This is
        // our own fixed article page, not arbitrary user content, which is what the lint warning
        // on this setting is guarding against.
        binding.articleWebView.settings.javaScriptEnabled = true
        binding.articleWebView.webViewClient = object : WebViewClient() {
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

    private fun loadArticle() {
        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            showFailure()
        } else {
            showLoading()
            binding.articleWebView.loadUrl(url)
        }
    }

    private fun showLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.loadingMessage.isVisible = sensoryMode
        binding.failureGroup.isVisible = false
        binding.articleWebView.isVisible = false
    }

    private fun showLoaded() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.failureGroup.isVisible = false
        binding.articleWebView.isVisible = true
    }

    private fun showFailure() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.articleWebView.isVisible = false
        binding.failureGroup.isVisible = true
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_URL = "url"

        fun newIntent(context: Context, title: String, url: String): Intent {
            return Intent(context, BlogArticleActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_URL, url)
        }
    }
}
