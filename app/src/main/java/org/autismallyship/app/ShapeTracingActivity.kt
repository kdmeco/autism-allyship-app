package org.autismallyship.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.databinding.ActivityShapeTracingBinding

class ShapeTracingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShapeTracingBinding
    private lateinit var settings: AppSettings
    private var lastHapticAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = AppSettings(this)
        applyAppTheme(settings)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityShapeTracingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.tracingRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.title = getString(R.string.sensory_tracing_name)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.hapticsSwitch.isChecked = settings.hapticsAllowed()
        binding.hapticsSwitch.setOnCheckedChangeListener { _, checked -> settings.setHaptics(checked) }

        binding.tracingView.onNewTouch = { onDrawTouch() }
        binding.tracingView.onComplete = { binding.completionText.isVisible = true }

        binding.shapeChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val shape = when (checkedIds.firstOrNull()) {
                binding.chipSquare.id -> TracingView.Shape.SQUARE
                binding.chipStar.id -> TracingView.Shape.STAR
                binding.chipSpiral.id -> TracingView.Shape.SPIRAL
                binding.chipWave.id -> TracingView.Shape.WAVE
                else -> TracingView.Shape.CIRCLE
            }
            binding.tracingView.shape = shape
            binding.completionText.isVisible = false
        }

        binding.clearButton.setOnClickListener {
            binding.tracingView.clear()
            binding.completionText.isVisible = false
        }
    }

    // A pulse on every newly touched sample would be a buzz storm while dragging fast across a
    // dense outline, so this holds pulses to roughly one every 100ms regardless of how many
    // points a single move event covers.
    private fun onDrawTouch() {
        if (!settings.hapticsAllowed()) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastHapticAt >= HAPTIC_THROTTLE_MS) {
            lastHapticAt = now
            hapticPulse(10)
        }
    }

    companion object {
        private const val HAPTIC_THROTTLE_MS = 100L
    }
}
