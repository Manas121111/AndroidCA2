package com.smarttour360.app.ui.hotel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.state.AppStateStore

class HotelDetailFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_hotel_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = activity as? AppNavigator
        val selectedHotel = AppStateStore.selectedHotel.value
        val selectedDestination = AppStateStore.selectedDestination.value

        view.findViewById<TextView>(R.id.text_hotel_detail_name).text =
            selectedHotel?.name ?: "Selected Hotel"
        view.findViewById<TextView>(R.id.text_hotel_detail_meta).text =
            selectedHotel?.let { "${it.priceText} | Eco Score ${it.ecoScore} | ${it.trend}" }
                ?: "Hotel details unavailable"
        view.findViewById<TextView>(R.id.text_hotel_detail_panel).text =
            selectedHotel?.let {
                val priceNote = if (it.priceText.contains("₹")) {
                    "Pricing note: this stay uses a mapped Google Places-style price band when available, otherwise a local estimate.\n"
                } else {
                    "Pricing note: this stay is shown from live place data and uses an estimated nightly rate.\n"
                }
                buildString {
                    append("Live stay signal: ${it.ecoScore}/100\n")
                    append("Source quality: ${it.trend}\n")
                    append(
                        if (it.hasHiddenFees) {
                            "Pricing warning: pricing detail needs verification before checkout.\n"
                        } else {
                            priceNote
                        }
                    )
                    append("Amenities depend on the live provider listing. Confirm details before booking.")
                }
            } ?: "Select a hotel to view detail."
        view.findViewById<TextView>(R.id.text_hotel_safety_banner).text =
            selectedDestination?.let {
                "Destination: ${it.name} ${it.flag} | Eco ${it.ecoScore}"
            } ?: "Destination safety context is not selected yet."

        view.findViewById<MaterialButton>(R.id.button_ask_ai_hotel).setOnClickListener {
            navigator?.openChatbot(
                destinationName = selectedDestination?.name,
                safetyFlag = selectedDestination?.flag,
                flagExplanation = selectedDestination?.subtitle,
                ecoScore = selectedDestination?.ecoScore,
                ethicalScore = selectedDestination?.ethicalScore,
                bookingMode = "HOTEL"
            )
        }
        view.findViewById<MaterialButton>(R.id.button_add_to_cart).setOnClickListener {
            AppStateStore.addSelectedHotelToCart()
            navigator?.openCart()
        }
        view.findViewById<MaterialButton>(R.id.button_view_cart).setOnClickListener {
            navigator?.openCart()
        }
    }
}
