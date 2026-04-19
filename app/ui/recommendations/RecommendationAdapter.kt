package com.smarttour360.app.ui.recommendations

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
import com.smarttour360.app.R
import com.smarttour360.app.ui.common.DestinationSummary
import com.smarttour360.app.ui.common.TravelSuggestion
import com.smarttour360.app.utils.DestinationImageUrl

class RecommendationAdapter(
    private val onDestinationClick: (String) -> Unit
) : RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder>() {
    private val items = mutableListOf<TravelSuggestion>()

    fun submitList(data: List<TravelSuggestion>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommendation_card, parent, false)
        return RecommendationViewHolder(view, onDestinationClick)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class RecommendationViewHolder(
        itemView: View,
        private val onDestinationClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.image_recommendation)
        private val imageCredit: TextView = itemView.findViewById(R.id.text_recommendation_image_credit)
        private val title: TextView = itemView.findViewById(R.id.text_recommendation_title)
        private val region: TextView = itemView.findViewById(R.id.text_recommendation_region)
        private val summary: TextView = itemView.findViewById(R.id.text_recommendation_summary)
        private val reason: TextView = itemView.findViewById(R.id.text_recommendation_reason)
        private val budget: TextView = itemView.findViewById(R.id.text_recommendation_budget)
        private val route: TextView = itemView.findViewById(R.id.text_recommendation_route)
        private val tags: TextView = itemView.findViewById(R.id.text_recommendation_tags)
        private var destinationName: String? = null

        init {
            itemView.setOnClickListener { destinationName?.let(onDestinationClick) }
        }

        fun bind(item: TravelSuggestion) {
            destinationName = item.destinationName
            val imageUrl = item.imageUrl ?: DestinationImageUrl.forDestination(item.destinationName)
            Log.d("SmartTourImage", "Recommendation load url=$imageUrl place=${item.destinationName}")
            image.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_hero)
                error(R.drawable.bg_hero)
                listener(
                    onSuccess = { _, result: SuccessResult ->
                        Log.d("SmartTourImage", "Recommendation success place=${item.destinationName} source=${result.dataSource}")
                    },
                    onError = { _, result: ErrorResult ->
                        Log.e("SmartTourImage", "Recommendation failed place=${item.destinationName} url=$imageUrl throwable=${result.throwable}")
                    }
                )
            }
            imageCredit.text = item.imageAttribution
            imageCredit.visibility = if (item.imageAttribution.isNullOrBlank()) View.GONE else View.VISIBLE
            title.text = item.title
            region.text = "${item.region} | ${item.safetyFlag} | Eco ${item.ecoScore}"
            summary.text = item.summary
            reason.text = "${item.rankReason} ${item.liveSignal}"
            budget.text = item.budgetLabel
            route.text = item.routeHint
            tags.text = item.vibeTags.joinToString("   ")
        }
    }
}

class ExploreDestinationAdapter(
    private val onClick: (DestinationSummary) -> Unit
) : RecyclerView.Adapter<ExploreDestinationAdapter.ExploreViewHolder>() {
    private val items = mutableListOf<DestinationSummary>()

    fun submitList(data: List<DestinationSummary>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_explore_destination, parent, false)
        return ExploreViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ExploreViewHolder(
        itemView: View,
        private val onClick: (DestinationSummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.text_explore_destination_name)
        private val meta: TextView = itemView.findViewById(R.id.text_explore_destination_meta)
        private val metric: TextView = itemView.findViewById(R.id.text_explore_destination_metric)
        private var item: DestinationSummary? = null

        init {
            itemView.setOnClickListener { item?.let(onClick) }
        }

        fun bind(data: DestinationSummary) {
            item = data
            title.text = data.name
            meta.text = data.subtitle
            metric.text = "${data.flag} | Eco ${data.ecoScore} | Rating ${data.rating}"
        }
    }
}
