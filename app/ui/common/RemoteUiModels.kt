package com.smarttour360.app.ui.common

data class ForecastUi(
    val day: String,
    val icon: String,
    val temperature: String
)

data class LiveEventUi(
    val id: String,
    val name: String,
    val venue: String,
    val location: String,
    val date: String
)

data class TravelSuggestion(
    val destinationName: String,
    val title: String,
    val summary: String,
    val reason: String,
    val region: String,
    val budgetLabel: String,
    val ecoScore: Int,
    val safetyFlag: String,
    val rating: Double,
    val imageUrl: String? = null,
    val imageAttribution: String? = null,
    val rankReason: String,
    val liveSignal: String,
    val routeHint: String,
    val vibeTags: List<String>
)
