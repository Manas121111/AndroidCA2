package com.smarttour360.app.ui.chatbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LearnedUserProfile(
    val favoriteDestinations: List<String> = emptyList(),
    val travelStyles: List<String> = emptyList(),
    val transportPreferences: List<String> = emptyList(),
    val budgetSignals: List<String> = emptyList(),
    val hotelSignals: List<String> = emptyList(),
    val concernSignals: List<String> = emptyList(),
    val updatedAtEpochMs: Long = 0L
) {
    fun summary(): String {
        val parts = buildList {
            if (favoriteDestinations.isNotEmpty()) add("favorite destinations ${favoriteDestinations.joinToString(", ")}")
            if (travelStyles.isNotEmpty()) add("travel styles ${travelStyles.joinToString(", ")}")
            if (transportPreferences.isNotEmpty()) add("transport ${transportPreferences.joinToString(", ")}")
            if (budgetSignals.isNotEmpty()) add("budget signals ${budgetSignals.joinToString(", ")}")
            if (hotelSignals.isNotEmpty()) add("stay preferences ${hotelSignals.joinToString(", ")}")
            if (concernSignals.isNotEmpty()) add("frequent concerns ${concernSignals.joinToString(", ")}")
        }
        return parts.joinToString("; ")
    }
}

class LocalUserPreferenceStore(private val context: Context) {
    private val profileDir: File by lazy {
        File(context.filesDir, "chat_profiles").apply { mkdirs() }
    }

    fun load(userKey: String): LearnedUserProfile {
        val file = profileFile(userKey)
        if (!file.exists()) return LearnedUserProfile()

        return runCatching {
            val json = JSONObject(file.readText())
            LearnedUserProfile(
                favoriteDestinations = json.optJSONArray("favoriteDestinations").toStringList(),
                travelStyles = json.optJSONArray("travelStyles").toStringList(),
                transportPreferences = json.optJSONArray("transportPreferences").toStringList(),
                budgetSignals = json.optJSONArray("budgetSignals").toStringList(),
                hotelSignals = json.optJSONArray("hotelSignals").toStringList(),
                concernSignals = json.optJSONArray("concernSignals").toStringList(),
                updatedAtEpochMs = json.optLong("updatedAtEpochMs")
            )
        }.getOrDefault(LearnedUserProfile())
    }

    fun learn(userKey: String, userMessage: String, context: ContextBuilder.ChatContext) {
        val normalized = userMessage.lowercase()
        if (normalized.length < 8) return

        val current = load(userKey)
        val updated = current.copy(
            favoriteDestinations = mergeLimited(
                current.favoriteDestinations,
                extractDestinations(normalized, context)
            ),
            travelStyles = mergeLimited(
                current.travelStyles,
                extractMatches(normalized, STYLE_TERMS)
            ),
            transportPreferences = mergeLimited(
                current.transportPreferences,
                extractMatches(normalized, TRANSPORT_TERMS)
            ),
            budgetSignals = mergeLimited(
                current.budgetSignals,
                extractMatches(normalized, BUDGET_TERMS)
            ),
            hotelSignals = mergeLimited(
                current.hotelSignals,
                extractMatches(normalized, HOTEL_TERMS)
            ),
            concernSignals = mergeLimited(
                current.concernSignals,
                extractMatches(normalized, CONCERN_TERMS)
            ),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        save(userKey, updated)
    }

    private fun extractDestinations(
        normalizedMessage: String,
        context: ContextBuilder.ChatContext
    ): List<String> {
        val candidates = buildList {
            context.destinationName?.let(::add)
            addAll(context.tripStops)
            addAll(context.liveSafetyPlaces.map { it.substringBefore(" (") })
            addAll(context.liveRecommendationNotes.mapNotNull {
                it.substringAfter(". ", "").substringBefore(" -").trim().takeIf(String::isNotBlank)
            })
        }.distinct()

        return candidates
            .filter { normalizedMessage.contains(it.lowercase()) }
            .take(6)
    }

    private fun extractMatches(normalizedMessage: String, terms: List<String>): List<String> {
        return terms.filter { normalizedMessage.contains(it) }.take(6)
    }

    private fun mergeLimited(existing: List<String>, incoming: List<String>): List<String> {
        return (existing + incoming)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
    }

    private fun save(userKey: String, profile: LearnedUserProfile) {
        val json = JSONObject().apply {
            put("favoriteDestinations", JSONArray(profile.favoriteDestinations))
            put("travelStyles", JSONArray(profile.travelStyles))
            put("transportPreferences", JSONArray(profile.transportPreferences))
            put("budgetSignals", JSONArray(profile.budgetSignals))
            put("hotelSignals", JSONArray(profile.hotelSignals))
            put("concernSignals", JSONArray(profile.concernSignals))
            put("updatedAtEpochMs", profile.updatedAtEpochMs)
        }
        profileFile(userKey).writeText(json.toString())
    }

    private fun profileFile(userKey: String): File {
        val safeName = userKey.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(profileDir, "$safeName.json")
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                add(optString(index))
            }
        }.filter { it.isNotBlank() }
    }

    companion object {
        private val STYLE_TERMS = listOf("adventure", "heritage", "nature", "beach", "food", "family", "luxury", "budget")
        private val TRANSPORT_TERMS = listOf("train", "flight", "bus", "hotel", "car", "cab")
        private val BUDGET_TERMS = listOf("budget", "cheap", "mid-range", "luxury", "premium", "affordable")
        private val HOTEL_TERMS = listOf("eco stay", "eco hotel", "resort", "hostel", "base stay", "stay")
        private val CONCERN_TERMS = listOf("safety", "safe", "weather", "season", "crowd", "eco", "price", "budget", "booking")
    }
}
