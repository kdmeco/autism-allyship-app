package org.autismallyship.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import org.autismallyship.app.data.Attachment
import org.autismallyship.app.data.Event
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.ActivityEventDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding

    private val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        // The screen is opened with an ID rather than the event itself, so it still works after the
        // system has killed and restored the Activity.
        loadEvent(intent.getStringExtra(EXTRA_EVENT_ID).orEmpty())
    }

    private fun loadEvent(eventId: String) {
        if (eventId.isBlank()) {
            showMessage(R.string.event_not_found)
            return
        }

        showLoading()
        Repository.loadEvent(
            eventId,
            onSuccess = { event, _ ->
                if (event == null) {
                    showMessage(R.string.event_not_found)
                } else {
                    showEvent(event)
                }
            },
            onError = { showMessage(R.string.event_load_failed) }
        )
    }

    private fun showEvent(event: Event) {
        binding.toolbar.title = event.title
        binding.eventTitle.text = event.title

        val startsAt = event.startsAt?.toDate()
        binding.eventDate.text = startsAt?.let { dateFormat.format(it) }.orEmpty()
        binding.eventDate.isVisible = startsAt != null

        showPrice(event)
        showCapacity(event)
        showRegisterButton(event)
        showDescription(event.description)
        setUpCalendar(event)
        showAttachments(event.attachments)

        binding.loadingSpinner.isVisible = false
        binding.detailMessage.isVisible = false
        binding.detailContent.isVisible = true
    }

    // SCHEMA.md sets price to 0 on a free event and isTicketed to false when there is no payment
    // step, so either one on its own is enough to call it free.
    private fun showPrice(event: Event) {
        binding.eventPrice.text = if (!event.isTicketed || event.price <= 0) {
            getString(R.string.event_free)
        } else {
            getString(R.string.event_price, formatRands(event.price))
        }
    }

    // Rands are whole numbers on every event the foundation has run, so a trailing ",00" would be
    // noise. Cents are only shown when there actually are any.
    private fun formatRands(price: Double): String {
        return if (price % 1.0 == 0.0) {
            price.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", price)
        }
    }

    // A capacity of 0 means unlimited, so the line is left out rather than claiming a number.
    private fun showCapacity(event: Event) {
        if (event.capacity <= 0) {
            binding.eventCapacity.isVisible = false
            return
        }

        val left = (event.capacity - event.ticketsSold).coerceAtLeast(0).toInt()
        binding.eventCapacity.text = if (left == 0) {
            getString(R.string.event_sold_out)
        } else {
            resources.getQuantityString(R.plurals.event_places_left, left, left)
        }
        binding.eventCapacity.isVisible = true
    }

    // The register button only ever offers what the Worker will actually accept: not sold out
    // and not already past. Mirrors the same checks registerTicket makes in the api repo, so
    // the button never invites a booking the server is about to refuse. A priced event is
    // still bookable: the price is collected by EFT or in cash at the event, never here.
    private fun showRegisterButton(event: Event) {
        val isSoldOut = event.capacity > 0 && event.ticketsSold >= event.capacity
        val canRegister = !isSoldOut && Repository.isUpcoming(event, Timestamp.now())

        binding.registerButton.isVisible = canRegister
        if (canRegister) {
            binding.registerButton.setOnClickListener {
                startActivity(EventRegistrationActivity.newIntent(this, event.id, event.title))
            }
        }
    }

    // SCHEMA.md calls description rich text, so it is parsed as HTML rather than printed with the
    // tags showing. Plain text passes through unchanged.
    private fun showDescription(description: String) {
        binding.eventDescription.text =
            HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding.eventDescription.isVisible = description.isNotBlank()
    }

    // ACTION_INSERT hands the event to whichever calendar app the person uses and lets them save it
    // themselves, so the app needs no calendar permission at all.
    //
    // No end time is sent, because SCHEMA.md has no endsAt and guessing a duration would put a
    // wrong finish time in someone's diary. No location is sent either, for the same reason: there
    // is no venue field. The description carries the venue, so it goes across as plain text and the
    // person can read the address there.
    private fun setUpCalendar(event: Event) {
        val startsAt = event.startsAt?.toDate()
        binding.calendarButton.isVisible = startsAt != null
        if (startsAt == null) return

        binding.calendarButton.setOnClickListener {
            val details = HtmlCompat
                .fromHtml(event.description, HtmlCompat.FROM_HTML_MODE_COMPACT)
                .toString()
                .trim()

            open(
                Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, event.title)
                    .putExtra(CalendarContract.Events.DESCRIPTION, details)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startsAt.time)
            )
        }
    }

    // Admins upload whatever they like, so the count is not known ahead of time and the buttons are
    // built from the list rather than sitting in the layout waiting to be filled.
    private fun showAttachments(attachments: List<Attachment>) {
        binding.attachmentsContainer.removeAllViews()

        val usable = attachments.filter { it.url.isNotBlank() }
        binding.attachmentsLabel.isVisible = usable.isNotEmpty()

        for (attachment in usable) {
            val button = layoutInflater.inflate(
                R.layout.item_attachment,
                binding.attachmentsContainer,
                false
            ) as MaterialButton

            button.text = attachment.name.ifBlank { getString(R.string.event_attachment) }
            button.setOnClickListener {
                open(Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url)))
            }
            binding.attachmentsContainer.addView(button)
        }
    }

    // An emulator with no calendar, no browser or no PDF viewer will throw rather than doing
    // nothing visible, and a person on a real phone can hit the same thing.
    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading() {
        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so it
        // is replaced with a line of text rather than slowed down.
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.detailMessage.setText(R.string.event_loading)
        binding.detailMessage.isVisible = sensoryMode
        binding.detailContent.isVisible = false
    }

    private fun showMessage(messageRes: Int) {
        binding.loadingSpinner.isVisible = false
        binding.detailContent.isVisible = false
        binding.detailMessage.setText(messageRes)
        binding.detailMessage.isVisible = true
    }

    companion object {
        private const val EXTRA_EVENT_ID = "event_id"

        fun newIntent(context: Context, eventId: String): Intent {
            return Intent(context, EventDetailActivity::class.java)
                .putExtra(EXTRA_EVENT_ID, eventId)
        }
    }
}
