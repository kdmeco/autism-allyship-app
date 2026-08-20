package org.autismallyship.app

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

    // Only ever invoked from a bubble tap, which cannot happen before onCreate has run and set
    // up settings, since nothing is on screen until then.
    private val adapter = BubbleAdapter(BUBBLE_COUNT) {
        if (settings.hapticsAllowed()) {
            hapticPulse()
        }
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

        binding.resetButton.setOnClickListener { adapter.reset() }
    }

    companion object {
        private const val SPAN_COUNT = 6
        private const val BUBBLE_COUNT = 30
    }
}
