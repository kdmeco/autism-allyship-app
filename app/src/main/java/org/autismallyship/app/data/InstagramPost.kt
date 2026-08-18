package org.autismallyship.app.data

import com.google.firebase.firestore.DocumentId

// The document ID is the shortcode from the Instagram URL, so pasting the same post twice
// overwrites rather than duplicating.
data class InstagramPost(
    @DocumentId val shortcode: String = "",
    val url: String = "",
    val order: Long = 0
)
