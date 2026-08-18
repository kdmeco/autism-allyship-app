package org.autismallyship.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.autismallyship.app.data.Repository
import org.autismallyship.app.data.Resource
import org.autismallyship.app.databinding.FragmentResourcesBinding

class ResourcesFragment : Fragment() {

    // Held as a nullable and cleared in onDestroyView, because a Firestore callback can arrive after
    // the view has gone and would otherwise write into a view that is no longer on screen.
    private var binding: FragmentResourcesBinding? = null
    private val adapter = ResourceAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentResourcesBinding.inflate(inflater, container, false)
        this.binding = binding

        binding.resourceList.layoutManager = LinearLayoutManager(requireContext())
        binding.resourceList.adapter = adapter

        loadResources()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun loadResources() {
        showLoading()
        Repository.loadResources(
            onSuccess = { resources -> showResources(resources) },
            onError = { showMessage(R.string.resources_load_failed) }
        )
    }

    private fun showLoading() {
        val binding = this.binding ?: return

        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so it
        // is replaced with a line of text rather than slowed down.
        val sensoryMode = AppSettings(requireContext()).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.listMessage.setText(R.string.resources_loading)
        binding.listMessage.isVisible = sensoryMode
        binding.resourceList.isVisible = false
    }

    private fun showResources(resources: List<Resource>) {
        if (resources.isEmpty()) {
            showMessage(R.string.resources_empty)
            return
        }

        val binding = this.binding ?: return
        adapter.showResources(resources)
        binding.loadingSpinner.isVisible = false
        binding.listMessage.isVisible = false
        binding.resourceList.isVisible = true
    }

    private fun showMessage(messageRes: Int) {
        val binding = this.binding ?: return
        binding.loadingSpinner.isVisible = false
        binding.resourceList.isVisible = false
        binding.listMessage.setText(messageRes)
        binding.listMessage.isVisible = true
    }

    companion object {
        const val TAG = "ResourcesFragment"
    }
}
