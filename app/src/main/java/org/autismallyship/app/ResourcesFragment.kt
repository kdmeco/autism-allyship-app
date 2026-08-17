package org.autismallyship.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.autismallyship.app.databinding.FragmentResourcesBinding

class ResourcesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentResourcesBinding.inflate(inflater, container, false).root
    }

    companion object {
        const val TAG = "ResourcesFragment"
    }
}
