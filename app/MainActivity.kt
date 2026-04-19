package com.smarttour360.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.MainTab
import com.smarttour360.app.ui.auth.LoginFragment
import com.smarttour360.app.ui.auth.OnboardingFragment
import com.smarttour360.app.ui.auth.SplashFragment
import com.smarttour360.app.ui.booking.BookingsFragment
import com.smarttour360.app.ui.booking.CartFragment
import com.smarttour360.app.ui.booking.OrderConfirmationFragment
import com.smarttour360.app.ui.destination.DestinationDetailFragment
import com.smarttour360.app.ui.home.HomeFragment
import com.smarttour360.app.ui.hotel.HotelDetailFragment
import com.smarttour360.app.ui.hotel.HotelListFragment
import com.smarttour360.app.ui.profile.ProfileFragment
import com.smarttour360.app.ui.chatbot.ChatbotFragment
import com.smarttour360.app.ui.recommendations.RecommendationsFragment
import com.smarttour360.app.ui.search.SearchFragment
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.ui.train.TrainDetailFragment
import com.smarttour360.app.ui.train.TrainResult
import com.smarttour360.app.ui.trip.TripPlannerFragment

class MainActivity : AppCompatActivity(), AppNavigator {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        AppStateStore.init(applicationContext)

        drawerLayout = findViewById(R.id.root_drawer)
        navigationView = findViewById(R.id.navigation_drawer)
        toolbar = findViewById(R.id.top_toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content_root)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_chat -> {
                    openChatbot()
                    true
                }
                else -> false
            }
        }
        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.menu_home -> enterApp(MainTab.HOME)
                R.id.menu_search -> enterApp(MainTab.SEARCH)
                R.id.menu_hotels -> openHotelList()
                R.id.menu_trips -> enterApp(MainTab.TRIPS)
                R.id.menu_cart -> openCart()
                R.id.menu_profile -> enterApp(MainTab.PROFILE)
            }
            true
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SplashFragment())
                .commit()
        }
        supportFragmentManager.addOnBackStackChangedListener {
            syncToolbarState()
        }
        setChromeVisible(false)
    }

    override fun openOnboarding() {
        setChromeVisible(false)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, OnboardingFragment())
            .commit()
    }

    override fun openLogin() {
        setChromeVisible(false)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    override fun enterApp(tab: MainTab) {
        clearBackStack()
        setChromeVisible(true)
        val fragment = when (tab) {
            MainTab.HOME -> HomeFragment()
            MainTab.SEARCH -> SearchFragment()
            MainTab.RECOMMENDATIONS -> RecommendationsFragment()
            MainTab.TRIPS -> TripPlannerFragment()
            MainTab.PROFILE -> ProfileFragment()
        }
        updateNavigationSelection(tab)
        toolbar.title = when (tab) {
            MainTab.HOME -> "SmartTour360"
            MainTab.SEARCH -> "Booking Portal"
            MainTab.TRIPS -> "Trips"
            MainTab.PROFILE -> "Profile"
            MainTab.RECOMMENDATIONS -> "Recommendations"
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        syncToolbarState()
    }

    override fun openDestinationDetail() {
        pushFragment(DestinationDetailFragment.newInstance())
    }

    override fun openHotelList() {
        toolbar.title = "Hotels"
        navigationView.setCheckedItem(R.id.menu_hotels)
        pushFragment(HotelListFragment())
    }

    override fun openHotelDetail() {
        pushFragment(HotelDetailFragment())
    }

    override fun openCart() {
        setChromeVisible(true)
        navigationView.setCheckedItem(R.id.menu_cart)
        toolbar.title = "Cart"
        pushFragment(CartFragment())
    }

    override fun openOrderConfirmation() {
        pushFragment(OrderConfirmationFragment())
    }

    override fun openBookings() {
        pushFragment(BookingsFragment())
    }

    override fun openRecommendations() {
        pushFragment(RecommendationsFragment())
    }

    override fun openChatbot(
        destinationName: String?,
        safetyFlag: String?,
        flagExplanation: String?,
        ecoScore: Int?,
        ethicalScore: String?,
        bookingMode: String?
    ) {
        toolbar.title = "Travel Assistant"
        pushFragment(
            ChatbotFragment.newInstance(
                destinationName = destinationName,
                safetyFlag = safetyFlag,
                flagExplanation = flagExplanation,
                ecoScore = ecoScore,
                ethicalScore = ethicalScore,
                bookingMode = bookingMode
            )
        )
    }

    override fun openTrainDetail(train: TrainResult) {
        pushFragment(TrainDetailFragment.newInstance(train))
    }

    override fun goBack() {
        onBackPressedDispatcher.onBackPressed()
    }

    private fun pushFragment(fragment: androidx.fragment.app.Fragment) {
        setChromeVisible(true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(fragment::class.java.simpleName)
            .commit()
        supportFragmentManager.executePendingTransactions()
        syncToolbarState()
    }

    private fun clearBackStack() {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun updateNavigationSelection(tab: MainTab) {
        val itemId = when (tab) {
            MainTab.HOME, MainTab.RECOMMENDATIONS -> R.id.menu_home
            MainTab.SEARCH -> R.id.menu_search
            MainTab.TRIPS -> R.id.menu_trips
            MainTab.PROFILE -> R.id.menu_profile
        }
        navigationView.setCheckedItem(itemId)
    }

    private fun setChromeVisible(visible: Boolean) {
        val visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        toolbar.visibility = visibility
        drawerLayout.setDrawerLockMode(
            if (visible) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )
    }

    private fun syncToolbarState() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val isChildScreen = supportFragmentManager.backStackEntryCount > 0

        if (isChildScreen) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            toolbar.setNavigationOnClickListener { goBack() }
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_menu)
            toolbar.setNavigationOnClickListener { drawerLayout.open() }
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }

        when (currentFragment) {
            is HomeFragment -> {
                toolbar.title = "SmartTour360"
                navigationView.setCheckedItem(R.id.menu_home)
            }
            is SearchFragment -> {
                toolbar.title = "Booking Portal"
                navigationView.setCheckedItem(R.id.menu_search)
            }
            is TripPlannerFragment -> {
                toolbar.title = "Trips"
                navigationView.setCheckedItem(R.id.menu_trips)
            }
            is ProfileFragment -> {
                toolbar.title = "Profile"
                navigationView.setCheckedItem(R.id.menu_profile)
            }
            is RecommendationsFragment -> {
                toolbar.title = "Recommendations"
                navigationView.setCheckedItem(R.id.menu_home)
            }
            is HotelListFragment -> {
                toolbar.title = "Hotels"
                navigationView.setCheckedItem(R.id.menu_hotels)
            }
            is ChatbotFragment -> {
                toolbar.title = "Travel Assistant"
            }
            is CartFragment -> {
                toolbar.title = "Cart"
                navigationView.setCheckedItem(R.id.menu_cart)
            }
        }
    }
}
