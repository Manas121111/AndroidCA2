package com.smarttour360.app.ui.chatbot.brain

import com.smarttour360.app.dto.GroqMessage
import com.smarttour360.app.ui.chatbot.ContextBuilder

object PromptEngineer {
    fun buildFinalPrompt(
        intent: UserIntent,
        memory: AssistantMemory,
        intentContext: String,
        chatContext: ContextBuilder.ChatContext,
        itineraryMode: Boolean,
        trainingExamplePrompt: String?,
        historySummary: String?,
        recentHistory: List<GroqMessage>,
        userMessage: String
    ): List<GroqMessage> {
        val systemPrompt = buildString {
            appendLine(ContextBuilder.buildSystemPrompt(chatContext, itineraryMode))
            appendLine()
            appendLine("ACTIVE INTENT: ${intent.name}")
            appendLine("YOUR THINKING STYLE:")
            appendLine("- Always check safety before recommending a place.")
            appendLine("- Quote specific live scores and app signals when available.")
            appendLine("- Prefer practical app actions over generic travel writing.")
            appendLine("- If eco priority is relevant, mention the greener option.")
            appendLine("- Keep answers concise and mobile-friendly.")
            appendLine()
            appendLine("USER MEMORY:")
            appendLine("Name: ${memory.userName.ifBlank { "Guest Explorer" }}")
            appendLine("Home city: ${memory.homeCity.ifBlank { "unknown" }}")
            appendLine("Budget: ${memory.budget.ifBlank { "unknown" }}")
            appendLine("Trip style: ${memory.tripStyle.ifBlank { "unknown" }}")
            appendLine("Preferred transport: ${memory.preferredTransport.ifBlank { "unknown" }}")
            appendLine("Recently discussed: ${memory.recentTopics.joinToString(", ").ifBlank { "none" }}")
            appendLine()
            appendLine("INTENT-SPECIFIC CONTEXT:")
            appendLine(intentContext)
            appendLine()
            appendLine("When helpful, you may emit compact helper lines in this format:")
            appendLine("SUGGEST: short follow-up prompt")
            appendLine("ACTION: one of OPEN_RECOMMENDATIONS, OPEN_BOOKINGS, OPEN_HOTELS, OPEN_TRIPS, OPEN_BOOKING_PORTAL, SHOW_TRAINS, ADD_TO_TRIP, OPEN_DESTINATION_DETAIL, OPEN_CART")
            appendLine("For target-aware actions, use this exact style:")
            appendLine("ACTION: ADD_TO_TRIP: Jaipur")
            appendLine("ACTION: OPEN_DESTINATION_DETAIL: Kochi")
            appendLine("ACTION: SHOW_TRAINS: Delhi to Jaipur on 20260330")
            appendLine("Use at most 3 SUGGEST lines and only when useful.")
        }

        return buildList {
            add(GroqMessage("system", systemPrompt))
            if (!trainingExamplePrompt.isNullOrBlank()) {
                add(GroqMessage("system", "--- APPROVED SMARTTOUR360 EXAMPLES ---\n$trainingExamplePrompt\n--- END APPROVED SMARTTOUR360 EXAMPLES ---"))
            }
            if (!historySummary.isNullOrBlank()) {
                add(GroqMessage("system", "Earlier conversation summary: $historySummary"))
            }
            addAll(recentHistory)
            add(GroqMessage("user", userMessage))
        }
    }
}
