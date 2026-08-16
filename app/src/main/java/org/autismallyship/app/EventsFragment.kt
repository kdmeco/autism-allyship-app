package org.autismallyship.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.autismallyship.app.databinding.FragmentEventsBinding

class EventsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentEventsBinding.inflate(inflater, container, false).root
    }

    companion object {
        const val TAG = "EventsFragment"
    }
}
