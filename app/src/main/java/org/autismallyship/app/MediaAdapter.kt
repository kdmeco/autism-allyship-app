package org.autismallyship.app

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.autismallyship.app.data.Media
import org.autismallyship.app.databinding.ItemMediaBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MediaAdapter(
    private val onOpenLink: (String) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private val entries = mutableListOf<Media>()

    fun showMedia(newEntries: List<Media>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(entries[position], onOpenLink)
    }

    override fun getItemCount(): Int = entries.size

    class MediaViewHolder(
        private val binding: ItemMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: Media, onOpenLink: (String) -> Unit) {
            val context = binding.root.context
            binding.mediaOutlet.text = entry.outlet
            binding.mediaMeta.text = buildMetaText(context, entry)

            binding.mediaPanel.isVisible = entry.panel.isNotBlank()
            binding.mediaPanel.text = context.getString(R.string.gallery_media_with, entry.panel)

            binding.mediaTopic.isVisible = entry.topic.isNotBlank()
            binding.mediaTopic.text = entry.topic

            val label = linkLabel(context, entry.url)
            binding.mediaLink.isVisible = label != null
            binding.mediaLink.text = label.orEmpty()

            // The whole row is the touch target when there is something to open, and the
            // content description says what it does so a TalkBack user hears the outlet
            // and the action rather than a row of text.
            binding.root.isClickable = label != null
            binding.root.isFocusable = label != null
            if (label != null) {
                binding.root.contentDescription =
                    context.getString(R.string.cd_media_link, entry.outlet, label)
                binding.root.setOnClickListener { onOpenLink(entry.url) }
            } else {
                binding.root.contentDescription = null
                binding.root.setOnClickListener(null)
            }
        }

        // The date is stored as YYYY-MM-DD and shown the way the website shows it, for
        // example 3 Nov 2025. A date that does not parse, because it was typed by hand
        // into the console, is shown exactly as stored rather than guessed at.
        private fun dateText(entry: Media): String {
            if (entry.date.isBlank()) {
                return ""
            }
            return try {
                LocalDate.parse(entry.date).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            } catch (_: Exception) {
                entry.date
            }
        }

        private fun buildMetaText(context: Context, entry: Media): String {
            val date = dateText(entry)
                .ifBlank { context.getString(R.string.gallery_media_undated) }
            val type = typeLabel(context, entry.type)
            return if (type != null) {
                date + " \u00b7 " + type
            } else {
                date
            }
        }

        // A type outside this map is left out of the meta line rather than shown raw,
        // the same choice the website makes.
        private fun typeLabel(context: Context, type: String): String? = when (type) {
            "television" -> context.getString(R.string.gallery_media_type_television)
            "radio" -> context.getString(R.string.gallery_media_type_radio)
            "newspaper" -> context.getString(R.string.gallery_media_type_newspaper)
            "article" -> context.getString(R.string.gallery_media_type_article)
            "online video" -> context.getString(R.string.gallery_media_type_online_video)
            "live stream" -> context.getString(R.string.gallery_media_type_live_stream)
            "visit and live stream" ->
                context.getString(R.string.gallery_media_type_visit_and_live_stream)
            else -> null
        }

        // The label comes from the host, per SCHEMA.md: omny.fm is Listen on Omny,
        // youtube.com or youtu.be Watch on YouTube, citizen.co.za Read on Rekord, anything
        // else Open link. An address with no readable host gets no link at all rather than
        // a guessed label.
        private fun linkLabel(context: Context, url: String): String? {
            if (url.isBlank()) {
                return null
            }
            val host = Uri.parse(url).host ?: return null
            return when {
                hostMatches(host, "omny.fm") -> context.getString(R.string.gallery_media_listen_omny)
                hostMatches(host, "youtube.com") || hostMatches(host, "youtu.be") ->
                    context.getString(R.string.gallery_media_watch_youtube)
                hostMatches(host, "citizen.co.za") ->
                    context.getString(R.string.gallery_media_read_rekord)
                else -> context.getString(R.string.gallery_media_open_link)
            }
        }

        private fun hostMatches(host: String, domain: String): Boolean =
            host == domain || host.endsWith(".$domain")
    }
}
