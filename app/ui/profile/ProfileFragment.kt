package com.smarttour360.app.ui.profile

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
import com.smarttour360.app.ui.state.AppStateStore
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val avatar = view.findViewById<TextView>(R.id.text_profile_avatar)
        val name = view.findViewById<TextView>(R.id.text_profile_name)
        val email = view.findViewById<TextView>(R.id.text_profile_email)
        val mobile = view.findViewById<TextView>(R.id.text_profile_mobile)
        val country = view.findViewById<TextView>(R.id.text_profile_country)
        val preferences = view.findViewById<TextView>(R.id.text_profile_preferences)

        view.findViewById<MaterialButton>(R.id.button_my_bookings).setOnClickListener {
            (activity as? AppNavigator)?.openBookings()
        }

        fun render() {
            val profile = AppStateStore.userPreferences.value
            val initials = profile.name
                .split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")

            avatar.text = initials.ifBlank { "GE" }
            name.text = profile.name
            email.text = "Email: ${profile.email.ifBlank { "Not added yet" }}"
            mobile.text = "Mobile: ${profile.mobile.ifBlank { "Not added yet" }}"
            country.text = "Country: ${profile.country.ifBlank { "Not added yet" }}"
            preferences.text = "Budget: ${profile.budget} | Styles: ${profile.tripTypes} | Focus: ${profile.preferredTransport}"
        }

        viewLifecycleOwner.lifecycleScope.launch { AppStateStore.userPreferences.collect { render() } }
        render()
    }
}
