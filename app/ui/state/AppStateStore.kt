package com.smarttour360.app.ui.state

import android.content.Context
import com.smarttour360.app.data.local.SmartTourDatabase
import com.smarttour360.app.data.local.UserProfileEntity
import com.smarttour360.app.ui.common.DestinationSummary
import com.smarttour360.app.ui.common.HotelSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

data class UserPreferencesState(
    val name: String = "Guest Explorer",
    val email: String = "",
    val mobile: String = "",
    val country: String = "India",
    val homeCity: String = "",
    val budget: String = "Mid-range",
    val tripTypes: String = "Adventure, Heritage",
    val preferredTransport: String = "Hotels, Train",
    val ecoPriority: Boolean = true
)

enum class TripItemType {
    DESTINATION,
    HOTEL
}

data class TripEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: TripItemType,
    val title: String,
    val subtitle: String,
    val timing: String,
    val scoreText: String,
    val budgetInr: Int,
    val routeLabel: String
)

data class TripPlanSummary(
    val headline: String,
    val routeLine: String,
    val durationLine: String,
    val transportLine: String,
    val stayLine: String,
    val totalLine: String,
    val plannerNote: String
)

data class CartEntry(
    val id: String = UUID.randomUUID().toString(),
    val hotelName: String,
    val stayInfo: String,
    val priceInr: Int,
    val flag: String,
    val acknowledged: Boolean
)

data class CartTotals(
    val subtotal: Int,
    val taxes: Int,
    val total: Int
)

data class BookingRecord(
    val id: String,
    val hotelName: String,
    val stayInfo: String,
    val totalInr: Int,
    val flagAtBooking: String,
    val blockchainRef: String,
    val ecoImpactSummary: String,
    val status: String,
    val acknowledged: Boolean
)

data class OrderConfirmationState(
    val bookingCount: Int,
    val headline: String,
    val bookingIdLine: String,
    val detailLine: String,
    val blockchainLine: String,
    val ecoImpactLine: String
)

data class LiveDestinationSnapshot(
    val name: String,
    val region: String,
    val safetyFlag: String,
    val ecoScore: Int,
    val rating: Double,
    val carbonKg: Int,
    val imageUrl: String? = null,
    val updatedAtEpochMs: Long
)

data class AssistantRecommendationSnapshot(
    val destinationName: String,
    val region: String,
    val safetyFlag: String,
    val ecoScore: Int,
    val rating: Double,
    val budgetLabel: String,
    val rankReason: String,
    val liveSignal: String,
    val routeHint: String,
    val vibeTags: List<String>
)

data class CurrentLocationSnapshot(
    val cityName: String,
    val safetyFlag: String,
    val matchedDestinationName: String? = null,
    val updatedAtEpochMs: Long
)

data class PendingTrainSearch(
    val fromQuery: String,
    val toQuery: String,
    val travelDate: String? = null
)

object AppStateStore {
    private var appContext: Context? = null

    var localPressureHpa: Float = 1013f
    var localPressureRisk: Float = 0.1f
    var localPressureLabel: String = ""
    var barometerAvailable: Boolean = false

    private val _selectedDestination = MutableStateFlow<DestinationSummary?>(null)
    val selectedDestination: StateFlow<DestinationSummary?> = _selectedDestination.asStateFlow()

    private val _selectedHotel = MutableStateFlow<HotelSummary?>(null)
    val selectedHotel: StateFlow<HotelSummary?> = _selectedHotel.asStateFlow()

    private val _tripEntries = MutableStateFlow<List<TripEntry>>(emptyList())
    val tripEntries: StateFlow<List<TripEntry>> = _tripEntries.asStateFlow()

    private val _cartEntries = MutableStateFlow<List<CartEntry>>(emptyList())
    val cartEntries: StateFlow<List<CartEntry>> = _cartEntries.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingRecord>>(emptyList())
    val bookings: StateFlow<List<BookingRecord>> = _bookings.asStateFlow()

