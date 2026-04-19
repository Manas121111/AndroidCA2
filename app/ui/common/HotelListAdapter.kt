package com.smarttour360.app.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R

class HotelListAdapter(
    private val onClick: (HotelSummary) -> Unit,
    private val onAddToTrip: ((HotelSummary) -> Unit)? = null
) : RecyclerView.Adapter<HotelListAdapter.HotelViewHolder>() {
    private val items = mutableListOf<HotelSummary>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hotel_card, parent, false)
        return HotelViewHolder(view, onClick, onAddToTrip)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<HotelSummary>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class HotelViewHolder(
        itemView: View,
        private val onClick: (HotelSummary) -> Unit,
        private val onAddToTrip: ((HotelSummary) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.text_hotel_name)
        private val subtitle: TextView = itemView.findViewById(R.id.text_hotel_subtitle)
        private val metrics: TextView = itemView.findViewById(R.id.text_hotel_metrics)
        private val warning: TextView = itemView.findViewById(R.id.text_hotel_warning)
        private val addButton: MaterialButton = itemView.findViewById(R.id.button_add_hotel_to_trip)
        private var item: HotelSummary? = null

        init {
            itemView.setOnClickListener { item?.let(onClick) }
            addButton.setOnClickListener { item?.let { current -> onAddToTrip?.invoke(current) } }
        }

        fun bind(data: HotelSummary) {
            item = data
            title.text = data.name
            subtitle.text = "${data.subtitle}\n${data.priceText}   |   Eco ${data.ecoScore}"
            metrics.text = "${if (data.ecoCertified) "Eco certified" else "Standard stay"}   |   Price ${data.trend}"
            warning.visibility = if (data.hasHiddenFees) View.VISIBLE else View.GONE
            addButton.visibility = if (onAddToTrip == null) View.GONE else View.VISIBLE
        }
    }
}
