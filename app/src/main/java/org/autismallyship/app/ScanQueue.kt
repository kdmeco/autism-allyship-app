package org.autismallyship.app

import android.content.Context
import android.content.SharedPreferences

// A scan made with no signal cannot go through a transaction, which needs a live round trip to
// the server even with offline persistence on. A queued token here is a scan the door has already
// accepted; it is replayed as a normal transaction the moment a connection comes back, on
// whichever staff phone happens to be online first, so the physical door never has to stop and
// wait for signal.
//
// Its own preferences file, same reasoning as TicketStore: this is a record of work still to sync,
// not a setting, so "clear cached data" in Settings must not silently drop a queued redemption.
class ScanQueue(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun tokens(): Set<String> = prefs.getStringSet(KEY_TOKENS, null) ?: emptySet()

    fun enqueue(token: String) {
        if (token.isBlank()) return

        // getStringSet hands back the set it is holding, and the documentation says not to change
        // it in place, so the copy is required rather than caution.
        val updated = tokens().toMutableSet()
        if (!updated.add(token)) return
        prefs.edit().putStringSet(KEY_TOKENS, updated).apply()
    }

    fun remove(token: String) {
        val updated = tokens().toMutableSet()
        if (!updated.remove(token)) return
        prefs.edit().putStringSet(KEY_TOKENS, updated).apply()
    }

    private companion object {
        const val PREFS_NAME = "aaf-scan-queue"
        const val KEY_TOKENS = "aaf-scan-queue-tokens"
    }
}
