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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BusBookingFragment : Fragment() {
    private var selectedDate: String =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    private val adapter = TransportOptionAdapter { option ->
        showExternalBookingHandoff("Continue to bus booking", option.handoffSummary, option.handoffUrl)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_bus_booking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val seatField = view.findViewById<AutoCompleteTextView>(R.id.input_bus_seat_type)
        val seatValues = listOf("Sleeper", "Semi Sleeper", "Seater")
        seatField.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, seatValues))
        seatField.setText(seatValues.first(), false)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_bus_results)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter
        recycler.isNestedScrollingEnabled = false

        val dateText = view.findViewById<TextView>(R.id.text_bus_date_value)
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
        view.findViewById<MaterialButton>(R.id.button_pick_bus_date).setOnClickListener { pickDate() }
        dateText.setOnClickListener { pickDate() }

        view.findViewById<MaterialButton>(R.id.button_search_buses).setOnClickListener {
            val from = view.findViewById<EditText>(R.id.input_bus_from).text.toString().trim()
            val to = view.findViewById<EditText>(R.id.input_bus_to).text.toString().trim()
            val seatType = seatField.text?.toString().orEmpty()
            val status = view.findViewById<TextView>(R.id.text_bus_status)
            val progress = view.findViewById<ProgressBar>(R.id.progress_bus_search)
            val resultsTitle = view.findViewById<TextView>(R.id.text_bus_results_count)

            if (from.isBlank() || to.isBlank()) {
                status.text = "Enter both boarding and destination cities."
                return@setOnClickListener
            }

            progress.isVisible = true
            resultsTitle.isVisible = false
            status.text = "Preparing live bus handoff..."
            val options = listOf(
                TransportOption(
                    title = "$from to $to",
                    subtitle = "Live handoff via RedBus",
                    meta = "$selectedDate | $seatType",
                    actionLabel = "Open RedBus",
                    handoffUrl = buildRedBusUrl(from, to, selectedDate),
                    handoffSummary = "From: $from\nTo: $to\nDate: $selectedDate\nSeat type: $seatType\nPreferred portal: RedBus"
                ),
                TransportOption(
                    title = "$from to $to",
                    subtitle = "Alternate handoff via AbhiBus",
                    meta = "Use the same route and seat preference",
                    actionLabel = "Open AbhiBus",
                    handoffUrl = "https://www.abhibus.com/bus-search?source=${URLEncoder.encode(from, "UTF-8")}&destination=${URLEncoder.encode(to, "UTF-8")}&journey_date=${formatAbhiBusDate(selectedDate)}",
                    handoffSummary = "From: $from\nTo: $to\nDate: $selectedDate\nSeat type: $seatType\nPreferred portal: AbhiBus"
                )
            )
            progress.isVisible = false
            status.text = "Bus handoff options are ready."
            resultsTitle.isVisible = true
            resultsTitle.text = "${options.size} booking options"
            adapter.submitList(options)
        }
    }

    private fun buildRedBusUrl(from: String, to: String, displayDate: String): String {
        val routeSlug = "${slugifyCity(from)}-to-${slugifyCity(to)}"
        return "https://www.redbus.in/bus-tickets/$routeSlug?doj=${formatRedBusDate(displayDate)}"
    }

    private fun slugifyCity(value: String): String {
        return value.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    private fun formatRedBusDate(displayDate: String): String {
        val parser = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply { isLenient = false }
        val parsed = runCatching { parser.parse(displayDate) }.getOrNull() ?: Date()
        return SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(parsed)
    }

    private fun formatAbhiBusDate(displayDate: String): String {
        val parser = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply { isLenient = false }
        val parsed = runCatching { parser.parse(displayDate) }.getOrNull() ?: Date()
        return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(parsed)
    }
}
