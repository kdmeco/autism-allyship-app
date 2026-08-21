package org.autismallyship.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.databinding.ActivityShopBinding

class ShopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.shopRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.catalogueButton.setOnClickListener { openCatalogue() }
        binding.retryButton.setOnClickListener { loadShop() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.shopWebView.canGoBack()) {
                        binding.shopWebView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        setUpWebView()
        loadShop()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        binding.shopWebView.settings.javaScriptEnabled = true
        binding.shopWebView.webViewClient = object : WebViewClient() {
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

    private fun loadShop() {
        showLoading()
        binding.shopWebView.loadUrl(SiteUrls.page(this, "shop.html"))
    }

    private fun openCatalogue() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SiteUrls.WHATSAPP_CATALOGUE)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.loadingMessage.isVisible = sensoryMode
        binding.failureGroup.isVisible = false
        binding.shopWebView.isVisible = false
    }

    private fun showLoaded() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.failureGroup.isVisible = false
        binding.shopWebView.isVisible = true
    }

    private fun showFailure() {
        binding.loadingSpinner.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.shopWebView.isVisible = false
        binding.failureGroup.isVisible = true
    }
}
