package com.smarttour360.app.ui.hotel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.smarttour360.app.R
import com.smarttour360.app.data.HotelRepository
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.common.HotelListAdapter
import com.smarttour360.app.ui.state.AppStateStore
import kotlinx.coroutines.launch

class HotelListFragment : Fragment() {
    private val hotelRepository = HotelRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_hotel_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = activity as? AppNavigator
        val title = view.findViewById<TextView>(R.id.text_hotels_title)
        val status = view.findViewById<TextView>(R.id.text_hotels_status)
        val progress = view.findViewById<ProgressBar>(R.id.progress_hotels)
        val adapter = HotelListAdapter(
            onClick = {
                AppStateStore.selectHotel(it)
                navigator?.openHotelDetail()
            },
            onAddToTrip = {
                AppStateStore.addHotelToTrip(it)
                Snackbar.make(view, "${it.name} added as a stay", Snackbar.LENGTH_SHORT).show()
            }
        )
        view.findViewById<RecyclerView>(R.id.recycler_hotels).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            val result = runCatching { hotelRepository.getHotelsForCurrentSelection(limit = 12) }.getOrElse {
                com.smarttour360.app.data.LiveHotelResult(
                    anchorDestination = AppStateStore.selectedDestination.value,
                    hotels = emptyList(),
                    headline = "Live stays",
                    statusMessage = "Unable to load live nearby stays right now."
                )
            }
            progress.visibility = View.GONE
            title.text = result.headline
            status.text = result.statusMessage
            adapter.submitList(result.hotels)
        }
    }
}
