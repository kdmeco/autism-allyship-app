package org.autismallyship.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import org.autismallyship.app.data.Repository
import org.autismallyship.app.data.Ticket
import org.autismallyship.app.databinding.ActivityMyTicketsBinding

class MyTicketsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyTicketsBinding
    private val adapter = TicketAdapter { ticket ->
        startActivity(TicketDetailActivity.newIntent(this, ticket.id))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMyTicketsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.ticketsRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.ticketList.layoutManager = LinearLayoutManager(this)
        binding.ticketList.adapter = adapter

        // Expanding the past section must not slide rows around in sensory mode, and there is no
        // reason to animate it in either mode, so the animator goes rather than being conditional.
        binding.ticketList.itemAnimator = null
    }

    // Reloaded on every return rather than only on create, so a ticket scanned at the door shows as
    // redeemed when someone comes back to this screen, and a ticket opened from a link since the
    // last visit is here.
    override fun onStart() {
        super.onStart()
        loadTickets()
    }

    private fun loadTickets() {
        val tokens = TicketStore(this).tokens().toList()
        if (tokens.isEmpty()) {
            showMessage(R.string.tickets_empty)
            return
        }

        showLoading()
        Repository.loadMyTickets(
            tokens,
            onSuccess = { upcoming, past, fromCache -> showLoaded(upcoming, past, fromCache) },
            onError = { showMessage(R.string.tickets_load_failed) }
        )
    }

    private fun showLoaded(upcoming: List<Ticket>, past: List<Ticket>, fromCache: Boolean) {
        if (upcoming.isEmpty() && past.isEmpty()) {
            showMessage(R.string.tickets_empty)
            return
        }

        adapter.showTickets(upcoming, past)
        binding.offlineBanner.isVisible = fromCache
        binding.loadingSpinner.isVisible = false
        binding.listMessage.isVisible = false
        binding.ticketList.isVisible = true
    }

    private fun showLoading() {
        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so it
        // is replaced with a line of text rather than slowed down.
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.offlineBanner.isVisible = false
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.listMessage.setText(R.string.tickets_loading)
        binding.listMessage.isVisible = sensoryMode
        binding.ticketList.isVisible = false
    }

    private fun showMessage(messageRes: Int) {
        binding.loadingSpinner.isVisible = false
        binding.ticketList.isVisible = false
        binding.offlineBanner.isVisible = false
        binding.listMessage.setText(messageRes)
        binding.listMessage.isVisible = true
    }
}
