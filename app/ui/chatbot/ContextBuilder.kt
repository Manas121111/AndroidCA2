package com.smarttour360.app.ui.chatbot

import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.ui.state.TripItemType

object ContextBuilder {
    data class ChatContext(
        val destinationName: String? = null,
        val destinationRegion: String? = null,
        val safetyFlag: String? = null,
        val flagExplanation: String? = null,
        val ecoScore: Int? = null,
        val destinationEthicalScore: String? = null,
        val bookingMode: String? = null,
        val destinationRating: Double? = null,
        val destinationCarbonKg: Int? = null,
        val selectedHotelName: String? = null,
        val tripStops: List<String> = emptyList(),
        val tripSummary: String = "",
        val userProfileSummary: String = "",
        val userName: String = "",
        val userBudget: String = "",
        val userTripStyles: String = "",
        val userTransportPreference: String = "",
        val cartItemCount: Int = 0,
        val bookingCount: Int = 0,
        val recentBookingIds: List<String> = emptyList(),
        val learnedUserSummary: String = "",
        val currentLocationSummary: String = "",
        val liveApiSnapshot: String = "",
        val liveSafetyPlaces: List<String> = emptyList(),
        val liveRecommendationNotes: List<String> = emptyList()
    )

    fun fromCurrentState(
        destinationName: String? = null,
        safetyFlag: String? = null,
        flagExplanation: String? = null,
        ecoScore: Int? = null,
        ethicalScore: String? = null,
        bookingMode: String? = null
    ): ChatContext {
        val selectedDestination = AppStateStore.selectedDestination.value
        val selectedHotel = AppStateStore.selectedHotel.value
        val appContext = AppStateStore.getAppContext()
        val userKey = AppStateStore.activeUserKey.value ?: "guest"
        val learnedProfileSummary = appContext
            ?.let { LocalUserPreferenceStore(it).load(userKey).summary() }
            .orEmpty()
        val tripStops = AppStateStore.tripEntries.value
            .filter { it.type == TripItemType.DESTINATION }
            .map { it.title }

        return ChatContext(
            userName = AppStateStore.userPreferences.value.name,
            userBudget = AppStateStore.userPreferences.value.budget,
            userTripStyles = AppStateStore.userPreferences.value.tripTypes,
            userTransportPreference = AppStateStore.userPreferences.value.preferredTransport,
            destinationName = destinationName ?: selectedDestination?.name,
            destinationRegion = selectedDestination?.subtitle,
            safetyFlag = safetyFlag ?: selectedDestination?.flag,
            flagExplanation = flagExplanation ?: selectedDestination?.subtitle,
            ecoScore = ecoScore ?: selectedDestination?.ecoScore,
            destinationEthicalScore = ethicalScore ?: selectedDestination?.ethicalScore,
            bookingMode = bookingMode,
            destinationRating = selectedDestination?.rating,
            destinationCarbonKg = selectedDestination?.carbonKg,
            selectedHotelName = selectedHotel?.name,
            tripStops = tripStops,
            tripSummary = AppStateStore.buildTripSummary().let {
                "${it.routeLine}; ${it.durationLine}; total ${it.totalLine}"
            },
            userProfileSummary = AppStateStore.userPreferences.value.let {
                "${it.name} from ${it.country}, budget ${it.budget}, trip styles ${it.tripTypes}, transport ${it.preferredTransport}, eco priority ${if (it.ecoPriority) "on" else "off"}"
            },
            cartItemCount = AppStateStore.cartEntries.value.size,
            bookingCount = AppStateStore.bookings.value.size,
            recentBookingIds = AppStateStore.bookings.value.take(3).map { it.id },
            learnedUserSummary = learnedProfileSummary,
            currentLocationSummary = AppStateStore.currentLocation.value?.let {
                "${it.cityName} - ${it.safetyFlag}" +
                    (it.matchedDestinationName?.let { matched -> " via $matched" } ?: "")
            }.orEmpty(),
            liveApiSnapshot = AppStateStore.buildLiveApiSnapshot(),
            liveSafetyPlaces = AppStateStore.liveDestinations.value
                .sortedWith(
                    compareByDescending<com.smarttour360.app.ui.state.LiveDestinationSnapshot> { it.safetyFlag == "GREEN" }
                        .thenByDescending { it.ecoScore }
                        .thenByDescending { it.rating }
                )
                .take(5)
                .map {
                    "${it.name} (${it.region}) - ${it.safetyFlag}, eco ${it.ecoScore}, rating ${"%.1f".format(it.rating)}"
                },
            liveRecommendationNotes = AppStateStore.assistantRecommendations.value
                .take(5)
                .mapIndexed { index, item ->
                    "${index + 1}. ${item.destinationName} - ${item.rankReason} ${item.liveSignal}"
                }
        )
    }

