package ru.netology.yandexmap

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.netology.yandexmap.databinding.FragmentItemBinding
import ru.netology.yandexmap.dto.Marker

interface OnInteractionListener {
    fun onMarker(marker: Marker) {}
}

class MyItemRecyclerViewAdapter(
    private val values: List<Marker>,
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
        val marker = values[position]
        holder.idView.text = (position + 1).toString() + ") "
        holder.contentView.text = marker.text
        holder.contentView.setOnClickListener {
            onInteractionListener.onMarker(marker)
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