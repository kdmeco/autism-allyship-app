package org.autismallyship.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast

// Decides which links a site WebView keeps for itself and which are handed to
// the phone. Only our own https pages stay inside the app; everything else a
// page can point at, mail, phone numbers, WhatsApp, PDF attachments, other
// sites, leaves the WebView, because it cannot usefully load any of them.
object SiteLinks {

    private const val SITE_HOST = "autism-allyship.pages.dev"

    // Returns true when the URL was handled outside the WebView, meaning the
    // caller's shouldOverrideUrlLoading must also return true.
    fun handleLink(activity: Activity, uri: Uri): Boolean {
        if (staysInWebView(uri)) {
            return false
        }
        handToPhone(activity, uri)
        return true
    }

    fun staysInWebView(uri: Uri): Boolean {
        if (uri.scheme != "https") {
            return false
        }
        val host = uri.host.orEmpty()
        val onSite = host == SITE_HOST || host.endsWith(".$SITE_HOST")
        val isPdf = uri.path.orEmpty().endsWith(".pdf")
        return onSite && !isPdf
    }

    private fun handToPhone(activity: Activity, uri: Uri) {
        val intent = when (uri.scheme) {
            "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
            "tel" -> Intent(Intent.ACTION_DIAL, uri)
            else -> Intent(Intent.ACTION_VIEW, uri)
        }
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }
}
