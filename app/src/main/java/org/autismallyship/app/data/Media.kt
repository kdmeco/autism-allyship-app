package org.autismallyship.app.data

import com.google.firebase.firestore.DocumentId

// One interview or appearance in the Media Gallery's media section. date is a plain
// YYYY-MM-DD string or an empty string, never a timestamp: nothing sorts or ranges on
// it in a query, the list sorts in code and the screens show it as written. outlet,
// panel and topic are the foundation's own words, and names and shows are not
// translated, so they render as stored.
data class Media(
    @DocumentId val id: String = "",
    val outlet: String = "",
    val type: String = "",
    val date: String = "",
    val panel: String = "",
    val topic: String = "",
    val url: String = "",
    val published: Boolean = false
)
