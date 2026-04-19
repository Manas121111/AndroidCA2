package com.smarttour360.app.ui.chatbot

import android.content.Context
import org.json.JSONArray

data class KnowledgeChunk(
    val id: String,
    val title: String,
    val text: String,
    val tags: List<String>
)

class KnowledgeRetriever(private val context: Context) {
    private val chunks: List<KnowledgeChunk> by lazy { loadKnowledgeBase() }
    private val stopWords = setOf(
        "the", "and", "for", "with", "from", "this", "that", "your", "you",
        "about", "have", "what", "when", "where", "which", "will", "into"
    )

    fun retrieve(query: String, topK: Int = 3): List<KnowledgeChunk> {
        val normalizedTerms = expandTerms(query)
        if (normalizedTerms.isEmpty()) return emptyList()

        return chunks
            .map { chunk -> chunk to score(query.lowercase(), normalizedTerms, chunk) }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun score(rawQuery: String, queryTerms: List<String>, chunk: KnowledgeChunk): Float {
        val title = chunk.title.lowercase()
        val text = chunk.text.lowercase()
        val tags = chunk.tags.map { it.lowercase() }
        val haystack = "$title $text ${tags.joinToString(" ")}"
        var weightedMatches = queryTerms.fold(0f) { total, term ->
            total + when {
                tags.any { it == term } -> 4f
                title.contains(term) -> 3f
                haystack.contains(term) -> 1f
                else -> 0f
            }
        }

        if (rawQuery.contains(chunk.title.lowercase())) {
            weightedMatches += 4f
        }

        val phraseMatches = phraseCandidates(rawQuery).count { phrase ->
            phrase.isNotBlank() && haystack.contains(phrase)
        }
        weightedMatches += phraseMatches * 2f

        return weightedMatches / queryTerms.size.coerceAtLeast(1)
    }

    private fun loadKnowledgeBase(): List<KnowledgeChunk> {
        val assetPaths = listOf(
            "knowledge/app_features.json",
            "knowledge/destinations.json",
            "knowledge/train_faq.json",
            "knowledge/hotel_policies.json",
            "knowledge/safety_guides.json",
            "knowledge/eco_travel.json"
        )

        return assetPaths.flatMap { path ->
            runCatching {
                context.assets.open(path).bufferedReader().use { reader ->
                    parseChunks(JSONArray(reader.readText()))
                }
            }.getOrDefault(emptyList())
        }
    }

    private fun parseChunks(array: JSONArray): List<KnowledgeChunk> {
        val output = mutableListOf<KnowledgeChunk>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val tagsArray = item.optJSONArray("tags")
            val tags = buildList {
                if (tagsArray != null) {
                    for (tagIndex in 0 until tagsArray.length()) {
                        add(tagsArray.optString(tagIndex))
                    }
                }
            }.filter { it.isNotBlank() }

            output += KnowledgeChunk(
                id = item.optString("id"),
                title = item.optString("title").ifBlank {
                    item.optString("destination").ifBlank { "SmartTour360 Knowledge" }
                },
                text = item.optString("chunk"),
                tags = tags
            )
        }
        return output.filter { it.text.isNotBlank() }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }

    private fun expandTerms(text: String): List<String> {
        val baseTerms = tokenize(text)
        val expanded = mutableSetOf<String>()
        expanded += baseTerms

        baseTerms.forEach { term ->
            when (term) {
                "booking", "bookings" -> expanded += listOf("cart", "booking", "history")
                "train", "trains" -> expanded += listOf("irctc", "erail", "rail")
                "hotel", "hotels", "stay", "stays" -> expanded += listOf("hotel", "stay", "eco")
                "safe", "safety" -> expanded += listOf("safety", "risk", "secure")
                "recommend", "recommendation" -> expanded += listOf("recommendation", "rank", "suggestion")
                "location" -> expanded += listOf("location", "city", "current")
                "pondicherry" -> expanded += listOf("puducherry")
                "puducherry" -> expanded += listOf("pondicherry")
                "kochi" -> expanded += listOf("cochin")
                "cochin" -> expanded += listOf("kochi")
            }
        }
        return expanded.toList()
    }

    private fun phraseCandidates(text: String): List<String> {
        val normalized = text.lowercase().replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) return emptyList()
        val parts = normalized.split(" ")
        if (parts.size < 2) return emptyList()
        return parts.windowed(size = 2, step = 1, partialWindows = false)
            .map { it.joinToString(" ") }
            .distinct()
    }
}
