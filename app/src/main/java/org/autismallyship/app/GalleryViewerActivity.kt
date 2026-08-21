package org.autismallyship.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.autismallyship.app.databinding.ActivityGalleryViewerBinding
import org.autismallyship.app.databinding.ItemGalleryViewerBinding

class GalleryViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGalleryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.viewerRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        val urls = intent.getStringArrayListExtra(EXTRA_URLS).orEmpty()
        val alts = intent.getStringArrayListExtra(EXTRA_ALTS).orEmpty()
        val startIndex = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, (urls.size - 1).coerceAtLeast(0))

        binding.imagePager.adapter = ViewerAdapter(urls, alts)
        binding.imagePager.setCurrentItem(startIndex, false)
    }

    private class ViewerAdapter(
        private val urls: List<String>,
        private val alts: List<String>
    ) : RecyclerView.Adapter<ViewerAdapter.ViewerHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewerHolder {
            val binding = ItemGalleryViewerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewerHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewerHolder, position: Int) {
            holder.bind(urls[position], alts.getOrNull(position).orEmpty(), position)
        }

        override fun getItemCount(): Int = urls.size

        class ViewerHolder(
            private val binding: ItemGalleryViewerBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(url: String, alt: String, position: Int) {
                binding.zoomImage.resetZoom()
                binding.zoomImage.contentDescription = alt.ifBlank {
                    binding.root.context.getString(R.string.cd_gallery_image, position + 1)
                }
                Glide.with(binding.zoomImage).load(url).into(binding.zoomImage)
            }
        }
    }

    companion object {
        private const val EXTRA_URLS = "urls"
        private const val EXTRA_ALTS = "alts"
        private const val EXTRA_INDEX = "index"

        fun newIntent(
            context: Context,
            urls: ArrayList<String>,
            alts: ArrayList<String>,
            index: Int
        ): Intent {
            return Intent(context, GalleryViewerActivity::class.java)
                .putStringArrayListExtra(EXTRA_URLS, urls)
                .putStringArrayListExtra(EXTRA_ALTS, alts)
                .putExtra(EXTRA_INDEX, index)
        }
    }
}
