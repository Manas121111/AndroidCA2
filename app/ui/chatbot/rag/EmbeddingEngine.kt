package com.smarttour360.app.ui.chatbot.rag

import com.smarttour360.app.BuildConfig
import com.smarttour360.app.data.remote.ApiClient
import com.smarttour360.app.data.remote.GroqEmbeddingRequest

class EmbeddingEngine {
    suspend fun embed(text: String): List<Float>? {
        return embedBatch(listOf(text)).firstOrNull()
    }

    suspend fun embedBatch(texts: List<String>): List<List<Float>> {
        if (BuildConfig.GROQ_API_KEY.isBlank()) return emptyList()
        val sanitized = texts.map { it.trim() }.filter { it.isNotBlank() }
        if (sanitized.isEmpty()) return emptyList()

        return runCatching {
            ApiClient.groqApi.embeddings(
                authorization = "Bearer ${BuildConfig.GROQ_API_KEY}",
                request = GroqEmbeddingRequest(
                    model = "nomic-embed-text-v1.5",
                    input = sanitized
                )
            ).data
                .sortedBy { it.index }
                .map { it.embedding }
        }.getOrDefault(emptyList())
    }
}
