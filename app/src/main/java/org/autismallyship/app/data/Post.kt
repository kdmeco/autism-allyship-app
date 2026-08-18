package org.autismallyship.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// body is HTML from the admin editor. There is no excerpt field, the blog list builds one from the
// first 160 characters of body with the tags stripped, the same way the website does.
data class Post(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val imageAlt: String = "",
    val publishedAt: Timestamp? = null,
    val published: Boolean = false
)
