package org.autismallyship.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// The only document the app writes. Set createdAt before calling Repository.sendSubmission,
// nothing further along fills it in.
data class Submission(
    @DocumentId val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val category: String = "",
    val message: String = "",
    val handled: Boolean = false,
    val createdAt: Timestamp? = null
)
