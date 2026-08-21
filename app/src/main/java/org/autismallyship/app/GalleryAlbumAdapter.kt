package org.autismallyship.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.autismallyship.app.data.Gallery
import org.autismallyship.app.databinding.ItemGalleryAlbumBinding

class GalleryAlbumAdapter(
    private val onAlbumClick: (Gallery) -> Unit
) : RecyclerView.Adapter<GalleryAlbumAdapter.AlbumViewHolder>() {

    private val albums = mutableListOf<Gallery>()

    fun showAlbums(newAlbums: List<Gallery>) {
        albums.clear()
        albums.addAll(newAlbums)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemGalleryAlbumBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(albums[position], onAlbumClick)
    }

    override fun getItemCount(): Int = albums.size

    class AlbumViewHolder(
        private val binding: ItemGalleryAlbumBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gallery: Gallery, onAlbumClick: (Gallery) -> Unit) {
            binding.albumTitle.text = gallery.title
            binding.albumYear.text = if (gallery.year > 0) gallery.year.toString() else ""

            val coverUrl = SiteUrls.assetUrl(gallery.coverImageUrl)
            if (gallery.coverImageUrl.isNotBlank()) {
                Glide.with(binding.coverImage).load(coverUrl).into(binding.coverImage)
            } else {
                Glide.with(binding.coverImage).clear(binding.coverImage)
            }

            binding.root.contentDescription = if (gallery.year > 0) {
                binding.root.context.getString(
                    R.string.cd_gallery_album,
                    gallery.title,
                    gallery.year.toString()
                )
            } else {
                gallery.title
            }
            binding.root.setOnClickListener { onAlbumClick(gallery) }
        }
    }
}
