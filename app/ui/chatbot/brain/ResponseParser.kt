package com.smarttour360.app.ui.chatbot.brain

import com.smarttour360.app.dto.QuickReply

enum class BotActionType {
    OPEN_RECOMMENDATIONS,
    OPEN_BOOKINGS,
    OPEN_HOTELS,
    OPEN_TRIPS,
    OPEN_BOOKING_PORTAL,
    SHOW_TRAINS,
    ADD_TO_TRIP,
    OPEN_DESTINATION_DETAIL,
    OPEN_CART,
    NONE
}

data class BotAction(
    val type: BotActionType,
    val raw: String,
    val target: String? = null
)

data class ParsedResponse(
    val displayText: String,
    val actions: List<BotAction>,
    val followUpChips: List<QuickReply>,
    val confidenceSignals: List<String>
)

object ResponseParser {
    fun parse(raw: String, intent: UserIntent): ParsedResponse {
        val lines = raw.lines()
        val actions = mutableListOf<BotAction>()
        val chips = mutableListOf<QuickReply>()
        val displayLines = mutableListOf<String>()

        lines.forEach { line ->
            when {
                line.startsWith("ACTION:", ignoreCase = true) -> {
                    parseAction(line.substringAfter("ACTION:").trim())?.let(actions::add)
                }
                line.startsWith("SUGGEST:", ignoreCase = true) -> {
                    val prompt = line.substringAfter("SUGGEST:").trim()
                    if (prompt.isNotBlank()) {
                        chips += QuickReply(prompt.take(18), prompt)
                    }
                }
                else -> displayLines += line
            }
        }

        val generatedChips = if (chips.isEmpty()) {
            generateFollowUpChips(intent)
        } else {
            chips.take(3)
        }

        return ParsedResponse(
            displayText = displayLines.joinToString("\n").trim(),
            actions = actions,
            followUpChips = generatedChips,
            confidenceSignals = emptyList()
        )
    }

    private fun parseAction(raw: String): BotAction? {
        if (raw.isBlank()) return null
        val normalized = raw.lowercase()
        val target = raw.substringAfter(":", "").takeIf { ":" in raw }?.trim()?.ifBlank { null }
        val type = when {
            normalized.startsWith("open_recommendations") || normalized.contains("open recommendations") ->
                BotActionType.OPEN_RECOMMENDATIONS
            normalized.startsWith("open_bookings") || normalized.contains("open bookings") ->
                BotActionType.OPEN_BOOKINGS
            normalized.startsWith("open_hotels") || normalized.contains("open hotels") ->
                BotActionType.OPEN_HOTELS
            normalized.startsWith("open_trips") || normalized.contains("open trips") || normalized.contains("trip planner") ->
                BotActionType.OPEN_TRIPS
            normalized.startsWith("show_trains") || normalized.contains("show trains") ->
                BotActionType.SHOW_TRAINS
            normalized.startsWith("open_booking_portal") || normalized.contains("booking portal") ->
                BotActionType.OPEN_BOOKING_PORTAL
            normalized.startsWith("add_to_trip") || normalized.contains("add to trip") ->
                BotActionType.ADD_TO_TRIP
            normalized.startsWith("open_destination_detail") || normalized.startsWith("open_destination") ||
                normalized.contains("destination detail") ->
                BotActionType.OPEN_DESTINATION_DETAIL
            normalized.startsWith("open_cart") || normalized.contains("open cart") ->
                BotActionType.OPEN_CART
            else -> BotActionType.NONE
        }
        return if (type == BotActionType.NONE) null else BotAction(type = type, raw = raw, target = target)
    }

    private fun generateFollowUpChips(intent: UserIntent): List<QuickReply> = when (intent) {
        UserIntent.SAFETY_QUESTION ->
            listOf(
                QuickReply("Alternatives", "Show safer alternatives right now."),
                QuickReply("Best season", "What is the best season for this place?"),
                QuickReply("Packing", "What should I pack for this trip?")
            )
        UserIntent.TRIP_PLANNING ->
            listOf(
                QuickReply("Add to trip", "Add this to my trip plan."),
                QuickReply("Show trains", "Show train options for this trip."),
                QuickReply("Budget", "Give me a budget breakdown.")
            )
        UserIntent.TRANSPORT_QUERY ->
            listOf(
                QuickReply("Train options", "Show train options."),
                QuickReply("Bus options", "Show bus options."),
                QuickReply("Flight options", "Show flight options.")
            )
        UserIntent.DESTINATION_DISCOVERY ->
            listOf(
                QuickReply("Check safety", "Check safety for this recommendation."),
                QuickReply("Add to trip", "Add this place to my trip."),
                QuickReply("Eco options", "Show greener options.")
            )
        else ->
            listOf(
                QuickReply("Plan trip", "Plan a short trip for me."),
                QuickReply("Check safety", "Check safety for this place."),
                QuickReply("Eco options", "Show eco-friendly options.")
            )
    }
}
