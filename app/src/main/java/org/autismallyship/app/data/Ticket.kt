package org.autismallyship.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// eventTitle and eventStartsAt are duplicated from the event on purpose, so a ticket renders and My
// Tickets sorts without reading the event document as well.
//
// The name is informational. What makes a ticket valid is the token and the fact it has not been
// redeemed, which is why a ticket transfers by forwarding the link. Show it as
// "Booked by [name], [quantity] people", never as belonging to someone.
data class Ticket(
    @DocumentId val id: String = "",
    val token: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val eventStartsAt: Timestamp? = null,
    val attendeeName: String = "",
    val attendeeEmail: String = "",
    val quantity: Long = 0,
    val price: Double = 0.0,
    val redeemed: Boolean = false,
    val redeemedAt: Timestamp? = null
)
