package org.autismallyship.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import org.autismallyship.app.data.Gallery
import org.autismallyship.app.data.Media
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.ActivityGalleryBinding

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private val albumAdapter = GalleryAlbumAdapter { gallery ->
        startActivity(GalleryAlbumActivity.newIntent(this, gallery.id, gallery.title))
    }
    private val mediaAdapter = MediaAdapter { url ->
        // SiteLinks hands everything that is not one of our own pages to the phone, and
        // shows the no app toast when nothing can open it.
        SiteLinks.handleLink(this, Uri.parse(url))
    }

    // Each section keeps its own answer to "did this load come off the disk", because
    // both load at once and the banner belongs to whichever section is on screen.
    private var photosBannerVisible = false
    private var mediaBannerVisible = false
    private var selectedSection = SECTION_PHOTOS

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
        binding.galleryList.adapter = albumAdapter
        binding.mediaList.layoutManager = LinearLayoutManager(this)
        binding.mediaList.adapter = mediaAdapter

        binding.galleryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedSection = tab.position
                showSection()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        // The selected section is saved by hand rather than trusted to TabLayout's own
        // restore, so it survives rotation even if a future version drops that.
        val restoredSection = savedInstanceState?.getInt(SELECTED_SECTION, SECTION_PHOTOS)
            ?: SECTION_PHOTOS
        if (restoredSection != SECTION_PHOTOS) {
            binding.galleryTabs.selectTab(binding.galleryTabs.getTabAt(restoredSection))
        } else {
            showSection()
        }

        loadGalleries()
        loadMedia()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_SECTION, selectedSection)
    }

    private fun showSection() {
        binding.photosSection.isVisible = selectedSection == SECTION_PHOTOS
        binding.mediaSection.isVisible = selectedSection == SECTION_MEDIA
        updateOfflineBanner()
    }

    private fun loadGalleries() {
        showPhotosLoading()
        Repository.loadGalleries(
            onSuccess = { albums, fromCache -> showAlbums(albums, fromCache) },
            onError = { showPhotosMessage(R.string.gallery_load_failed) }
        )
    }

    private fun showPhotosLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        photosBannerVisible = false
        updateOfflineBanner()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.listMessage.setText(R.string.gallery_loading)
        binding.listMessage.isVisible = sensoryMode
        binding.galleryList.isVisible = false
    }

    private fun showAlbums(albums: List<Gallery>, fromCache: Boolean) {
        albumAdapter.showAlbums(albums)
        binding.loadingSpinner.isVisible = false
        if (albums.isEmpty()) {
            showPhotosMessage(R.string.gallery_empty)
        } else {
            photosBannerVisible = fromCache
            updateOfflineBanner()
            binding.listMessage.isVisible = false
            binding.galleryList.isVisible = true
        }
    }

    private fun showPhotosMessage(messageRes: Int) {
        photosBannerVisible = false
        updateOfflineBanner()
        binding.loadingSpinner.isVisible = false
        binding.galleryList.isVisible = false
        binding.listMessage.setText(messageRes)
        binding.listMessage.isVisible = true
    }

    private fun loadMedia() {
        showMediaLoading()
        Repository.loadMedia(
            onSuccess = { entries, fromCache -> showMedia(entries, fromCache) },
            onError = { showMediaMessage(R.string.gallery_media_load_failed) }
        )
    }

    // The media section's states mirror the photos section's one for one, including the
    // sensory mode choice between a spinner and plain text, so the two tabs behave the
    // same way wherever the foundation's own words are not involved.
    private fun showMediaLoading() {
        val sensoryMode = AppSettings(this).isSensoryMode()
        mediaBannerVisible = false
        updateOfflineBanner()
        binding.mediaLoadingSpinner.isVisible = !sensoryMode
        binding.mediaListMessage.setText(R.string.gallery_media_loading)
        binding.mediaListMessage.isVisible = sensoryMode
        binding.mediaList.isVisible = false
    }

    private fun showMedia(entries: List<Media>, fromCache: Boolean) {
        mediaAdapter.showMedia(entries)
        binding.mediaLoadingSpinner.isVisible = false
        if (entries.isEmpty()) {
            showMediaMessage(R.string.gallery_media_empty)
        } else {
            mediaBannerVisible = fromCache
            updateOfflineBanner()
            binding.mediaListMessage.isVisible = false
            binding.mediaList.isVisible = true
        }
    }

    private fun showMediaMessage(messageRes: Int) {
        mediaBannerVisible = false
        updateOfflineBanner()
        binding.mediaLoadingSpinner.isVisible = false
        binding.mediaList.isVisible = false
        binding.mediaListMessage.setText(messageRes)
        binding.mediaListMessage.isVisible = true
    }

    private fun updateOfflineBanner() {
        binding.offlineBanner.isVisible =
            if (selectedSection == SECTION_PHOTOS) photosBannerVisible else mediaBannerVisible
    }

    companion object {
        private const val SELECTED_SECTION = "selectedSection"
        private const val SECTION_PHOTOS = 0
        private const val SECTION_MEDIA = 1
    }
}
