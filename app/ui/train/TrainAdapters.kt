package com.smarttour360.app.ui.train

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R

class StationSuggestAdapter(
    private val onStationSelected: (Station) -> Unit
) : RecyclerView.Adapter<StationSuggestAdapter.StationViewHolder>() {
    private val items = mutableListOf<Station>()

    fun submitList(data: List<Station>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station_suggestion, parent, false)
        return StationViewHolder(view, onStationSelected)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class StationViewHolder(
        itemView: View,
        private val onStationSelected: (Station) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tvStationName)
        private val state: TextView = itemView.findViewById(R.id.tvStationState)
        private val code: TextView = itemView.findViewById(R.id.tvStationCode)
        private var item: Station? = null

        init {
            itemView.setOnClickListener { item?.let(onStationSelected) }
        }

        fun bind(station: Station) {
            item = station
            name.text = station.name
            state.text = if (station.state.isBlank()) station.code else station.state
            code.text = station.code
        }
    }
}

class TrainResultsAdapter(
    private val onTrainClick: (TrainResult) -> Unit,
    private val onBookClick: (TrainResult) -> Unit
) : RecyclerView.Adapter<TrainResultsAdapter.TrainViewHolder>() {
    private val items = mutableListOf<TrainResult>()

    fun submitList(data: List<TrainResult>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_train_result, parent, false)
        return TrainViewHolder(view, onTrainClick, onBookClick)
    }

    override fun onBindViewHolder(holder: TrainViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TrainViewHolder(
        itemView: View,
        private val onTrainClick: (TrainResult) -> Unit,
        private val onBookClick: (TrainResult) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val trainNumber: TextView = itemView.findViewById(R.id.tvTrainNumber)
        private val trainName: TextView = itemView.findViewById(R.id.tvTrainName)
        private val departure: TextView = itemView.findViewById(R.id.tvDeparture)
        private val arrival: TextView = itemView.findViewById(R.id.tvArrival)
        private val duration: TextView = itemView.findViewById(R.id.tvDuration)
        private val fromStation: TextView = itemView.findViewById(R.id.tvFromStation)
        private val toStation: TextView = itemView.findViewById(R.id.tvToStation)
        private val classes: TextView = itemView.findViewById(R.id.tvClasses)
        private val days: TextView = itemView.findViewById(R.id.tvRunsDays)
        private val liveTag: TextView = itemView.findViewById(R.id.tvLiveTag)
        private val scheduleButton: MaterialButton = itemView.findViewById(R.id.btnViewSchedule)
        private val bookButton: MaterialButton = itemView.findViewById(R.id.btnBook)
        private var item: TrainResult? = null

        init {
            itemView.setOnClickListener { item?.let(onTrainClick) }
            scheduleButton.setOnClickListener { item?.let(onTrainClick) }
            bookButton.setOnClickListener { item?.let(onBookClick) }
        }

        fun bind(train: TrainResult) {
            item = train
            trainNumber.text = train.number
            trainName.text = train.name
            departure.text = train.departure
            arrival.text = train.arrival
            duration.text = train.duration
            fromStation.text = train.fromStation.name
            toStation.text = train.toStation.name
            liveTag.text = if (train.classes.isEmpty()) "CHECK" else "LIVE"
            classes.text = if (train.classes.isEmpty()) {
                "Classes unavailable"
            } else {
                "Classes: ${train.classes.joinToString(" • ")}"
            }
            days.text = if (train.runsDays.isEmpty()) "Runs on request" else "Runs: ${train.runsDays.joinToString(", ")}"
        }
    }
}

class TrainStopAdapter : RecyclerView.Adapter<TrainStopAdapter.StopViewHolder>() {
    private val items = mutableListOf<TrainStop>()

    fun submitList(data: List<TrainStop>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_train_stop, parent, false)
        return StopViewHolder(view)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.bind(items[position], position == 0, position == itemCount - 1)
    }

    override fun getItemCount(): Int = items.size

    class StopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stationName: TextView = itemView.findViewById(R.id.tvStationName)
        private val stationCode: TextView = itemView.findViewById(R.id.tvStationCode)
        private val arrival: TextView = itemView.findViewById(R.id.tvArrival)
        private val departure: TextView = itemView.findViewById(R.id.tvDeparture)
        private val halt: TextView = itemView.findViewById(R.id.tvHalt)
        private val distance: TextView = itemView.findViewById(R.id.tvDistance)
        private val day: TextView = itemView.findViewById(R.id.tvDay)
        private val dot: View = itemView.findViewById(R.id.stationDot)

        fun bind(stop: TrainStop, isFirst: Boolean, isLast: Boolean) {
            stationName.text = stop.stationName
            stationCode.text = stop.stationCode
            arrival.text = if (stop.scheduledArrival.equals("Source", true)) "--" else stop.scheduledArrival
            departure.text = if (stop.scheduledDeparture.equals("Destination", true)) "--" else stop.scheduledDeparture
            halt.text = if (stop.haltMinutes > 0) "${stop.haltMinutes} min halt" else "No halt"
            distance.text = "${stop.distanceKm} km"
            day.text = "Day ${stop.day}"

            val colorRes = when {
                isFirst -> R.color.primary
                isLast -> R.color.secondary
                else -> R.color.train_line
            }
            dot.backgroundTintList = itemView.context.getColorStateList(colorRes)
        }
    }
}
