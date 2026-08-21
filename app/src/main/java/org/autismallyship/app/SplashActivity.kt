package org.autismallyship.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.autismallyship.app.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var routed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val settings = AppSettings(this)
        applyAppTheme(settings)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Sensory mode: no transition animation. Otherwise a brief pause under one second.
        val delayMs = if (settings.isSensoryMode()) 0L else 600L
        handler.postDelayed({ routeNext(settings) }, delayMs)
    }

    private fun routeNext(settings: AppSettings) {
        if (routed) return
        routed = true
        val next = if (settings.isFirstRunCompleted()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, FirstRunActivity::class.java)
        }
        startActivity(next)
        if (settings.isSensoryMode()) {
            overridePendingTransition(0, 0)
        }
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
