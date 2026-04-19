package com.smarttour360.app.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.blockchain.AcknowledgeRiskDialogFragment
import com.smarttour360.app.ui.state.AppStateStore
import kotlinx.coroutines.launch

class CartFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cart, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = CartEntryAdapter()
        val status = view.findViewById<TextView>(R.id.text_cart_status)
        val subtotal = view.findViewById<TextView>(R.id.text_cart_subtotal)
        val taxes = view.findViewById<TextView>(R.id.text_cart_taxes)
        val total = view.findViewById<TextView>(R.id.text_cart_total)
        val placeOrder = view.findViewById<MaterialButton>(R.id.button_place_order)
        view.findViewById<RecyclerView>(R.id.recycler_cart_entries).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        view.findViewById<MaterialButton>(R.id.button_acknowledge).setOnClickListener {
            AcknowledgeRiskDialogFragment().show(childFragmentManager, "ack_dialog")
        }
        placeOrder.setOnClickListener {
            val confirmation = AppStateStore.placeOrder()
            if (confirmation == null) {
                Toast.makeText(requireContext(), "Add at least one hotel before checkout.", Toast.LENGTH_SHORT).show()
            } else {
                (activity as? AppNavigator)?.openOrderConfirmation()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AppStateStore.cartEntries.collect { entries ->
                adapter.submitList(entries)
                val totals = AppStateStore.getCartTotals()
                subtotal.text = "Subtotal: ${AppStateStore.formatCurrency(totals.subtotal)}"
                taxes.text = "Taxes: ${AppStateStore.formatCurrency(totals.taxes)}"
                total.text = "Total: ${AppStateStore.formatCurrency(totals.total)}"
                placeOrder.isEnabled = entries.isNotEmpty()
                status.text = if (entries.isEmpty()) {
                    "Your cart is empty. Add a hotel from the detail screen."
                } else {
                    "${entries.size} booking item(s) ready for checkout."
                }
            }
        }
    }
}
