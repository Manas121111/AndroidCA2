package com.smarttour360.app.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.MainTab
import com.smarttour360.app.ui.state.AppStateStore
import kotlinx.coroutines.launch

class OrderConfirmationFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_order_confirmation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = activity as? AppNavigator
        val headline = view.findViewById<TextView>(R.id.text_confirmation_headline)
        val primary = view.findViewById<TextView>(R.id.text_confirmation_primary)
        val details = view.findViewById<TextView>(R.id.text_confirmation_details)
        val blockchain = view.findViewById<TextView>(R.id.text_confirmation_blockchain)
        val ecoImpact = view.findViewById<TextView>(R.id.text_confirmation_eco)

        view.findViewById<MaterialButton>(R.id.button_view_bookings).setOnClickListener {
            navigator?.openBookings()
        }
        view.findViewById<MaterialButton>(R.id.button_go_home).setOnClickListener {
            navigator?.enterApp(MainTab.HOME)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AppStateStore.latestOrderConfirmation.collect { confirmation ->
                if (confirmation == null) {
                    headline.text = "No recent checkout"
                    primary.text = "Return to cart to complete a booking."
                    details.text = ""
                    blockchain.text = ""
                    ecoImpact.text = ""
                } else {
                    headline.text = confirmation.headline
                    primary.text = confirmation.bookingIdLine
                    details.text = confirmation.detailLine
                    blockchain.text = confirmation.blockchainLine
                    ecoImpact.text = confirmation.ecoImpactLine
                }
            }
        }
    }
}
