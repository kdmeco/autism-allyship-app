package org.autismallyship.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.autismallyship.app.databinding.ActivityBreathingBinding

// Sensory mode allows no animation and no haptics anywhere in the app, so this screen runs the
// same timed cycle in both modes but only lets it touch the screen and the motor when sensory
// mode is off. The phase text updates either way, so the tool stays fully usable rather than
// disappearing exactly when someone is most likely to reach for it.
class BreathingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBreathingBinding
    private lateinit var settings: AppSettings
    private var animator: ValueAnimator? = null
    private var growing = true
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = AppSettings(this)
        applyAppTheme(settings)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBreathingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.breathingRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.title = getString(R.string.sensory_breathing_name)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.breathingCircle.scaleX = MIN_SCALE
        binding.breathingCircle.scaleY = MIN_SCALE

        // Same pattern as SettingsActivity: the switch shows the effective value, sensory mode
        // included, and writes the underlying preference. One stored setting, read the same way
        // in both places, rather than a second haptics value that could disagree with it.
        binding.hapticsSwitch.isChecked = settings.hapticsAllowed()
        binding.hapticsSwitch.setOnCheckedChangeListener { _, checked -> settings.setHaptics(checked) }

        binding.paceSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && running) restart()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        binding.startPauseButton.setOnClickListener {
            if (running) stop() else start()
        }
    }

    private fun cycleMs(): Long {
        val progress = binding.paceSlider.progress
        return PACE_SLOW_MS - (progress.toDouble() / binding.paceSlider.max * (PACE_SLOW_MS - PACE_FAST_MS)).toLong()
    }

    private fun start() {
        running = true
        binding.startPauseButton.setText(R.string.breathing_pause)
        growing = true
        showPhase()

        val sensoryMode = settings.isSensoryMode()

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = cycleMs() / 2
            interpolator = AccelerateDecelerateInterpolator()
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE

            // In sensory mode this animator drives only the phase timing, nothing on screen. No
            // update listener means no property changes, so nothing moves, matching the same
            // hard limit that already governs the rest of the app.
            if (!sensoryMode) {
                addUpdateListener {
                    val fraction = it.animatedValue as Float
                    val scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * fraction
                    binding.breathingCircle.scaleX = scale
                    binding.breathingCircle.scaleY = scale
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationRepeat(animation: Animator) {
                    growing = !growing
                    showPhase()
                }
            })
        }
        animator = anim
        anim.start()
    }

    private fun restart() {
        animator?.cancel()
        start()
    }

    private fun stop() {
        running = false
        animator?.cancel()
        animator = null
        binding.startPauseButton.setText(R.string.breathing_start)
        binding.phaseText.setText(R.string.breathing_ready)
        binding.breathingCircle.scaleX = MIN_SCALE
        binding.breathingCircle.scaleY = MIN_SCALE
    }

    private fun showPhase() {
        binding.phaseText.setText(if (growing) R.string.breathing_phase_in else R.string.breathing_phase_out)
        if (settings.hapticsAllowed()) {
            hapticPulse()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        animator?.cancel()
    }

    companion object {
        private const val MIN_SCALE = 0.6f
        private const val MAX_SCALE = 1f

        // Half-cycle range, so a full inhale-exhale pair runs 6 to 16 seconds depending on pace.
        private const val PACE_SLOW_MS = 16000L
        private const val PACE_FAST_MS = 6000L
    }
}
