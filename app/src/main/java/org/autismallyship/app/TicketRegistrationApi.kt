package org.autismallyship.app

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Registers a free ticket by POSTing to the autism-allyship-api Worker's /ticket endpoint, the
// same one the website uses. Nothing here talks to Firestore directly: the security rules refuse
// a ticket create from any client, so the Worker holds the service account credential that
// actually writes the ticket. See DECISIONS.md.
//
// Plain HttpURLConnection on a background thread rather than a networking library, since this is
// the only HTTP call the app makes outside the Firebase SDK, and Android already does everything a
// library would add here.
object TicketRegistrationApi {

    private const val BASE_URL = "https://autism-allyship-api.kdmeco-dev.workers.dev"
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 15000

    private val mainThread = Handler(Looper.getMainLooper())

    // onError carries the Worker's own message when it sent one, for example "This event is sold
    // out." or "Only 3 spots left." Those are already worded for a visitor, so the caller shows
    // them as they are rather than the app inventing its own copy for every case the server can
    // return.
    fun register(
        eventId: String,
        attendeeName: String,
        attendeeEmail: String,
        quantity: Int,
        onSuccess: (token: String) -> Unit,
        onError: (message: String?) -> Unit
    ) {
        Thread {
            try {
                val body = JSONObject()
                    .put("eventId", eventId)
                    .put("attendeeName", attendeeName)
                    .put("attendeeEmail", attendeeEmail)
                    .put("quantity", quantity)
                    .toString()

                val connection = URL("$BASE_URL/ticket").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.setRequestProperty("Content-Type", "application/json")

                OutputStreamWriter(connection.outputStream).use { it.write(body) }

                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val response = if (responseText.isNotBlank()) JSONObject(responseText) else JSONObject()

                if (status in 200..299 && response.optBoolean("ok")) {
                    mainThread.post { onSuccess(response.optString("token")) }
                } else {
                    val message = response.optString("error").ifBlank { null }
                    mainThread.post { onError(message) }
                }
            } catch (error: Exception) {
                mainThread.post { onError(null) }
            }
        }.start()
    }
}
