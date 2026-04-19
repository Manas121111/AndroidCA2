package com.smarttour360.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R

class TransportOptionAdapter(
    private val onActionClick: (TransportOption) -> Unit
) : RecyclerView.Adapter<TransportOptionAdapter.TransportOptionViewHolder>() {
    private val items = mutableListOf<TransportOption>()

    fun submitList(data: List<TransportOption>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransportOptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transport_option, parent, false)
        return TransportOptionViewHolder(view, onActionClick)
    }

    override fun onBindViewHolder(holder: TransportOptionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TransportOptionViewHolder(
        itemView: View,
        private val onActionClick: (TransportOption) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val badge: TextView = itemView.findViewById(R.id.text_transport_badge)
        private val hint: TextView = itemView.findViewById(R.id.text_transport_hint)
        private val title: TextView = itemView.findViewById(R.id.text_transport_title)
        private val subtitle: TextView = itemView.findViewById(R.id.text_transport_subtitle)
        private val meta: TextView = itemView.findViewById(R.id.text_transport_meta)
        private val footer: TextView = itemView.findViewById(R.id.text_transport_footer)
        private val action: MaterialButton = itemView.findViewById(R.id.button_transport_action)
        private var item: TransportOption? = null

        init {
            action.setOnClickListener { item?.let(onActionClick) }
            itemView.setOnClickListener { item?.let(onActionClick) }
        }

        fun bind(option: TransportOption) {
            item = option
            badge.text = if (option.actionLabel.contains("MakeMyTrip", true) || option.actionLabel.contains("Cleartrip", true)) {
                "FLIGHT"
            } else if (option.actionLabel.contains("RedBus", true) || option.actionLabel.contains("AbhiBus", true)) {
                "BUS"
            } else {
                "BOOKING OPTION"
            }
            hint.text = option.actionLabel
            title.text = option.title
            subtitle.text = option.subtitle
            meta.text = option.meta
            footer.text = "Trip summary is ready before handoff."
            action.text = option.actionLabel
        }
    }
}
