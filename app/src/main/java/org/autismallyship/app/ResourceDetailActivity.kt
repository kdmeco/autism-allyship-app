package org.autismallyship.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.autismallyship.app.data.Repository
import org.autismallyship.app.data.Resource
import org.autismallyship.app.databinding.ActivityResourceDetailBinding

class ResourceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResourceDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResourceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        // The screen is opened with an ID rather than the resource itself, so it still works after
        // the system has killed and restored the Activity.
        loadResource(intent.getStringExtra(EXTRA_RESOURCE_ID).orEmpty())
    }

    private fun loadResource(resourceId: String) {
        if (resourceId.isBlank()) {
            showMessage(R.string.resource_not_found)
            return
        }

        showLoading()
        Repository.loadResource(
            resourceId,
            onSuccess = { resource, fromCache ->
                if (resource == null) {
                    showMessage(R.string.resource_not_found)
                } else {
                    showResource(resource, fromCache)
                }
            },
            onError = { showMessage(R.string.resource_load_failed) }
        )
    }

    private fun showResource(resource: Resource, fromCache: Boolean) {
        binding.toolbar.title = resource.name
        binding.resourceName.text = resource.name

        // The foundation fills these in as they get them, so an empty one is taken out of the
        // layout rather than left as a gap.
        binding.resourceCategory.text = resource.category
        binding.resourceCategory.isVisible = resource.category.isNotBlank()

        binding.resourceProvinces.text = resource.provinces.joinToString(", ")
        binding.resourceProvinces.isVisible = resource.provinces.isNotEmpty()

        binding.resourceDescription.text = resource.description
        binding.resourceDescription.isVisible = resource.description.isNotBlank()

        setUpCall(resource.phone)
        setUpEmail(resource.email)
        setUpWebsite(resource.website)
        setUpMap(resource.name)

        binding.offlineBanner.isVisible = fromCache
        binding.loadingSpinner.isVisible = false
        binding.detailMessage.isVisible = false
        binding.detailContent.isVisible = true
    }

    // ACTION_DIAL opens the dialler with the number filled in and the person presses call. ACTION_CALL
    // would place the call itself and needs the CALL_PHONE permission, which is not worth asking for.
    private fun setUpCall(phone: String) {
        binding.callButton.isVisible = phone.isNotBlank()
        if (phone.isBlank()) return

        binding.callButton.text = getString(R.string.resource_call, phone)
        binding.callButton.setOnClickListener {
            open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }
    }

    private fun setUpEmail(email: String) {
        binding.emailButton.isVisible = email.isNotBlank()
        if (email.isBlank()) return

        binding.emailButton.text = getString(R.string.resource_email, email)
        binding.emailButton.setOnClickListener {
            open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
        }
    }

    private fun setUpWebsite(website: String) {
        binding.websiteButton.isVisible = website.isNotBlank()
        if (website.isBlank()) return

        // Whoever adds a resource types this field by hand, so a bare domain gets a scheme before it
        // goes anywhere near an intent. Without one the intent does not resolve at all.
        val address = if (website.startsWith("http://") || website.startsWith("https://")) {
            website
        } else {
            "https://$website"
        }

        binding.websiteButton.setOnClickListener {
            open(Intent(Intent.ACTION_VIEW, Uri.parse(address)))
        }
    }

    // SCHEMA.md has no address field, the address lives inside the description, so the map can only
    // search for the organisation by name.
    private fun setUpMap(name: String) {
        binding.mapButton.isVisible = name.isNotBlank()
        if (name.isBlank()) return

        binding.mapButton.setOnClickListener {
            open(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(name)}")))
        }
    }

    // An emulator with no dialler, no mail app or no maps will throw rather than doing nothing
    // visible, and a person on a real phone can hit the same thing with the browser disabled.
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
        binding.offlineBanner.isVisible = false
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.detailMessage.setText(R.string.resources_loading)
        binding.detailMessage.isVisible = sensoryMode
        binding.detailContent.isVisible = false
    }

    private fun showMessage(messageRes: Int) {
        binding.offlineBanner.isVisible = false
        binding.loadingSpinner.isVisible = false
        binding.detailContent.isVisible = false
        binding.detailMessage.setText(messageRes)
        binding.detailMessage.isVisible = true
    }

    companion object {
        private const val EXTRA_RESOURCE_ID = "resource_id"

        fun newIntent(context: Context, resourceId: String): Intent {
            return Intent(context, ResourceDetailActivity::class.java)
                .putExtra(EXTRA_RESOURCE_ID, resourceId)
        }
    }
}
