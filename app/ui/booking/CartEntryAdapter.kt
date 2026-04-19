package com.smarttour360.app.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smarttour360.app.R
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.ui.state.CartEntry

class CartEntryAdapter : RecyclerView.Adapter<CartEntryAdapter.CartViewHolder>() {
    private val items = mutableListOf<CartEntry>()

    fun submitList(data: List<CartEntry>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart_entry, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val hotelName: TextView = itemView.findViewById(R.id.text_cart_hotel_name)
        private val stayInfo: TextView = itemView.findViewById(R.id.text_cart_stay_info)
        private val price: TextView = itemView.findViewById(R.id.text_cart_price)
        private val flag: TextView = itemView.findViewById(R.id.text_cart_flag)

        fun bind(item: CartEntry) {
            hotelName.text = item.hotelName
            stayInfo.text = item.stayInfo
            price.text = AppStateStore.formatCurrency(item.priceInr)
            flag.text = if (item.acknowledged) {
                "${item.flag} risk acknowledged"
            } else {
                "${item.flag} travel status"
            }
        }
    }
}
