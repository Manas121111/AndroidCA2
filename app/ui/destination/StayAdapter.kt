package com.smarttour360.app.ui.destination

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.smarttour360.app.R

class StayAdapter : RecyclerView.Adapter<StayAdapter.StayViewHolder>() {

    private val items = mutableListOf<EcoStay>()
    private var onClick: ((EcoStay) -> Unit)? = null

    fun setOnClickListener(listener: (EcoStay) -> Unit) {
        onClick = listener
    }

    fun submitList(newItems: List<EcoStay>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stay, parent, false)
        return StayViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: StayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class StayViewHolder(
        itemView: View,
        private val onClick: ((EcoStay) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val imageTone: View = itemView.findViewById(R.id.image_tone)
        private val textStayName: TextView = itemView.findViewById(R.id.text_stay_name)
        private val textStayMeta: TextView = itemView.findViewById(R.id.text_stay_meta)
        private val textStayPrice: TextView = itemView.findViewById(R.id.text_stay_price)
        private val textStayRating: TextView = itemView.findViewById(R.id.text_stay_rating)

        private var item: EcoStay? = null

        init {
            itemView.setOnClickListener { item?.let { data -> onClick?.invoke(data) } }
        }

        fun bind(item: EcoStay) {
            this.item = item
            textStayName.text = item.name
            textStayMeta.text = item.meta
            textStayPrice.text = item.price
            textStayRating.text = item.rating
            imageTone.setBackgroundColor(
                ContextCompat.getColor(itemView.context, item.imageTone)
            )
        }
    }
}
