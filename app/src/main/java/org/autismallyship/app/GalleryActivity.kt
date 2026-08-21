package org.autismallyship.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.ActivityGalleryBinding

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private val adapter = GalleryAlbumAdapter { gallery ->
        startActivity(GalleryAlbumActivity.newIntent(this, gallery.id, gallery.title))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.galleryRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.galleryList.layoutManager = GridLayoutManager(this, 2)
        binding.galleryList.adapter = adapter
        loadGalleries()
    }

    private fun loadGalleries() {
        showLoading()
        Repository.loadGalleries(
            onSuccess = { albums -> showAlbums(albums) },
            onError = { showMessage(R.string.gallery_load_failed) }
        )
    }

    private fun showLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.listMessage.setText(R.string.gallery_loading)
        binding.listMessage.isVisible = sensoryMode
        binding.galleryList.isVisible = false
    }

    private fun showAlbums(albums: List<org.autismallyship.app.data.Gallery>) {
        adapter.showAlbums(albums)
        binding.loadingSpinner.isVisible = false
        if (albums.isEmpty()) {
            showMessage(R.string.gallery_empty)
        } else {
            binding.listMessage.isVisible = false
            binding.galleryList.isVisible = true
        }
    }

    private fun showMessage(messageRes: Int) {
        binding.loadingSpinner.isVisible = false
        binding.galleryList.isVisible = false
        binding.listMessage.setText(messageRes)
        binding.listMessage.isVisible = true
    }
}
