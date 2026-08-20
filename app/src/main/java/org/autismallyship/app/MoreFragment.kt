package org.autismallyship.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.autismallyship.app.databinding.FragmentMoreBinding

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ticketsButton.setOnClickListener {
            startActivity(Intent(requireContext(), MyTicketsActivity::class.java))
        }
        binding.blogButton.setOnClickListener {
            startActivity(Intent(requireContext(), BlogActivity::class.java))
        }
        binding.galleryButton.setOnClickListener {
            startActivity(PlaceholderActivity.newIntent(requireContext(), R.string.more_gallery))
        }
        binding.shopButton.setOnClickListener {
            startActivity(PlaceholderActivity.newIntent(requireContext(), R.string.more_shop))
        }
        binding.donateButton.setOnClickListener {
            startActivity(PlaceholderActivity.newIntent(requireContext(), R.string.more_donate))
        }
        binding.aboutButton.setOnClickListener {
            startActivity(PlaceholderActivity.newIntent(requireContext(), R.string.more_about))
        }
        binding.contactButton.setOnClickListener {
            startActivity(PlaceholderActivity.newIntent(requireContext(), R.string.more_contact))
        }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MoreFragment"
    }
}
