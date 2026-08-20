package org.autismallyship.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.autismallyship.app.data.Event
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    // Held as a nullable and cleared in onDestroyView, because a Firestore callback can arrive after
    // the view has gone and would otherwise write into a view that is no longer on screen.
    private var binding: FragmentHomeBinding? = null
    private val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        this.binding = binding
        loadNextEvent()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // Quiet on failure rather than showing an error banner: the Events tab is the real place to
    // see what is wrong, this block is a preview and staying hidden is enough.
    private fun loadNextEvent() {
        Repository.loadNextEvent(
            onSuccess = { event -> showNextEvent(event) },
            onError = { }
        )
    }

    private fun showNextEvent(event: Event?) {
        val binding = this.binding ?: return
        if (event == null) {
            binding.nextEventCard.isVisible = false
            return
        }

        val startsAt = event.startsAt?.toDate()
        val date = startsAt?.let { dateFormat.format(it) }.orEmpty()
        binding.nextEventTitle.text = event.title
        binding.nextEventDate.text = date

        val happeningToday = startsAt != null && isToday(startsAt)
        binding.nextEventBadge.isVisible = happeningToday

        // Same shape as EventAdapter's row description: the badge first, since on the day it is
        // the part that changes what someone does next.
        binding.nextEventCard.contentDescription = when {
            happeningToday -> getString(R.string.cd_event_row_today, getString(R.string.event_happening_today), event.title, date)
            date.isBlank() -> event.title
            else -> getString(R.string.cd_event_row, event.title, date)
        }

        binding.nextEventCard.setOnClickListener {
            startActivity(EventWebViewActivity.newIntent(requireContext(), event.id, event.title))
        }
        binding.nextEventCard.isVisible = true
    }

    // Compared as a calendar date in the phone's own time zone, matching EventAdapter's rule for
    // the events list badge.
    private fun isToday(startsAt: Date): Boolean {
        val day = startsAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        return day == LocalDate.now()
    }

    companion object {
        const val TAG = "HomeFragment"
    }
}
