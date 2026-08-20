package org.autismallyship.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// Nothing in the app ever writes one of these. The security rules refuse a donation write from any
// client, they are created server side by the Worker once Paystack confirms the payment.
data class Donation(
    @DocumentId val id: String = "",
    val amount: Double = 0.0,
    val donorName: String = "",
    val donorEmail: String = "",
    val message: String = "",
    val paystackRef: String = "",
    val status: String = "",
    val createdAt: Timestamp? = null
)
