package com.smarttour360.app.ui.search

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import com.smarttour360.app.data.TrainRepository
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.ui.train.Station
import com.smarttour360.app.ui.train.StationSuggestAdapter
import com.smarttour360.app.ui.train.TrainBookingHandoff
import com.smarttour360.app.ui.train.TrainResult
import com.smarttour360.app.ui.train.TrainResultsAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrainBookingFragment : Fragment() {
    private val trainRepository = TrainRepository()

    private var selectedFromStation: Station? = null
    private var selectedToStation: Station? = null
    private var selectedTrainDate: String = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().time)
    private var updatingStationFields = false
    private var fromSuggestJob: Job? = null
    private var toSuggestJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_train_booking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = activity as? AppNavigator
        val fromInput = view.findViewById<EditText>(R.id.input_train_from)
        val toInput = view.findViewById<EditText>(R.id.input_train_to)
        val fromSuggestions = view.findViewById<RecyclerView>(R.id.recycler_train_from_suggestions)
        val toSuggestions = view.findViewById<RecyclerView>(R.id.recycler_train_to_suggestions)
        val results = view.findViewById<RecyclerView>(R.id.recycler_train_results)
        val status = view.findViewById<TextView>(R.id.text_train_status)
        val resultsCount = view.findViewById<TextView>(R.id.text_train_results_count)
        val progress = view.findViewById<ProgressBar>(R.id.progress_train_search)

        val fromAdapter = StationSuggestAdapter { station ->
            selectedFromStation = station
            updateStationField(fromInput, station)
            fromSuggestions.isVisible = false
        }
        val toAdapter = StationSuggestAdapter { station ->
            selectedToStation = station
            updateStationField(toInput, station)
            toSuggestions.isVisible = false
        }
        val trainAdapter = TrainResultsAdapter(
            onTrainClick = { train -> navigator?.openTrainDetail(train) },
            onBookClick = { train -> openClassPicker(train) }
        )

        fromSuggestions.layoutManager = LinearLayoutManager(context)
        fromSuggestions.adapter = fromAdapter
        toSuggestions.layoutManager = LinearLayoutManager(context)
        toSuggestions.adapter = toAdapter
        results.layoutManager = object : LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean = false
        }
        results.adapter = trainAdapter
        results.isNestedScrollingEnabled = false

        fromInput.addTextChangedListener(createStationWatcher(true, fromInput, fromSuggestions, fromAdapter))
        toInput.addTextChangedListener(createStationWatcher(false, toInput, toSuggestions, toAdapter))

        suspend fun runTrainSearch() {
            progress.isVisible = true
            status.text = "Resolving stations..."
            results.isVisible = false
            resultsCount.isVisible = false
            fromSuggestions.isVisible = false
            toSuggestions.isVisible = false

            val from = selectedFromStation ?: trainRepository.resolveStation(fromInput.text?.toString().orEmpty())
            val to = selectedToStation ?: trainRepository.resolveStation(toInput.text?.toString().orEmpty())

            if (from == null || to == null) {
                progress.isVisible = false
                status.text = "I could not match one of those stations. Try a station code like NDLS or MMCT."
                return
            }

            selectedFromStation = from
            selectedToStation = to
            updateStationField(fromInput, from)
            updateStationField(toInput, to)

            runCatching {
                status.text = "Searching live trains..."
                trainRepository.searchTrains(from.code, to.code, selectedTrainDate)
            }.onSuccess { trains ->
                progress.isVisible = false
                trainAdapter.submitList(trains)
                results.isVisible = trains.isNotEmpty()
                resultsCount.isVisible = trains.isNotEmpty()
                resultsCount.text = if (trains.isEmpty()) "" else "${trains.size} trains found"
                status.text = if (trains.isEmpty()) {
                    "No trains found for ${from.code} to ${to.code} on ${formatDateLabel(selectedTrainDate)}."
                } else {
                    "Live railway results are ready."
                }
            }.onFailure {
                progress.isVisible = false
                status.text = it.message ?: "Unable to search trains right now."
            }
        }

        view.findViewById<MaterialButton>(R.id.button_swap_stations).setOnClickListener {
            val from = selectedFromStation
            val to = selectedToStation
            selectedFromStation = to
            selectedToStation = from
            updatingStationFields = true
            fromInput.setText(to?.let { formatStation(it) }.orEmpty())
            toInput.setText(from?.let { formatStation(it) }.orEmpty())
            updatingStationFields = false
        }

        view.findViewById<TextView>(R.id.text_train_date_value).text = formatDateLabel(selectedTrainDate)
        val pickDate = { showDatePicker(view) }
        view.findViewById<MaterialButton>(R.id.button_pick_train_date).setOnClickListener { pickDate() }
        view.findViewById<TextView>(R.id.text_train_date_value).setOnClickListener { pickDate() }

        view.findViewById<MaterialButton>(R.id.button_search_train).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch { runTrainSearch() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val pending = AppStateStore.consumePendingTrainSearch() ?: return@launch
            updatingStationFields = true
            fromInput.setText(pending.fromQuery)
            toInput.setText(pending.toQuery)
            updatingStationFields = false
            pending.travelDate?.let {
                selectedTrainDate = it
                view.findViewById<TextView>(R.id.text_train_date_value).text = formatDateLabel(it)
            }
            runTrainSearch()
        }
    }

    private fun createStationWatcher(
        isFrom: Boolean,
        input: EditText,
        target: RecyclerView,
        adapter: StationSuggestAdapter
    ) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: android.text.Editable?) {
            if (updatingStationFields) return
            if (isFrom) selectedFromStation = null else selectedToStation = null
            scheduleSuggestionFetch(input.text?.toString().orEmpty(), isFrom, target, adapter)
        }
    }

    private fun scheduleSuggestionFetch(
        query: String,
        isFrom: Boolean,
        target: RecyclerView,
        adapter: StationSuggestAdapter
    ) {
        val currentJob = if (isFrom) fromSuggestJob else toSuggestJob
        currentJob?.cancel()
        if (query.length < 2) {
            adapter.submitList(emptyList())
            target.isVisible = false
            if (isFrom) fromSuggestJob = null else toSuggestJob = null
            return
        }
        val job = viewLifecycleOwner.lifecycleScope.launch {
            delay(250)
            val stations = runCatching { trainRepository.searchStations(query) }.getOrDefault(emptyList())
            adapter.submitList(stations)
            target.isVisible = stations.isNotEmpty()
        }
        if (isFrom) fromSuggestJob = job else toSuggestJob = job
    }

    private fun updateStationField(input: EditText, station: Station) {
        updatingStationFields = true
        input.setText(formatStation(station))
        input.setSelection(input.text?.length ?: 0)
        updatingStationFields = false
    }

    private fun formatStation(station: Station): String = "${station.name} (${station.code})"

    private fun showDatePicker(root: View) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedTrainDate = String.format(Locale.getDefault(), "%04d%02d%02d", year, month + 1, day)
                root.findViewById<TextView>(R.id.text_train_date_value).text = formatDateLabel(selectedTrainDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = calendar.timeInMillis
        }.show()
    }

    private fun openClassPicker(train: TrainResult) {
        val availableClasses = if (train.classes.isEmpty()) arrayOf("SL", "3A", "2A", "CC") else train.classes.toTypedArray()
        val labels = availableClasses.map {
            when (it) {
                "SL" -> "Sleeper (SL)"
                "3A" -> "3rd AC (3A)"
                "2A" -> "2nd AC (2A)"
                "1A" -> "1st AC (1A)"
                "CC" -> "Chair Car (CC)"
                "2S" -> "Second Sitting (2S)"
                else -> it
            }
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Choose class")
            .setItems(labels) { _, index ->
                val handoff = TrainBookingHandoff.build(
                    trainNumber = train.number,
                    trainName = train.name,
                    fromCode = train.fromStation.code,
                    toCode = train.toStation.code,
                    travelDate = train.travelDate,
                    selectedClass = availableClasses[index]
                )
                val summary = buildString {
                    append("Train: ").append(handoff.trainNumber).append(" ").append(handoff.trainName).append('\n')
                    append("From: ").append(handoff.fromCode).append('\n')
                    append("To: ").append(handoff.toCode).append('\n')
                    append("Date: ").append(formatDateLabel(handoff.travelDate)).append('\n')
                    append("Class: ").append(handoff.selectedClass)
                }
                showExternalBookingHandoff("Continue to IRCTC", summary, handoff.irctcUrl)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatDateLabel(raw: String): String {
        if (raw.length != 8) return raw
        val parser = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(parser.parse(raw) ?: return raw)
    }
}
