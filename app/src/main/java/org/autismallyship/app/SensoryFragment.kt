package org.autismallyship.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.autismallyship.app.databinding.FragmentSensoryBinding

class SensoryFragment : Fragment() {

    private var binding: FragmentSensoryBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSensoryBinding.inflate(inflater, container, false)
        this.binding = binding

        // Read once here rather than in onResume. MainActivity already recreates itself whenever
        // sensory mode changes, per its own onResume check, so this Fragment is always freshly
        // built with the current value by the time anyone sees it.
        binding.sensoryModeBanner.isVisible = AppSettings(requireContext()).isSensoryMode()

        // A card not yet built opens PlaceholderActivity, the same pattern the More sheet uses.
        // Swapping a card to its real Activity is a one-line change when that tool lands.
        setUpCard(binding.breathingCard, R.string.sensory_breathing_name, R.string.sensory_breathing_desc) {
            startActivity(Intent(requireContext(), BreathingActivity::class.java))
        }
        setUpCard(binding.popItCard, R.string.sensory_pop_it_name, R.string.sensory_pop_it_desc) {
            startActivity(Intent(requireContext(), PopItActivity::class.java))
        }
        setUpCard(binding.soundsCard, R.string.sensory_sounds_name, R.string.sensory_sounds_desc) {
            startActivity(Intent(requireContext(), CalmingSoundsActivity::class.java))
        }
        setUpCard(binding.tracingCard, R.string.sensory_tracing_name, R.string.sensory_tracing_desc) {
            startActivity(Intent(requireContext(), ShapeTracingActivity::class.java))
        }

        return binding.root
    }

    private fun setUpCard(card: View, nameRes: Int, descRes: Int, onClick: () -> Unit) {
        card.contentDescription = getString(R.string.cd_sensory_tool, getString(nameRes), getString(descRes))
        card.setOnClickListener { onClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        const val TAG = "SensoryFragment"
    }
}
