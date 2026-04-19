package com.smarttour360.app.ui.search

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.BuildConfig
import com.smarttour360.app.R
import com.smarttour360.app.data.remote.ApiClient
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FlightBookingFragment : Fragment() {
    private var selectedDate: String =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    private val adapter = TransportOptionAdapter { option ->
        showExternalBookingHandoff("Continue to flight booking", option.handoffSummary, option.handoffUrl)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_flight_booking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cabinField = view.findViewById<AutoCompleteTextView>(R.id.input_flight_cabin)
        val cabinValues = listOf("Economy", "Premium Economy", "Business")
        cabinField.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, cabinValues))
        cabinField.setText(cabinValues.first(), false)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_flight_results)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter
        recycler.isNestedScrollingEnabled = false

        val dateText = view.findViewById<TextView>(R.id.text_flight_date_value)
        dateText.text = selectedDate

        val pickDate = {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDate =
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
                    dateText.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        view.findViewById<MaterialButton>(R.id.button_pick_flight_date).setOnClickListener { pickDate() }
        dateText.setOnClickListener { pickDate() }

        view.findViewById<MaterialButton>(R.id.button_search_flights).setOnClickListener {
            val from = view.findViewById<EditText>(R.id.input_flight_from).text.toString().trim()
            val to = view.findViewById<EditText>(R.id.input_flight_to).text.toString().trim()
            val cabin = cabinField.text?.toString().orEmpty()
            val status = view.findViewById<TextView>(R.id.text_flight_status)
            val progress = view.findViewById<ProgressBar>(R.id.progress_flight_search)
            val resultsTitle = view.findViewById<TextView>(R.id.text_flight_results_count)

            if (from.isBlank() || to.isBlank()) {
                status.text = "Enter both departure and arrival cities."
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                progress.isVisible = true
                resultsTitle.isVisible = false
                status.text = "Searching live flights..."
                val options = searchFlightOptions(from, to, cabin)
                progress.isVisible = false

                if (options.isEmpty()) {
                    status.text = if (BuildConfig.AVIATIONSTACK_API_KEY.isBlank()) {
                        "Live flight search is ready when AVIATIONSTACK_API_KEY is added. You can continue to external booking meanwhile."
                    } else {
                        "No live flights were returned right now. Continue with external booking."
                    }
                    resultsTitle.isVisible = true
                    resultsTitle.text = "Flight booking options"
                    adapter.submitList(fallbackFlightOptions(from, to, cabin))
                } else {
                    status.text = "Live flight options are ready."
                    resultsTitle.isVisible = true
                    resultsTitle.text = "${options.size} live options"
                    adapter.submitList(options)
                }
            }
        }
    }

    private suspend fun searchFlightOptions(
        from: String,
        to: String,
        cabin: String
    ): List<TransportOption> {
        val departureCode = AirportCodeResolver.resolve(from)
        val arrivalCode = AirportCodeResolver.resolve(to)
        if (departureCode == null || arrivalCode == null || BuildConfig.AVIATIONSTACK_API_KEY.isBlank()) {
            return emptyList()
        }

        val apiDate = normalizeApiDate(selectedDate) ?: return emptyList()
        val cleartripUrl = buildCleartripUrl(from, to)
        val flights = runCatching {
            ApiClient.aviationstackApi.searchFlights(
                accessKey = BuildConfig.AVIATIONSTACK_API_KEY,
                departureIata = departureCode,
                arrivalIata = arrivalCode,
                flightDate = apiDate
            ).data
        }.getOrDefault(emptyList())

        return flights
            .filter {
                it.departure?.iata.equals(departureCode, ignoreCase = true) &&
                    it.arrival?.iata.equals(arrivalCode, ignoreCase = true)
            }
            .take(6)
            .map { flight ->
                val airline = flight.airline?.name ?: "Airline"
                val flightCode = flight.flight?.iata ?: flight.flight?.number ?: "Flight"
                val liveStatus = flight.flight_status
                    ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    ?: "Scheduled"
                TransportOption(
                    title = "$airline $flightCode",
                    subtitle = "$from to $to",
                    meta = "$selectedDate | ${friendlyTime(flight.departure?.scheduled)} -> ${friendlyTime(flight.arrival?.scheduled)} | $liveStatus | $cabin",
                    actionLabel = "Open Cleartrip",
                    handoffUrl = cleartripUrl,
                    handoffSummary = buildString {
                        appendLine("Flight: $airline $flightCode")
                        appendLine("From: $from ($departureCode)")
                        appendLine("To: $to ($arrivalCode)")
                        appendLine("Date: $selectedDate")
                        appendLine("Status: $liveStatus")
                        append("Cabin: $cabin")
                    }
                )
            }
    }

    private fun fallbackFlightOptions(
        from: String,
        to: String,
        cabin: String
    ): List<TransportOption> {
        return listOf(
            TransportOption(
                title = "$from to $to",
                subtitle = "Search on Cleartrip with your route details",
                meta = "$selectedDate | $cabin",
                actionLabel = "Open Cleartrip",
                handoffUrl = buildCleartripUrl(from, to),
                handoffSummary = "From: $from\nTo: $to\nDate: $selectedDate\nCabin: $cabin\nPreferred portal: Cleartrip"
            ),
            TransportOption(
                title = "$from to $to",
                subtitle = "Backup handoff via MakeMyTrip",
                meta = "Use the same route and travel date",
                actionLabel = "Open MakeMyTrip",
                handoffUrl = "https://www.makemytrip.com/flights/",
                handoffSummary = "From: $from\nTo: $to\nDate: $selectedDate\nCabin: $cabin\nPreferred portal: MakeMyTrip"
            )
        )
    }

    private fun buildCleartripUrl(from: String, to: String): String {
        val encodedFrom = URLEncoder.encode(from, "UTF-8")
        val encodedTo = URLEncoder.encode(to, "UTF-8")
        val encodedDate = URLEncoder.encode(selectedDate, "UTF-8")
        return "https://www.cleartrip.com/flights?from=$encodedFrom&to=$encodedTo&depart_date=$encodedDate"
    }

    private fun normalizeApiDate(input: String): String? {
        val parser = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply { isLenient = false }
        val parsed = parser.parse(input, ParsePosition(0)) ?: return null
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed)
    }

    private fun friendlyTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "Time TBD"
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(raw)
        }.getOrNull() ?: return raw.substringAfter('T').take(5).ifBlank { "Time TBD" }
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsed)
    }
}
