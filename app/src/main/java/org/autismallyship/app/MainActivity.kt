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
            showFragment(R.id.nav_home)
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

    private fun addAllFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, HomeFragment(), HomeFragment.TAG)
            add(R.id.fragment_container, EventsFragment(), EventsFragment.TAG)
            add(R.id.fragment_container, SensoryFragment(), SensoryFragment.TAG)
            add(R.id.fragment_container, ResourcesFragment(), ResourcesFragment.TAG)
            add(R.id.fragment_container, MoreFragment(), MoreFragment.TAG)
        }.commit()
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
