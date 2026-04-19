package com.smarttour360.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smarttour360.app.R
import com.smarttour360.app.ui.common.DestinationSummary

class SafetySpotlightAdapter(
    private val onClick: (DestinationSummary) -> Unit
) : RecyclerView.Adapter<SafetySpotlightAdapter.SafetyViewHolder>() {
    private val items = mutableListOf<DestinationSummary>()

    fun submitList(data: List<DestinationSummary>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SafetyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_safety_spotlight, parent, false)
        return SafetyViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: SafetyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class SafetyViewHolder(
        itemView: View,
        private val onClick: (DestinationSummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.text_safety_name)
        private val meta: TextView = itemView.findViewById(R.id.text_safety_meta)
        private var item: DestinationSummary? = null

        init {
            itemView.setOnClickListener { item?.let(onClick) }
        }

        fun bind(destination: DestinationSummary) {
            item = destination
            title.text = "${destination.flag}  ${destination.name}"
            val status = when (destination.flag) {
                "GREEN" -> "Safe to travel"
                "YELLOW" -> "Travel with caution"
                else -> "Avoid for now"
            }
            meta.text = "$status  |  Eco ${destination.ecoScore}  |  ${destination.subtitle}"
        }
    }
}
