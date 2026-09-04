package org.autismallyship.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.autismallyship.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: AppSettings
    private var sensoryModeApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = AppSettings(this)
        applyAppTheme(settings)
        sensoryModeApplied = settings.isSensoryMode()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // bottom is left at zero because the bottom nav insets itself for the gesture bar
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            showFragment(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            addAllFragments()
        } else {
            showFragment(binding.bottomNav.selectedItemId)
        }
    }

    override fun onResume() {
        super.onResume()
        if (settings.isSensoryMode() != sensoryModeApplied) {
            recreate()
        }
    }

    // All five tabs are added once and kept alive, so switching tab is a hide and
    // a show rather than a rebuild and each tab keeps its scroll position.
    //
    // The four inactive tabs are hidden inside this same transaction rather than
    // by a showFragment() call after it. commit() only queues the transaction, so
    // at the moment that call would run, supportFragmentManager.fragments is still
    // empty: it would hide nothing, and all five tabs would paint on top of each
    // other. Home is the tab BottomNavigationView selects by default, so it is the
    // one left showing.
    private fun addAllFragments() {
        val home = HomeFragment()
        val events = EventsFragment()
        val sensory = SensoryFragment()
        val resources = ResourcesFragment()
        val more = MoreFragment()
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, home, HomeFragment.TAG)
            add(R.id.fragment_container, events, EventsFragment.TAG)
            add(R.id.fragment_container, sensory, SensoryFragment.TAG)
            add(R.id.fragment_container, resources, ResourcesFragment.TAG)
            add(R.id.fragment_container, more, MoreFragment.TAG)
            hide(events)
            hide(sensory)
            hide(resources)
            hide(more)
        }.commit()
    }

    fun selectTab(menuItemId: Int) {
        binding.bottomNav.selectedItemId = menuItemId
    }

    private fun showFragment(menuItemId: Int) {
        val tag = when (menuItemId) {
            R.id.nav_events -> EventsFragment.TAG
            R.id.nav_sensory -> SensoryFragment.TAG
            R.id.nav_resources -> ResourcesFragment.TAG
            R.id.nav_more -> MoreFragment.TAG
            else -> HomeFragment.TAG
        }
        val transaction = supportFragmentManager.beginTransaction()
        for (fragment in supportFragmentManager.fragments) {
            if (fragment.tag == tag) transaction.show(fragment) else transaction.hide(fragment)
        }
        transaction.commit()
    }
}
