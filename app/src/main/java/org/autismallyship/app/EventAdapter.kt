package org.autismallyship.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.autismallyship.app.data.Event
import org.autismallyship.app.databinding.ItemEventBinding
import org.autismallyship.app.databinding.ItemEventHeaderBinding
import java.text.SimpleDateFormat
import java.util.Locale

// Upcoming and past share one list rather than two, so the two headings scroll away with their own
// events instead of one section staying pinned while the other moves.
class EventAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Row {
        data class Heading(val titleRes: Int) : Row
        data class Entry(val event: Event) : Row
    }

    private val rows = mutableListOf<Row>()

    fun showEvents(upcoming: List<Event>, past: List<Event>) {
        rows.clear()
        if (upcoming.isNotEmpty()) {
            rows.add(Row.Heading(R.string.events_upcoming))
            rows.addAll(upcoming.map { Row.Entry(it) })
        }
        if (past.isNotEmpty()) {
            rows.add(Row.Heading(R.string.events_past))
            rows.addAll(past.map { Row.Entry(it) })
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Heading -> TYPE_HEADING
        is Row.Entry -> TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADING) {
            HeadingViewHolder(ItemEventHeaderBinding.inflate(inflater, parent, false))
        } else {
            EventViewHolder(ItemEventBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Heading -> (holder as HeadingViewHolder).bind(row.titleRes)
            is Row.Entry -> (holder as EventViewHolder).bind(row.event)
        }
    }

    override fun getItemCount(): Int = rows.size

    class HeadingViewHolder(
        private val binding: ItemEventHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(titleRes: Int) {
            binding.root.setText(titleRes)

            // Marked as a heading so TalkBack can jump between the two sections rather than reading
            // through every event to reach the second one.
            ViewCompat.setAccessibilityHeading(binding.root, true)
        }
    }

    class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Day before month, matching South African date convention rather than the ambiguous numeric
        // MM/DD some locales default to. The time is on the same line because an event without one
        // is not much use to anyone deciding whether they can get there.
        private val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())

        fun bind(event: Event) {
            binding.eventTitle.text = event.title

            binding.eventDate.text = event.startsAt?.toDate()?.let { dateFormat.format(it) }.orEmpty()
            binding.eventDate.isVisible = event.startsAt != null

            binding.eventTickets.setText(
                if (event.isTicketed) R.string.event_ticketed else R.string.event_free
            )

            // No image loading library is wired in yet, so the ImageView keeps its placeholder
            // background colour rather than showing a broken image icon. Same as the blog list.
        }
    }

    private companion object {
        const val TYPE_HEADING = 0
        const val TYPE_EVENT = 1
    }
}
