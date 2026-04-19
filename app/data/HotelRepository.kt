package com.smarttour360.app.data

import com.smarttour360.app.data.remote.ApiClient
import com.smarttour360.app.BuildConfig
import com.smarttour360.app.ui.common.DestinationSummary
import com.smarttour360.app.ui.common.HotelSummary
import com.smarttour360.app.ui.state.AppStateStore
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class LiveHotelResult(
    val anchorDestination: DestinationSummary?,
    val hotels: List<HotelSummary>,
    val headline: String,
    val statusMessage: String
)

class HotelRepository {
    companion object {
        private val cache = mutableMapOf<String, Pair<Long, List<HotelSummary>>>()
        private val googlePriceCache = mutableMapOf<String, Pair<Long, String>>()
        private const val cacheTtlMs = 10 * 60 * 1000L
    }

    suspend fun getHotelsForCurrentSelection(limit: Int = 8): LiveHotelResult {
        val anchor = resolveAnchorDestination()
        if (anchor == null) {
            return LiveHotelResult(
                anchorDestination = null,
                hotels = emptyList(),
                headline = "Live stays",
                statusMessage = "Select a destination first to load live nearby stays."
            )
        }
        val hotels = getHotelsForDestination(anchor, limit)
        val status = if (hotels.isEmpty()) {
            "No live nearby stays were returned for ${anchor.name} right now."
        } else {
            "${hotels.size} live nearby stays found around ${anchor.name}."
        }
        return LiveHotelResult(
            anchorDestination = anchor,
            hotels = hotels,
            headline = "Live stays near ${anchor.name}",
            statusMessage = status
        )
    }

    suspend fun getHotelsForDestination(
        destination: DestinationSummary,
        limit: Int = 8
    ): List<HotelSummary> {
        val cacheKey = "${destination.name}|${destination.subtitle}"
        val now = System.currentTimeMillis()
        cache[cacheKey]?.takeIf { now - it.first < cacheTtlMs }?.let { return it.second.take(limit) }

        val geocoded = ApiClient.geocodingApi.search(destination.name, count = 3).results
            .orEmpty()
            .firstOrNull {
                it.country.equals("India", ignoreCase = true) &&
                    (
                        it.name.contains(destination.name, ignoreCase = true) ||
                            destination.name.contains(it.name, ignoreCase = true)
                        )
            } ?: return emptyList()

        val radiusMeters = if (destination.subtitle.contains("Kerala", true) || destination.subtitle.contains("Goa", true)) {
            9000
        } else {
            7000
        }

        val query = buildString {
            append("[out:json][timeout:20];(")
            append("node[\"tourism\"~\"hotel|guest_house|hostel|motel|resort|apartment\"](around:$radiusMeters,${geocoded.latitude},${geocoded.longitude});")
            append("way[\"tourism\"~\"hotel|guest_house|hostel|motel|resort|apartment\"](around:$radiusMeters,${geocoded.latitude},${geocoded.longitude});")
            append("relation[\"tourism\"~\"hotel|guest_house|hostel|motel|resort|apartment\"](around:$radiusMeters,${geocoded.latitude},${geocoded.longitude});")
            append(");out center tags;")
        }

        val rawHotels = ApiClient.overpassApi.query(query).elements
            .orEmpty()
            .mapNotNull { element ->
                val tags = element.tags.orEmpty()
                val name = tags["name"]?.trim().orEmpty()
                if (name.isBlank() || isBadHotelName(name)) return@mapNotNull null

                val lat = element.lat ?: element.center?.lat ?: return@mapNotNull null
                val lon = element.lon ?: element.center?.lon ?: return@mapNotNull null
                val tourismType = tags["tourism"].orEmpty().ifBlank { "hotel" }
                val stars = tags["stars"]?.filter(Char::isDigit)?.toIntOrNull()
                val distanceKm = haversineKm(geocoded.latitude, geocoded.longitude, lat, lon)
                val ecoCertified = hasEcoSignals(tags)
                val ecoScore = estimateEcoScore(tourismType, ecoCertified, tags)
                val nightlyEstimate = estimateNightlyPrice(tourismType, stars, ecoCertified)
                val subtitle = buildString {
                    append(destination.name)
                    append(" - ")
                    append(tourismTypeLabel(tourismType))
                    append(" - ")
                    append(String.format("%.1f km from center", distanceKm))
                }

                HotelSummary(
                    id = "live-${element.type}-${element.id}",
                    name = name,
                    subtitle = subtitle,
                    priceText = "Approx Rs $nightlyEstimate / night",
                    ecoScore = ecoScore,
                    trend = if (tags["website"] != null || tags["phone"] != null) "DIRECT" else "LIVE MAP",
                    hasHiddenFees = false,
                    ecoCertified = ecoCertified
                )
            }
            .distinctBy { it.name.lowercase() }
            .sortedWith(
                compareByDescending<HotelSummary> { it.ecoCertified }
                    .thenByDescending { it.ecoScore }
                    .thenBy { it.name }
            )
            .take(limit)

        val hotels = enrichWithGooglePriceRanges(rawHotels, destination)
        cache[cacheKey] = now to hotels
        return hotels
    }

