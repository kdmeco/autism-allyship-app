package org.autismallyship.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

// Every bubble only ever moves because a finger tapped it, so this tool needs no special
// handling in sensory mode, per RULES-APP.md: "A tool that is still when nobody is touching it
// needs no special handling in sensory mode." The pop animation below is a direct, immediate
// response to that tap, not something that runs on its own.
class BubbleAdapter(
    private val bubbleCount: Int,
    private val onPop: () -> Unit
) : RecyclerView.Adapter<BubbleAdapter.BubbleViewHolder>() {

    private val popped = BooleanArray(bubbleCount)

    fun reset() {
        popped.fill(false)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bubble, parent, false)
        return BubbleViewHolder(view)
    }

    override fun onBindViewHolder(holder: BubbleViewHolder, position: Int) {
        holder.bind(popped[position]) {
            if (!popped[position]) {
                popped[position] = true
                onPop()
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = bubbleCount

    class BubbleViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        fun bind(isPopped: Boolean, onClick: () -> Unit) {
            val tint = if (isPopped) R.color.sensory_pop_it_fill else R.color.sensory_pop_it
            view.backgroundTintList = ContextCompat.getColorStateList(view.context, tint)
            view.isEnabled = !isPopped
            view.contentDescription = view.context.getString(
                if (isPopped) R.string.pop_it_bubble_popped else R.string.pop_it_bubble_unpopped
            )

            view.setOnClickListener {
                // A quick, direct response to the tap that made it happen, not a lingering or
                // autonomous animation, so this is fine in every mode including sensory mode.
                view.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(80)
                    .withEndAction {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    }
                    .start()
                onClick()
            }
        }
    }
}
