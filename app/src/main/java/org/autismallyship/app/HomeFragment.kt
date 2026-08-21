package org.autismallyship.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import org.autismallyship.app.data.Event
import org.autismallyship.app.data.Post
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    // Held as a nullable and cleared in onDestroyView, because a Firestore callback can arrive after
    // the view has gone and would otherwise write into a view that is no longer on screen.
    private var binding: FragmentHomeBinding? = null
    private val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())
    private val postDateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        this.binding = binding
        binding.greeting.setText(greetingRes())
        setUpQuickAccess(binding)
        loadNextEvent()
        loadLatestPost()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun greetingRes(): Int {
        return when (LocalTime.now().hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..16 -> R.string.home_greeting_afternoon
            in 17..21 -> R.string.home_greeting_evening
            else -> R.string.home_greeting_welcome
        }
    }

    private fun setUpQuickAccess(binding: FragmentHomeBinding) {
        binding.quickEvents.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_events)
        }
        binding.quickSensory.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_sensory)
        }
        binding.quickResources.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_resources)
        }
        binding.quickDonate.setOnClickListener {
            startActivity(Intent(requireContext(), DonateActivity::class.java))
        }
        binding.quickContact.setOnClickListener {
            startActivity(Intent(requireContext(), ContactActivity::class.java))
        }
        binding.quickWhatsapp.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SiteUrls.WHATSAPP_DIRECT)))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Quiet on failure rather than showing an error banner: the Events tab is the real place to
    // see what is wrong, this block is a preview and staying hidden is enough.
    private var nextEventFromCache = false
    private var latestPostFromCache = false
    private var nextEventVisible = false
    private var latestPostVisible = false

    private fun loadNextEvent() {
        Repository.loadNextEvent(
            onSuccess = { event, fromCache ->
                nextEventFromCache = fromCache
                showNextEvent(event)
                refreshOfflineBanner()
            },
            onError = { }
        )
    }

    private fun showNextEvent(event: Event?) {
        val binding = this.binding ?: return
        if (event == null) {
            binding.nextEventCard.isVisible = false
            nextEventVisible = false
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
            happeningToday -> getString(
                R.string.cd_event_row_today,
                getString(R.string.event_happening_today),
                event.title,
                date
            )
            date.isBlank() -> event.title
            else -> getString(R.string.cd_event_row, event.title, date)
        }

        binding.nextEventCard.setOnClickListener {
            startActivity(EventWebViewActivity.newIntent(requireContext(), event.id, event.title))
        }
        binding.nextEventCard.isVisible = true
        nextEventVisible = true
    }

    private fun loadLatestPost() {
        Repository.loadLatestPost(
            onSuccess = { post, fromCache ->
                latestPostFromCache = fromCache
                showLatestPost(post)
                refreshOfflineBanner()
            },
            onError = { }
        )
    }

    private fun showLatestPost(post: Post?) {
        val binding = this.binding ?: return
        if (post == null) {
            binding.latestPostCard.isVisible = false
            latestPostVisible = false
            return
        }

        binding.latestPostTitle.text = post.title
        val date = post.publishedAt?.toDate()?.let { postDateFormat.format(it) }.orEmpty()
        binding.latestPostMeta.text = when {
            post.category.isNotBlank() && date.isNotBlank() ->
                getString(R.string.cd_post_row, post.category, date)
            post.category.isNotBlank() -> post.category
            else -> date
        }

        binding.latestPostImage.isVisible = post.imageUrl.isNotBlank()
        if (post.imageUrl.isNotBlank()) {
            Glide.with(binding.latestPostImage).load(post.imageUrl).into(binding.latestPostImage)
            binding.latestPostImage.contentDescription = post.imageAlt.ifBlank { null }
        } else {
            Glide.with(binding.latestPostImage).clear(binding.latestPostImage)
        }

        binding.latestPostCard.contentDescription = if (post.category.isBlank()) {
            post.title
        } else {
            getString(R.string.cd_post_row, post.title, post.category)
        }
        binding.latestPostCard.setOnClickListener {
            startActivity(
                BlogArticleActivity.newIntent(
                    requireContext(),
                    post.title,
                    SiteUrls.blogArticle(requireContext(), post.id)
                )
            )
        }
        binding.latestPostCard.isVisible = true
        latestPostVisible = true
    }

    // Compact banner only when a preview card that is actually on screen came from the disk cache.
    private fun refreshOfflineBanner() {
        val binding = this.binding ?: return
        val show = (nextEventVisible && nextEventFromCache) || (latestPostVisible && latestPostFromCache)
        binding.offlineBanner.isVisible = show
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