    fun peekCachedHotelsForDestination(
        destination: DestinationSummary,
        limit: Int = 8
    ): List<HotelSummary>? {
        val cacheKey = "${destination.name}|${destination.subtitle}"
        val now = System.currentTimeMillis()
        return cache[cacheKey]
            ?.takeIf { now - it.first < cacheTtlMs }
            ?.second
            ?.take(limit)
    }

    private fun resolveAnchorDestination(): DestinationSummary? {
        AppStateStore.selectedDestination.value?.let { return it }
        val currentMatch = AppStateStore.currentLocation.value?.matchedDestinationName
        if (!currentMatch.isNullOrBlank()) {
            AppStateStore.liveDestinations.value.firstOrNull {
                it.name.equals(currentMatch, ignoreCase = true)
            }?.let {
                return DestinationSummary(
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
        }
        return AppStateStore.liveDestinations.value
            .sortedWith(
                compareByDescending<com.smarttour360.app.ui.state.LiveDestinationSnapshot> { it.safetyFlag == "GREEN" }
                    .thenByDescending { it.ecoScore }
                    .thenByDescending { it.rating }
            )
            .firstOrNull()
            ?.let {
                DestinationSummary(
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
    }

    private fun isBadHotelName(name: String): Boolean {
        val normalized = name.lowercase()
        return listOf("home", "house", "residence", "flat", "villa", "building").any {
            normalized == it || normalized.startsWith("$it ")
        }
    }

    private fun hasEcoSignals(tags: Map<String, String>): Boolean {
        return tags.any { (key, value) ->
            val probe = "$key $value".lowercase()
            listOf("eco", "green", "solar", "sustainable", "organic").any(probe::contains)
        }
    }

    private fun estimateEcoScore(
        tourismType: String,
        ecoCertified: Boolean,
        tags: Map<String, String>
    ): Int {
        val base = when (tourismType.lowercase()) {
            "hostel" -> 78
            "guest_house" -> 76
            "apartment" -> 72
            "resort" -> 58
            "motel" -> 60
            else -> 68
        }
        val withEcoBonus = if (ecoCertified) base + 14 else base
        val contactBonus = if (tags["website"] != null || tags["phone"] != null) 4 else 0
        return (withEcoBonus + contactBonus).coerceIn(48, 92)
    }

    private fun estimateNightlyPrice(
        tourismType: String,
        stars: Int?,
        ecoCertified: Boolean
    ): Int {
        val base = when (tourismType.lowercase()) {
            "hostel" -> 1200
            "guest_house" -> 1900
            "apartment" -> 2600
            "motel" -> 2200
            "resort" -> 5200
            else -> 3200
        }
        val starsPremium = (stars ?: 0) * 650
        val ecoPremium = if (ecoCertified) 350 else 0
        return base + starsPremium + ecoPremium
    }

    private suspend fun enrichWithGooglePriceRanges(
        hotels: List<HotelSummary>,
        destination: DestinationSummary
    ): List<HotelSummary> {
        if (hotels.isEmpty() || BuildConfig.GOOGLE_PLACES_API_KEY.isBlank()) return hotels
        return hotels.map { hotel ->
            val priceText = fetchGooglePriceRange(hotel.name, destination.name)
            if (priceText == null) hotel else hotel.copy(priceText = priceText)
        }
    }

    private suspend fun fetchGooglePriceRange(hotelName: String, destinationName: String): String? {
        val cacheKey = "$hotelName|$destinationName".lowercase()
        val now = System.currentTimeMillis()
        googlePriceCache[cacheKey]?.takeIf { now - it.first < cacheTtlMs }?.let { return it.second }

        val query = "$hotelName $destinationName hotel"
        val result = runCatching {
            ApiClient.googlePlacesApi.textSearch(
                query = query,
                key = BuildConfig.GOOGLE_PLACES_API_KEY
            ).results.firstOrNull()
        }.getOrNull() ?: return null

        val priceText = mapPriceLevelToRange(result.price_level) ?: return null
        googlePriceCache[cacheKey] = now to priceText
        return priceText
    }

    private fun mapPriceLevelToRange(priceLevel: Int?): String? {
        return when (priceLevel) {
            1 -> "₹800 - ₹1,500 / night"
            2 -> "₹1,500 - ₹3,500 / night"
            3 -> "₹3,500 - ₹7,000 / night"
            4 -> "₹7,000+ / night"
            else -> null
        }
    }

    private fun tourismTypeLabel(type: String): String = when (type.lowercase()) {
        "guest_house" -> "Guest house"
        "hostel" -> "Hostel"
        "motel" -> "Motel"
        "resort" -> "Resort"
        "apartment" -> "Apartment stay"
        else -> "Hotel"
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return 6371.0 * acos(
            (sin(Math.toRadians(lat1)) * sin(Math.toRadians(lat2))) +
                (cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    cos(Math.toRadians(lon2 - lon1)))
        )
    }
}
