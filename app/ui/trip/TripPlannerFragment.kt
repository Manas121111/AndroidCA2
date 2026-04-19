package com.smarttour360.app.ui.trip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import com.smarttour360.app.data.remote.ChatbotRepository
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.MainTab
import com.smarttour360.app.ui.chatbot.ContextBuilder
import com.smarttour360.app.ui.chatbot.ItineraryBottomSheet
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.ui.state.TripItemType
import kotlinx.coroutines.launch

class TripPlannerFragment : Fragment() {
    private val chatbotRepository = ChatbotRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_trip_planner, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = activity as? AppNavigator
        val adapter = TripEntryAdapter { entry ->
            AppStateStore.removeTripEntry(entry)
        }
        val status = view.findViewById<TextView>(R.id.text_trip_status)
        val badge = view.findViewById<TextView>(R.id.text_trip_summary_badge)
        val title = view.findViewById<TextView>(R.id.text_trip_summary_title)
        val route = view.findViewById<TextView>(R.id.text_trip_route)
        val duration = view.findViewById<TextView>(R.id.text_trip_duration)
        val transport = view.findViewById<TextView>(R.id.text_trip_transport)
        val stay = view.findViewById<TextView>(R.id.text_trip_stay)
        val total = view.findViewById<TextView>(R.id.text_trip_total)
        val note = view.findViewById<TextView>(R.id.text_trip_note)
        val stopsChip = view.findViewById<TextView>(R.id.text_trip_stops_chip)
        val staysChip = view.findViewById<TextView>(R.id.text_trip_stays_chip)

        view.findViewById<RecyclerView>(R.id.recycler_trip_entries).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        view.findViewById<MaterialButton>(R.id.button_open_home_trip).setOnClickListener {
            navigator?.enterApp(MainTab.HOME)
        }
        view.findViewById<MaterialButton>(R.id.button_clear_trip).setOnClickListener {
            AppStateStore.clearTripPlan()
        }
        view.findViewById<MaterialButton>(R.id.button_share_trip).setOnClickListener {
            shareTrip()
        }
        view.findViewById<MaterialButton>(R.id.button_generate_day_plan).setOnClickListener {
            generateDayPlan()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AppStateStore.tripEntries.collect { entries ->
                adapter.submitList(entries)
                val summary = AppStateStore.buildTripSummary(entries)
                val stopCount = entries.count { it.type == TripItemType.DESTINATION }
                val stayCount = entries.count { it.type == TripItemType.HOTEL }

                badge.text = if (entries.isEmpty()) "START HERE" else "PLAN READY"
                status.text = if (entries.isEmpty()) {
                    "Pick places and stays, then this page turns them into one simple route."
                } else {
                    "Your saved items are now linked into one trip idea."
                }
                title.text = summary.headline
                route.text = summary.routeLine
                duration.text = summary.durationLine
                transport.text = summary.transportLine
                stay.text = summary.stayLine
                total.text = summary.totalLine
                note.text = summary.plannerNote
                stopsChip.text = "$stopCount stop${if (stopCount == 1) "" else "s"}"
                staysChip.text = "$stayCount stay${if (stayCount == 1) "" else "s"}"
            }
        }
    }

    private fun shareTrip() {
        val entries = AppStateStore.tripEntries.value
        val summary = AppStateStore.buildTripSummary(entries)
        val text = buildString {
            appendLine("My SmartTour360 Trip Plan")
            appendLine(summary.routeLine)
            appendLine("Total: ${summary.totalLine}")
            appendLine("Planned via SmartTour360")
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Share Trip"
            )
        )
    }

    private fun generateDayPlan() {
        val entries = AppStateStore.tripEntries.value
        if (entries.isEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val context = ContextBuilder.fromCurrentState()
            val reply = chatbotRepository.sendMessage(
                userMessage = "Create a concise day-by-day itinerary for my trip.",
                history = emptyList(),
                screenContext = context,
                itineraryMode = true
            )
            ItineraryBottomSheet.newInstance(reply.displayText).show(childFragmentManager, "itinerary_sheet")
        }
    }
}
