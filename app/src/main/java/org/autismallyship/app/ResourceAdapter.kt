package org.autismallyship.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.autismallyship.app.data.Resource
import org.autismallyship.app.databinding.ItemResourceBinding

class ResourceAdapter(
    private val onResourceClick: (Resource) -> Unit
) : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    private val resources = mutableListOf<Resource>()

    fun showResources(newResources: List<Resource>) {
        resources.clear()
        resources.addAll(newResources)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val binding = ItemResourceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ResourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        holder.bind(resources[position], onResourceClick)
    }

    override fun getItemCount(): Int = resources.size

    class ResourceViewHolder(
        private val binding: ItemResourceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(resource: Resource, onResourceClick: (Resource) -> Unit) {
            binding.resourceName.text = resource.name

            // Category and description are both optional in practice, so an empty one is taken out
            // of the layout rather than left as a blank line the row still pays for.
            binding.resourceCategory.text = resource.category
            binding.resourceCategory.isVisible = resource.category.isNotBlank()

            binding.resourceDescription.text = resource.description
            binding.resourceDescription.isVisible = resource.description.isNotBlank()

            binding.root.setOnClickListener { onResourceClick(resource) }

            // TalkBack treats a tappable row as one item, so the row carries its own description
            // rather than leaving the reader to stitch three separate text views together.
            val context = binding.root.context
            binding.root.contentDescription = if (resource.category.isBlank()) {
                resource.name
            } else {
                context.getString(R.string.cd_resource_row, resource.name, resource.category)
            }
        }
    }
}
