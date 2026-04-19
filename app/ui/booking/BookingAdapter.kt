package com.smarttour360.app.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smarttour360.app.R
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.ui.state.BookingRecord

class BookingAdapter : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {
    private val items = mutableListOf<BookingRecord>()

    fun submitList(data: List<BookingRecord>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking_entry, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.text_booking_title)
        private val meta: TextView = itemView.findViewById(R.id.text_booking_meta)
        private val total: TextView = itemView.findViewById(R.id.text_booking_total)
        private val status: TextView = itemView.findViewById(R.id.text_booking_status)
        private val ref: TextView = itemView.findViewById(R.id.text_booking_ref)

        fun bind(item: BookingRecord) {
            title.text = "${item.id} - ${item.hotelName}"
            meta.text = item.stayInfo
            total.text = AppStateStore.formatCurrency(item.totalInr)
            status.text = buildString {
                append(item.status)
                append(" - Flag at booking: ${item.flagAtBooking}")
                if (item.acknowledged) append(" - Acknowledged")
            }
            ref.text = "${item.blockchainRef}\n${item.ecoImpactSummary}"
        }
    }
}
