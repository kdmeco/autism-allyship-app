package org.autismallyship.app

import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.autismallyship.app.databinding.ActivityCalmingSoundsBinding
import org.autismallyship.app.databinding.ItemSoundBinding

// Nothing here ever starts on its own. RULES-APP.md is explicit that sound only plays when the
// user presses play, so there is no autoplay, no resume on open and no starting state.
class CalmingSoundsActivity : AppCompatActivity() {

    private data class Sound(val nameRes: Int, val rawRes: Int)

    private lateinit var binding: ActivityCalmingSoundsBinding
    private lateinit var audioManager: AudioManager
    private var player: MediaPlayer? = null
    private var playingIndex: Int? = null
    private var focusRequest: AudioFocusRequest? = null
    private var fade: ValueAnimator? = null
    private var currentVolume = 0f
    private val rows = mutableListOf<ItemSoundBinding>()

    private val sounds = listOf(
        Sound(R.string.sounds_rain, R.raw.rain),
        Sound(R.string.sounds_ocean, R.raw.ocean),
        Sound(R.string.sounds_waterfall, R.raw.waterfall)
    )

    // Another app taking the audio output is handled the way Android expects: a short
    // interruption ducks this down rather than stopping it, anything longer pauses it. Coming
    // back only restores the volume, it never restarts a sound the user had stopped.
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> stopPlayback()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player?.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                cancelFade()
                fadeTo(targetVolume(DUCK_MULTIPLIER), DUCK_FADE_MS)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (playingIndex != null) player?.start()
                cancelFade()
                fadeTo(targetVolume(), DUCK_FADE_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalmingSoundsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        ViewCompat.setOnApplyWindowInsetsListener(binding.soundsRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.title = getString(R.string.sensory_sounds_name)
        binding.toolbar.setNavigationOnClickListener { finish() }

        buildRows()

        binding.volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) applyVolume()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun buildRows() {
        val inflater = LayoutInflater.from(this)
        sounds.forEachIndexed { index, sound ->
            val row = ItemSoundBinding.inflate(inflater, binding.soundList, false)
            row.soundName.setText(sound.nameRes)
            row.root.setOnClickListener { toggle(index) }
            binding.soundList.addView(row.root)
            rows.add(row)
        }
        refreshRows()
    }

    private fun toggle(index: Int) {
        if (playingIndex == index) {
            stopPlayback()
        } else {
            play(index)
        }
        refreshRows()
    }

    private fun play(index: Int) {
        cancelFade()
        releasePlayer()

        if (!requestFocus()) return

        player = MediaPlayer.create(this, sounds[index].rawRes)?.apply {
            isLooping = true
            // Keeps the CPU alive so the sound carries on when the screen goes off, which is
            // exactly when someone is most likely to be lying down listening to it.
            setWakeMode(this@CalmingSoundsActivity, PowerManager.PARTIAL_WAKE_LOCK)
            setVolume(0f, 0f)
            start()
        }
        playingIndex = if (player == null) null else index
        if (player != null) fadeTo(targetVolume(), FADE_IN_MS)
    }

    private fun stopPlayback() {
        cancelFade()
        val current = player
        if (current == null) {
            finishStop()
            return
        }
        // Faded rather than cut, because an abrupt silence is its own jolt on a tool whose whole
        // purpose is to settle someone.
        fadeTo(0f, FADE_OUT_MS) { finishStop() }
    }

    private fun finishStop() {
        releasePlayer()
        playingIndex = null
        abandonFocus()
        refreshRows()
    }

    private fun releasePlayer() {
        player?.apply {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    private fun fadeTo(target: Float, durationMs: Long, onEnd: (() -> Unit)? = null) {
        val current = player ?: run {
            onEnd?.invoke()
            return
        }
        val from = currentVolume
        fade = ValueAnimator.ofFloat(from, target).apply {
            duration = durationMs
            addUpdateListener {
                val level = it.animatedValue as Float
                currentVolume = level
                current.setVolume(level, level)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    private fun cancelFade() {
        fade?.removeAllListeners()
        fade?.cancel()
        fade = null
    }

    private fun targetVolume(multiplier: Float = 1f): Float =
        binding.volumeSlider.progress / 100f * multiplier

    private fun applyVolume(multiplier: Float = 1f) {
        val level = targetVolume(multiplier)
        currentVolume = level
        player?.setVolume(level, level)
    }

    private fun requestFocus(): Boolean {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun refreshRows() {
        rows.forEachIndexed { index, row ->
            val isPlaying = playingIndex == index
            row.playIcon.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            val label = getString(sounds[index].nameRes)
            row.root.contentDescription = getString(
                if (isPlaying) R.string.cd_sounds_pause else R.string.cd_sounds_play,
                label
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelFade()
        releasePlayer()
        abandonFocus()
    }

    companion object {
        private const val DUCK_MULTIPLIER = 0.2f
        private const val FADE_IN_MS = 1200L
        private const val FADE_OUT_MS = 800L
        private const val DUCK_FADE_MS = 300L
    }
}
