package org.autismallyship.app

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class DonationResult(
    val reference: String,
    val status: String,
    val amount: Double,
    val donorName: String,
    val gatewayResponse: String?
)

// Talks to the autism-allyship-api Worker's /donations endpoints, the same ones the website's
// donate.js calls. The donations collection refuses every write from a client, see SCHEMA.md, so
// the Worker holds the service account credential and the Paystack secret key. Same plain
// HttpURLConnection pattern as TicketRegistrationApi, for the same reason: these are the only HTTP
// calls the app makes outside the Firebase SDK.
object DonationApi {

    private const val BASE_URL = "https://autism-allyship-api.kdmeco-dev.workers.dev"
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 15000

    private val mainThread = Handler(Looper.getMainLooper())

    fun initialize(
        amount: Double,
        donorName: String,
        donorEmail: String,
        message: String,
        onSuccess: (accessCode: String, reference: String) -> Unit,
        onError: (message: String?) -> Unit
    ) {
        Thread {
            try {
                val body = JSONObject()
                    .put("amount", amount)
                    .put("donorName", donorName)
                    .put("donorEmail", donorEmail)
                    .put("message", message)
                    .toString()

                val connection = URL("$BASE_URL/donations/initialize").openConnection() as HttpURLConnection
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
                    mainThread.post {
                        onSuccess(response.optString("accessCode"), response.optString("reference"))
                    }
                } else {
                    val message = response.optString("error").ifBlank { null }
                    mainThread.post { onError(message) }
                }
            } catch (error: Exception) {
                mainThread.post { onError(null) }
            }
        }.start()
    }

    // Matches donate-result.js on the website: this is always asked to confirm a donation with
    // Paystack before the app says it succeeded, never the checkout screen's own client-side
    // signal, because a WebView cannot prove what a popup callback can already only half prove.
    fun verify(
        reference: String,
        onSuccess: (result: DonationResult) -> Unit,
        onError: (message: String?) -> Unit
    ) {
        Thread {
            try {
                val body = JSONObject().put("reference", reference).toString()

                val connection = URL("$BASE_URL/donations/verify").openConnection() as HttpURLConnection
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
                    val result = DonationResult(
                        reference = response.optString("reference"),
                        status = response.optString("status"),
                        amount = response.optDouble("amount"),
                        donorName = response.optString("donorName"),
                        gatewayResponse = response.optString("gatewayResponse").ifBlank { null }
                    )
                    mainThread.post { onSuccess(result) }
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
