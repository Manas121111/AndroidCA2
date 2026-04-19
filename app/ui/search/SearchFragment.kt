package com.smarttour360.app.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smarttour360.app.R
import com.smarttour360.app.ui.AppNavigator

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_booking_portal, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = activity as? AppNavigator
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_booking_modes)
        val pager = view.findViewById<ViewPager2>(R.id.pager_booking_modes)

        pager.adapter = BookingPortalPagerAdapter(this)
        pager.offscreenPageLimit = 3

        view.findViewById<View>(R.id.text_help_me_choose).setOnClickListener {
            val bookingMode = when (pager.currentItem) {
                0 -> "TRAIN"
                1 -> "FLIGHT"
                else -> "BUS"
            }
            navigator?.openChatbot(bookingMode = bookingMode)
        }

        TabLayoutMediator(tabLayout, pager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Train"
                    tab.setIcon(R.drawable.ic_tab_train)
                }
                1 -> {
                    tab.text = "Flight"
                    tab.setIcon(R.drawable.ic_tab_flight)
                }
                else -> {
                    tab.text = "Bus"
                    tab.setIcon(R.drawable.ic_tab_bus)
                }
            }
        }.attach()
    }

    private class BookingPortalPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TrainBookingFragment()
                1 -> FlightBookingFragment()
                else -> BusBookingFragment()
            }
        }
    }
}
