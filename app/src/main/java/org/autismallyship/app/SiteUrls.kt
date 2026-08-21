package org.autismallyship.app

import android.content.Context
import android.content.res.Configuration
import android.net.Uri

// Builds website page URLs for in-app WebViews. app=1 drops the site chrome; theme and sensory
// are passed because the WebView cannot read the app's SharedPreferences.
object SiteUrls {

    const val SITE_URL = "https://autism-allyship.pages.dev"
    const val WHATSAPP_DIRECT = "https://wa.me/27663859936"
    const val WHATSAPP_CATALOGUE = "https://wa.me/c/27663859936"
    const val CONTACT_NOTIFY_URL =
        "https://autism-allyship-api.kdmeco-dev.workers.dev/contact-notify"

    fun page(context: Context, path: String): String {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val theme = if (nightMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
        val sensory = if (AppSettings(context).isSensoryMode()) "on" else "off"

        val normalised = path.trimStart('/')
        return Uri.parse(SITE_URL).buildUpon()
            .appendEncodedPath(normalised)
            .appendQueryParameter("app", "1")
            .appendQueryParameter("theme", theme)
            .appendQueryParameter("sensory", sensory)
            .build()
            .toString()
    }

    fun blogArticle(context: Context, postId: String): String {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val theme = if (nightMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
        val sensory = if (AppSettings(context).isSensoryMode()) "on" else "off"

        return Uri.parse(SITE_URL).buildUpon()
            .appendPath("blog-post.html")
            .appendQueryParameter("id", postId)
            .appendQueryParameter("app", "1")
            .appendQueryParameter("theme", theme)
            .appendQueryParameter("sensory", sensory)
            .build()
            .toString()
    }

    // Gallery image paths in Firestore are relative, for example assets/uploads/gallery/….
    fun assetUrl(relativePath: String): String {
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath
        }
        return "$SITE_URL/${relativePath.trimStart('/')}"
    }
}