    private val _latestOrderConfirmation = MutableStateFlow<OrderConfirmationState?>(null)
    val latestOrderConfirmation: StateFlow<OrderConfirmationState?> =
        _latestOrderConfirmation.asStateFlow()

    private val _userPreferences = MutableStateFlow(UserPreferencesState())
    val userPreferences: StateFlow<UserPreferencesState> = _userPreferences.asStateFlow()

    private val _activeUserKey = MutableStateFlow<String?>(null)
    val activeUserKey: StateFlow<String?> = _activeUserKey.asStateFlow()

    private val _liveDestinations = MutableStateFlow<List<LiveDestinationSnapshot>>(emptyList())
    val liveDestinations: StateFlow<List<LiveDestinationSnapshot>> = _liveDestinations.asStateFlow()

    private val _assistantRecommendations =
        MutableStateFlow<List<AssistantRecommendationSnapshot>>(emptyList())
    val assistantRecommendations: StateFlow<List<AssistantRecommendationSnapshot>> =
        _assistantRecommendations.asStateFlow()

    private val _currentLocation = MutableStateFlow<CurrentLocationSnapshot?>(null)
    val currentLocation: StateFlow<CurrentLocationSnapshot?> = _currentLocation.asStateFlow()

    private val _pendingTrainSearch = MutableStateFlow<PendingTrainSearch?>(null)
    val pendingTrainSearch: StateFlow<PendingTrainSearch?> = _pendingTrainSearch.asStateFlow()

    val chatHistory: MutableList<Pair<String, String>> = mutableListOf()

    fun init(context: Context) {
        appContext = context.applicationContext
        _activeUserKey.value = null
        _userPreferences.value = UserPreferencesState()
        _liveDestinations.value = emptyList()
        _assistantRecommendations.value = emptyList()
        _currentLocation.value = null
        _pendingTrainSearch.value = null
        chatHistory.clear()
    }

    fun startUserSession(context: Context, identity: String) {
        val userKey = normalizeUserKey(identity)
        val profile = SmartTourDatabase.getInstance(context).userProfileDao().getProfile(userKey)
        _activeUserKey.value = userKey
        _userPreferences.value = profile?.toState() ?: defaultProfileFor(identity)
        _liveDestinations.value = emptyList()
        _assistantRecommendations.value = emptyList()
        _currentLocation.value = null
        _pendingTrainSearch.value = null
        chatHistory.clear()
    }

    fun savePreferences(
        context: Context,
        name: String,
        email: String,
        mobile: String,
        country: String,
        homeCity: String,
        budget: String,
        tripTypes: String,
        preferredTransport: String,
        ecoPriority: Boolean
    ) {
        val userKey = _activeUserKey.value ?: normalizeUserKey(email.ifBlank { mobile.ifBlank { name } })
        val state = UserPreferencesState(
            name = name,
            email = email,
            mobile = mobile,
            country = country,
            homeCity = homeCity,
            budget = budget,
            tripTypes = tripTypes,
            preferredTransport = preferredTransport,
            ecoPriority = ecoPriority
        )
        SmartTourDatabase.getInstance(context).userProfileDao().upsert(state.toEntity(userKey))
        _activeUserKey.value = userKey
        _userPreferences.value = state
    }

    fun selectDestination(destination: DestinationSummary) {
        _selectedDestination.value = destination
    }

    fun selectHotel(hotel: HotelSummary) {
        _selectedHotel.value = hotel
    }

    fun addDestinationToTrip(destination: DestinationSummary) {
        val entry = TripEntry(
            type = TripItemType.DESTINATION,
            title = destination.name,
            subtitle = destination.subtitle,
            timing = "${destination.flag} flag - rating ${"%.1f".format(destination.rating)}",
            scoreText = "Eco ${destination.ecoScore} - Ethical ${destination.ethicalScore} - ~${destination.carbonKg}kg CO2",
            budgetInr = 2200 + destination.carbonKg * 55,
            routeLabel = "Reach ${destination.name} and spend 2 nights exploring local circuits."
        )
        appendTripEntry(entry)
    }

