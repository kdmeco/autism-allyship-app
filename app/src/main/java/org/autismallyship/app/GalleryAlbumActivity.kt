package org.autismallyship.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import org.autismallyship.app.data.GalleryImage
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.ActivityGalleryAlbumBinding

class GalleryAlbumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryAlbumBinding
    private var images: List<GalleryImage> = emptyList()
    private val adapter = GalleryImageAdapter { position ->
        startActivity(
            GalleryViewerActivity.newIntent(
                this,
                ArrayList(images.map { SiteUrls.assetUrl(it.url.ifBlank { it.thumbUrl }) }),
                ArrayList(images.map { it.alt }),
                position
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGalleryAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.albumRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.imageGrid.layoutManager = GridLayoutManager(this, 3)
        binding.imageGrid.adapter = adapter

        val galleryId = intent.getStringExtra(EXTRA_GALLERY_ID).orEmpty()
        if (galleryId.isBlank()) {
            showMessage(R.string.gallery_load_failed)
        } else {
            loadAlbum(galleryId)
        }
    }

    private fun loadAlbum(galleryId: String) {
        showLoading()
        Repository.loadGallery(
            galleryId,
            onSuccess = { gallery, fromCache ->
                if (gallery == null) {
                    showMessage(R.string.gallery_empty)
                } else {
                    images = gallery.images
                    showImages(gallery.images, fromCache)
                }
            },
            onError = { showMessage(R.string.gallery_load_failed) }
        )
    }

    private fun showLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.offlineBanner.isVisible = false
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.listMessage.setText(R.string.gallery_loading)
        binding.listMessage.isVisible = sensoryMode
        binding.imageGrid.isVisible = false
    }

    private fun showImages(images: List<GalleryImage>, fromCache: Boolean) {
        adapter.showImages(images)
        binding.loadingSpinner.isVisible = false
        if (images.isEmpty()) {
            showMessage(R.string.gallery_empty)
        } else {
            binding.offlineBanner.isVisible = fromCache
            binding.listMessage.isVisible = false
            binding.imageGrid.isVisible = true
        }
    }

    private fun showMessage(messageRes: Int) {
        binding.offlineBanner.isVisible = false
        binding.loadingSpinner.isVisible = false
        binding.imageGrid.isVisible = false
        binding.listMessage.setText(messageRes)
        binding.listMessage.isVisible = true
    }

    companion object {
        private const val EXTRA_GALLERY_ID = "galleryId"
        private const val EXTRA_TITLE = "title"

        fun newIntent(context: Context, galleryId: String, title: String): Intent {
            return Intent(context, GalleryAlbumActivity::class.java)
                .putExtra(EXTRA_GALLERY_ID, galleryId)
                .putExtra(EXTRA_TITLE, title)
        }
    }
}
