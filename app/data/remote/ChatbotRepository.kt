package com.smarttour360.app.data.remote

import com.smarttour360.app.BuildConfig
import com.smarttour360.app.data.TravelRepository
import com.smarttour360.app.dto.ChatMessage
import com.smarttour360.app.dto.ChatRole
import com.smarttour360.app.dto.GroqChatRequest
import com.smarttour360.app.dto.GroqMessage
import com.smarttour360.app.ui.chatbot.ChatMemoryManager
import com.smarttour360.app.ui.chatbot.ContextBuilder
import com.smarttour360.app.ui.chatbot.AssistantExampleRetriever
import com.smarttour360.app.ui.chatbot.EpisodicLogger
import com.smarttour360.app.ui.chatbot.KnowledgeRetriever
import com.smarttour360.app.ui.chatbot.LocalTrainingExportStore
import com.smarttour360.app.ui.chatbot.LocalUserMemoryStore
import com.smarttour360.app.ui.chatbot.LocalUserPreferenceStore
import com.smarttour360.app.ui.chatbot.brain.AssistantMemory
import com.smarttour360.app.ui.chatbot.brain.ContextInjector
import com.smarttour360.app.ui.chatbot.brain.IntentClassifier
import com.smarttour360.app.ui.chatbot.brain.ParsedResponse
import com.smarttour360.app.ui.chatbot.brain.PromptEngineer
import com.smarttour360.app.ui.chatbot.brain.ResponseParser
import com.smarttour360.app.ui.chatbot.rag.Retriever
import com.smarttour360.app.ui.state.AppStateStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class ChatbotRepository {
    private data class RetrievalBudget(
        val knowledgeTopK: Int,
        val exampleTopK: Int,
        val memoryTopK: Int,
        val fetchFreshApi: Boolean,
        val maxTokens: Int
    )

    private data class RetrievalResults(
        val freshApiContext: String?,
        val knowledgeChunks: List<com.smarttour360.app.ui.chatbot.KnowledgeChunk>,
        val trainingExamples: List<com.smarttour360.app.ui.chatbot.AssistantExample>,
        val personalMemories: List<com.smarttour360.app.ui.chatbot.UserMemoryEntry>
    )

    private val memoryManager = ChatMemoryManager()
    private val travelRepository = TravelRepository()
    private var cachedKnowledgeRetriever: KnowledgeRetriever? = null
    private var cachedExampleRetriever: AssistantExampleRetriever? = null
    private var cachedRoomRetriever: Retriever? = null
    private val refusalMessage =
        "I'm SmartTour360's travel assistant. I can only help with trips, destinations, transport, stays, bookings, and travel planning inside this app."

    suspend fun sendMessage(
        userMessage: String,
        history: List<ChatMessage>,
        screenContext: ContextBuilder.ChatContext,
        itineraryMode: Boolean = false
    ): ParsedResponse {
        val intent = IntentClassifier.classify(userMessage)
        if (shouldRefuse(userMessage, screenContext)) {
            return ParsedResponse(refusalMessage, emptyList(), emptyList(), emptyList())
        }
        deterministicFastReply(userMessage, screenContext)?.let { return it }

        val appContext = AppStateStore.getAppContext()
        val userKey = AppStateStore.activeUserKey.value ?: "guest"
        val memory = AssistantMemory.from(history, screenContext)
        val memoryStore = appContext?.let(::LocalUserMemoryStore)
        val preferenceStore = appContext?.let(::LocalUserPreferenceStore)
        val exportStore = appContext?.let(::LocalTrainingExportStore)
        val budget = retrievalBudget(intent, userMessage, itineraryMode)
        if (appContext != null) {
            if (cachedKnowledgeRetriever == null) {
                cachedKnowledgeRetriever = KnowledgeRetriever(appContext)
            }
            if (cachedExampleRetriever == null) {
                cachedExampleRetriever = AssistantExampleRetriever(appContext)
            }
            if (cachedRoomRetriever == null) {
                cachedRoomRetriever = Retriever(appContext)
            }
        }
        val retrievals = coroutineScope {
            val freshApiDeferred = async {
                if (!budget.fetchFreshApi) {
                    null
                } else {
                    withTimeoutOrNull(900) {
                        fetchFreshApiContext(userMessage, screenContext, userKey)
                    }
                }
            }
            val knowledgeDeferred = async {
                val roomKnowledge = cachedRoomRetriever?.retrieve(userMessage, topK = budget.knowledgeTopK)
                    .orEmpty()
                    .map {
                        com.smarttour360.app.ui.chatbot.KnowledgeChunk(
                            id = it.id,
                            title = it.title,
                            text = it.content,
                            tags = it.tags
                        )
                    }
                val assetKnowledge = cachedKnowledgeRetriever?.retrieve(userMessage, topK = budget.knowledgeTopK).orEmpty()
                (roomKnowledge + assetKnowledge)
                    .distinctBy { it.id.ifBlank { "${it.title}-${it.text.take(32)}" } }
                    .take(budget.knowledgeTopK + 1)
            }
            val examplesDeferred = async {
                cachedExampleRetriever?.retrieve(userMessage, topK = budget.exampleTopK).orEmpty()
            }
            val personalMemoryDeferred = async {
                memoryStore?.retrieve(userKey, userMessage, topK = budget.memoryTopK).orEmpty()
            }
            RetrievalResults(
                freshApiContext = freshApiDeferred.await(),
                knowledgeChunks = knowledgeDeferred.await(),
                trainingExamples = examplesDeferred.await(),
                personalMemories = personalMemoryDeferred.await()
            )
        }
        val freshApiContext = retrievals.freshApiContext
        val knowledgeChunks = retrievals.knowledgeChunks
        val trainingExamples = retrievals.trainingExamples
        val personalMemories = retrievals.personalMemories
        val personalMemoryPrompt = personalMemories.takeIf { it.isNotEmpty() }?.joinToString("\n\n") {
            it.summary
        }
        val trainingExamplePrompt = trainingExamples.takeIf { it.isNotEmpty() }?.joinToString("\n\n") {
            "Example user: ${it.user}\nExample assistant: ${it.assistant}"
        }
        val (historySummary, recentHistory) = memoryManager.summarizedHistory(history)
        val recentGroqHistory = recentHistory.map { item ->
            GroqMessage(
                role = if (item.role == ChatRole.USER) "user" else "assistant",
                content = item.content
            )
        }
        val intentContext = ContextInjector.buildContextForIntent(
            intent = intent,
            memory = memory,
            ragChunks = knowledgeChunks,
            freshApiContext = freshApiContext,
            personalMemoryPrompt = personalMemoryPrompt
        )

        if (BuildConfig.GROQ_API_KEY.isBlank()) {
            val rawReply = fallbackReply(
                userMessage,
                screenContext,
                itineraryMode,
                knowledgeChunks.map { it.text },
                freshApiContext
            )
            val parsed = ResponseParser.parse(sanitizeReply(rawReply), intent)
            val finalText = parsed.displayText.ifBlank { rawReply }
            maybeLearn(memoryStore, preferenceStore, exportStore, userKey, userMessage, finalText, screenContext)
            EpisodicLogger.log(appContext, userMessage, intent, screenContext.destinationName)
            return parsed.copy(displayText = finalText)
        }

        val request = GroqChatRequest(
            model = "llama-3.1-8b-instant",
            messages = PromptEngineer.buildFinalPrompt(
                intent = intent,
                memory = memory,
                intentContext = intentContext,
                chatContext = screenContext,
                itineraryMode = itineraryMode,
                trainingExamplePrompt = trainingExamplePrompt,
                historySummary = historySummary,
                recentHistory = recentGroqHistory,
                userMessage = userMessage
            ),
            temperature = 0.25,
            max_tokens = budget.maxTokens
        )

        val rawReply = runCatching {
            ApiClient.groqApi.chat(
                authorization = "Bearer ${BuildConfig.GROQ_API_KEY}",
                request = request
            ).choices.firstOrNull()?.message?.content
        }.getOrNull()?.ifBlank { null } ?: fallbackReply(
            userMessage,
            screenContext,
            itineraryMode,
            knowledgeChunks.map { it.text },
            freshApiContext
        )
        val parsed = ResponseParser.parse(sanitizeReply(rawReply), intent)
        val finalText = parsed.displayText.ifBlank { sanitizeReply(rawReply) }
        maybeLearn(memoryStore, preferenceStore, exportStore, userKey, userMessage, finalText, screenContext)
        EpisodicLogger.log(appContext, userMessage, intent, screenContext.destinationName)
        return parsed.copy(displayText = finalText)
    }

    fun submitFeedback(
        userMessage: String,
        assistantReply: String,
        context: ContextBuilder.ChatContext,
        positive: Boolean
    ) {
        val appContext = AppStateStore.getAppContext() ?: return
        val userKey = AppStateStore.activeUserKey.value ?: "guest"
        val memoryStore = LocalUserMemoryStore(appContext)
        val exportStore = LocalTrainingExportStore(appContext)

        memoryStore.recordFeedback(
            userKey = userKey,
            userMessage = userMessage,
            assistantReply = assistantReply,
            context = context,
            positive = positive
        )

        if (positive) {
            exportStore.approveLatestTurn(userKey)
        } else {
            exportStore.rejectLatestTurn(userKey)
        }
    }

    private fun deterministicFastReply(
        userMessage: String,
        context: ContextBuilder.ChatContext
    ): ParsedResponse? {
        val normalized = userMessage.lowercase().trim()
        val profile = AppStateStore.userPreferences.value

        if (isLocationQuestion(normalized)) {
            return ParsedResponse(
                displayText = locationReply(context, freshApiContext = null),
                actions = emptyList(),
                followUpChips = emptyList(),
                confidenceSignals = listOf("live_location_state")
            )
        }

        if (mentionsProfileName(normalized)) {
            val name = profile.name.trim().ifBlank { "Guest Explorer" }
            return ParsedResponse(
                displayText = "Your profile name is $name.",
                actions = emptyList(),
                followUpChips = emptyList(),
                confidenceSignals = listOf("profile_state")
            )
        }

        if (mentionsBudget(normalized)) {
            val budget = profile.budget.trim().ifBlank { "not set yet" }
            return ParsedResponse(
                displayText = "Your saved budget preference is $budget.",
                actions = emptyList(),
                followUpChips = emptyList(),
                confidenceSignals = listOf("profile_state")
            )
        }

        if (mentionsTransportPreference(normalized)) {
            val transport = profile.preferredTransport.trim().ifBlank { "not set yet" }
            return ParsedResponse(
                displayText = "Your saved transport preference is $transport.",
                actions = emptyList(),
                followUpChips = emptyList(),
                confidenceSignals = listOf("profile_state")
            )
        }

        if (mentionsTripStyles(normalized)) {
            val styles = profile.tripTypes.trim().ifBlank { "not set yet" }
            return ParsedResponse(
                displayText = "Your saved trip styles are $styles.",
                actions = emptyList(),
                followUpChips = emptyList(),
                confidenceSignals = listOf("profile_state")
            )
        }

        if (mentionsDestination(normalized) && !context.destinationName.isNullOrBlank()) {
            return ParsedResponse(
                displayText = buildString {
                    append("Your current selected destination is ${context.destinationName}. ")
                    context.destinationRegion?.let { append("Region: $it. ") }
                    context.safetyFlag?.let { append("Safety flag: $it. ") }
                    context.ecoScore?.let { append("Eco score: $it. ") }
                    context.destinationRating?.let { append("Rating: ${"%.1f".format(it)}.") }
                }.trim(),
                actions = emptyList(),
                followUpChips = emptyList(),
                confidenceSignals = listOf("selected_destination_state")
            )
        }

        if (mentionsSelectedHotel(normalized)) {
            val hotel = context.selectedHotelName
            val text = if (hotel.isNullOrBlank()) {
                "You do not have a selected hotel in the app right now."
            } else {
                "Your current selected hotel is $hotel."
            }
            return ParsedResponse(text, emptyList(), emptyList(), listOf("selected_hotel_state"))
        }

        if (mentionsCart(normalized)) {
            val count = context.cartItemCount
            val text = if (count == 0) {
                "Your cart is empty right now."
            } else {
                "Your cart currently has $count item${if (count == 1) "" else "s"}."
            }
            return ParsedResponse(text, emptyList(), emptyList(), listOf("cart_state"))
        }

        if (mentionsBookings(normalized)) {
            val count = context.bookingCount
            val text = if (count == 0) {
                "You do not have any bookings in the app yet."
            } else {
                buildString {
                    append("You currently have $count booking")
                    append(if (count == 1) "" else "s")
                    append(" in the app.")
                    if (context.recentBookingIds.isNotEmpty()) {
                        append(" Recent booking IDs: ${context.recentBookingIds.joinToString(", ")}.")
                    }
                }
            }
            return ParsedResponse(text, emptyList(), emptyList(), listOf("bookings_state"))
        }

        if (mentionsTripSummary(normalized)) {
            val stops = context.tripStops
            val text = if (stops.isEmpty()) {
                "You do not have any destination stops in Trips yet."
            } else {
                "Your current trip stops are ${stops.joinToString(" -> ")}. Summary: ${context.tripSummary}."
            }
            return ParsedResponse(text, emptyList(), emptyList(), listOf("trip_state"))
        }

        return null
    }

    private fun fallbackReply(
        userMessage: String,
        screenContext: ContextBuilder.ChatContext,
        itineraryMode: Boolean,
        knowledge: List<String>,
        freshApiContext: String?
    ): String {
        if (shouldRefuse(userMessage, screenContext)) {
            return refusalMessage
        }

        if (itineraryMode) {
            return itineraryReply(screenContext)
        }

        val place = screenContext.destinationName ?: screenContext.tripStops.firstOrNull() ?: "your selected destination"
        val message = userMessage.lowercase()
        val groundedKnowledge = knowledge.firstOrNull()
        val freshSignal = freshApiContext?.takeIf { it.isNotBlank() }
        return when {
            isLocationQuestion(message) ->
                locationReply(screenContext, freshSignal)
            message.contains("safe") || message.contains("risk") ->
                safetyReply(place, screenContext, groundedKnowledge, freshSignal)
            message.contains("budget") || message.contains("cost") || message.contains("price") ->
                budgetReply(place, screenContext, groundedKnowledge, freshSignal)
            message.contains("season") || message.contains("weather") || message.contains("best time") ->
                seasonReply(place, screenContext, groundedKnowledge, freshSignal)
            message.contains("recommend") || message.contains("where should") || message.contains("which place") ->
                recommendationReply(screenContext, groundedKnowledge, freshSignal)
            message.contains("trip") || message.contains("route") || message.contains("plan") ->
                itineraryReply(screenContext)
            else ->
                generalReply(place, screenContext, groundedKnowledge, freshSignal)
        }
    }

    private fun locationReply(
        @Suppress("UNUSED_PARAMETER") context: ContextBuilder.ChatContext,
        freshApiContext: String?
    ): String {
        val current = AppStateStore.currentLocation.value
        return if (current == null) {
            buildString {
                append("I do not have a live location snapshot right now. Open Home and allow location once so I can use your current city safety context. ")
                freshApiContext?.let { append(shortFreshLead(it)) }
            }
        } else {
            buildString {
                append("Your current app location is ${current.cityName}. ")
                append("The latest location safety signal is ${current.safetyFlag}. ")
                current.matchedDestinationName?.let {
                    append("It is currently matched against $it in the live catalog. ")
                }
                append("You can also confirm this from the Home location banner. ")
                freshApiContext?.let { append(shortFreshLead(it)) }
            }
        }
    }

    private fun safetyReply(
        place: String,
        context: ContextBuilder.ChatContext,
        knowledge: String?,
        freshApiContext: String?
    ): String {
        val currentFlag = context.safetyFlag ?: "unknown"
        val topLive = context.liveSafetyPlaces.firstOrNull()
        return buildString {
            append("$place currently carries a $currentFlag safety signal")
            context.flagExplanation?.takeIf { it.isNotBlank() }?.let { append(" for the $it area") }
            append(". ")
            knowledge?.let { append(shortKnowledgeLead(it)) }
            freshApiContext?.let { append(shortFreshLead(it)) }
            append("Prefer central stays, daytime arrival, and verified transport. ")
            if (topLive != null) {
                append("Best live low-risk option in the app right now: $topLive.")
            }
        }
    }

    private fun budgetReply(
        place: String,
        context: ContextBuilder.ChatContext,
        knowledge: String?,
        freshApiContext: String?
    ): String {
        val firstRecommendation = context.liveRecommendationNotes.firstOrNull()
        return buildString {
            append("For $place, use the trip summary as the working baseline: ")
            append(context.tripSummary.ifBlank { "no saved route yet" })
            append(". ")
            knowledge?.let { append(shortKnowledgeLead(it)) }
            freshApiContext?.let { append(shortFreshLead(it)) }
            append("Budget control usually improves by keeping one base hotel and minimizing intercity jumps. ")
            if (firstRecommendation != null) {
                append("Current top recommendation signal: $firstRecommendation")
            }
        }
    }

    private fun seasonReply(
        place: String,
        context: ContextBuilder.ChatContext,
        knowledge: String?,
        freshApiContext: String?
    ): String {
        val live = context.liveRecommendationNotes.firstOrNull { it.contains(place, ignoreCase = true) }
        return buildString {
            append("For $place, decide using the live destination refresh rather than a static season rule. ")
            append("Check the destination detail forecast and safety flag before you lock dates. ")
            knowledge?.let { append(shortKnowledgeLead(it)) }
            freshApiContext?.let { append(shortFreshLead(it)) }
            if (live != null) {
                append("Current ranking signal: $live")
            }
        }
    }

    private fun recommendationReply(
        context: ContextBuilder.ChatContext,
        knowledge: String?,
        freshApiContext: String?
    ): String {
        val ranked = context.liveRecommendationNotes.take(3)
        if (ranked.isEmpty()) {
            return buildString {
                append("Open Recommendations and refresh once to build a live ranked shortlist. ")
                freshApiContext?.let { append(shortFreshLead(it)) }
                append("Right now there is not enough grounded recommendation data in memory.")
            }
        }
        return buildString {
            append("Top live picks right now are ")
            append(ranked.joinToString(" "))
            knowledge?.let { append(" ${shortKnowledgeLead(it)}") }
            freshApiContext?.let { append(shortFreshLead(it)) }
            append(" Choose the one that best fits your budget and transport preference.")
        }
    }

    private fun itineraryReply(context: ContextBuilder.ChatContext): String {
        val route = context.tripStops.ifEmpty {
            context.liveSafetyPlaces.take(3).map { it.substringBefore(" (") }
        }.ifEmpty {
            listOf(context.destinationName ?: "your selected stops")
        }
        return buildString {
            append("Day 1: Arrive in ${route.first()} and keep the first evening light.\n")
            route.drop(1).forEachIndexed { index, stop ->
                append("Day ${index + 2}: Transfer to $stop, explore one core area, and keep backup transport ready.\n")
            }
            append("Final Day: Use the safest departure window and avoid adding a new stop before return travel.")
        }
    }

    private fun generalReply(
        place: String,
        context: ContextBuilder.ChatContext,
        knowledge: String?,
        freshApiContext: String?
    ): String {
        val recommendation = context.liveRecommendationNotes.firstOrNull()
        return buildString {
            append("I can help with live safety, ranking, budget, and route planning for $place. ")
            append("Your current profile is ${context.userProfileSummary.ifBlank { "not set" }}. ")
            knowledge?.let { append(shortKnowledgeLead(it)) }
            freshApiContext?.let { append(shortFreshLead(it)) }
            if (recommendation != null) {
                append("Right now the strongest grounded recommendation is $recommendation")
            } else {
                append("Refresh Recommendations to get a stronger live shortlist.")
            }
        }
    }

    private fun shortKnowledgeLead(text: String): String {
        val sentence = text.substringBefore(". ").trim().ifBlank { text.trim() }
        return if (sentence.isBlank()) "" else "$sentence. "
    }

    private fun shortFreshLead(text: String): String {
        val sentence = text.substringBefore(" | ").trim().ifBlank { text.trim() }
        return if (sentence.isBlank()) "" else "$sentence. "
    }

    private fun maybeLearn(
        memoryStore: LocalUserMemoryStore?,
        preferenceStore: LocalUserPreferenceStore?,
        exportStore: LocalTrainingExportStore?,
        userKey: String,
        userMessage: String,
        assistantReply: String,
        context: ContextBuilder.ChatContext
    ) {
        if (memoryStore == null) return
        if (assistantReply == refusalMessage) return
        memoryStore.learn(userKey, userMessage, assistantReply, context)
        preferenceStore?.learn(userKey, userMessage, context)
        exportStore?.appendTurn(userKey, userMessage, assistantReply, context)
    }

    private fun shouldRefuse(
        message: String,
        context: ContextBuilder.ChatContext
    ): Boolean {
        val normalized = message.lowercase()
        if (normalized.isBlank()) return false

        val greetingsOnly = listOf(
            "hi", "hello", "hey", "good morning", "good evening", "help", "what can you do"
        )
        if (greetingsOnly.any { normalized == it }) return false

        val hardBlockTerms = listOf(
            "how were you developed",
            "who made you",
            "how are you built",
            "what model are you",
            "who trained you",
            "your architecture",
            "write code",
            "solve this math",
            "programming",
            "recipe",
            "bitcoin",
            "stock market",
            "politics",
            "physics",
            "chemistry",
            "movie",
            "lyrics",
            "joke",
            "story",
            "essay",
            "homework"
        )

        if (hardBlockTerms.any { normalized.contains(it) }) return true

        val explicitTravelTerms = listOf(
            "trip", "travel", "destination", "destinations", "train", "trains", "flight", "flights",
            "bus", "buses", "hotel", "hotels", "booking", "bookings", "itinerary", "route", "routes",
            "stay", "stays", "planner", "plan", "safety", "safe", "eco", "weather", "season",
            "budget", "cart", "profile", "recommend", "recommendation", "transport", "irctc", "erail"
        )

        val appTerms = listOf(
            "smarttour360", "this app", "my bookings", "my booking", "my trip", "my trips",
            "my cart", "trip planner", "recommendations", "booking portal"
        )

        val contextTerms = buildList {
            context.destinationName?.lowercase()?.let { add(it) }
            context.destinationRegion?.lowercase()?.let { add(it) }
            context.selectedHotelName?.lowercase()?.let { add(it) }
            addAll(context.tripStops.map { it.lowercase() })
            context.currentLocationSummary.lowercase().takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(context.liveSafetyPlaces.map { it.substringBefore(" (").lowercase() })
            addAll(context.liveRecommendationNotes.mapNotNull { note ->
                note.substringAfter(". ", "").substringBefore(" -").trim().lowercase().takeIf { it.isNotBlank() }
            })
        }.distinct()

        if (appTerms.any { normalized.contains(it) }) return false
        if (explicitTravelTerms.any { normalized.contains(it) }) return false
        if (contextTerms.any { term -> term.isNotBlank() && normalized.contains(term) }) return false

        val indiaTravelTerms = listOf(
            "india", "indian", "goa", "jaipur", "manali", "varanasi", "kochi", "udupi", "munnar",
            "rishikesh", "udaipur", "gangtok", "amritsar", "delhi", "mumbai", "kerala", "rajasthan"
        )
        if (indiaTravelTerms.any { normalized.contains(it) }) return false
        if (isLocationQuestion(normalized)) return false

        return true
    }

    private suspend fun fetchFreshApiContext(
        userMessage: String,
        context: ContextBuilder.ChatContext,
        userKey: String
    ): String? {
        val normalized = userMessage.lowercase()
        if (!isApiFetchWorthy(normalized)) return null

        val destinationQuery = resolveDestinationQuery(normalized, context)
        val prefs = AppStateStore.userPreferences.value

        return runCatching {
            val parts = mutableListOf<String>()

            if (!destinationQuery.isNullOrBlank()) {
                val liveMatch = (
                    travelRepository.searchDestinations(destinationQuery) +
                        travelRepository.getLiveIndianDestinationCatalog(
                            forceRefresh = true,
                            query = destinationQuery,
                            preferences = prefs
                        )
                    )
                    .distinctBy { "${it.name}-${it.subtitle}" }
                    .firstOrNull()

                liveMatch?.let {
                    parts += "Fresh destination fetch for ${it.name}: ${it.flag}, eco ${it.ecoScore}, rating ${"%.1f".format(it.rating)}, region ${it.subtitle}"
                    val forecast = travelRepository.getDestinationForecast(it.name)
                    if (forecast.isNotEmpty()) {
                        parts += "Forecast ${forecast.joinToString(", ") { day -> "${day.day} ${day.temperature}" }}"
                    }
                }
            } else {
                val liveCatalog = travelRepository.getLiveIndianDestinationCatalog(
                    forceRefresh = true,
                    preferences = prefs
                )
                if (liveCatalog.isNotEmpty()) {
                    parts += "Fresh live ranking ${liveCatalog.take(3).joinToString(" | ") { "${it.name}: ${it.flag}, eco ${it.ecoScore}, rating ${"%.1f".format(it.rating)}" }}"
                }
                val suggestions = travelRepository.getSuggestions(
                    refreshToken = System.currentTimeMillis(),
                    preferences = prefs,
                    userKey = userKey
                )
                if (suggestions.isNotEmpty()) {
                    parts += "Fresh recommendation mix ${suggestions.take(3).joinToString(" | ") { "${it.destinationName}: ${it.safetyFlag}, eco ${it.ecoScore}, ${it.budgetLabel}" }}"
                }
            }

            parts.joinToString(" | ").ifBlank { null }
        }.getOrNull()
    }

    private fun isApiFetchWorthy(message: String): Boolean {
        val fetchTerms = listOf(
            "safe", "safety", "weather", "season", "recommend", "recommendation", "where should",
            "which place", "destination", "goa", "jaipur", "manali", "varanasi", "kochi",
            "trip", "plan", "itinerary", "best place", "best destination", "location", "where am i",
            "current city", "my city", "where i am", "where am i right now"
        )
        return fetchTerms.any { message.contains(it) }
    }

    private fun isLocationQuestion(message: String): Boolean {
        val locationTerms = listOf(
            "where am i",
            "where i am",
            "my location",
            "current location",
            "current city",
            "my city",
            "where am i right now",
            "can you see my location",
            "location safety"
        )
        return locationTerms.any { message.contains(it) }
    }

    private fun retrievalBudget(
        intent: com.smarttour360.app.ui.chatbot.brain.UserIntent,
        userMessage: String,
        itineraryMode: Boolean
    ): RetrievalBudget {
        val normalized = userMessage.lowercase()
        val shortPrompt = normalized.length <= 55
        val liveHeavyPrompt = listOf(
            "right now",
            "currently",
            "today",
            "weather",
            "forecast",
            "safe",
            "safety",
            "location"
        ).any(normalized::contains)

        return when {
            itineraryMode || intent == com.smarttour360.app.ui.chatbot.brain.UserIntent.TRIP_PLANNING ->
                RetrievalBudget(3, 2, 3, liveHeavyPrompt, 340)
            intent == com.smarttour360.app.ui.chatbot.brain.UserIntent.GENERAL_TRAVEL && shortPrompt ->
                RetrievalBudget(1, 1, 1, false, 170)
            intent == com.smarttour360.app.ui.chatbot.brain.UserIntent.ACTION_REQUEST ||
                intent == com.smarttour360.app.ui.chatbot.brain.UserIntent.TRANSPORT_QUERY ->
                RetrievalBudget(2, 1, 2, false, 220)
            else ->
                RetrievalBudget(
                    knowledgeTopK = if (shortPrompt) 2 else 3,
                    exampleTopK = 1,
                    memoryTopK = 2,
                    fetchFreshApi = liveHeavyPrompt,
                    maxTokens = if (shortPrompt) 190 else 240
                )
        }
    }

    private fun mentionsProfileName(message: String): Boolean {
        return listOf(
            "my name",
            "say my name",
            "what is my name",
            "what's my name",
            "name from my profile",
            "profile name"
        ).any(message::contains)
    }

    private fun mentionsBudget(message: String): Boolean {
        return listOf("my budget", "budget preference", "saved budget").any(message::contains)
    }

    private fun mentionsTransportPreference(message: String): Boolean {
        return listOf(
            "preferred transport",
            "transport preference",
            "my transport"
        ).any(message::contains)
    }

    private fun mentionsTripStyles(message: String): Boolean {
        return listOf(
            "trip style",
            "trip styles",
            "travel style",
            "travel styles"
        ).any(message::contains)
    }

    private fun mentionsDestination(message: String): Boolean {
        return listOf(
            "selected destination",
            "current destination",
            "which destination",
            "what destination",
            "destination selected"
        ).any(message::contains)
    }

    private fun mentionsSelectedHotel(message: String): Boolean {
        return listOf(
            "selected hotel",
            "current hotel",
            "which hotel",
            "hotel selected"
        ).any(message::contains)
    }

    private fun mentionsCart(message: String): Boolean {
        return listOf("my cart", "cart items", "how many items in cart", "cart count").any(message::contains)
    }

    private fun mentionsBookings(message: String): Boolean {
        return listOf(
            "my bookings",
            "booking count",
            "how many bookings",
            "recent booking"
        ).any(message::contains)
    }

    private fun mentionsTripSummary(message: String): Boolean {
        return listOf(
            "my trip",
            "trip summary",
            "trip stops",
            "saved trip",
            "current trip"
        ).any(message::contains)
    }

    private fun resolveDestinationQuery(
        message: String,
        context: ContextBuilder.ChatContext
    ): String? {
        val candidates = buildList {
            context.destinationName?.let(::add)
            context.destinationRegion?.let(::add)
            context.selectedHotelName?.let(::add)
            addAll(context.tripStops)
            addAll(context.liveSafetyPlaces.map { it.substringBefore(" (") })
            addAll(context.liveRecommendationNotes.mapNotNull {
                it.substringAfter(". ", "").substringBefore(" -").trim().takeIf(String::isNotBlank)
            })
        }.distinct()

        return candidates.firstOrNull { candidate ->
            message.contains(candidate.lowercase())
        } ?: context.destinationName
    }

    private fun sanitizeReply(text: String): String {
        return text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("`([^`]*)`"), "$1")
            .replace(Regex("ACTION:[^\\n]*"), "")
            .replace("•", "-")
            .replace("—", "-")
            .replace(Regex("[ \t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .replace(Regex("(?m)^\\s*[-*]\\s+"), "")
            .trim()
    }
}
