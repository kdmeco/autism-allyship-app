package org.autismallyship.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.autismallyship.app.data.Repository
import org.autismallyship.app.data.Resource
import org.autismallyship.app.databinding.FragmentResourcesBinding

class ResourcesFragment : Fragment() {

    // Held as a nullable and cleared in onDestroyView, because a Firestore callback can arrive after
    // the view has gone and would otherwise write into a view that is no longer on screen.
    private var binding: FragmentResourcesBinding? = null
    private val adapter = ResourceAdapter { resource ->
        startActivity(ResourceDetailActivity.newIntent(requireContext(), resource.id))
    }

    // The whole published list, kept so that search and both filters run over it in memory. That is
    // what SCHEMA.md settled on for provinces, it needs no index, and it keeps all three working
    // with no connection.
    private var allResources: List<Resource> = emptyList()
    private var loaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentResourcesBinding.inflate(inflater, container, false)
        this.binding = binding

        binding.resourceList.layoutManager = LinearLayoutManager(requireContext())
        binding.resourceList.adapter = adapter

        binding.searchInput.doAfterTextChanged { applyFilters() }
        binding.categoryInput.setOnItemClickListener { _, _, _, _ -> applyFilters() }
        binding.provinceInput.setOnItemClickListener { _, _, _, _ -> applyFilters() }

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
            onSuccess = { resources ->
                allResources = resources
                loaded = true
                fillFilterOptions()
                applyFilters()
            },
            onError = { showMessage(R.string.resources_load_failed) }
        )
    }

    // Both filter lists come from what actually arrived, so neither offers a category or a province
    // that no resource uses, and neither has to be kept in step with the schema by hand.
    private fun fillFilterOptions() {
        val binding = this.binding ?: return
        val all = getString(R.string.filter_all)

        val categories = allResources
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val provinces = allResources
            .flatMap { it.provinces }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        setOptions(binding.categoryInput, listOf(all) + categories)
        setOptions(binding.provinceInput, listOf(all) + provinces)
    }

    private fun setOptions(field: AutoCompleteTextView, options: List<String>) {
        field.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        )
        // A rotation restores the text in these fields before the data comes back, so a choice the
        // person already made is kept as long as it is still one of the options.
        if (field.text.toString() !in options) {
            field.setText(options.first(), false)
        }
    }

    private fun applyFilters() {
        if (!loaded) return
        val binding = this.binding ?: return

        val all = getString(R.string.filter_all)
        val search = binding.searchInput.text.toString().trim()
        val category = binding.categoryInput.text.toString()
        val province = binding.provinceInput.text.toString()

        val matches = allResources.filter { resource ->
            matchesSearch(resource, search) &&
                (category == all || resource.category == category) &&
                (province == all || resource.provinces.contains(province))
        }

        when {
            allResources.isEmpty() -> showMessage(R.string.resources_empty)
            matches.isEmpty() -> showMessage(R.string.resources_no_matches)
            else -> showResources(matches)
        }
    }

    private fun matchesSearch(resource: Resource, search: String): Boolean {
        if (search.isBlank()) return true
        return resource.name.contains(search, ignoreCase = true) ||
            resource.description.contains(search, ignoreCase = true) ||
            resource.category.contains(search, ignoreCase = true)
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
