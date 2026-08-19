package org.autismallyship.app

import android.os.Bundle
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
    private val adapter = BlogAdapter()

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

        loadPosts()
    }

    private fun loadPosts() {
        showLoading()
        Repository.loadPosts(
            onSuccess = { posts -> showPosts(posts) },
            onError = { showMessage(R.string.blog_load_failed) }
        )
    }

    private fun showLoading() {
        // Sensory mode allows no animation anywhere in the app, and a spinner is an animation, so
        // it is replaced with a line of text rather than slowed down.
        val sensoryMode = AppSettings(this).isSensoryMode()
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
}
