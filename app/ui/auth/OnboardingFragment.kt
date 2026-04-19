package com.smarttour360.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.smarttour360.app.R
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.MainTab
import com.smarttour360.app.ui.state.AppStateStore

class OnboardingFragment : Fragment() {
    private val countries = listOf("India", "United States", "United Kingdom", "Canada", "Australia", "Singapore", "UAE")
    private val budgets = listOf("Budget", "Mid-range", "Luxury")
    private val transports = listOf("Train", "Hotels", "Flight", "Bus")
    private val tripStyles = listOf("Adventure", "Heritage", "Nature", "Beach", "Food", "Family")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_onboarding, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val current = AppStateStore.userPreferences.value
        val inputName = view.findViewById<EditText>(R.id.input_name)
        val inputEmail = view.findViewById<EditText>(R.id.input_email)
        val inputMobile = view.findViewById<EditText>(R.id.input_mobile)
        val inputHomeCity = view.findViewById<EditText>(R.id.input_home_city)
        val inputCountry = view.findViewById<AutoCompleteTextView>(R.id.input_country)
        val inputBudget = view.findViewById<AutoCompleteTextView>(R.id.input_budget)
        val inputTransport = view.findViewById<AutoCompleteTextView>(R.id.input_transport)
        val tripTypesGroup = view.findViewById<ChipGroup>(R.id.group_trip_types)
        val ecoGroup = view.findViewById<ChipGroup>(R.id.group_eco_priority)

        bindDropdown(inputCountry, countries)
        bindDropdown(inputBudget, budgets)
        bindDropdown(inputTransport, transports)
        bindTripTypes(tripTypesGroup, current.tripTypes)

        inputName.setText(current.name.takeIf { it != "Guest Explorer" }.orEmpty())
        inputEmail.setText(current.email)
        inputMobile.setText(current.mobile)
        inputHomeCity.setText(current.homeCity)
        inputCountry.setText(current.country, false)
        inputBudget.setText(current.budget, false)
        inputTransport.setText(current.preferredTransport, false)
        ecoGroup.check(if (current.ecoPriority) R.id.chip_eco_yes else R.id.chip_eco_no)

        view.findViewById<MaterialButton>(R.id.button_get_started).setOnClickListener {
            val tripTypes = tripTypesGroup.children
                .filterIsInstance<Chip>()
                .filter { it.isChecked }
                .joinToString(", ") { it.text.toString() }
                .ifBlank { "Adventure, Heritage" }

            AppStateStore.savePreferences(
                requireContext(),
                name = inputName.text.toString().ifBlank { "Guest Explorer" },
                email = inputEmail.text.toString().trim(),
                mobile = inputMobile.text.toString().trim(),
                country = inputCountry.text.toString().ifBlank { "India" },
                homeCity = inputHomeCity.text.toString().trim(),
                budget = inputBudget.text.toString().ifBlank { "Mid-range" },
                tripTypes = tripTypes,
                preferredTransport = inputTransport.text.toString().ifBlank { "Hotels" },
                ecoPriority = ecoGroup.checkedChipId == R.id.chip_eco_yes
            )
            (activity as? AppNavigator)?.enterApp(MainTab.HOME)
        }
    }

    private fun bindDropdown(view: AutoCompleteTextView, options: List<String>) {
        view.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options))
    }

    private fun bindTripTypes(group: ChipGroup, selected: String) {
        val selectedSet = selected.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        group.removeAllViews()
        tripStyles.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = label in selectedSet
            }
            group.addView(chip)
        }
    }
}
