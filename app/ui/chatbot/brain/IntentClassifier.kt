package com.smarttour360.app.ui.chatbot.brain

enum class UserIntent {
    SAFETY_QUESTION,
    DESTINATION_DISCOVERY,
    TRIP_PLANNING,
    TRANSPORT_QUERY,
    ECO_QUESTION,
    BOOKING_HELP,
    PATENT_EXPLANATION,
    ACTION_REQUEST,
    GENERAL_TRAVEL
}

object IntentClassifier {
    fun classify(userMessage: String): UserIntent {
        val lower = userMessage.lowercase()
        return when {
            lower.containsAny("safe", "danger", "risk", "flag", "avoid") ->
                UserIntent.SAFETY_QUESTION
            lower.containsAny("suggest", "recommend", "where should", "best place") ->
                UserIntent.DESTINATION_DISCOVERY
            lower.containsAny("plan", "itinerary", "days", "trip plan", "schedule") ->
                UserIntent.TRIP_PLANNING
            lower.containsAny("train", "flight", "bus", "reach", "travel to", "how to go") ->
                UserIntent.TRANSPORT_QUERY
            lower.containsAny("eco", "green", "carbon", "sustainable", "environment") ->
                UserIntent.ECO_QUESTION
            lower.containsAny("book", "hotel", "stay", "reservation", "checkout") ->
                UserIntent.BOOKING_HELP
            lower.containsAny("how does", "scoring", "blockchain", "algorithm", "patent") ->
                UserIntent.PATENT_EXPLANATION
            lower.containsAny("add to trip", "save this", "open booking", "show me trains") ->
                UserIntent.ACTION_REQUEST
            else -> UserIntent.GENERAL_TRAVEL
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { contains(it) }
    }
}
