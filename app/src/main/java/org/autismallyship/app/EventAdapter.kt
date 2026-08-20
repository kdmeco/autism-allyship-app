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
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

// Upcoming and past share one list rather than two, so the two headings scroll away with their own
// events instead of one section staying pinned while the other moves.
class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            is Row.Entry -> (holder as EventViewHolder).bind(row.event, onEventClick)
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

        fun bind(event: Event, onEventClick: (Event) -> Unit) {
            binding.eventTitle.text = event.title

            val startsAt = event.startsAt?.toDate()
            val date = startsAt?.let { dateFormat.format(it) }.orEmpty()
            binding.eventDate.text = date
            binding.eventDate.isVisible = date.isNotBlank()

            binding.eventTickets.setText(
                if (event.isTicketed) R.string.event_ticketed else R.string.event_free
            )

            // The badge says today rather than now because SCHEMA.md has no end time. At 07:00 on
            // the morning of an all day picnic "happening now" would be a lie, while "happening
            // today" is true from midnight to midnight, which is the precision the data supports.
            val happeningToday = startsAt != null && isToday(startsAt)
            binding.eventBadge.isVisible = happeningToday

            binding.root.setOnClickListener { onEventClick(event) }

            // TalkBack treats a tappable row as one item, so the row carries its own description
            // rather than leaving the reader to stitch the text views together. The badge goes
            // first, because on the day it is the part that changes what someone does next.
            val context = binding.root.context
            binding.root.contentDescription = when {
                happeningToday -> context.getString(
                    R.string.cd_event_row_today,
                    context.getString(R.string.event_happening_today),
                    event.title,
                    date
                )

                date.isBlank() -> event.title
                else -> context.getString(R.string.cd_event_row, event.title, date)
            }

            // No image loading library is wired in yet, so the ImageView keeps its placeholder
            // background colour rather than showing a broken image icon. Same as the blog list.
        }

        // Compared as a calendar date in the phone's own time zone, not as a number of hours, so an
        // event at 23:00 tonight still counts as today and one at 00:30 tomorrow does not.
        private fun isToday(startsAt: Date): Boolean {
            val day = startsAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            return day == LocalDate.now()
        }
    }

    private companion object {
        const val TYPE_HEADING = 0
        const val TYPE_EVENT = 1
    }
}