    fun buildSystemPrompt(context: ChatContext, itineraryMode: Boolean): String {
        val base = buildString {
            append("You are SmartTour360 Assistant, an AI helper exclusively for the SmartTour360 Android app. ")
            append("You help users with Indian travel destinations, recommendations, safety, eco scores, train booking guidance, flight and bus search within the app, trip planning, hotel selection, cart, bookings, and profile-aware suggestions. ")
            append("Keep answers short, practical, and easy to read on mobile. ")
            append("Do not use markdown, bold markers, bullet symbols, emojis, or generic AI disclaimers. ")
            append("Use plain clean text with short paragraphs or simple numbered lines only when needed.\n")
            append("STRICT RULES:\n")
            append("1. Never answer unrelated questions outside SmartTour360 or Indian travel.\n")
            append("2. If the user asks something out of scope, say: I'm SmartTour360's travel assistant. I can only help with trips, destinations, transport, stays, bookings, and travel planning inside this app.\n")
            append("3. Always use the live app context below.\n")
            append("4. Never invent train numbers, hotel prices, booking IDs, or confirmations.\n")
            append("5. If relevant, suggest a concrete next step inside the app such as opening Recommendations, adding a stop to Trips, checking hotels, or reviewing bookings.\n")
            append("6. Prefer Indian destinations and SmartTour360 data over generic travel advice.\n")
            append("7. Do not answer questions about your own development, model, training, internal architecture, or how you were built.\n")
            append("--- LIVE APP CONTEXT ---\n")
            append("User: ${context.userName.ifBlank { "Guest Explorer" }}\n")
            append("Budget: ${context.userBudget.ifBlank { "unknown" }}\n")
            append("Travel styles: ${context.userTripStyles.ifBlank { "unknown" }}\n")
            append("Preferred transport: ${context.userTransportPreference.ifBlank { "unknown" }}\n")
            append("Current destination: ${context.destinationName ?: "none"}\n")
            append("Booking mode: ${context.bookingMode ?: "general"}\n")
            append("Destination region: ${context.destinationRegion ?: "none"}\n")
            append("Safety flag: ${context.safetyFlag ?: "unknown"}\n")
            append("Flag context: ${context.flagExplanation ?: "not provided"}\n")
            append("Eco score: ${context.ecoScore ?: 0}\n")
            append("Ethical score: ${context.destinationEthicalScore ?: "unknown"}\n")
            append("Destination rating: ${context.destinationRating ?: 0.0}\n")
            append("Destination carbon estimate: ${context.destinationCarbonKg ?: 0}kg\n")
            append("Selected hotel: ${context.selectedHotelName ?: "none"}\n")
            append("Trip stops: ${context.tripStops.joinToString(" -> ").ifBlank { "none" }}\n")
            append("Trip summary: ${context.tripSummary.ifBlank { "none" }}\n")
            append("Cart items: ${context.cartItemCount}\n")
            append("Booking count: ${context.bookingCount}\n")
            append("Recent booking IDs: ${context.recentBookingIds.joinToString(", ").ifBlank { "none" }}\n")
            append("User profile: ${context.userProfileSummary.ifBlank { "none" }}\n")
            append("Learned user memory profile: ${context.learnedUserSummary.ifBlank { "none" }}\n")
            append("Current location safety: ${context.currentLocationSummary.ifBlank { "none" }}\n")
            append("Live API snapshot: ${context.liveApiSnapshot.ifBlank { "none" }}\n")
            append("Live safety places: ${context.liveSafetyPlaces.joinToString(" | ").ifBlank { "none" }}\n")
            append("Latest ranked recommendations: ${context.liveRecommendationNotes.joinToString(" | ").ifBlank { "none" }}\n")
        }
        return if (itineraryMode) {
            base + "Generate a day-by-day itinerary with one short line per day. Use only places that fit the live safety and trip context above."
        } else {
            base + "Answer directly. Ground every answer in the live app context above. If booking mode is set, prioritize that transport mode in your answer. If you mention recommended places, use the ranked recommendations or live safety places. Suggest practical next actions when useful."
        }
    }
}
