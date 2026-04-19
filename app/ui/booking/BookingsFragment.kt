package com.smarttour360.app.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smarttour360.app.R
import com.smarttour360.app.ui.state.AppStateStore
import kotlinx.coroutines.launch

class BookingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_bookings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = BookingAdapter()
        val status = view.findViewById<TextView>(R.id.text_bookings_status)
        val summary = view.findViewById<TextView>(R.id.text_bookings_summary)
        view.findViewById<RecyclerView>(R.id.recycler_bookings).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AppStateStore.bookings.collect { bookings ->
                adapter.submitList(bookings)
                if (bookings.isEmpty()) {
                    status.text = "No confirmed stays yet."
                    summary.text = "Complete checkout from the cart to create blockchain-linked bookings."
                } else {
                    val confirmed = bookings.count { it.status == "Confirmed" }
                    status.text = "${bookings.size} booking(s) saved"
                    summary.text = "$confirmed confirmed now, ${bookings.size - confirmed} upcoming follow-on stay(s)."
                }
            }
        }
    }
}
