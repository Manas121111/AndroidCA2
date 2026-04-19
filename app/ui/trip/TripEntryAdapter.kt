package com.smarttour360.app.ui.trip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smarttour360.app.R
import com.smarttour360.app.ui.state.TripEntry
import com.smarttour360.app.ui.state.TripItemType

class TripEntryAdapter(
    private val onRemoveClick: (TripEntry) -> Unit
) : RecyclerView.Adapter<TripEntryAdapter.TripViewHolder>() {
    private val items = mutableListOf<TripEntry>()

    fun submitList(data: List<TripEntry>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip_entry, parent, false)
        return TripViewHolder(view, onRemoveClick)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class TripViewHolder(
        itemView: View,
        private val onRemoveClick: (TripEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val type: TextView = itemView.findViewById(R.id.text_trip_item_type)
        private val budget: TextView = itemView.findViewById(R.id.text_trip_item_budget)
        private val title: TextView = itemView.findViewById(R.id.text_trip_item_title)
        private val subtitle: TextView = itemView.findViewById(R.id.text_trip_item_subtitle)
        private val timing: TextView = itemView.findViewById(R.id.text_trip_item_timing)
        private val route: TextView = itemView.findViewById(R.id.text_trip_item_route)
        private val score: TextView = itemView.findViewById(R.id.text_trip_item_score)
        private val remove: ImageButton = itemView.findViewById(R.id.button_remove_trip_item)
        private var item: TripEntry? = null

        init {
            remove.setOnClickListener { item?.let(onRemoveClick) }
        }

        fun bind(item: TripEntry) {
            this.item = item
            type.text = if (item.type == TripItemType.DESTINATION) "DESTINATION" else "STAY"
            budget.text = "₹${item.budgetInr}"
            title.text = item.title
            subtitle.text = item.subtitle
            timing.text = item.timing
            route.text = item.routeLabel
            score.text = item.scoreText
        }
    }
}
