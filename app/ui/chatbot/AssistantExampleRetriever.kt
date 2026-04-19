package com.smarttour360.app.ui.chatbot

import android.content.Context
import org.json.JSONArray

data class AssistantExample(
    val id: String,
    val category: String,
    val user: String,
    val assistant: String,
    val tags: List<String>
)

class AssistantExampleRetriever(private val context: Context) {
    private val examples: List<AssistantExample> by lazy { loadExamples() }

    fun retrieve(query: String, topK: Int = 2): List<AssistantExample> {
        val terms = tokenize(query)
        if (terms.isEmpty()) return emptyList()

        return examples
            .map { example -> example to score(example, terms, query.lowercase()) }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun score(example: AssistantExample, terms: List<String>, rawQuery: String): Float {
        val haystack = buildString {
            append(example.category)
            append(' ')
            append(example.user)
            append(' ')
            append(example.assistant)
            append(' ')
            append(example.tags.joinToString(" "))
        }.lowercase()

        var score = 0f
        terms.forEach { term ->
            when {
                example.tags.any { it.equals(term, ignoreCase = true) } -> score += 3f
                example.category.equals(term, ignoreCase = true) -> score += 2.5f
                haystack.contains(term) -> score += 1f
            }
        }

        if (rawQuery.contains("how were you developed") && example.category == "scope") score += 4f
        if (rawQuery.contains("location") && example.category == "location") score += 3f
        if (rawQuery.contains("recommend") && example.category == "recommendation") score += 3f
        if (rawQuery.contains("book") && example.category == "booking") score += 3f
        if (rawQuery.contains("safe") && example.category == "safety") score += 3f
        if (rawQuery.contains("plan") && example.category == "itinerary") score += 3f

        return score / terms.size.coerceAtLeast(1)
    }

    private fun loadExamples(): List<AssistantExample> {
        return runCatching {
            context.assets.open("knowledge/assistant_examples.json").bufferedReader().use { reader ->
                parse(JSONArray(reader.readText()))
            }
        }.getOrDefault(emptyList())
    }

    private fun parse(array: JSONArray): List<AssistantExample> {
        return buildList {
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

                add(
                    AssistantExample(
                        id = item.optString("id"),
                        category = item.optString("category"),
                        user = item.optString("user"),
                        assistant = item.optString("assistant"),
                        tags = tags
                    )
                )
            }
        }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 }
            .distinct()
    }
}
