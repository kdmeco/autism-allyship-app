package org.autismallyship.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.autismallyship.app.data.Post
import org.autismallyship.app.databinding.ItemPostBinding
import java.text.SimpleDateFormat
import java.util.Locale

class BlogAdapter(
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<BlogAdapter.PostViewHolder>() {

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
        holder.bind(posts[position], onPostClick)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {

        // Day before month, matching South African date convention rather than the ambiguous
        // numeric MM/DD some locales default to.
        private val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

        fun bind(post: Post, onPostClick: (Post) -> Unit) {
            binding.postTitle.text = post.title

            binding.postCategory.text = post.category
            binding.postCategory.isVisible = post.category.isNotBlank()

            binding.postDate.text = post.publishedAt?.toDate()?.let { dateFormat.format(it) }.orEmpty()
            binding.postDate.isVisible = post.publishedAt != null

            // Glide handles the caching and, more importantly, cancels the previous request when a
            // row is recycled, so a slow image cannot land in the wrong card while someone scrolls.
            // A post with no image gets no box at all rather than an empty grey square.
            binding.postImage.isVisible = post.imageUrl.isNotBlank()
            if (post.imageUrl.isNotBlank()) {
                Glide.with(binding.postImage).load(post.imageUrl).into(binding.postImage)
            } else {
                Glide.with(binding.postImage).clear(binding.postImage)
            }

            // imageAlt is optional in SCHEMA.md and nothing makes the admin fill it in. Null is the
            // correct treatment for a decorative image, and it is what an empty alt means on the
            // website. The row's own description below carries the meaning either way.
            binding.postImage.contentDescription = post.imageAlt.ifBlank { null }

            binding.root.setOnClickListener { onPostClick(post) }

            // TalkBack treats a tappable row as one item, so the row carries its own description
            // rather than leaving the reader to stitch the separate text views together.
            val context = binding.root.context
            binding.root.contentDescription = if (post.category.isBlank()) {
                post.title
            } else {
                context.getString(R.string.cd_post_row, post.title, post.category)
            }
        }
    }
}
