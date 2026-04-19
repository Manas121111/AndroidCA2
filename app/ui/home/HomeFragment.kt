package com.smarttour360.app.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.smarttour360.app.data.HotelRepository
import com.smarttour360.app.R
import com.smarttour360.app.data.TravelRepository
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.MainTab
import com.smarttour360.app.ui.common.DestinationAdapter
import com.smarttour360.app.ui.common.HotelListAdapter
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.utils.DestinationImagePrefetcher
import com.smarttour360.app.utils.LocationHelper
import com.smarttour360.app.utils.SafetyAlertNotifier
import com.smarttour360.app.utils.ShakeDetector
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class HomeFragment : Fragment() {
    companion object {
        private const val homeRefreshTtlMs = 10 * 60 * 1000L
        private const val locationRefreshTtlMs = 30 * 60 * 1000L
    }

    private val repository = TravelRepository()
    private val hotelRepository = HotelRepository()

    private lateinit var greeting: TextView
    private lateinit var statusText: TextView
    private lateinit var snapshotCard: MaterialCardView
    private lateinit var snapshotRoute: TextView
    private lateinit var snapshotTotal: TextView
    private lateinit var locationSafetyBanner: View
    private lateinit var locationCityText: TextView
    private lateinit var locationFlagText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var topAdapter: DestinationAdapter
    private lateinit var ecoAdapter: DestinationAdapter
    private lateinit var safetyAdapter: SafetySpotlightAdapter
    private lateinit var hotelAdapter: HotelListAdapter
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var rootView: View

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadCurrentLocationSafety()
        } else if (this::locationSafetyBanner.isInitialized) {
            locationSafetyBanner.isVisible = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rootView = view
        val navigator = activity as? AppNavigator
        greeting = view.findViewById(R.id.text_home_greeting)
        statusText = view.findViewById(R.id.text_home_status)
        snapshotCard = view.findViewById(R.id.card_trip_snapshot)
        snapshotRoute = view.findViewById(R.id.text_trip_snapshot_route)
        snapshotTotal = view.findViewById(R.id.text_trip_snapshot_total)
        swipeRefreshLayout = view.findViewById(R.id.swipe_home_refresh)
        locationSafetyBanner = view.findViewById(R.id.locationSafetyBanner)
        locationCityText = view.findViewById(R.id.tvLocationCity)
        locationFlagText = view.findViewById(R.id.tvLocationFlag)

        topAdapter = DestinationAdapter(
            onClick = {
                AppStateStore.selectDestination(it)
                navigator?.openDestinationDetail()
            },
            onAddToTrip = {
                AppStateStore.addDestinationToTrip(it)
                Snackbar.make(view, "${it.name} added to Trip Planner", Snackbar.LENGTH_SHORT).show()
            }
        )
        ecoAdapter = DestinationAdapter(
            onClick = {
                AppStateStore.selectDestination(it)
                navigator?.openDestinationDetail()
            },
            onAddToTrip = {
                AppStateStore.addDestinationToTrip(it)
                Snackbar.make(view, "${it.name} added to Trip Planner", Snackbar.LENGTH_SHORT).show()
            }
        )
        safetyAdapter = SafetySpotlightAdapter {
            AppStateStore.selectDestination(it)
            navigator?.openDestinationDetail()
        }

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector {
            view.post { refreshSafetyFlags(isShakeRefresh = true) }
        }

        greeting.text =
            "Good morning, ${AppStateStore.userPreferences.value.name.substringBefore(' ').ifBlank { "Traveler" }}"

        view.findViewById<RecyclerView>(R.id.recycler_top_destinations).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = topAdapter
        }
        view.findViewById<RecyclerView>(R.id.recycler_eco_picks).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = ecoAdapter
        }
        view.findViewById<RecyclerView>(R.id.recycler_safety_spotlight).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = safetyAdapter
        }
        hotelAdapter = HotelListAdapter(
            onClick = {
                AppStateStore.selectHotel(it)
                navigator?.openHotelDetail()
            },
            onAddToTrip = {
                AppStateStore.addHotelToTrip(it)
                Snackbar.make(view, "${it.name} added as a stay", Snackbar.LENGTH_SHORT).show()
            }
        )
        view.findViewById<RecyclerView>(R.id.recycler_featured_hotels).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = hotelAdapter
        }

        view.findViewById<TextView>(R.id.text_home_search).setOnClickListener {
            navigator?.openRecommendations()
        }
        view.findViewById<MaterialButton>(R.id.button_open_trip_snapshot).setOnClickListener {
            navigator?.enterApp(MainTab.TRIPS)
        }
        swipeRefreshLayout.setOnRefreshListener {
            AppStateStore.clearLiveDestinations()
            AppStateStore.clearCurrentLocation()
            repository.invalidateFeaturedDestinationsCache()
            renderFastHomeSections()
            loadDestinationSections(forceFeaturedRefresh = true) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    loadCurrentLocationSafety(forceRefresh = true)
                }
            }
        }

        renderTripSnapshot()
        statusText.text = buildHomeSubheading()
        renderFastHomeSections()
        if (shouldRefreshHomeData()) {
            loadDestinationSections(forceFeaturedRefresh = false)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (AppStateStore.hasFreshCurrentLocation(locationRefreshTtlMs)) {
                renderCurrentLocationFromState()
            } else {
                loadCurrentLocationSafety()
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        val catalog = repository.getIndianDestinationCatalog()
        SafetyAlertNotifier.checkAndNotify(requireContext(), catalog)
        if (this::shakeDetector.isInitialized) {
            shakeDetector.start(sensorManager)
        }
    }

    override fun onPause() {
        if (this::shakeDetector.isInitialized) {
            shakeDetector.stop(sensorManager)
        }
        super.onPause()
    }

    private fun shouldRefreshHomeData(): Boolean {
        return !AppStateStore.hasFreshLiveDestinations(homeRefreshTtlMs)
    }

    private fun renderTripSnapshot() {
        val summary = AppStateStore.buildTripSummary()
        val hasTrip = AppStateStore.tripEntries.value.isNotEmpty()
        snapshotCard.isVisible = hasTrip
        snapshotRoute.text = summary.routeLine
        snapshotTotal.text = summary.totalLine
    }

    private fun renderFastHomeSections() {
        val preferences = AppStateStore.userPreferences.value
        val cachedCatalog = repository.getCachedLiveIndianDestinationCatalog(preferences = preferences)
            ?: AppStateStore.liveDestinations.value.takeIf { it.isNotEmpty() }?.map(::destinationFromSnapshot)
            ?: repository.getIndianDestinationCatalog(preferences = preferences)
        renderDestinationSections(cachedCatalog)
        loadFeaturedHotels(cachedCatalog.firstOrNull(), allowNetwork = false)
    }

    private fun loadDestinationSections(
        forceFeaturedRefresh: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        if (forceFeaturedRefresh) {
            repository.invalidateFeaturedDestinationsCache()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val preferences = AppStateStore.userPreferences.value
                val liveCatalog = runCatching {
                    repository.getLiveIndianDestinationCatalog(
                        forceRefresh = forceFeaturedRefresh,
                        preferences = preferences
                    )
                }.getOrElse {
                    repository.getIndianDestinationCatalog(preferences = preferences)
                }

                AppStateStore.setLiveDestinations(liveCatalog)
                renderDestinationSections(liveCatalog, enrichImages = true)
                loadFeaturedHotels(liveCatalog.firstOrNull(), allowNetwork = true)
            } finally {
                swipeRefreshLayout.isRefreshing = false
                onComplete?.invoke()
            }
        }
    }

    private fun renderDestinationSections(
        catalog: List<com.smarttour360.app.ui.common.DestinationSummary>,
        enrichImages: Boolean = false
    ) {
        val seed = (
            (System.currentTimeMillis() / 60_000L) +
                AppStateStore.activeUserKey.value.orEmpty().hashCode().toLong().absoluteValue
            ).toInt()
        val featured = rotateList(
            catalog.sortedWith(
                compareByDescending<com.smarttour360.app.ui.common.DestinationSummary> { it.flag == "GREEN" }
                    .thenByDescending { it.rating }
                    .thenByDescending { it.ecoScore }
            ),
            seed
        ).take(8)
            .ifEmpty { repository.getFeaturedDestinationsFallback() }
        val spotlight = catalog
            .sortedWith(
                compareByDescending<com.smarttour360.app.ui.common.DestinationSummary> { it.flag == "GREEN" }
                    .thenByDescending { it.rating }
                    .thenByDescending { it.ecoScore }
            )
            .take(5)
        val ecoPicks = rotateList(
            catalog.filter { it.ecoScore >= 72 }.sortedByDescending { it.ecoScore },
            seed / 2 + 3
        ).take(6)

        if (!enrichImages) {
            safetyAdapter.submitList(spotlight)
            ecoAdapter.submitList(ecoPicks)
            topAdapter.submitList(featured)
            statusText.text = buildHomeSubheading(featured.firstOrNull())
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val visibleCards = (featured + spotlight + ecoPicks).distinctBy { "${it.name}-${it.subtitle}" }
            val enrichedVisibleCards = repository.populateImages(visibleCards)
            val enrichedLookup = enrichedVisibleCards.associateBy { "${it.name}-${it.subtitle}" }
            val featuredWithImages = featured.map { enrichedLookup["${it.name}-${it.subtitle}"] ?: it }
            val spotlightWithImages = spotlight.map { enrichedLookup["${it.name}-${it.subtitle}"] ?: it }
            val ecoPicksWithImages = ecoPicks.map { enrichedLookup["${it.name}-${it.subtitle}"] ?: it }
            DestinationImagePrefetcher.prefetch(
                requireContext(),
                enrichedVisibleCards.mapNotNull { it.imageUrl }
            )
            safetyAdapter.submitList(spotlightWithImages)
            ecoAdapter.submitList(ecoPicksWithImages)
            topAdapter.submitList(featuredWithImages)
            statusText.text = buildHomeSubheading(featuredWithImages.firstOrNull())
        }
    }

    private fun loadFeaturedHotels(
        fallbackDestination: com.smarttour360.app.ui.common.DestinationSummary?,
        allowNetwork: Boolean
    ) {
        val anchor = AppStateStore.currentLocation.value?.matchedDestinationName
                ?.let { matched ->
                    AppStateStore.liveDestinations.value.firstOrNull { it.name.equals(matched, ignoreCase = true) }
                        ?.let {
                            com.smarttour360.app.ui.common.DestinationSummary(
                                id = it.name,
                                name = it.name,
                                subtitle = it.region,
                                flag = it.safetyFlag,
                                ecoScore = it.ecoScore,
                                ethicalScore = if (it.ecoScore >= 75) "HIGH" else "MODERATE",
                                carbonKg = it.carbonKg,
                                rating = it.rating,
                                imageUrl = it.imageUrl
                            )
                        }
                } ?: AppStateStore.selectedDestination.value ?: fallbackDestination
        if (anchor == null) {
            hotelAdapter.submitList(emptyList())
            return
        }

        hotelRepository.peekCachedHotelsForDestination(anchor, limit = 4)?.let {
            hotelAdapter.submitList(it)
            if (!allowNetwork) return
        } ?: run {
            if (!allowNetwork) {
                hotelAdapter.submitList(emptyList())
                return
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val hotels = runCatching { hotelRepository.getHotelsForDestination(anchor, limit = 4) }
                .getOrDefault(emptyList())
            hotelAdapter.submitList(hotels)
        }
    }

    private fun <T> rotateList(items: List<T>, seed: Int): List<T> {
        if (items.isEmpty()) return emptyList()
        val rotation = (seed.absoluteValue % items.size)
        return items.drop(rotation) + items.take(rotation)
    }

    private fun loadCurrentLocationSafety(forceRefresh: Boolean = false) {
        if (!forceRefresh && AppStateStore.hasFreshCurrentLocation(locationRefreshTtlMs)) {
            renderCurrentLocationFromState()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            LocationHelper.getCurrentLocation(requireContext()).collect { userLocation ->
                if (userLocation == null) {
                    locationSafetyBanner.isVisible = false
                    AppStateStore.clearCurrentLocation()
                    return@collect
                }

                val matchedDestination = (
                    AppStateStore.liveDestinations.value.takeIf { it.isNotEmpty() }?.map(::destinationFromSnapshot)
                        ?: repository.getCachedLiveIndianDestinationCatalog()
                        ?: repository.getIndianDestinationCatalog()
                    )
                    .firstOrNull { destination ->
                        destination.name.contains(userLocation.cityName, ignoreCase = true) ||
                            userLocation.cityName.contains(destination.name, ignoreCase = true)
                    }

                val flag = matchedDestination?.flag ?: "GREEN"
                AppStateStore.setCurrentLocation(
                    cityName = userLocation.cityName,
                    safetyFlag = flag,
                    matchedDestinationName = matchedDestination?.name
                )
                renderCurrentLocationFromState()
            }
        }
    }

    private fun renderCurrentLocationFromState() {
        val current = AppStateStore.currentLocation.value
        if (current == null) {
            locationSafetyBanner.isVisible = false
            return
        }
        locationCityText.text = "Location: ${current.cityName}"
        locationFlagText.text = "${flagEmoji(current.safetyFlag)} ${flagLabel(current.safetyFlag)}"
        locationSafetyBanner.setBackgroundColor(
            ContextCompat.getColor(requireContext(), flagSurfaceColor(current.safetyFlag))
        )
        locationSafetyBanner.isVisible = true
    }

    private fun destinationFromSnapshot(snapshot: com.smarttour360.app.ui.state.LiveDestinationSnapshot): com.smarttour360.app.ui.common.DestinationSummary {
        return com.smarttour360.app.ui.common.DestinationSummary(
            id = snapshot.name,
            name = snapshot.name,
            subtitle = snapshot.region,
            flag = snapshot.safetyFlag,
            ecoScore = snapshot.ecoScore,
            ethicalScore = if (snapshot.ecoScore >= 75) "HIGH" else "MODERATE",
            carbonKg = snapshot.carbonKg,
            rating = snapshot.rating,
            imageUrl = snapshot.imageUrl
        )
    }

    private fun refreshSafetyFlags(isShakeRefresh: Boolean = false) {
        val snackbarMessage = if (isShakeRefresh) {
            "Refreshing destinations..."
        } else {
            "Refreshing travel signals..."
        }
        Snackbar.make(rootView, snackbarMessage, Snackbar.LENGTH_SHORT).show()
        loadDestinationSections(forceFeaturedRefresh = true)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            loadCurrentLocationSafety()
        }
        renderTripSnapshot()
    }

    private fun flagEmoji(flag: String): String = when (flag.uppercase()) {
        "RED" -> "RED"
        "YELLOW" -> "YELLOW"
        else -> "GREEN"
    }

    private fun flagLabel(flag: String): String = when (flag.uppercase()) {
        "RED" -> "Avoid - high risk"
        "YELLOW" -> "Travel with caution"
        else -> "Safe to travel"
    }

    private fun flagSurfaceColor(flag: String): Int = when (flag.uppercase()) {
        "RED" -> R.color.flag_red_surface
        "YELLOW" -> R.color.flag_yellow_surface
        else -> R.color.flag_green_surface
    }

    private fun buildHomeSubheading(topPick: com.smarttour360.app.ui.common.DestinationSummary? = null): String {
        val user = AppStateStore.userPreferences.value
        val style = user.tripTypes.split(",").firstOrNull()?.trim()?.lowercase().orEmpty()
        return when {
            topPick != null -> "${topPick.name} is trending now for ${style.ifBlank { "your" }} trips."
            style.isNotBlank() -> "Fresh destination picks for your $style travel style."
            else -> "Fresh destination picks based on current travel conditions."
        }
    }
}
