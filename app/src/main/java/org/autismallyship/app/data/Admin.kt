package org.autismallyship.app.data

import com.google.firebase.firestore.DocumentId

// The document ID is the Firebase Auth UID. role exists so editors can be added later without a
// migration. The app does not read this collection, the admin panel on the website does.
data class Admin(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = ""
)
