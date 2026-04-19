package com.smarttour360.app.ui.train

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainDetailFragment : Fragment() {
    private val repository = TrainRepository()
    private val stopAdapter = TrainStopAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_train_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val trainNumber = requireArguments().getString(ARG_TRAIN_NUMBER).orEmpty()
        val trainName = requireArguments().getString(ARG_TRAIN_NAME).orEmpty()
        val fromCode = requireArguments().getString(ARG_FROM_CODE).orEmpty()
        val toCode = requireArguments().getString(ARG_TO_CODE).orEmpty()
        val travelDate = requireArguments().getString(ARG_TRAVEL_DATE).orEmpty()

        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val effectiveDate = if (travelDate.isBlank()) today else travelDate

        view.findViewById<TextView>(R.id.text_train_title).text = trainName.ifBlank { "Train $trainNumber" }
        view.findViewById<TextView>(R.id.text_train_subtitle).text = "$trainNumber  •  $fromCode to $toCode"
        view.findViewById<TextView>(R.id.text_train_date).text = formatDate(effectiveDate)
        view.findViewById<RecyclerView>(R.id.recycler_train_stops).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stopAdapter
        }

        view.findViewById<MaterialButton>(R.id.button_back_from_train).setOnClickListener {
            (activity as? AppNavigator)?.goBack()
        }
        view.findViewById<MaterialButton>(R.id.button_refresh_train).setOnClickListener {
            loadLiveStatus(view, trainNumber, effectiveDate)
        }
        view.findViewById<MaterialButton>(R.id.button_book_train).setOnClickListener {
            openBookingDialog(trainNumber, trainName, fromCode, toCode, effectiveDate)
        }

        loadSchedule(view, trainNumber)
        loadLiveStatus(view, trainNumber, effectiveDate)
    }

    private fun loadSchedule(root: View, trainNumber: String) {
        val progress = root.findViewById<ProgressBar>(R.id.progress_train_schedule)
        val error = root.findViewById<TextView>(R.id.text_train_schedule_error)
        val list = root.findViewById<RecyclerView>(R.id.recycler_train_stops)
        val stopCount = root.findViewById<TextView>(R.id.text_stop_count)

        viewLifecycleOwner.lifecycleScope.launch {
            progress.isVisible = true
            error.isVisible = false
            list.isVisible = false
            runCatching { repository.getSchedule(trainNumber) }
                .onSuccess { stops ->
                    progress.isVisible = false
                    if (stops.isEmpty()) {
                        error.isVisible = true
                        error.text = "Schedule is not available for this train right now."
                    } else {
                        list.isVisible = true
                        stopCount.text = "${stops.size} scheduled stops"
                        stopAdapter.submitList(stops)
                    }
                }
                .onFailure {
                    progress.isVisible = false
                    error.isVisible = true
                    error.text = it.message ?: "Unable to load train schedule."
                }
        }
    }

    private fun loadLiveStatus(root: View, trainNumber: String, date: String) {
        val progress = root.findViewById<ProgressBar>(R.id.progress_train_live)
        val position = root.findViewById<TextView>(R.id.text_train_position)
        val status = root.findViewById<TextView>(R.id.text_train_status)
        val delay = root.findViewById<TextView>(R.id.text_train_delay)

        viewLifecycleOwner.lifecycleScope.launch {
            progress.isVisible = true
            runCatching { repository.getLiveStatus(trainNumber, date) }
                .onSuccess { live ->
                    progress.isVisible = false
                    position.text = live.position
                    status.text = live.status
                    delay.text = when {
                        live.delayMinutes <= 0 -> "On time"
                        else -> "Delayed by ${live.delayMinutes} min"
                    }
                    val delayColor = if (live.delayMinutes > 15) R.color.secondary else R.color.primary
                    delay.setTextColor(requireContext().getColor(delayColor))
                }
                .onFailure {
                    progress.isVisible = false
                    position.text = "Live running status is unavailable."
                    status.text = it.message ?: "Try again in a moment."
                    delay.text = "No delay estimate"
                    delay.setTextColor(requireContext().getColor(R.color.text_secondary))
                }
        }
    }

    private fun openBookingDialog(
        trainNumber: String,
        trainName: String,
        fromCode: String,
        toCode: String,
        travelDate: String
    ) {
        val classes = arrayOf("SL", "3A", "2A", "1A", "CC", "2S")
        val labels = arrayOf("Sleeper (SL)", "3rd AC (3A)", "2nd AC (2A)", "1st AC (1A)", "Chair Car (CC)", "Second Sitting (2S)")
        AlertDialog.Builder(requireContext())
            .setTitle("Book $trainNumber")
            .setItems(labels) { _, which ->
                val handoff = TrainBookingHandoff.build(
                    trainNumber = trainNumber,
                    trainName = trainName,
                    fromCode = fromCode,
                    toCode = toCode,
                    travelDate = travelDate,
                    selectedClass = classes[which]
                )
                confirmIrctcHandoff(handoff)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmIrctcHandoff(handoff: TrainBookingHandoff) {
        val bookingSummary = buildIrctcBookingSummary(handoff)
        AlertDialog.Builder(requireContext())
            .setTitle("Continue on IRCTC")
            .setMessage(
                "IRCTC does not reliably support full auto-fill from app links.\n\n" +
                    "When you continue, this app will copy your selected booking details to the clipboard and open IRCTC.\n\n" +
                    bookingSummary
            )
            .setPositiveButton("Open IRCTC") { _, _ ->
                copyBookingSummaryToClipboard(bookingSummary)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(handoff.irctcUrl)))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildIrctcBookingSummary(handoff: TrainBookingHandoff): String {
        return buildString {
            append("Train: ").append(handoff.trainNumber).append(" ").append(handoff.trainName).append('\n')
            append("From: ").append(handoff.fromCode).append('\n')
            append("To: ").append(handoff.toCode).append('\n')
            append("Date: ").append(formatDate(handoff.travelDate)).append('\n')
            append("Class: ").append(handoff.selectedClass)
        }
    }

    private fun copyBookingSummaryToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("IRCTC booking details", text))
    }

    private fun formatDate(raw: String): String {
        if (raw.length != 8) return raw
        val parsed = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(raw) ?: return raw
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)
    }

    companion object {
        private const val ARG_TRAIN_NUMBER = "train_number"
        private const val ARG_TRAIN_NAME = "train_name"
        private const val ARG_FROM_CODE = "from_code"
        private const val ARG_TO_CODE = "to_code"
        private const val ARG_TRAVEL_DATE = "travel_date"

        fun newInstance(train: TrainResult): TrainDetailFragment {
            return TrainDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRAIN_NUMBER, train.number)
                    putString(ARG_TRAIN_NAME, train.name)
                    putString(ARG_FROM_CODE, train.fromStation.code)
                    putString(ARG_TO_CODE, train.toStation.code)
                    putString(ARG_TRAVEL_DATE, train.travelDate)
                }
            }
        }
    }
}
