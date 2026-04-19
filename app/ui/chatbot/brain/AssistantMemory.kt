package com.smarttour360.app.ui.chatbot.brain

import com.smarttour360.app.dto.ChatMessage
import com.smarttour360.app.dto.ChatRole
import com.smarttour360.app.dto.GroqMessage
import com.smarttour360.app.ui.chatbot.ContextBuilder
import com.smarttour360.app.ui.chatbot.EpisodicLogger
import com.smarttour360.app.ui.state.AppStateStore

data class AssistantMemory(
    val conversationHistory: List<GroqMessage>,
    val currentDestination: String?,
    val currentFlag: String?,
    val currentEcoScore: Int?,
    val currentRating: Double?,
    val liveApiSnapshot: String,
    val currentLocationSummary: String,
    val userName: String,
    val homeCity: String,
    val budget: String,
    val tripStyle: String,
    val preferredTransport: String,
    val savedDestinations: List<String>,
    val recentTopics: List<String>
) {
    companion object {
        fun from(
            history: List<ChatMessage>,
            context: ContextBuilder.ChatContext
        ): AssistantMemory {
            val userPreferences = AppStateStore.userPreferences.value
            return AssistantMemory(
                conversationHistory = history.map {
                    GroqMessage(
                        role = if (it.role == ChatRole.USER) "user" else "assistant",
                        content = it.content
                    )
                },
                currentDestination = context.destinationName,
                currentFlag = context.safetyFlag,
                currentEcoScore = context.ecoScore,
                currentRating = context.destinationRating,
                liveApiSnapshot = context.liveApiSnapshot,
                currentLocationSummary = context.currentLocationSummary,
                userName = userPreferences.name,
                homeCity = userPreferences.homeCity,
                budget = userPreferences.budget,
                tripStyle = userPreferences.tripTypes,
                preferredTransport = userPreferences.preferredTransport,
                savedDestinations = context.tripStops,
                recentTopics = EpisodicLogger.getRecentTopics(AppStateStore.getAppContext()).take(5)
            )
        }
    }
}