    fun addCurrentDestinationToTrip() {
        val destination = _selectedDestination.value ?: return
        addDestinationToTrip(destination)
    }

    fun addHotelToTrip(hotel: HotelSummary) {
        val nightly = extractNightlyEstimate(hotel)
        val location = hotel.subtitle.substringBefore(" -").ifBlank { hotel.subtitle }
        val entry = TripEntry(
            type = TripItemType.HOTEL,
            title = hotel.name,
            subtitle = location,
            timing = if (hotel.ecoCertified) "Stay pick - eco certified" else "Stay pick - standard",
            scoreText = "Approx ${hotel.priceText} - Trend ${hotel.trend}",
            budgetInr = nightly * 2,
            routeLabel = "Use ${hotel.name} as your base stay around $location."
        )
        appendTripEntry(entry)
    }

    fun addSelectedHotelToTrip() {
        val hotel = _selectedHotel.value ?: return
        addHotelToTrip(hotel)
    }

    fun addSelectedHotelToCart() {
        val hotel = _selectedHotel.value ?: return
        val nightly = extractNightlyEstimate(hotel)
        val destinationLabel = hotel.subtitle.substringBefore(" -").trim().ifBlank { hotel.subtitle }
        val entry = CartEntry(
            hotelName = hotel.name,
            stayInfo = "2 nights - 2 guests - $destinationLabel",
            priceInr = nightly * 2,
            flag = if (hotel.hasHiddenFees) "YELLOW" else "GREEN",
            acknowledged = hotel.hasHiddenFees
        )
        _cartEntries.value = _cartEntries.value + entry
    }

    fun buildTripSummary(entries: List<TripEntry> = _tripEntries.value): TripPlanSummary {
        if (entries.isEmpty()) {
            return TripPlanSummary(
                headline = "No trip added yet",
                routeLine = "Add destinations to build your route.",
                durationLine = "0 days",
                transportLine = formatCurrency(0),
                stayLine = formatCurrency(0),
                totalLine = formatCurrency(0),
                plannerNote = "Start by adding a place from Home or Recommendations."
            )
        }

        val destinations = entries.filter { it.type == TripItemType.DESTINATION }
        val hotels = entries.filter { it.type == TripItemType.HOTEL }
        val uniqueStops = destinations.map { it.title }.distinct()
        val travelDays = (destinations.size * 2) + hotels.size.coerceAtLeast(1)
        val transportBudget = if (destinations.isEmpty()) 0 else 1800 + ((destinations.size - 1).coerceAtLeast(0) * 2200)
        val stayBudget = hotels.sumOf { it.budgetInr }
        val total = transportBudget + stayBudget
        val route = if (uniqueStops.isEmpty()) {
            hotels.map { it.subtitle }.distinct().joinToString(" -> ")
        } else {
            uniqueStops.joinToString(" -> ")
        }
        val anchorStay = hotels.firstOrNull()?.title ?: "add a stay near your main stop"

        return TripPlanSummary(
            headline = if (uniqueStops.size > 1) "Multi-stop itinerary is ready" else "Single-region itinerary is ready",
            routeLine = route,
            durationLine = "$travelDays days",
            transportLine = formatCurrency(transportBudget),
            stayLine = formatCurrency(stayBudget),
            totalLine = formatCurrency(total),
            plannerNote = "Start in ${uniqueStops.firstOrNull() ?: hotels.firstOrNull()?.subtitle ?: "your first stop"}. If possible, use $anchorStay as your base."
        )
    }

    fun clearTripPlan() {
        _tripEntries.value = emptyList()
    }

    fun removeTripEntry(entry: TripEntry) {
        _tripEntries.value = _tripEntries.value.filterNot { it.id == entry.id }
    }

    fun appendChat(role: String, content: String) {
        chatHistory += role to content
    }

