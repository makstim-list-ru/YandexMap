package ru.netology.yandexmap

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.netology.yandexmap.databinding.FragmentItemBinding
import ru.netology.yandexmap.dto.Post

interface OnInteractionListener {
    fun onLike(post: Post) {}
}

class MyItemRecyclerViewAdapter(
    private val values: List<Post>,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = values[position]
        holder.idView.text = post.id.toString()
        holder.contentView.text = post.text
        holder.contentView.setOnClickListener {
            onInteractionListener.onLike(post)
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content


        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}