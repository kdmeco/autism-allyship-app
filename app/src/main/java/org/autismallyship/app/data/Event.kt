package org.autismallyship.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// startsAt is nullable so a document saved without it still maps instead of throwing. Repository
// leaves such an event out of both the upcoming and past lists rather than guessing where it sits.
//
// isTicketed carries a PropertyName because the Firestore mapper strips the "is" from a boolean
// getter, which would otherwise look for a field called "ticketed" and always read back false.
data class Event(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val startsAt: Timestamp? = null,
    @get:PropertyName("isTicketed") @field:PropertyName("isTicketed")
    val isTicketed: Boolean = false,
    val price: Double = 0.0,
    val capacity: Long = 0,
    val ticketsSold: Long = 0,
    val imageUrl: String = "",
    val imageAlt: String = "",
    val attachments: List<Attachment> = emptyList(),
    val published: Boolean = false
)

data class Attachment(
    val url: String = "",
    val name: String = "",
    val type: String = ""
)
