package org.autismallyship.app

import android.content.Context
import android.content.SharedPreferences

// There are no accounts, so a ticket belongs to a phone rather than to a person. Opening the
// emailed link once is what puts its token here, and My Tickets reads back whatever is in the set.
// Forwarding that link to someone else puts it on their phone too, which is how a ticket transfers.
//
// Its own preferences file rather than the one AppSettings uses, because these are records and
// those are settings. "Clear cached data" in Settings must not take anyone's tickets with it.
class TicketStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun tokens(): Set<String> = prefs.getStringSet(KEY_TOKENS, null) ?: emptySet()

    fun remember(token: String) {
        if (token.isBlank()) return

        // getStringSet hands back the set it is holding, and the documentation says not to change
        // it in place, so the copy is required rather than caution.
        val updated = tokens().toMutableSet()
        if (!updated.add(token)) return
        prefs.edit().putStringSet(KEY_TOKENS, updated).apply()
    }

    private companion object {
        const val PREFS_NAME = "aaf-tickets"
        const val KEY_TOKENS = "aaf-ticket-tokens"
    }
}
