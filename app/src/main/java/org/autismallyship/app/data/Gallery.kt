package org.autismallyship.app.data

import com.google.firebase.firestore.DocumentId

// images holds paths into the website repository rather than full URLs to a storage bucket, for
// example assets/uploads/gallery/2026-picnic/img-014.webp. alt is optional and often empty, which is
// correct for a decorative photo. The album title carries the meaning instead.
data class Gallery(
    @DocumentId val id: String = "",
    val title: String = "",
    val eventName: String = "",
    val year: Long = 0,
    val coverImageUrl: String = "",
    val images: List<GalleryImage> = emptyList()
)

data class GalleryImage(
    val url: String = "",
    val thumbUrl: String = "",
    val alt: String = ""
)
