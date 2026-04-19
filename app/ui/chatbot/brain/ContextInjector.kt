package com.smarttour360.app.ui.chatbot.brain

import com.smarttour360.app.ui.chatbot.KnowledgeChunk
import com.smarttour360.app.ui.state.AppStateStore

object ContextInjector {
    fun buildContextForIntent(
        intent: UserIntent,
        memory: AssistantMemory,
        ragChunks: List<KnowledgeChunk>,
        freshApiContext: String?,
        personalMemoryPrompt: String?
    ): String = buildString {
        appendLine("INTENT: ${intent.name}")
        when (intent) {
            UserIntent.SAFETY_QUESTION -> {
                appendLine("SAFETY DATA:")
                appendLine("Current destination: ${memory.currentDestination ?: "none"}")
                appendLine("Current flag: ${memory.currentFlag ?: "unknown"}")
                appendLine("Current eco score: ${memory.currentEcoScore ?: 0}")
                appendLine("Current rating: ${memory.currentRating ?: 0.0}")
                appendLine("Location signal: ${memory.currentLocationSummary.ifBlank { "none" }}")
            }
            UserIntent.TRIP_PLANNING -> {
                appendLine("USER TRIP CONTEXT:")
                appendLine("Home city: ${memory.homeCity.ifBlank { "unknown" }}")
                appendLine("Budget: ${memory.budget.ifBlank { "unknown" }}")
                appendLine("Style: ${memory.tripStyle.ifBlank { "unknown" }}")
                appendLine("Transport: ${memory.preferredTransport.ifBlank { "unknown" }}")
                appendLine("Already saved: ${memory.savedDestinations.joinToString(", ").ifBlank { "none" }}")
            }
            UserIntent.TRANSPORT_QUERY -> {
                appendLine("TRANSPORT CONTEXT:")
                appendLine("Home city: ${memory.homeCity.ifBlank { "unknown" }}")
                appendLine("Current destination: ${memory.currentDestination ?: "none"}")
                appendLine("Preferred transport: ${memory.preferredTransport.ifBlank { "unknown" }}")
            }
            UserIntent.ECO_QUESTION -> {
                appendLine("ECO DATA:")
                appendLine("Current destination: ${memory.currentDestination ?: "none"}")
                appendLine("Current eco score: ${memory.currentEcoScore ?: 0}/100")
            }
            UserIntent.PATENT_EXPLANATION -> {
                appendLine("SCORING DATA:")
                appendLine("Current destination: ${memory.currentDestination ?: "none"}")
                appendLine("Live snapshot: ${memory.liveApiSnapshot.ifBlank { "none" }}")
            }
            UserIntent.ACTION_REQUEST -> {
                appendLine("ACTION CONTEXT:")
                appendLine("Current destination: ${memory.currentDestination ?: "none"}")
                appendLine("Saved destinations: ${memory.savedDestinations.joinToString(", ").ifBlank { "none" }}")
            }
            else -> {
                appendLine("GENERAL TRAVEL CONTEXT:")
                appendLine("Current destination: ${memory.currentDestination ?: "none"}")
                appendLine("Live snapshot: ${memory.liveApiSnapshot.ifBlank { "none" }}")
            }
        }

        if (freshApiContext != null) {
            appendLine("FRESH API CONTEXT:")
            appendLine(freshApiContext)
        }

        if (!personalMemoryPrompt.isNullOrBlank()) {
            appendLine("PERSONAL MEMORY:")
            appendLine(personalMemoryPrompt)
        }

        if (ragChunks.isNotEmpty()) {
            appendLine("RAG KNOWLEDGE:")
            ragChunks.forEach { chunk ->
                appendLine("${chunk.title}: ${chunk.text}")
            }
        }

        val liveStops = AppStateStore.tripEntries.value.map { it.title }.distinct()
        if (liveStops.isNotEmpty()) {
            appendLine("TRIP ENTRIES: ${liveStops.joinToString(" -> ")}")
        }
    }
}
