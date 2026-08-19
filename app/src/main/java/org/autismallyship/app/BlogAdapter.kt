package org.autismallyship.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.autismallyship.app.data.Post
import org.autismallyship.app.databinding.ItemPostBinding
import java.text.SimpleDateFormat
import java.util.Locale

class BlogAdapter : RecyclerView.Adapter<BlogAdapter.PostViewHolder>() {

    private val posts = mutableListOf<Post>()

    fun showPosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {

        // Day before month, matching South African date convention rather than the ambiguous
        // numeric MM/DD some locales default to.
        private val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

        fun bind(post: Post) {
            binding.postTitle.text = post.title

            binding.postCategory.text = post.category
            binding.postCategory.isVisible = post.category.isNotBlank()

            binding.postDate.text = post.publishedAt?.toDate()?.let { dateFormat.format(it) }.orEmpty()
            binding.postDate.isVisible = post.publishedAt != null

            // No image loading library is wired in yet, so the ImageView keeps its placeholder
            // background colour rather than showing a broken image icon.
        }
    }
}
