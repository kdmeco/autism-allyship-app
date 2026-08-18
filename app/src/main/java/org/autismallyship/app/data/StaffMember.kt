package org.autismallyship.app.data

import com.google.firebase.firestore.DocumentId

// The document ID is the Firebase Auth UID, so the scanner looks a staff member up directly by the
// signed in user's uid. active is what an admin toggles to grant or revoke scanner access.
data class StaffMember(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val active: Boolean = false
)
