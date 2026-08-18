package org.autismallyship.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// Nothing in the app ever writes one of these. The security rules refuse a donation write from any
// client, they are created server side by the Worker once Paystack confirms the payment.
//
// isRecurring carries a PropertyName because the Firestore mapper strips the "is" from a boolean
// getter, which would otherwise look for a field called "recurring".
data class Donation(
    @DocumentId val id: String = "",
    val amount: Double = 0.0,
    @get:PropertyName("isRecurring") @field:PropertyName("isRecurring")
    val isRecurring: Boolean = false,
    val donorName: String = "",
    val donorEmail: String = "",
    val message: String = "",
    val anonymous: Boolean = false,
    val paystackRef: String = "",
    val status: String = "",
    val createdAt: Timestamp? = null
)
