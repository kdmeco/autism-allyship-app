package org.autismallyship.app

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import org.autismallyship.app.databinding.ActivityPopItBinding

class PopItActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPopItBinding
    private lateinit var settings: AppSettings

    private var soundPool: SoundPool? = null
    private var popSoundId = 0
    private var popSoundReady = false

    // Only ever invoked from a bubble tap, which cannot happen before onCreate has run and set
    // up settings, since nothing is on screen until then.
    private val adapter = BubbleAdapter(BUBBLE_COUNT) {
        if (settings.hapticsAllowed()) {
            hapticPulse()
        }
        playPop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = AppSettings(this)
        applyAppTheme(settings)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPopItBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.popItRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.title = getString(R.string.sensory_pop_it_name)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.bubbleGrid.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        binding.bubbleGrid.adapter = adapter

        binding.hapticsSwitch.isChecked = settings.hapticsAllowed()
        binding.hapticsSwitch.setOnCheckedChangeListener { _, checked -> settings.setHaptics(checked) }

        binding.soundSwitch.isChecked = settings.popSoundAllowed()
        binding.soundSwitch.setOnCheckedChangeListener { _, checked -> settings.setPopSound(checked) }

        binding.resetButton.setOnClickListener { adapter.reset() }

        setUpSound()
    }

    // SoundPool rather than MediaPlayer, because this is a short clip fired repeatedly and
    // sometimes several at once. It keeps the sample decoded in memory, so a pop lands with the
    // tap instead of a beat behind it, and overlapping taps do not cut each other off.
    private fun setUpSound() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(MAX_OVERLAPPING_POPS)
            .setAudioAttributes(attributes)
            .build()
        pool.setOnLoadCompleteListener { _, _, status -> popSoundReady = status == 0 }
        popSoundId = pool.load(this, R.raw.bubble_pop, 1)
        soundPool = pool
    }

    private fun playPop() {
        if (!popSoundReady || !settings.popSoundAllowed()) return
        soundPool?.play(popSoundId, 1f, 1f, 1, 0, 1f)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    companion object {
        private const val SPAN_COUNT = 6
        private const val BUBBLE_COUNT = 30
        private const val MAX_OVERLAPPING_POPS = 6
    }
}