    fun setLiveDestinations(destinations: List<DestinationSummary>) {
        val now = System.currentTimeMillis()
        _liveDestinations.value = destinations.map {
            LiveDestinationSnapshot(
                name = it.name,
                region = it.subtitle,
                safetyFlag = it.flag,
                ecoScore = it.ecoScore,
                rating = it.rating,
                carbonKg = it.carbonKg,
                imageUrl = it.imageUrl,
                updatedAtEpochMs = now
            )
        }
    }

    fun setAssistantRecommendations(recommendations: List<AssistantRecommendationSnapshot>) {
        _assistantRecommendations.value = recommendations
    }

    fun setCurrentLocation(cityName: String, safetyFlag: String, matchedDestinationName: String?) {
        _currentLocation.value = CurrentLocationSnapshot(
            cityName = cityName,
            safetyFlag = safetyFlag,
            matchedDestinationName = matchedDestinationName,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    fun clearCurrentLocation() {
        _currentLocation.value = null
    }

    fun clearLiveDestinations() {
        _liveDestinations.value = emptyList()
    }

    fun queueTrainSearch(fromQuery: String, toQuery: String, travelDate: String? = null) {
        val from = fromQuery.trim()
        val to = toQuery.trim()
        if (from.isBlank() || to.isBlank()) return
        _pendingTrainSearch.value = PendingTrainSearch(
            fromQuery = from,
            toQuery = to,
            travelDate = travelDate?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    fun consumePendingTrainSearch(): PendingTrainSearch? {
        val pending = _pendingTrainSearch.value
        _pendingTrainSearch.value = null
        return pending
    }

    fun getCartTotals(): CartTotals {
        val subtotal = _cartEntries.value.sumOf { it.priceInr }
        val taxes = (subtotal * 0.12).toInt()
        return CartTotals(subtotal, taxes, subtotal + taxes)
    }

    fun placeOrder(): OrderConfirmationState? {
        val entries = _cartEntries.value
        if (entries.isEmpty()) return null

        val newBookings = entries.mapIndexed { index, entry ->
            val bookingId = buildBookingId(_bookings.value.size + index + 1)
            BookingRecord(
                id = bookingId,
                hotelName = entry.hotelName,
                stayInfo = entry.stayInfo,
                totalInr = applyTaxes(entry.priceInr),
                flagAtBooking = entry.flag,
                blockchainRef = buildBlockchainRef(bookingId),
                ecoImpactSummary = "~${18 + ((entry.priceInr / 1000) * 7)}kg CO2 for this stay",
                status = if (index == 0) "Confirmed" else "Upcoming",
                acknowledged = entry.acknowledged
            )
        }

        _bookings.value = newBookings + _bookings.value
        _cartEntries.value = emptyList()

        val firstBooking = newBookings.first()
        val ecoImpact = if (newBookings.size == 1) {
            firstBooking.ecoImpactSummary
        } else {
            "~${newBookings.sumOf { 18 + ((it.totalInr / 1000) * 5) }}kg CO2 across this checkout"
        }

        val confirmation = OrderConfirmationState(
            bookingCount = newBookings.size,
            headline = if (newBookings.size == 1) "Booking Confirmed!" else "${newBookings.size} bookings confirmed!",
            bookingIdLine = if (newBookings.size == 1) {
                "Booking ID: ${firstBooking.id}"
            } else {
                "Primary booking: ${firstBooking.id}"
            },
            detailLine = buildString {
                append(firstBooking.hotelName)
                append('\n')
                append(firstBooking.stayInfo)
                if (newBookings.size > 1) {
                    append('\n')
                    append("${newBookings.size - 1} more stay(s) added to My Bookings")
                }
            },
            blockchainLine = "Blockchain Ref: ${firstBooking.blockchainRef}",
            ecoImpactLine = "Eco impact summary: $ecoImpact"
        )
        _latestOrderConfirmation.value = confirmation
        return confirmation
    }

    fun formatCurrency(value: Int): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }.format(value)
    }

    fun getAppContext(): Context? = appContext

    fun buildLiveApiSnapshot(): String {
        val liveSummary = _liveDestinations.value
            .sortedWith(
                compareByDescending<LiveDestinationSnapshot> { it.safetyFlag == "GREEN" }
                    .thenByDescending { it.ecoScore }
                    .thenByDescending { it.rating }
            )
            .take(5)
            .joinToString(" | ") {
                "${it.name}: ${it.safetyFlag}, eco ${it.ecoScore}, rating ${"%.1f".format(it.rating)}"
            }

        val locationSummary = _currentLocation.value?.let {
            "Current location ${it.cityName}, flag ${it.safetyFlag}, matched destination ${it.matchedDestinationName ?: "none"}"
        }.orEmpty()

        return listOf(locationSummary, liveSummary)
            .filter { it.isNotBlank() }
            .joinToString(" | ")
    }

    fun latestLiveDestinationUpdateEpochMs(): Long {
        return _liveDestinations.value.maxOfOrNull { it.updatedAtEpochMs } ?: 0L
    }

    fun hasFreshLiveDestinations(maxAgeMs: Long): Boolean {
        val latest = latestLiveDestinationUpdateEpochMs()
        return latest > 0L && (System.currentTimeMillis() - latest) <= maxAgeMs
    }

    fun hasFreshCurrentLocation(maxAgeMs: Long): Boolean {
        val snapshot = _currentLocation.value ?: return false
        return (System.currentTimeMillis() - snapshot.updatedAtEpochMs) <= maxAgeMs
    }

    private fun appendTripEntry(entry: TripEntry) {
        if (_tripEntries.value.none { it.type == entry.type && it.title == entry.title && it.subtitle == entry.subtitle }) {
            _tripEntries.value = _tripEntries.value + entry
        }
    }

    private fun applyTaxes(amount: Int): Int = amount + ((amount * 0.12).toInt())

    private fun extractNightlyEstimate(hotel: HotelSummary): Int {
        val parsed = hotel.priceText.filter { it.isDigit() }.toIntOrNull()
        if (parsed != null) return parsed
        return 1800 + (hotel.ecoScore * 18) + if (hotel.ecoCertified) 350 else 0
    }

    private fun buildBookingId(sequence: Int): String =
        "BKG-25137-${sequence.toString().padStart(3, '0')}"

    private fun buildBlockchainRef(seed: String): String {
        val compact = seed.filter { it.isLetterOrDigit() }.lowercase(Locale.US).takeLast(8)
        return "0x${compact.padStart(16, '0')}"
    }

    private fun defaultProfileFor(identity: String): UserPreferencesState {
        val trimmed = identity.trim()
        return UserPreferencesState(
            email = trimmed.takeIf { '@' in it }.orEmpty(),
            mobile = trimmed.takeIf { it.any(Char::isDigit) && '@' !in it }.orEmpty()
        )
    }

    private fun normalizeUserKey(identity: String): String {
        val cleaned = identity.trim().lowercase(Locale.US)
        return if (cleaned.isBlank()) "guest" else cleaned.replace(" ", "")
    }

    private fun UserProfileEntity.toState(): UserPreferencesState {
        return UserPreferencesState(
            name = name,
            email = email,
            mobile = mobile,
            country = country,
            homeCity = homeCity,
            budget = budget,
            tripTypes = tripTypes,
            preferredTransport = preferredTransport,
            ecoPriority = ecoPriority
        )
    }

    private fun UserPreferencesState.toEntity(userKey: String): UserProfileEntity {
        return UserProfileEntity(
            userKey = userKey,
            name = name,
            email = email,
            mobile = mobile,
            country = country,
            homeCity = homeCity,
            budget = budget,
            tripTypes = tripTypes,
            preferredTransport = preferredTransport,
            ecoPriority = ecoPriority
        )
    }
}
