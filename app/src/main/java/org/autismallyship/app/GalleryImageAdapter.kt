package org.autismallyship.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.autismallyship.app.data.GalleryImage
import org.autismallyship.app.databinding.ItemGalleryImageBinding

class GalleryImageAdapter(
    private val onImageClick: (position: Int) -> Unit
) : RecyclerView.Adapter<GalleryImageAdapter.ImageViewHolder>() {

    private val images = mutableListOf<GalleryImage>()

    fun showImages(newImages: List<GalleryImage>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position, onImageClick)
    }

    override fun getItemCount(): Int = images.size

    class ImageViewHolder(
        private val binding: ItemGalleryImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(image: GalleryImage, position: Int, onImageClick: (Int) -> Unit) {
            val thumb = image.thumbUrl.ifBlank { image.url }
            val url = SiteUrls.assetUrl(thumb)
            if (thumb.isNotBlank()) {
                Glide.with(binding.thumbImage).load(url).into(binding.thumbImage)
            } else {
                Glide.with(binding.thumbImage).clear(binding.thumbImage)
            }
            binding.thumbImage.contentDescription = image.alt.ifBlank {
                binding.root.context.getString(R.string.cd_gallery_image, position + 1)
            }
            binding.root.setOnClickListener { onImageClick(position) }
        }
    }
}
