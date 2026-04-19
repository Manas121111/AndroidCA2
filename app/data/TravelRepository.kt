package com.smarttour360.app.data

import com.smarttour360.app.BuildConfig
import com.smarttour360.app.data.remote.ApiClient
import com.smarttour360.app.ui.common.DestinationSummary
import com.smarttour360.app.ui.common.ForecastUi
import com.smarttour360.app.ui.common.LiveEventUi
import com.smarttour360.app.ui.common.SampleData
import com.smarttour360.app.ui.common.TravelSuggestion
import com.smarttour360.app.ui.state.UserPreferencesState
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.utils.DestinationImageUrl
import android.text.Html
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class TravelRepository {
    companion object {
        private val imageCache = mutableMapOf<String, CachedPlaceImage>()
        private val imageCacheTtlMs = TimeUnit.HOURS.toMillis(24)
        private var sharedLiveCatalog: List<DestinationSummary>? = null
        private var sharedLiveCatalogEpochMs: Long = 0L
        private val liveCatalogTtlMs = TimeUnit.MINUTES.toMillis(15)
    }

    private var cachedFeaturedDestinations: List<DestinationSummary>? = null
    private val crimeBaselineRepository: CrimeBaselineRepository? by lazy {
        AppStateStore.getAppContext()?.let(::CrimeBaselineRepository)
    }

    suspend fun getFeaturedDestinations(): List<DestinationSummary> = coroutineScope {
        cachedFeaturedDestinations?.let { return@coroutineScope it }
        val liveCatalog = getLiveIndianDestinationCatalog(forceRefresh = false)
        val resolved = if (liveCatalog.isEmpty()) {
            SampleData.destinations.take(8)
        } else {
            liveCatalog
                .sortedWith(
                    compareByDescending<DestinationSummary> { it.flag == "GREEN" }
                        .thenByDescending { it.ecoScore }
                        .thenByDescending { it.rating }
                )
                .take(8)
        }
        cachedFeaturedDestinations = resolved
        resolved
    }

    fun invalidateFeaturedDestinationsCache() {
        cachedFeaturedDestinations = null
        sharedLiveCatalog = null
        sharedLiveCatalogEpochMs = 0L
    }

    fun getFeaturedDestinationsFallback(): List<DestinationSummary> {
        return cachedFeaturedDestinations ?: SampleData.destinations.take(8)
    }

    fun getCachedLiveIndianDestinationCatalog(
        query: String = "",
        preferences: UserPreferencesState? = null
    ): List<DestinationSummary>? {
        val now = System.currentTimeMillis()
        val cached = sharedLiveCatalog?.takeIf { now - sharedLiveCatalogEpochMs < liveCatalogTtlMs }
            ?: return null
        return applyCatalogFilters(cached, query, preferences)
    }

    suspend fun searchDestinations(query: String): List<DestinationSummary> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return getFeaturedDestinations()

        val localMatches = getLiveIndianDestinationCatalog(forceRefresh = false).filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.subtitle.contains(trimmed, ignoreCase = true)
        }

        val remoteMatches = runCatching {
            val results = ApiClient.geocodingApi.search(trimmed, count = 8).results.orEmpty()
            coroutineScope {
                results.map { location ->
                    async {
                        val forecast = ApiClient.forecastApi.forecast(location.latitude, location.longitude)
                        val current = forecast.current ?: return@async null
                        val liveSignal = buildLiveSignalMetrics(
                            city = location.name,
                            region = location.admin1 ?: location.country,
                            temperature = current.temperature_2m,
                            windSpeed = current.wind_speed_10m,
                            weatherCode = current.weather_code
                        )
                        DestinationSummary(
                            id = location.id.toString(),
                            name = location.name,
                            subtitle = location.admin1 ?: location.country,
                            flag = liveSignal.flag,
                            ecoScore = liveSignal.ecoScore,
                            ethicalScore = ethicalScoreFor(liveSignal.ecoScore),
                            carbonKg = liveSignal.carbonKg,
                            rating = liveSignal.rating
                        )
                    }
                }.awaitAll().filterNotNull()
            }
        }.getOrDefault(emptyList())

        return (localMatches + remoteMatches)
            .distinctBy { "${it.name}-${it.subtitle}" }
            .sortedByDescending { it.ecoScore }
    }

    fun getIndianDestinationCatalog(
        query: String = "",
        preferences: UserPreferencesState? = null
    ): List<DestinationSummary> {
        val trimmed = query.trim()
        return SampleData.destinations
            .filter {
                trimmed.isBlank() ||
                    it.name.contains(trimmed, ignoreCase = true) ||
                    it.subtitle.contains(trimmed, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<DestinationSummary> { personalizedDestinationScore(it, preferences) }
                    .thenByDescending { it.ecoScore }
                    .thenBy { it.name }
            )
    }

    suspend fun getLiveIndianDestinationCatalog(
        forceRefresh: Boolean,
        query: String = "",
        preferences: UserPreferencesState? = null
    ): List<DestinationSummary> = coroutineScope {
        val now = System.currentTimeMillis()
        if (!forceRefresh && query.isBlank()) {
            sharedLiveCatalog
                ?.takeIf { now - sharedLiveCatalogEpochMs < liveCatalogTtlMs }
                ?.let { return@coroutineScope applyCatalogFilters(it, query, preferences) }
        }

        val live = SampleData.destinations.map { seed ->
            async {
                val updated = runCatching {
                    val location = ApiClient.geocodingApi.search(seed.name, count = 3).results
                        .orEmpty()
                        .firstOrNull { result ->
                            val region = result.admin1.orEmpty()
                            region.contains(seed.subtitle, ignoreCase = true) ||
                                seed.subtitle.contains(region, ignoreCase = true) ||
                                result.country.equals("India", ignoreCase = true)
                        } ?: return@runCatching null

                    val forecast = ApiClient.forecastApi.forecast(location.latitude, location.longitude)
                    val current = forecast.current ?: return@runCatching null
                    val liveSignal = buildLiveSignalMetrics(
                        city = location.name,
                        region = location.admin1 ?: seed.subtitle,
                        temperature = current.temperature_2m,
                        windSpeed = current.wind_speed_10m,
                        weatherCode = current.weather_code
                    )
                    seed.copy(
                        id = location.id.toString(),
                        name = location.name,
                        subtitle = location.admin1 ?: seed.subtitle,
                        flag = liveSignal.flag,
                        ecoScore = liveSignal.ecoScore,
                        ethicalScore = ethicalScoreFor(liveSignal.ecoScore),
                        carbonKg = liveSignal.carbonKg,
                        rating = liveSignal.rating
                    )
                }.getOrNull()

                if (updated != null) {
                    updated
                } else {
                    seed
                }
            }
        }.awaitAll()

        if (query.isBlank()) {
            sharedLiveCatalog = live
            sharedLiveCatalogEpochMs = now
        }

        applyCatalogFilters(live, query, preferences)
    }

    suspend fun getDestinationForecast(city: String): List<ForecastUi> {
        val location = ApiClient.geocodingApi.search(city).results?.firstOrNull() ?: return emptyList()
        val forecast = ApiClient.forecastApi.forecast(location.latitude, location.longitude)
        val daily = forecast.daily ?: return emptyList()
        return daily.time.take(3).mapIndexed { index, day ->
            val label = day.takeLast(2)
            val maxTemp = daily.temperature_2m_max.getOrNull(index)?.roundToInt() ?: 0
            ForecastUi(
                day = label,
                icon = weatherIcon(daily.weather_code.getOrNull(index) ?: 0),
                temperature = "${maxTemp}C"
            )
        }
    }

    suspend fun populateImages(destinations: List<DestinationSummary>): List<DestinationSummary> = coroutineScope {
        destinations.map { destination ->
            async {
                if (!destination.imageUrl.isNullOrBlank()) {
                    destination
                } else {
                    val imageAsset = fetchPlaceImage(destination.name, destination.subtitle)
                    destination.copy(
                        imageUrl = imageAsset?.url ?: DestinationImageUrl.forDestination(destination.name),
                        imageAttribution = imageAsset?.attribution
                    )
                }
            }
        }.awaitAll()
    }

    suspend fun getLiveEvents(keyword: String): List<LiveEventUi> {
        if (BuildConfig.TICKETMASTER_API_KEY.isBlank()) return emptyList()
        return runCatching {
            ApiClient.ticketmasterApi.events(BuildConfig.TICKETMASTER_API_KEY, keyword)
                ._embedded?.events.orEmpty()
                .mapNotNull { event ->
                    val venue = event._embedded?.venues?.firstOrNull()
                    LiveEventUi(
                        id = event.id,
                        name = event.name,
                        venue = venue?.name ?: "Venue TBA",
                        location = listOfNotNull(venue?.city?.name, venue?.country?.name).joinToString(", "),
                        date = listOfNotNull(event.dates?.start?.localDate, event.dates?.start?.localTime).joinToString(" ")
                    )
                }
        }.getOrDefault(emptyList())
    }

    suspend fun getSuggestions(
        query: String = "",
        refreshToken: Long = System.currentTimeMillis(),
        preferences: UserPreferencesState? = null,
        userKey: String = ""
    ): List<TravelSuggestion> {
        val catalog = runCatching {
            getLiveIndianDestinationCatalog(forceRefresh = false, query = query, preferences = preferences)
        }.getOrElse {
            getIndianDestinationCatalog(query, preferences)
        }
        val live = runCatching { searchDestinations(query) }.getOrDefault(emptyList())
        val merged = (live + catalog)
            .distinctBy { "${it.name}-${it.subtitle}" }
            .ifEmpty { SampleData.destinations }
            .sortedByDescending { personalizedDestinationScore(it, preferences) }

        val events = getLiveEvents(if (query.isBlank()) "travel" else query)
        val seed = refreshToken + userKey.hashCode().toLong().absoluteValue
        val rotation = if (merged.isEmpty()) 0 else (seed % merged.size).toInt()
        val rotated = merged.drop(rotation) + merged.take(rotation)

        return rotated.take(10).mapIndexed { index, destination ->
            val nightlyBudget = 2200 + (100 - destination.ecoScore) * 35
            val eventReason = events.getOrNull(index)?.name
            val profileReason = buildProfileReason(destination, preferences)
            val rankReason = buildRankReason(destination, preferences, index)
            val liveSignal = buildLiveSignal(destination, eventReason)
            TravelSuggestion(
                destinationName = destination.name,
                title = "${destination.name} Circuit",
                summary = buildString {
                    append(destination.name)
                    append(" matches your ")
                    append(profileReason)
                    append(" with eco score ")
                    append(destination.ecoScore)
                    append(" and rating ")
                    append(String.format("%.1f", destination.rating))
                    append(".")
                },
                reason = eventReason?.let { "Fresh signal: nearby event $it." }
                    ?: personalizedReasonLine(destination, preferences),
                region = destination.subtitle,
                budgetLabel = "Approx Rs ${nightlyBudget}/night",
                ecoScore = destination.ecoScore,
                safetyFlag = destination.flag,
                rating = destination.rating,
                imageUrl = destination.imageUrl,
                imageAttribution = destination.imageAttribution,
                rankReason = rankReason,
                liveSignal = liveSignal,
                routeHint = buildRouteHint(destination, rotated, index),
                vibeTags = buildTags(destination, preferences)
            )
        }
    }

    private fun buildRouteHint(
        destination: DestinationSummary,
        pool: List<DestinationSummary>,
        index: Int
    ): String {
        val next = pool.getOrNull(index + 1)?.name
        return if (next == null) {
            "Works best as a 2-3 night anchor stop."
        } else {
            "Pair ${destination.name} with $next for a smoother multi-stop route."
        }
    }

    private fun buildTags(
        destination: DestinationSummary,
        preferences: UserPreferencesState? = null
    ): List<String> {
        val tags = mutableListOf<String>()
        if (destination.ecoScore >= 80) tags += "Eco strong"
        if (destination.rating >= 4.7) tags += "Top rated"
        if (destination.flag == "GREEN") tags += "Low risk"
        if (destination.subtitle.contains("Kerala", true) || destination.subtitle.contains("Goa", true)) {
            tags += "Coastal"
        }
        if (destination.subtitle.contains("Himachal", true) || destination.subtitle.contains("Uttarakhand", true) || destination.name == "Gangtok") {
            tags += "Mountain"
        }
        if (destination.name in listOf("Varanasi", "Madurai", "Amritsar")) tags += "Heritage"
        preferences?.let {
            if (it.country.isNotBlank() && !it.country.equals("India", ignoreCase = true)) {
                tags += "International friendly"
            }
            if (it.budget.equals("Budget", ignoreCase = true) && destination.ecoScore >= 75) {
                tags += "Value pick"
            }
            if (it.budget.equals("Luxury", ignoreCase = true) && destination.rating >= 4.7) {
                tags += "Premium feel"
            }
        }
        return tags.ifEmpty { listOf("Flexible") }
    }

    private fun personalizedDestinationScore(
        destination: DestinationSummary,
        preferences: UserPreferencesState?
    ): Int {
        var score = destination.ecoScore + (destination.rating * 10).roundToInt()
        if (preferences == null) return score

        val styles = preferences.tripTypes.lowercase()
        val destinationText = "${destination.name} ${destination.subtitle}".lowercase()

        if (preferences.ecoPriority) score += destination.ecoScore / 2

        if ("adventure" in styles && hasAny(destinationText, "manali", "rishikesh", "leh", "gangtok", "darjeeling", "kaziranga")) {
            score += 24
        }
        if ("heritage" in styles && hasAny(destinationText, "jaipur", "udaipur", "varanasi", "agra", "amritsar", "madurai", "mysuru")) {
            score += 24
        }
        if ("nature" in styles && hasAny(destinationText, "munnar", "alleppey", "gangtok", "srinagar", "kaziranga", "meghalaya", "coastal")) {
            score += 22
        }
        if ("beach" in styles && hasAny(destinationText, "goa", "pondicherry", "kochi", "alleppey", "puri", "coastal")) {
            score += 24
        }
        if ("food" in styles && hasAny(destinationText, "amritsar", "hyderabad", "kolkata", "jaipur", "kochi", "lucknow")) {
            score += 18
        }
        if ("family" in styles && destination.flag == "GREEN" && destination.rating >= 4.6) {
            score += 18
        }

        when {
            preferences.budget.equals("Budget", ignoreCase = true) -> score += (95 - destination.ecoScore)
            preferences.budget.equals("Luxury", ignoreCase = true) -> score += (destination.rating * 8).roundToInt()
        }

        if (preferences.preferredTransport.contains("Train", ignoreCase = true) &&
            hasAny(destinationText, "jaipur", "udaipur", "agra", "varanasi", "amritsar", "kolkata", "kochi", "hyderabad")
        ) {
            score += 14
        }
        if (preferences.preferredTransport.contains("Flight", ignoreCase = true) &&
            hasAny(destinationText, "leh", "gangtok", "srinagar", "goa", "kochi")
        ) {
            score += 14
        }

        if (!preferences.country.equals("India", ignoreCase = true) &&
            hasAny(destinationText, "jaipur", "udaipur", "goa", "varanasi", "agra", "kerala")
        ) {
            score += 12
        }

        return score
    }

    private fun buildProfileReason(
        destination: DestinationSummary,
        preferences: UserPreferencesState?
    ): String {
        if (preferences == null) return "travel profile"
        val styles = preferences.tripTypes.lowercase()
        return when {
            "beach" in styles && hasAny("${destination.name} ${destination.subtitle}".lowercase(), "goa", "pondicherry", "alleppey", "kochi", "puri") -> "beach-first style"
            "heritage" in styles && hasAny("${destination.name} ${destination.subtitle}".lowercase(), "jaipur", "udaipur", "varanasi", "agra", "amritsar", "mysuru") -> "heritage interest"
            "nature" in styles && hasAny("${destination.name} ${destination.subtitle}".lowercase(), "munnar", "gangtok", "srinagar", "kaziranga", "meghalaya") -> "nature preference"
            "adventure" in styles && hasAny("${destination.name} ${destination.subtitle}".lowercase(), "manali", "rishikesh", "leh", "darjeeling", "gangtok") -> "adventure profile"
            preferences.ecoPriority && destination.ecoScore >= 80 -> "eco-aware profile"
            else -> "${preferences.budget.lowercase()} travel style"
        }
    }

    private fun personalizedReasonLine(
        destination: DestinationSummary,
        preferences: UserPreferencesState?
    ): String {
        if (preferences == null) return "Refreshed mix based on weather, eco score, and your latest search."
        return buildString {
            append("Tailored for ")
            append(preferences.country.ifBlank { "your region" })
            append(" travelers who prefer ")
            append(preferences.tripTypes.split(",").firstOrNull()?.trim()?.lowercase().orEmpty().ifBlank { "flexible trips" })
            append(" and ")
            append(if (preferences.ecoPriority) "strong eco options near ${destination.name}." else "balanced practicality near ${destination.name}.")
        }
    }

    private fun hasAny(text: String, vararg terms: String): Boolean {
        return terms.any(text::contains)
    }

    private fun scoreEco(temp: Double, wind: Double, weatherCode: Int): Int {
        val comfortScore = climateComfortScore(temp)
        val windScore = windSafetyScore(wind)
        val weatherScore = weatherStabilityScore(weatherCode)
        return ((comfortScore * 0.45) + (weatherScore * 0.35) + (windScore * 0.20))
            .roundToInt()
            .coerceIn(42, 94)
    }

    private fun weatherIcon(code: Int): String {
        return when (code) {
            0 -> "SUN"
            1, 2, 3 -> "CLOUD"
            45, 48 -> "FOG"
            51, 53, 55, 61, 63, 65 -> "RAIN"
            71, 73, 75, 77, 85, 86 -> "SNOW"
            95, 96, 99 -> "STORM"
            else -> "SKY"
        }
    }

    private fun applyCatalogFilters(
        catalog: List<DestinationSummary>,
        query: String,
        preferences: UserPreferencesState?
    ): List<DestinationSummary> {
        val trimmed = query.trim()
        return catalog
            .filter {
                trimmed.isBlank() ||
                    it.name.contains(trimmed, ignoreCase = true) ||
                    it.subtitle.contains(trimmed, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<DestinationSummary> { personalizedDestinationScore(it, preferences) }
                    .thenByDescending { it.ecoScore }
                    .thenBy { it.name }
            )
    }

    private fun ethicalScoreFor(eco: Int): String {
        return when {
            eco >= 75 -> "HIGH"
            eco >= 62 -> "MODERATE"
            else -> "LOW"
        }
    }

    private fun buildLiveSignalMetrics(
        city: String,
        region: String,
        temperature: Double,
        windSpeed: Double,
        weatherCode: Int
    ): LiveSignalMetrics {
        val comfortScore = climateComfortScore(temperature)
        val weatherScore = weatherStabilityScore(weatherCode)
        val windScore = windSafetyScore(windSpeed)
        val crimeSafetyScore = crimeBaselineRepository?.crimeSafetyScoreFor(city, region) ?: 68
        val safetyScore = (
            (windScore * 0.40) +
                (weatherScore * 0.22) +
                (crimeSafetyScore * 0.38)
            ).roundToInt().coerceIn(18, 96)
        val ecoScore = scoreEco(temperature, windSpeed, weatherCode)
        val carbonKg = (18 + ((100 - ecoScore) * 0.42) + (windSpeed * 0.35)).roundToInt().coerceIn(18, 56)
        val ratingSignal = (
            (safetyScore * 0.36) +
                (ecoScore * 0.24) +
                (comfortScore * 0.16) +
                (weatherScore * 0.10) +
                (crimeSafetyScore * 0.14)
            )
        val rating = (2.2 + (ratingSignal / 38.0)).coerceIn(3.0, 4.9)
        val flag = when {
            safetyScore < 45 -> "RED"
            safetyScore < 66 -> "YELLOW"
            else -> "GREEN"
        }
        return LiveSignalMetrics(
            flag = flag,
            ecoScore = ecoScore,
            carbonKg = carbonKg,
            rating = (rating * 10).roundToInt() / 10.0
        )
    }

    private fun climateComfortScore(temp: Double): Int {
        return (100 - abs(temp - 24.0) * 4.2).roundToInt().coerceIn(20, 100)
    }

    private fun windSafetyScore(wind: Double): Int {
        return (100 - (wind * 2.9)).roundToInt().coerceIn(15, 100)
    }

    private fun weatherStabilityScore(code: Int): Int {
        return when (code) {
            0 -> 96
            1, 2 -> 88
            3 -> 78
            45, 48 -> 60
            51, 53, 55 -> 66
            56, 57, 61, 63, 65 -> 52
            66, 67, 80, 81, 82 -> 44
            71, 73, 75, 77, 85, 86 -> 40
            95, 96, 99 -> 22
            else -> 62
        }
    }

    private fun buildRankReason(
        destination: DestinationSummary,
        preferences: UserPreferencesState?,
        index: Int
    ): String {
        val reasons = mutableListOf<String>()
        if (destination.flag == "GREEN") reasons += "current safety flag is green"
        if (destination.ecoScore >= 80) reasons += "eco score is strong"
        if (destination.rating >= 4.7) reasons += "traveler rating is high"
        crimeSignalLabel(destination.name, destination.subtitle)?.let { reasons += it }
        preferences?.let {
            val styles = it.tripTypes.lowercase()
            val text = "${destination.name} ${destination.subtitle}".lowercase()
            if ("adventure" in styles && hasAny(text, "manali", "rishikesh", "leh", "gangtok", "darjeeling")) {
                reasons += "matches your adventure style"
            }
            if ("heritage" in styles && hasAny(text, "jaipur", "udaipur", "varanasi", "agra", "amritsar", "mysuru")) {
                reasons += "fits your heritage preference"
            }
            if ("nature" in styles && hasAny(text, "munnar", "srinagar", "gangtok", "kaziranga", "meghalaya")) {
                reasons += "aligns with your nature preference"
            }
            if ("beach" in styles && hasAny(text, "goa", "pondicherry", "alleppey", "kochi", "puri")) {
                reasons += "fits your beach preference"
            }
            if (it.ecoPriority && destination.ecoScore >= 75) {
                reasons += "supports your eco-first preference"
            }
        }
        return when {
            reasons.isEmpty() -> "Ranks #${index + 1} because its live safety, practicality, and appeal stay balanced."
            else -> "Ranks #${index + 1} because ${reasons.take(3).joinToString(", ")}."
        }
    }

    private fun buildLiveSignal(
        destination: DestinationSummary,
        eventReason: String?
    ): String {
        val safetyText = when (destination.flag) {
            "RED" -> "risk is elevated right now"
            "YELLOW" -> "conditions need some caution"
            else -> "conditions are currently steady"
        }
        val crimeNote = crimeSignalSource(destination.name, destination.subtitle)
            ?.let { " Backed by $it." }
            .orEmpty()
        return eventReason ?: "${destination.name}: $safetyText, eco ${destination.ecoScore}, rating ${String.format("%.1f", destination.rating)}.$crimeNote"
    }

    fun explainLiveScore(destinationName: String, region: String): String {
        val parts = mutableListOf(
            "Live score blends weather stability, wind safety, climate comfort, eco impact, and carbon efficiency."
        )
        crimeSignalSource(destinationName, region)?.let {
            parts += "Crime baseline uses $it."
        }
        return parts.joinToString(" ")
    }

    private suspend fun fetchPlaceImage(name: String, subtitle: String): CachedPlaceImage? {
        val cacheKey = "$name|$subtitle".lowercase()
        val now = System.currentTimeMillis()
        imageCache[cacheKey]?.takeIf { now - it.cachedAtEpochMs < imageCacheTtlMs }?.let { return it }

        val wikipediaFirst = fetchWikipediaFallback(name, subtitle, now)
        if (wikipediaFirst != null) {
            imageCache[cacheKey] = wikipediaFirst
            return wikipediaFirst
        }

        val placeTerms = searchTermsFor(name, subtitle)
        val candidates = placeTerms.flatMap { term ->
            listOf(
                "\"$term\" \"$subtitle\" India tourism",
                "\"$term\" \"$subtitle\" India landmark",
                "\"$term\" India tourism",
                "\"$term\" India landmark"
            )
        }.distinct()

        candidates.forEach { candidate ->
            val searchHits = runCatching {
                ApiClient.wikimediaCommonsApi.searchFiles(search = candidate).query?.search.orEmpty()
            }.getOrDefault(emptyList())

            searchHits.forEach hitLoop@ { hit ->
                if (!isRelevantImageTitle(hit.title, name, subtitle)) return@hitLoop
                val imageAsset = runCatching {
                    val pages = ApiClient.wikimediaCommonsApi.imageInfo(titles = hit.title).query?.pages.orEmpty().values
                    val info = pages.firstNotNullOfOrNull { page -> page.imageInfo.firstOrNull() } ?: return@runCatching null
                    val url = buildWikimediaRedirectUrlFromTitle(hit.title)
                        ?: (info.thumburl ?: info.url)
                            ?.replace("http://", "https://")
                            ?.let(::buildWikimediaRedirectUrlFromRawUrl)
                        ?: return@runCatching null
                    val creator = sanitizeMetadata(info.extMetadata?.artist?.value ?: info.user)
                        .ifBlank { "Wikimedia contributor" }
                    val license = sanitizeMetadata(info.extMetadata?.licenseShortName?.value)
                        .ifBlank { "license noted on Commons" }
                    CachedPlaceImage(
                        url = url,
                        attribution = "Photo by $creator via Wikimedia Commons ($license)",
                        cachedAtEpochMs = now
                    )
                }.getOrNull()

                if (imageAsset != null) {
                    imageCache[cacheKey] = imageAsset
                    return imageAsset
                }
            }
        }

        return null
    }

    private suspend fun fetchWikipediaFallback(
        name: String,
        subtitle: String,
        cachedAtEpochMs: Long
    ): CachedPlaceImage? {
        val candidates = searchTermsFor(name, subtitle).flatMap { term ->
            listOf(
                "$term, $subtitle, India",
                "$term, India",
                "$term, $subtitle",
                term
            )
        }.distinct()
        candidates.forEach { candidate ->
            val encoded = URLEncoder.encode(candidate, StandardCharsets.UTF_8.toString())
                .replace("+", "%20")
            val summary = runCatching {
                ApiClient.wikipediaApi.pageSummary(encoded)
            }.getOrNull() ?: return@forEach
            if (!isRelevantPageTitle(summary.title, name, subtitle)) return@forEach
            val imageUrl = (summary.originalimage?.source ?: summary.thumbnail?.source)
                ?.replace("http://", "https://")
                ?.let(::buildWikimediaRedirectUrlFromRawUrl)
                ?: return@forEach
            return CachedPlaceImage(
                url = imageUrl,
                attribution = "Image via Wikipedia",
                cachedAtEpochMs = cachedAtEpochMs
            )
        }
        return null
    }

    private fun sanitizeMetadata(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun crimeSignalSource(city: String, region: String): String? {
        val crimeRepo = crimeBaselineRepository ?: return null
        return crimeRepo.takeIf { it.hasOfficialBaseline(city, region) }?.crimeSourceLabelFor(city, region)
    }

    private fun crimeSignalLabel(city: String, region: String): String? {
        return when (crimeSignalSource(city, region)) {
            "NCRB 2023 metro-city violent-crime baseline" -> "official NCRB metro baseline is included"
            "NCRB 2023 state/UT violent-crime baseline" -> "official NCRB state baseline is included"
            else -> null
        }
    }

    private fun searchTermsFor(name: String, subtitle: String): List<String> {
        val normalizedName = name.trim()
        val normalizedSubtitle = subtitle.trim()
        val aliases = when (normalizedName.lowercase()) {
            "pondicherry" -> listOf("Puducherry", "Pondicherry")
            "kochi" -> listOf("Kochi", "Cochin")
            "mysuru" -> listOf("Mysuru", "Mysore")
            "varanasi" -> listOf("Varanasi", "Banaras")
            "alleppey" -> listOf("Alleppey", "Alappuzha")
            "kolkata" -> listOf("Kolkata", "Calcutta")
            else -> listOf(normalizedName)
        }
        return aliases.flatMap { alias ->
            listOf(
                alias,
                "$alias, $normalizedSubtitle"
            )
        }.distinct()
    }

    private fun isRelevantPageTitle(title: String?, name: String, subtitle: String): Boolean {
        if (title.isNullOrBlank()) return false
        val normalizedTitle = normalizeForMatch(title)
        if (containsRejectedImageTerm(normalizedTitle)) return false

        val nameMatches = matchTermsFor(name).any { normalizedTitle.contains(it) }
        val subtitleMatches = matchTermsFor(subtitle).any { normalizedTitle.contains(it) }
        val indiaSpecific = normalizedTitle.contains("india")
        val ambiguityRequiresRegion = needsRegionDisambiguation(name)

        return when {
            nameMatches && !ambiguityRequiresRegion -> true
            nameMatches && (subtitleMatches || indiaSpecific) -> true
            subtitleMatches && normalizedTitle.contains("tourism") -> true
            subtitleMatches && normalizedTitle.contains("temple") -> true
            else -> false
        }
    }

    private fun isRelevantImageTitle(title: String, name: String, subtitle: String): Boolean {
        val normalizedTitle = normalizeForMatch(title.removePrefix("File:"))
        if (containsRejectedImageTerm(normalizedTitle)) return false

        val nameMatches = matchTermsFor(name).any { normalizedTitle.contains(it) }
        val subtitleMatches = matchTermsFor(subtitle).any { normalizedTitle.contains(it) }
        val indiaSpecific = normalizedTitle.contains("india")
        val tourismSignal = listOf(
            "temple", "fort", "palace", "beach", "lake", "ghat", "river", "hill",
            "mountain", "backwater", "cathedral", "mosque", "church", "harbor",
            "harbour", "skyline", "tourism", "landmark"
        ).any(normalizedTitle::contains)

        return when {
            nameMatches && !needsRegionDisambiguation(name) && tourismSignal -> true
            nameMatches && (subtitleMatches || indiaSpecific) && tourismSignal -> true
            nameMatches && subtitleMatches -> true
            subtitleMatches && tourismSignal && indiaSpecific -> true
            else -> false
        }
    }

    private fun containsRejectedImageTerm(text: String): Boolean {
        return listOf(
            "house", "home", "residence", "portrait", "selfie", "person", "people",
            "family", "apartment", "flat", "school", "college", "hospital", "office",
            "interior", "bedroom", "kitchen", "drawing room", "living room"
        ).any(text::contains)
    }

    private fun needsRegionDisambiguation(name: String): Boolean {
        return name.lowercase() in setOf("kochi", "pondicherry", "mysuru", "alleppey", "kolkata")
    }

    private fun matchTermsFor(text: String): List<String> {
        val normalized = normalizeForMatch(text)
        val aliases = when (normalized) {
            "pondicherry" -> listOf("pondicherry", "puducherry")
            "kochi" -> listOf("kochi", "cochin")
            "mysuru" -> listOf("mysuru", "mysore")
            "varanasi" -> listOf("varanasi", "banaras")
            "alleppey" -> listOf("alleppey", "alappuzha")
            "kolkata" -> listOf("kolkata", "calcutta")
            else -> listOf(normalized)
        }
        return aliases.filter { it.isNotBlank() }
    }

    private fun normalizeForMatch(text: String): String {
        return text.lowercase()
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun buildWikimediaRedirectUrlFromTitle(title: String?): String? {
        if (title.isNullOrBlank()) return null
        val fileName = title.removePrefix("File:").trim()
        if (fileName.isBlank()) return null
        val encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
        return "https://commons.wikimedia.org/wiki/Special:Redirect/file/$encoded?width=900"
    }

    private fun buildWikimediaRedirectUrlFromRawUrl(url: String): String {
        if (!url.contains("upload.wikimedia.org", ignoreCase = true)) return url
        val rawFileName = url.substringAfterLast("/")
        if (rawFileName.isBlank()) return url
        val decoded = runCatching {
            URLDecoder.decode(rawFileName, StandardCharsets.UTF_8.toString())
        }.getOrDefault(rawFileName)
        val encoded = URLEncoder.encode(decoded, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
        return "https://commons.wikimedia.org/wiki/Special:Redirect/file/$encoded?width=900"
    }

}

private data class CachedPlaceImage(
    val url: String,
    val attribution: String,
    val cachedAtEpochMs: Long
)

private data class LiveSignalMetrics(
    val flag: String,
    val ecoScore: Int,
    val carbonKg: Int,
    val rating: Double
)
