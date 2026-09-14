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
import com.google.android.material.chip.Chip
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

    // The same fixed groups as the website's resources page, over the same stored category values.
    // The foundation asked for the pills up front and the resources behind them, so nothing is
    // listed until a group is chosen; typing in the search box with no group searches everything.
    private data class CategoryGroup(val key: String, val labelRes: Int, val categories: List<String>)

    private val categoryGroups = listOf(
        CategoryGroup("schools", R.string.resources_group_schools, listOf("School")),
        CategoryGroup("specialists", R.string.resources_group_specialists, listOf("Diagnosis & assessment", "Therapy", "Early intervention")),
        CategoryGroup("support", R.string.resources_group_support, listOf("Support group")),
        CategoryGroup("recreation", R.string.resources_group_recreation, listOf("Recreation")),
        CategoryGroup("organisations", R.string.resources_group_organisations, listOf("National organisation", "Helpline")),
        CategoryGroup("adults", R.string.resources_group_adults, listOf("Adult services")),
        CategoryGroup("grants", R.string.resources_group_grants, listOf("Grants & financial", "Sensory & equipment")),
    )
    private val otherGroupKey = "other"

    private var selectedGroup: String? = null
    private val groupChips = LinkedHashMap<CategoryGroup, Chip>()
    private var suppressChipListener = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentResourcesBinding.inflate(inflater, container, false)
        this.binding = binding

        selectedGroup = savedInstanceState?.getString(STATE_GROUP)

        binding.resourceList.layoutManager = LinearLayoutManager(requireContext())
        binding.resourceList.adapter = adapter

        binding.searchInput.doAfterTextChanged { applyFilters() }
        binding.provinceInput.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        loadResources()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_GROUP, selectedGroup)
    }

    private fun loadResources() {
        showLoading()
        Repository.loadResources(
            onSuccess = { resources, fromCache -> showLoaded(resources, fromCache) },
            onError = { showMessage(R.string.resources_load_failed) }
        )
    }

    private fun showLoaded(resources: List<Resource>, fromCache: Boolean) {
        val binding = this.binding ?: return
        allResources = resources
        loaded = true

        // Firestore tells us whether the answer came off the disk rather than the server, so the
        // banner appears exactly when what is on screen might be out of date.
        binding.offlineBanner.isVisible = fromCache

        fillFilterOptions()
        applyFilters()
    }

    // The province list comes from what actually arrived, so it offers nothing unused. The category
    // groups are fixed, matching the website, and Other joins them only when something is in it.
    private fun fillFilterOptions() {
        val binding = this.binding ?: return
        val all = getString(R.string.filter_all)

        val provinces = allResources
            .flatMap { it.provinces }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        setOptions(binding.provinceInput, listOf(all) + provinces)
        buildCategoryChips()
    }

    private fun buildCategoryChips() {
        val binding = this.binding ?: return
        binding.categoryChips.removeAllViews()
        groupChips.clear()

        val hasOther = allResources.any { groupOf(it.category) == null }
        val groups = categoryGroups +
            if (hasOther) listOf(CategoryGroup(otherGroupKey, R.string.resources_group_other, emptyList())) else emptyList()

        suppressChipListener = true
        for (group in groups) {
            val chip = layoutInflater.inflate(R.layout.item_filter_chip, binding.categoryChips, false) as Chip
            chip.text = getString(group.labelRes)
            chip.isChecked = group.key == selectedGroup
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (suppressChipListener) return@setOnCheckedChangeListener
                selectedGroup = if (isChecked) group.key else null
                applyFilters()
            }
            groupChips[group] = chip
            binding.categoryChips.addView(chip)
        }
        suppressChipListener = false
    }

    private fun groupOf(category: String): CategoryGroup? {
        return categoryGroups.firstOrNull { group -> group.categories.contains(category) }
    }

    private fun inGroup(resource: Resource, groupKey: String): Boolean {
        if (groupKey == otherGroupKey) {
            return groupOf(resource.category) == null
        }
        val group = categoryGroups.firstOrNull { it.key == groupKey } ?: return false
        return group.categories.contains(resource.category)
    }

    private fun setOptions(field: AutoCompleteTextView, options: List<String>) {
        field.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        )
        // A rotation restores the text in this field before the data comes back, so a choice the
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
        val province = binding.provinceInput.text.toString()
        val group = selectedGroup
        val showingList = group != null || search.isNotBlank()

        val matches = allResources.filter { resource ->
            matchesSearch(resource, search) &&
                (group == null || inGroup(resource, group)) &&
                (province == all || resource.provinces.contains(province))
        }

        when {
            allResources.isEmpty() -> showMessage(R.string.resources_empty)
            !showingList -> showMessage(R.string.resources_choose_category)
            matches.isEmpty() -> showMessage(R.string.resources_no_matches)
            else -> showResources(matches)
        }

        updateChipCounts(search, province, all)
    }

    private fun updateChipCounts(search: String, province: String, all: String) {
        for ((group, chip) in groupChips) {
            val matching = allResources.count { resource ->
                matchesSearch(resource, search) &&
                    inGroup(resource, group.key) &&
                    (province == all || resource.provinces.contains(province))
            }
            chip.text = getString(group.labelRes) + " (" + matching + ")"
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
        binding.offlineBanner.isVisible = false
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
        private const val STATE_GROUP = "selectedGroup"
    }
}
