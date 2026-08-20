package org.autismallyship.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.data.Repository
import org.autismallyship.app.data.Ticket
import org.autismallyship.app.databinding.ActivityTicketDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TicketDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTicketDetailBinding

    private val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTicketDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.ticketRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { goBack() }
        onBackPressedDispatcher.addCallback(this) { goBack() }

        loadTicket(tokenFromIntent())
    }

    // Two ways in. My Tickets passes the token as an extra, and the link in the confirmation email
    // arrives as the ticket URL itself, with the token in the query string.
    private fun tokenFromIntent(): String {
        val fromLink = intent.data?.getQueryParameter(TOKEN_PARAMETER)
        return fromLink ?: intent.getStringExtra(EXTRA_TOKEN).orEmpty()
    }

    // Raised on open so the code reads in daylight, which is where someone will be standing, and put
    // back on the way out so the phone is not left at full brightness eating the battery.
    override fun onStart() {
        super.onStart()
        setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL)
    }

    override fun onStop() {
        super.onStop()
        setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
    }

    private fun setScreenBrightness(level: Float) {
        val attributes = window.attributes
        attributes.screenBrightness = level
        window.attributes = attributes
    }

    private fun loadTicket(token: String) {
        if (token.isBlank()) {
            showMessage(R.string.ticket_not_found)
            return
        }

        showLoading()
        Repository.loadTicketByToken(
            token,
            onSuccess = { ticket, fromCache ->
                if (ticket == null) {
                    showMessage(R.string.ticket_not_found)
                } else {
                    // Saved only once the ticket has actually come back, so a mistyped or expired
                    // link does not leave a token in My Tickets that will never resolve.
                    TicketStore(this).remember(token)
                    showTicket(ticket, token, fromCache)
                }
            },
            onError = { showMessage(R.string.ticket_load_failed) }
        )
    }

    private fun showTicket(ticket: Ticket, token: String, fromCache: Boolean) {
        binding.ticketEvent.text = ticket.eventTitle

        val startsAt = ticket.eventStartsAt?.toDate()
        binding.ticketDate.text = startsAt?.let { dateFormat.format(it) }.orEmpty()
        binding.ticketDate.isVisible = startsAt != null

        // "Booked by", never "belongs to". SCHEMA.md is explicit: the name is informational, and
        // what makes a ticket valid is the token and the fact it has not been redeemed.
        val people = ticket.quantity.toInt()
        binding.ticketBookedBy.text = resources.getQuantityString(
            R.plurals.ticket_booked_by,
            people,
            ticket.attendeeName,
            people
        )

        showQr(token)

        // Said in words rather than shown as a colour change, and the QR stays on screen underneath
        // it, because the door may need to look at the code again to see when it was used.
        binding.ticketRedeemed.isVisible = ticket.redeemed

        // The token in a cached ticket is the same token, so the code still scans. What may be
        // behind is redeemed, which is why the wording says the ticket works rather than warning
        // that the whole thing is stale.
        binding.offlineBanner.isVisible = fromCache

        binding.loadingSpinner.isVisible = false
        binding.ticketMessage.isVisible = false
        binding.ticketContent.isVisible = true
    }

    // The full ticket URL rather than the bare token, settled in DECISIONS.md: a phone camera reads
    // whatever text is in a code, and a bare token is just text, so scanning it offered a web search
    // instead of the ticket. It is also the exact string an App Link intercepts.
    private fun showQr(token: String) {
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            QR_SIZE_DP,
            resources.displayMetrics
        ).toInt()

        val ticketUrl = TICKET_URL_BASE + Uri.encode(token)
        binding.ticketQr.setImageBitmap(ticketQrBitmap(ticketUrl, sizePx))
    }

    // Opened from a link, this screen is the only thing in the task, so plain finish would drop
    // somebody back into their mail app with none of the app open. Sending them to My Tickets
    // instead leaves them where the ticket now lives.
    private fun goBack() {
        if (isTaskRoot) {
            startActivity(Intent(this, MyTicketsActivity::class.java))
        }
        finish()
    }

    private fun showLoading() {
        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so it
        // is replaced with a line of text rather than slowed down.
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.ticketMessage.setText(R.string.ticket_loading)
        binding.ticketMessage.isVisible = sensoryMode
        binding.ticketContent.isVisible = false
    }

    private fun showMessage(messageRes: Int) {
        binding.loadingSpinner.isVisible = false
        binding.ticketContent.isVisible = false
        binding.ticketMessage.setText(messageRes)
        binding.ticketMessage.isVisible = true
    }

    companion object {
        private const val EXTRA_TOKEN = "ticket_token"
        private const val TOKEN_PARAMETER = "token"

        // The live address from CONSOLE-STEPS.md. The Worker builds the emailed link against
        // whichever Pages host called it, so a link from a preview build still opens here, and the
        // QR this screen draws always points at the live page.
        private const val TICKET_URL_BASE = "https://autism-allyship.pages.dev/ticket.html?token="

        // Well past the 200dp floor in RULES-APP.md, because the person scanning it is holding a
        // phone at arm's length in the sun.
        private const val QR_SIZE_DP = 280f

        fun newIntent(context: Context, token: String): Intent {
            return Intent(context, TicketDetailActivity::class.java)
                .putExtra(EXTRA_TOKEN, token)
        }
    }
}
