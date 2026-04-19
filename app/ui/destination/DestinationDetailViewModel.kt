package com.smarttour360.app.ui.destination

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttour360.app.R
import com.smarttour360.app.data.TravelRepository
import com.smarttour360.app.ui.common.DestinationSummary
import com.smarttour360.app.ui.state.AppStateStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DestinationDetailViewModel : ViewModel() {
    private val repository = TravelRepository()

    private val _uiState = MutableLiveData(buildState())
    val uiState: LiveData<DestinationDetailUiState> = _uiState

    init {
        refreshSafetyData()
    }

    fun refreshSafetyData() {
        _uiState.value = buildState()
        refreshLiveData()
    }

    private fun buildState(): DestinationDetailUiState {
        val destination = resolveCurrentDestination()
        val scoreExplanation = repository.explainLiveScore(destination.name, destination.subtitle)
        val pressureRisk = if (AppStateStore.barometerAvailable) AppStateStore.localPressureRisk else 0f
        val baseSafety = when (destination.flag.uppercase()) {
            "RED" -> 42
            "YELLOW" -> 58
            else -> 74
        }
        val safetyScore = (baseSafety - (pressureRisk * 18).roundToInt()).coerceIn(18, 96)
        val resolvedFlag = when {
            safetyScore < 48 -> "RED"
            safetyScore < 65 -> "YELLOW"
            else -> "GREEN"
        }
        val environmentalScore = (24 + (pressureRisk * 40).roundToInt()).coerceIn(18, 92)
        val explanation = buildString {
            append("${destination.name} currently trends ")
            append(flagPhrase(resolvedFlag))
            append(". ")
            append(scoreExplanation)
            if (AppStateStore.barometerAvailable) {
                append(" Device pressure signal suggests ${AppStateStore.localPressureLabel.lowercase()}.")
            }
        }

        return DestinationDetailUiState(
            destinationName = "${destination.name}, ${destination.subtitle}",
            flagLabel = resolvedFlag,
            flagDisplay = "${flagDot(resolvedFlag)} $resolvedFlag FLAG",
            safetyText = "$safetyScore/100",
            ecoScoreText = "${destination.ecoScore}/100",
            carbonText = "~${destination.carbonKg}kg",
            temperatureText = "${temperatureFor(destination.ecoScore)}C",
            weatherSummary = if (AppStateStore.barometerAvailable) {
                AppStateStore.localPressureLabel
            } else {
                "${destination.flag} live signal with rating ${"%.1f".format(destination.rating)}"
            },
            updatedText = if (AppStateStore.barometerAvailable) {
                "LIVE - Sensor-updated now"
            } else {
                "LIVE - Updated from current travel signals"
            },
            explanation = explanation,
            dominantFactor = if (AppStateStore.barometerAvailable && pressureRisk >= 0.5f) {
                "Environmental (Device pressure signal)"
            } else {
                "Structural (Live destination baseline)"
            },
            riskScore = String.format("%.3f", 1f - (safetyScore / 100f)),
            structuralScore = (safetyScore + 8).coerceAtMost(95),
            situationalScore = (safetyScore - 4).coerceAtLeast(20),
            environmentalScore = environmentalScore,
            blockchainRef = "0x3f4a9b2c1d8e7f0a",
            blockchainTimestamp = "2026-03-24 03:00:00 IST",
            reviewInitials = "ST",
            reviewerName = "SmartTour Signal",
            reviewerMeta = if (AppStateStore.barometerAvailable) {
                "Live device sync - just now"
            } else {
                "Grounded live signals - just now"
            },
            reviewBody = "\"Use the latest safety flag, official NCRB crime baseline, eco score, and local conditions together before finalizing your trip.\"",
            flagBackgroundColor = flagBackground(resolvedFlag),
            flagTextColor = flagTextColor(resolvedFlag),
            heroImageUrl = destination.imageUrl,
            heroImageAttribution = destination.imageAttribution,
            forecast = listOf(
                ForecastDay("Tue", "SUN", "24C"),
                ForecastDay("Wed", "CLOUD", "22C"),
                ForecastDay("Thu", "RAIN", "21C")
            ),
            stays = listOf(
                EcoStay(
                    name = "The Himalayan Sanctuary",
                    meta = "${destination.name} - Eco-Gold",
                    price = "Rs 12,400/night",
                    rating = "4.8",
                    imageTone = R.color.stay_card_blue
                ),
                EcoStay(
                    name = "Snowy Peaks Retreat",
                    meta = "${destination.subtitle} - Sustainable",
                    price = "Rs 8,900/night",
                    rating = "4.6",
                    imageTone = R.color.stay_card_brown
                )
            )
        )
    }

    private fun refreshLiveData() {
        viewModelScope.launch {
            val city = AppStateStore.selectedDestination.value?.name ?: "Manali"
            runCatching {
                repository.getLiveIndianDestinationCatalog(forceRefresh = true)
            }.getOrNull()?.let { liveCatalog ->
                val refreshed = liveCatalog.firstOrNull { it.name.equals(city, ignoreCase = true) }
                if (refreshed != null) {
                    AppStateStore.selectDestination(refreshed)
                    _uiState.value = buildState()
                }
            }

            val liveForecast = repository.getDestinationForecast(city)
            if (liveForecast.isNotEmpty()) {
                val current = _uiState.value ?: return@launch
                _uiState.value = current.copy(
                    forecast = liveForecast.map { ForecastDay(it.day, it.icon, it.temperature) }
                )
            }
        }
    }

    private fun resolveCurrentDestination(): DestinationSummary {
        val selected = AppStateStore.selectedDestination.value ?: return fallbackDestination()
        val liveMatch = AppStateStore.liveDestinations.value.firstOrNull {
            it.name.equals(selected.name, ignoreCase = true)
        }
        return if (liveMatch == null) {
            selected
        } else {
            selected.copy(
                subtitle = liveMatch.region,
                flag = liveMatch.safetyFlag,
                ecoScore = liveMatch.ecoScore,
                carbonKg = liveMatch.carbonKg,
                rating = liveMatch.rating,
                imageUrl = liveMatch.imageUrl
            )
        }
    }

    private fun fallbackDestination(): DestinationSummary {
        return DestinationSummary(
            id = "IND-MAN-01",
            name = "Manali",
            subtitle = "Himachal Pradesh",
            flag = "GREEN",
            ecoScore = 78,
            ethicalScore = "HIGH",
            carbonKg = 32,
            rating = 4.7
        )
    }

    private fun temperatureFor(ecoScore: Int): Int {
        return (16 + ((ecoScore - 50) / 3.0)).roundToInt().coerceIn(14, 29)
    }

    private fun flagPhrase(flag: String): String = when (flag) {
        "RED" -> "high risk"
        "YELLOW" -> "caution"
        else -> "safe"
    }

    private fun flagDot(flag: String): String = when (flag) {
        "RED" -> "RED"
        "YELLOW" -> "YELLOW"
        else -> "GREEN"
    }

    private fun flagBackground(flag: String): Int = when (flag) {
        "RED" -> R.color.flag_red_surface
        "YELLOW" -> R.color.flag_yellow_surface
        else -> R.color.flag_green_soft
    }

    private fun flagTextColor(flag: String): Int = when (flag) {
        "RED" -> R.color.flag_red
        "YELLOW" -> R.color.flag_yellow
        else -> R.color.primary
    }
}

data class DestinationDetailUiState(
    val destinationName: String,
    val flagLabel: String,
    val flagDisplay: String,
    val safetyText: String,
    val ecoScoreText: String,
    val carbonText: String,
    val temperatureText: String,
    val weatherSummary: String,
    val updatedText: String,
    val explanation: String,
    val dominantFactor: String,
    val riskScore: String,
    val structuralScore: Int,
    val situationalScore: Int,
    val environmentalScore: Int,
    val blockchainRef: String,
    val blockchainTimestamp: String,
    val reviewInitials: String,
    val reviewerName: String,
    val reviewerMeta: String,
    val reviewBody: String,
    val flagBackgroundColor: Int,
    val flagTextColor: Int,
    val heroImageUrl: String?,
    val heroImageAttribution: String?,
    val forecast: List<ForecastDay>,
    val stays: List<EcoStay>
)

data class ForecastDay(
    val day: String,
    val icon: String,
    val temperature: String
)

data class EcoStay(
    val name: String,
    val meta: String,
    val price: String,
    val rating: String,
    val imageTone: Int
)
