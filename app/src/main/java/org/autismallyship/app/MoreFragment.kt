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
            startActivity(Intent(requireContext(), GalleryActivity::class.java))
        }
        binding.shopButton.setOnClickListener {
            startActivity(Intent(requireContext(), ShopActivity::class.java))
        }
        binding.donateButton.setOnClickListener {
            startActivity(Intent(requireContext(), DonateActivity::class.java))
        }
        binding.aboutButton.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        binding.contactButton.setOnClickListener {
            startActivity(Intent(requireContext(), ContactActivity::class.java))
        }
        binding.legalButton.setOnClickListener {
            startActivity(Intent(requireContext(), LegalActivity::class.java))
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
