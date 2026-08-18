package org.autismallyship.app.data

import com.google.firebase.firestore.DocumentId

// provinces holds every province a service covers, so the province filter is a membership check
// rather than an equality one. Values are any of the nine provinces, "National", or
// "Not applicable".
data class Resource(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val provinces: List<String> = emptyList(),
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val published: Boolean = false
)
