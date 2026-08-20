package org.autismallyship.app

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import org.autismallyship.app.data.Post
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.ActivityBlogBinding

class BlogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlogBinding
    private val adapter = BlogAdapter { post ->
        startActivity(BlogArticleActivity.newIntent(this, post.title, articleUrl(post)))
    }

    // The whole published list, kept so the category filter runs over it in memory rather than
    // as a second query. The categories offered are exactly the ones present, so a selection can
    // never come back empty and there is no separate "no matches" state to design for.
    private var allPosts: List<Post> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBlogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.blogRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.title = getString(R.string.title_blog)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.postList.layoutManager = LinearLayoutManager(this)
        binding.postList.adapter = adapter
        binding.categoryInput.setOnItemClickListener { _, _, _, _ -> applyFilter() }

        loadPosts()
    }

    private fun loadPosts() {
        showLoading()
        Repository.loadPosts(
            onSuccess = { posts, fromCache -> showLoaded(posts, fromCache) },
            onError = { showMessage(R.string.blog_load_failed) }
        )
    }

    private fun showLoaded(posts: List<Post>, fromCache: Boolean) {
        allPosts = posts

        // Firestore tells us whether the answer came off the disk rather than the server, so the
        // banner appears exactly when what is on screen might be out of date.
        binding.offlineBanner.isVisible = fromCache

        fillCategoryOptions()
        applyFilter()
    }

    private fun fillCategoryOptions() {
        val all = getString(R.string.filter_all)
        val categories = allPosts
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val field = binding.categoryInput
        field.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf(all) + categories))
        // A rotation restores the text in this field before the data comes back, so a choice the
        // person already made is kept as long as it is still one of the options.
        if (field.text.toString() !in listOf(all) + categories) {
            field.setText(all, false)
        }
    }

    private fun applyFilter() {
        val all = getString(R.string.filter_all)
        val category = binding.categoryInput.text.toString()
        val matches = allPosts.filter { category == all || it.category == category }
        showPosts(matches)
    }

    private fun showLoading() {
        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so
        // it is replaced with a line of text rather than slowed down.
        val sensoryMode = AppSettings(this).isSensoryMode()
        binding.offlineBanner.isVisible = false
        binding.loadingSpinner.isVisible = !sensoryMode
        binding.listMessage.setText(R.string.blog_loading)
        binding.listMessage.isVisible = sensoryMode
        binding.postList.isVisible = false
    }

    private fun showPosts(posts: List<Post>) {
        adapter.showPosts(posts)
        binding.loadingSpinner.isVisible = false
        binding.listMessage.isVisible = false

        if (posts.isEmpty()) {
            showMessage(R.string.blog_empty)
        } else {
            binding.postList.isVisible = true
        }
    }

    private fun showMessage(messageRes: Int) {
        binding.loadingSpinner.isVisible = false
        binding.postList.isVisible = false
        binding.listMessage.setText(messageRes)
        binding.listMessage.isVisible = true
    }

    // The article itself is a page on the website, opened in a WebView. app=1 is what tells that
    // page to drop its own header, footer and share row, since this screen already has a toolbar
    // and the app has its own settings. Theme and sensory mode go across because the WebView keeps
    // its own storage and cannot see what was chosen in the app. If the website has not been
    // updated yet, the extra parameters are ignored and the full page loads, which still reads.
    private fun articleUrl(post: Post): String {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val theme = if (nightMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
        val sensory = if (AppSettings(this).isSensoryMode()) "on" else "off"

        return Uri.parse(SITE_URL).buildUpon()
            .appendPath("blog-post.html")
            .appendQueryParameter("id", post.id)
            .appendQueryParameter("app", "1")
            .appendQueryParameter("theme", theme)
            .appendQueryParameter("sensory", sensory)
            .build()
            .toString()
    }

    companion object {
        private const val SITE_URL = "https://autism-allyship.pages.dev"
    }
}
