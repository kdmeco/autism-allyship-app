package org.autismallyship.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.autismallyship.app.data.Event
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.FragmentEventsBinding

class EventsFragment : Fragment() {

    // Held as a nullable and cleared in onDestroyView, because a Firestore callback can arrive after
    // the view has gone and would otherwise write into a view that is no longer on screen.
    private var binding: FragmentEventsBinding? = null
    private val adapter = EventAdapter { event ->
        startActivity(EventWebViewActivity.newIntent(requireContext(), event.id, event.title))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEventsBinding.inflate(inflater, container, false)
        this.binding = binding

        binding.eventList.layoutManager = LinearLayoutManager(requireContext())
        binding.eventList.adapter = adapter

        loadEvents()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // Both calls run the same published query underneath, so the second is answered from the cache
    // Firestore filled on the first and costs no extra read. Nesting them keeps the screen from
    // drawing half a list while the other half is still arriving.
    private fun loadEvents() {
        showLoading()
        Repository.loadUpcomingEvents(
            onSuccess = { upcoming, fromCacheUpcoming ->
                Repository.loadPastEvents(
                    onSuccess = { past, fromCachePast ->
                        showLoaded(upcoming, past, fromCacheUpcoming || fromCachePast)
                    },
                    onError = { showMessage(R.string.events_load_failed) }
                )
            },
            onError = { showMessage(R.string.events_load_failed) }
        )
    }

    private fun showLoaded(upcoming: List<Event>, past: List<Event>, fromCache: Boolean) {
        val binding = this.binding ?: return

        if (upcoming.isEmpty() && past.isEmpty()) {
            showMessage(R.string.events_empty)
            return
        }

        adapter.showEvents(upcoming, past)
        binding.offlineBanner.isVisible = fromCache
        binding.loadingSpinner.isVisible = false
        binding.listMessage.isVisible = false
        binding.eventList.isVisible = true
    }

    private fun showLoading() {
        val binding = this.binding ?: return

        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so it
        // is replaced with a line of text rather than slowed down.
        val sensoryMode = AppSettings(requireContext()).isSensoryMode()
        binding.offlineBanner.isVisible = false
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.listMessage.setText(R.string.events_loading)
        binding.listMessage.isVisible = sensoryMode
        binding.eventList.isVisible = false
    }

    private fun showMessage(messageRes: Int) {
        val binding = this.binding ?: return
        binding.offlineBanner.isVisible = false
        binding.loadingSpinner.isVisible = false
        binding.eventList.isVisible = false
        binding.listMessage.setText(messageRes)
        binding.listMessage.isVisible = true
    }

    companion object {
        const val TAG = "EventsFragment"
    }
}
