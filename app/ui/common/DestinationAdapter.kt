package com.smarttour360.app.ui.common

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.request.ErrorResult
import coil.request.SuccessResult
import coil.load
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import com.smarttour360.app.utils.DestinationImageUrl

class DestinationAdapter(
    private val onClick: (DestinationSummary) -> Unit,
    private val onAddToTrip: ((DestinationSummary) -> Unit)? = null
) : RecyclerView.Adapter<DestinationAdapter.DestinationViewHolder>() {
    private val items = mutableListOf<DestinationSummary>()

    fun submitList(data: List<DestinationSummary>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destination_card, parent, false)
        return DestinationViewHolder(view, onClick, onAddToTrip)
    }

    override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class DestinationViewHolder(
        itemView: View,
        private val onClick: (DestinationSummary) -> Unit,
        private val onAddToTrip: ((DestinationSummary) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.image_destination)
        private val imageCredit: TextView = itemView.findViewById(R.id.text_destination_image_credit)
        private val title: TextView = itemView.findViewById(R.id.text_destination_name)
        private val subtitle: TextView = itemView.findViewById(R.id.text_destination_subtitle)
        private val metrics: TextView = itemView.findViewById(R.id.text_destination_metrics)
        private val addButton: MaterialButton = itemView.findViewById(R.id.button_add_destination)
        private var item: DestinationSummary? = null

        init {
            itemView.setOnClickListener { item?.let(onClick) }
            addButton.setOnClickListener { item?.let { current -> onAddToTrip?.invoke(current) } }
        }

        fun bind(data: DestinationSummary) {
            item = data
            title.text = "${data.name}, ${data.subtitle}"
            subtitle.text = "${data.flag} risk | Eco ${data.ecoScore} | Ethical ${data.ethicalScore}"
            metrics.text = "Rating ${data.rating}   Carbon ~${data.carbonKg}kg"
            val imageUrl = data.imageUrl ?: DestinationImageUrl.forDestination(data.name)
            Log.d("SmartTourImage", "Destination card load url=$imageUrl place=${data.name}")
            image.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_hero)
                error(R.drawable.bg_hero)
                listener(
                    onSuccess = { _, result: SuccessResult ->
                        Log.d("SmartTourImage", "Destination card success place=${data.name} source=${result.dataSource}")
                    },
                    onError = { _, result: ErrorResult ->
                        Log.e("SmartTourImage", "Destination card failed place=${data.name} url=$imageUrl throwable=${result.throwable}")
                    }
                )
            }
            imageCredit.text = data.imageAttribution
            imageCredit.visibility = if (data.imageAttribution.isNullOrBlank()) View.GONE else View.VISIBLE
            addButton.visibility = if (onAddToTrip == null) View.GONE else View.VISIBLE
        }
    }
}
