package org.autismallyship.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.autismallyship.app.data.Ticket
import org.autismallyship.app.databinding.ItemTicketBinding
import org.autismallyship.app.databinding.ItemTicketHeaderBinding
import org.autismallyship.app.databinding.ItemTicketPastHeaderBinding
import java.text.SimpleDateFormat
import java.util.Locale

// Past tickets start collapsed behind a button. Someone opening this screen is usually standing at
// an event and wants today's ticket, and after a couple of years the picnics they have already been
// to would push it off the top of the list.
class TicketAdapter(
    private val onTicketClick: (Ticket) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Row {
        data class Heading(val titleRes: Int) : Row
        data class PastToggle(val count: Int, val expanded: Boolean) : Row
        data class Entry(val ticket: Ticket) : Row
    }

    private val rows = mutableListOf<Row>()
    private var upcoming: List<Ticket> = emptyList()
    private var past: List<Ticket> = emptyList()
    private var pastExpanded = false

    fun showTickets(upcoming: List<Ticket>, past: List<Ticket>) {
        this.upcoming = upcoming
        this.past = past
        buildRows()
    }

    private fun buildRows() {
        rows.clear()
        if (upcoming.isNotEmpty()) {
            rows.add(Row.Heading(R.string.tickets_upcoming))
            rows.addAll(upcoming.map { Row.Entry(it) })
        }
        if (past.isNotEmpty()) {
            rows.add(Row.PastToggle(past.size, pastExpanded))
            if (pastExpanded) {
                rows.addAll(past.map { Row.Entry(it) })
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Heading -> TYPE_HEADING
        is Row.PastToggle -> TYPE_PAST_TOGGLE
        is Row.Entry -> TYPE_TICKET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADING -> HeadingViewHolder(ItemTicketHeaderBinding.inflate(inflater, parent, false))
            TYPE_PAST_TOGGLE ->
                PastToggleViewHolder(ItemTicketPastHeaderBinding.inflate(inflater, parent, false))

            else -> TicketViewHolder(ItemTicketBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Heading -> (holder as HeadingViewHolder).bind(row.titleRes)
            is Row.PastToggle -> (holder as PastToggleViewHolder).bind(row.count, row.expanded) {
                pastExpanded = !pastExpanded
                buildRows()
            }

            is Row.Entry -> (holder as TicketViewHolder).bind(row.ticket, onTicketClick)
        }
    }

    override fun getItemCount(): Int = rows.size

    class HeadingViewHolder(
        private val binding: ItemTicketHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(titleRes: Int) {
            binding.root.setText(titleRes)
            ViewCompat.setAccessibilityHeading(binding.root, true)
        }
    }

    class PastToggleViewHolder(
        private val binding: ItemTicketPastHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // The button says what it will do and how many tickets are behind it, so TalkBack reads
        // "Show 3 past tickets" from the label itself and needs nothing added on top.
        fun bind(count: Int, expanded: Boolean, onToggle: () -> Unit) {
            val plural = if (expanded) R.plurals.tickets_hide_past else R.plurals.tickets_show_past
            binding.root.text = binding.root.resources.getQuantityString(plural, count, count)
            binding.root.setOnClickListener { onToggle() }
        }
    }

    class TicketViewHolder(
        private val binding: ItemTicketBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())

        fun bind(ticket: Ticket, onTicketClick: (Ticket) -> Unit) {
            val context = binding.root.context

            binding.ticketEvent.text = ticket.eventTitle

            val startsAt = ticket.eventStartsAt?.toDate()
            val date = startsAt?.let { dateFormat.format(it) }.orEmpty()
            binding.ticketDate.text = date
            binding.ticketDate.isVisible = date.isNotBlank()

            // "Booked by", never "belongs to". SCHEMA.md is explicit about it: the name is
            // informational, and a ticket someone forwarded on WhatsApp is still valid.
            val bookedBy = context.resources.getQuantityString(
                R.plurals.ticket_booked_by,
                ticket.quantity.toInt(),
                ticket.attendeeName,
                ticket.quantity.toInt()
            )
            binding.ticketBookedBy.text = bookedBy

            // Redeemed is said in words as well as shown in a muted colour, because nothing in the
            // app may rely on colour alone to carry meaning.
            binding.ticketRedeemed.isVisible = ticket.redeemed

            binding.root.setOnClickListener { onTicketClick(ticket) }

            // TalkBack treats a tappable row as one item, so the row carries its own description
            // rather than leaving the reader to stitch four text views together.
            val parts = mutableListOf(ticket.eventTitle)
            if (date.isNotBlank()) parts.add(date)
            parts.add(bookedBy)
            if (ticket.redeemed) parts.add(context.getString(R.string.ticket_redeemed))
            binding.root.contentDescription = parts.joinToString(", ")
        }
    }

    private companion object {
        const val TYPE_HEADING = 0
        const val TYPE_PAST_TOGGLE = 1
        const val TYPE_TICKET = 2
    }
}
