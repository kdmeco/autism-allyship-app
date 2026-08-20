package org.autismallyship.app

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.autismallyship.app.data.RedeemOutcome
import org.autismallyship.app.data.Repository
import org.autismallyship.app.data.Ticket
import org.autismallyship.app.databinding.ActivityScannerBinding
import java.text.SimpleDateFormat
import java.util.Locale

// Staff only, reached only from ScannerSignInActivity. RULES-APP.md's three outcomes for a scan
// are valid and now redeemed, already redeemed with the time, and not a valid ticket. Two more
// are ours: queued for no signal, and a plain failure for anything else that goes wrong.
class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var scanQueue: ScanQueue

    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val connectivityManager by lazy { getSystemService<ConnectivityManager>() }

    // Registered only while this screen is visible, so a scan queued here starts syncing itself
    // the moment the phone finds a signal again, rather than waiting for the next manual scan or
    // app relaunch.
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread { flushQueue() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scanQueue = ScanQueue(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scannerRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.scanButton.setOnClickListener { scan() }
        binding.signOutButton.setOnClickListener { signOut() }

        updateQueueBanner()
    }

    override fun onStart() {
        super.onStart()
        flushQueue()
        connectivityManager?.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
    }

    override fun onStop() {
        super.onStop()
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
    }

    private fun scan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        GmsBarcodeScanning.getClient(this, options).startScan()
            .addOnSuccessListener { barcode -> onScanned(barcode.rawValue) }
            .addOnFailureListener { showResultError(getString(R.string.scanner_scan_failed)) }
    }

    // DECISIONS.md is explicit that the ticket QR encodes the full ticket URL, not the bare
    // token, so the token has to be pulled out of the query string. Falling back to the raw
    // scanned text keeps a hand typed or older bare token working too.
    private fun onScanned(rawValue: String?) {
        val token = rawValue?.let { value ->
            runCatching { Uri.parse(value).getQueryParameter(TOKEN_PARAMETER) }.getOrNull() ?: value
        }
        if (token.isNullOrBlank()) {
            showResultError(getString(R.string.scanner_result_not_valid))
            return
        }
        showChecking()
        redeem(token)
    }

    private fun redeem(token: String) {
        Repository.redeemTicket(token) { outcome ->
            when (outcome) {
                is RedeemOutcome.Redeemed -> showRedeemed(outcome.ticket)
                is RedeemOutcome.AlreadyRedeemed -> showAlreadyRedeemed(outcome.ticket)
                RedeemOutcome.NotFound -> showResultError(getString(R.string.scanner_result_not_valid))
                RedeemOutcome.Offline -> queueAndShow(token)
                is RedeemOutcome.Error -> showResultError(getString(R.string.scanner_result_failed))
            }
        }
    }

    private fun queueAndShow(token: String) {
        scanQueue.enqueue(token)
        updateQueueBanner()
        showResultInfo(getString(R.string.scanner_result_queued))
    }

    private fun showRedeemed(ticket: Ticket) {
        val heading = getString(R.string.scanner_result_redeemed)
        showResultInfo(heading + "\n" + ticket.eventTitle + "\n" + bookedByLine(ticket))
    }

    private fun showAlreadyRedeemed(ticket: Ticket) {
        val time = ticket.redeemedAt?.toDate()?.let { dateFormat.format(it) }.orEmpty()
        val heading = getString(R.string.scanner_result_already_redeemed_at, time)
        showResultInfo(heading + "\n" + ticket.eventTitle + "\n" + bookedByLine(ticket))
    }

    // "Booked by", never "belongs to", the same wording SCHEMA.md uses on the ticket itself: what
    // makes a ticket valid is the token and whether it has been redeemed, not the name on it.
    private fun bookedByLine(ticket: Ticket): String {
        val people = ticket.quantity.toInt()
        return resources.getQuantityString(R.plurals.ticket_booked_by, people, ticket.attendeeName, people)
    }

    private fun showChecking() {
        binding.resultInfo.isVisible = false
        binding.resultError.isVisible = false
        binding.checkingRow.isVisible = true
        // Sensory mode allows no animation anywhere in the app, and a spinner is one, so only the
        // "checking ticket" text stays up in that mode.
        binding.checkingSpinner.isVisible = !AppSettings(this).isSensoryMode()
    }

    private fun showResultInfo(message: String) {
        binding.checkingRow.isVisible = false
        binding.resultError.isVisible = false
        binding.resultInfo.text = message
        binding.resultInfo.isVisible = true
    }

    private fun showResultError(message: String) {
        binding.checkingRow.isVisible = false
        binding.resultInfo.isVisible = false
        binding.resultError.text = message
        binding.resultError.isVisible = true
    }

    private fun updateQueueBanner() {
        val count = scanQueue.tokens().size
        binding.queueBanner.isVisible = count > 0
        if (count > 0) {
            binding.queueBanner.text =
                resources.getQuantityString(R.plurals.scanner_queue_pending, count, count)
        }
    }

    // Retried on every visible open and every time connectivity returns. A queued token that
    // resolves definitively, redeemed here, already redeemed elsewhere in the meantime, or simply
    // not found, is removed. One that is still offline or hit a transient error is left for the
    // next attempt.
    private fun flushQueue() {
        for (token in scanQueue.tokens()) {
            Repository.redeemTicket(token) { outcome ->
                when (outcome) {
                    is RedeemOutcome.Redeemed, is RedeemOutcome.AlreadyRedeemed, RedeemOutcome.NotFound -> {
                        scanQueue.remove(token)
                        updateQueueBanner()
                    }
                    RedeemOutcome.Offline, is RedeemOutcome.Error -> Unit
                }
            }
        }
    }

    private fun signOut() {
        Firebase.auth.signOut()
        finish()
    }

    private companion object {
        const val TOKEN_PARAMETER = "token"
    }
}
