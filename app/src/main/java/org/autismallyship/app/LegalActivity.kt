package org.autismallyship.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.autismallyship.app.databinding.ActivityLegalBinding

class LegalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLegalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLegalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.legalRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.privacyButton.setOnClickListener {
            startActivity(SiteWebViewActivity.newIntent(this, R.string.legal_privacy, "privacy.html"))
        }
        binding.termsButton.setOnClickListener {
            startActivity(SiteWebViewActivity.newIntent(this, R.string.legal_terms, "terms.html"))
        }
        binding.accessibilityButton.setOnClickListener {
            startActivity(
                SiteWebViewActivity.newIntent(this, R.string.legal_accessibility, "accessibility.html")
            )
        }
        binding.paiaButton.setOnClickListener {
            startActivity(SiteWebViewActivity.newIntent(this, R.string.legal_paia, "paia.html"))
        }
    }
}
