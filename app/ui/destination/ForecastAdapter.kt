package com.smarttour360.app.ui.destination

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smarttour360.app.R

class ForecastAdapter : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    private val items = mutableListOf<ForecastDay>()

    fun submitList(newItems: List<ForecastDay>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast_day, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ForecastViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val textDay: TextView = itemView.findViewById(R.id.text_day)
        private val textIcon: TextView = itemView.findViewById(R.id.text_icon)
        private val textTemp: TextView = itemView.findViewById(R.id.text_temp)

        fun bind(item: ForecastDay) {
            textDay.text = item.day
            textIcon.text = item.icon
            textTemp.text = item.temperature
        }
    }
}
