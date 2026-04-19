package com.smarttour360.app.ui.chatbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class UserMemoryEntry(
    val id: String,
    val summary: String,
    val keywords: List<String>,
    val createdAtEpochMs: Long,
    val lastAccessedEpochMs: Long,
    val hitCount: Int,
    val feedbackScore: Int = 0
)

class LocalUserMemoryStore(private val context: Context) {
    private val memoryDir: File by lazy {
        File(context.filesDir, "chat_memory").apply { mkdirs() }
    }

    fun retrieve(userKey: String, query: String, topK: Int = 3): List<UserMemoryEntry> {
        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty()) return emptyList()

        val memories = load(userKey)
        val ranked = memories
            .filter { it.feedbackScore > -3 }
            .map { memory -> memory to score(queryTerms, memory) }
            .filter { it.second > 0f }
            .sortedWith(
                compareByDescending<Pair<UserMemoryEntry, Float>> { it.second }
                    .thenByDescending { it.first.feedbackScore }
                    .thenByDescending { it.first.hitCount }
                    .thenByDescending { it.first.lastAccessedEpochMs }
            )
            .take(topK)
            .map { it.first }

        if (ranked.isNotEmpty()) {
            markAccessed(userKey, ranked.map { it.id }.toSet())
        }
        return ranked
    }

    fun learn(
        userKey: String,
        userMessage: String,
        assistantReply: String,
        context: ContextBuilder.ChatContext
    ) {
        if (userMessage.isBlank() || assistantReply.isBlank()) return
        if (!isWorthLearning(userMessage, assistantReply)) return

        val now = System.currentTimeMillis()
        val summary = buildSummary(userMessage, assistantReply, context)
        val keywords = buildKeywords(userMessage, assistantReply, context)
        if (summary.isBlank() || keywords.isEmpty()) return

        val memories = load(userKey).toMutableList()
        val existingIndex = memories.indexOfFirst { existing ->
            existing.summary.equals(summary, ignoreCase = true) ||
                overlapRatio(existing.keywords, keywords) >= 0.75f
        }

        if (existingIndex >= 0) {
            val existing = memories[existingIndex]
            memories[existingIndex] = existing.copy(
                summary = summary,
                keywords = (existing.keywords + keywords).distinct().take(24),
                lastAccessedEpochMs = now,
                hitCount = existing.hitCount + 1,
                feedbackScore = existing.feedbackScore
            )
        } else {
            memories += UserMemoryEntry(
                id = UUID.randomUUID().toString(),
                summary = summary,
                keywords = keywords.take(24),
                createdAtEpochMs = now,
                lastAccessedEpochMs = now,
                hitCount = 1,
                feedbackScore = 0
            )
        }

        val compacted = memories
            .sortedWith(
                compareByDescending<UserMemoryEntry> { it.hitCount }
                    .thenByDescending { it.lastAccessedEpochMs }
            )
            .take(120)

        save(userKey, compacted)
    }

    fun recordFeedback(
        userKey: String,
        userMessage: String,
        assistantReply: String,
        context: ContextBuilder.ChatContext,
        positive: Boolean
    ) {
        val summary = buildSummary(userMessage, assistantReply, context)
        val keywords = buildKeywords(userMessage, assistantReply, context)
        if (summary.isBlank() || keywords.isEmpty()) return

        val now = System.currentTimeMillis()
        val memories = load(userKey).toMutableList()
        val index = memories.indexOfFirst { existing ->
            existing.summary.equals(summary, ignoreCase = true) ||
                overlapRatio(existing.keywords, keywords) >= 0.75f
        }

        if (index >= 0) {
            val existing = memories[index]
            memories[index] = existing.copy(
                lastAccessedEpochMs = now,
                hitCount = if (positive) existing.hitCount + 2 else existing.hitCount,
                feedbackScore = (existing.feedbackScore + if (positive) 3 else -4).coerceIn(-8, 12)
            )
        } else if (positive) {
            memories += UserMemoryEntry(
                id = UUID.randomUUID().toString(),
                summary = summary,
                keywords = keywords.take(24),
                createdAtEpochMs = now,
                lastAccessedEpochMs = now,
                hitCount = 3,
                feedbackScore = 3
            )
        } else {
            memories += UserMemoryEntry(
                id = UUID.randomUUID().toString(),
                summary = summary,
                keywords = keywords.take(24),
                createdAtEpochMs = now,
                lastAccessedEpochMs = now,
                hitCount = 0,
                feedbackScore = -4
            )
        }

        save(
            userKey,
            memories
                .sortedWith(
                    compareByDescending<UserMemoryEntry> { it.feedbackScore }
                        .thenByDescending { it.hitCount }
                        .thenByDescending { it.lastAccessedEpochMs }
                )
                .take(120)
        )
    }

    private fun buildSummary(
        userMessage: String,
        assistantReply: String,
        context: ContextBuilder.ChatContext
    ): String {
        val userPart = userMessage.trim().replace(Regex("\\s+"), " ").take(140)
        val replyPart = assistantReply.trim().replace(Regex("\\s+"), " ").take(220)
        val destinationPart = context.destinationName?.takeIf { it.isNotBlank() }?.let {
            " Destination in context: $it."
        }.orEmpty()
        return "User asked: $userPart. Assistant guidance: $replyPart.$destinationPart"
    }

    private fun buildKeywords(
        userMessage: String,
        assistantReply: String,
        context: ContextBuilder.ChatContext
    ): List<String> {
        return (
            tokenize(userMessage) +
                tokenize(assistantReply) +
                listOfNotNull(
                    context.destinationName?.lowercase(),
                    context.destinationRegion?.lowercase(),
                    context.selectedHotelName?.lowercase()
                ) +
                context.tripStops.map { it.lowercase() }
            )
            .filter { it.length > 2 }
            .distinct()
            .take(24)
    }

    private fun score(queryTerms: List<String>, memory: UserMemoryEntry): Float {
        val haystack = "${memory.summary} ${memory.keywords.joinToString(" ")}".lowercase()
        val matches = queryTerms.map { term ->
            when {
                memory.keywords.any { it.equals(term, ignoreCase = true) } -> 3
                haystack.contains(term) -> 1
                else -> 0
            }
        }.sum()
        val base = matches.toFloat() / queryTerms.size
        val feedbackBoost = memory.feedbackScore * 0.35f
        return (base + feedbackBoost).coerceAtLeast(0f)
    }

    private fun overlapRatio(existing: List<String>, incoming: List<String>): Float {
        if (existing.isEmpty() || incoming.isEmpty()) return 0f
        val overlap = existing.intersect(incoming.toSet()).size
        return overlap.toFloat() / minOf(existing.size, incoming.size)
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 }
            .distinct()
    }

    private fun isWorthLearning(userMessage: String, assistantReply: String): Boolean {
        val normalizedUser = userMessage.trim().lowercase()
        val normalizedReply = assistantReply.trim().lowercase()
        if (normalizedUser.length < 12) return false
        if (normalizedReply.length < 24) return false
        if (normalizedReply.startsWith("i'm smarttour360's travel assistant")) return false
        val weakPrompts = listOf("hi", "hello", "hey", "thanks", "ok", "okay")
        if (weakPrompts.any { normalizedUser == it }) return false
        return true
    }

    private fun markAccessed(userKey: String, ids: Set<String>) {
        val now = System.currentTimeMillis()
        val updated = load(userKey).map { memory ->
            if (memory.id in ids) {
                memory.copy(
                    lastAccessedEpochMs = now,
                    hitCount = memory.hitCount + 1
                )
            } else {
                memory
            }
        }
        save(userKey, updated)
    }

    private fun load(userKey: String): List<UserMemoryEntry> {
        val file = memoryFile(userKey)
        if (!file.exists()) return emptyList()

        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val keywordsArray = item.optJSONArray("keywords")
                    val keywords = buildList {
                        if (keywordsArray != null) {
                            for (tagIndex in 0 until keywordsArray.length()) {
                                add(keywordsArray.optString(tagIndex))
                            }
                        }
                    }.filter { it.isNotBlank() }

                    add(
                        UserMemoryEntry(
                            id = item.optString("id"),
                            summary = item.optString("summary"),
                            keywords = keywords,
                            createdAtEpochMs = item.optLong("createdAtEpochMs"),
                            lastAccessedEpochMs = item.optLong("lastAccessedEpochMs"),
                            hitCount = item.optInt("hitCount", 1),
                            feedbackScore = item.optInt("feedbackScore", 0)
                        )
                    )
                }
            }.filter { it.summary.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun save(userKey: String, memories: List<UserMemoryEntry>) {
        val array = JSONArray()
        memories.forEach { memory ->
            array.put(
                JSONObject().apply {
                    put("id", memory.id)
                    put("summary", memory.summary)
                    put("createdAtEpochMs", memory.createdAtEpochMs)
                    put("lastAccessedEpochMs", memory.lastAccessedEpochMs)
                    put("hitCount", memory.hitCount)
                    put("feedbackScore", memory.feedbackScore)
                    put("keywords", JSONArray(memory.keywords))
                }
            )
        }
        memoryFile(userKey).writeText(array.toString())
    }

    private fun memoryFile(userKey: String): File {
        val safeName = userKey.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(memoryDir, "$safeName.json")
    }
}
