package com.smarttour360.app.ui.chatbot.rag

import android.content.Context
import kotlin.math.sqrt

class Retriever(context: Context) {
    private val knowledgeBase = KnowledgeBase(context)
    private val embeddingEngine = EmbeddingEngine()

    suspend fun retrieve(query: String, topK: Int = 3): List<KnowledgeChunk> {
        knowledgeBase.seedIfNeeded()
        val candidates = knowledgeBase.findCandidateChunks(query, limit = 12)
        if (candidates.isEmpty()) return emptyList()

        val queryEmbedding = embeddingEngine.embed(query)
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return candidates.take(topK)
        }

        val missingEmbeddings = candidates.filter { it.embedding.isNullOrEmpty() }
        if (missingEmbeddings.isNotEmpty()) {
            val generated = embeddingEngine.embedBatch(missingEmbeddings.map { chunkText(it) })
            if (generated.isNotEmpty()) {
                val updated = missingEmbeddings.mapIndexedNotNull { index, chunk ->
                    generated.getOrNull(index)?.takeIf { it.isNotEmpty() }?.let { chunk.copy(embedding = it) }
                }
                if (updated.isNotEmpty()) {
                    knowledgeBase.upsertEmbeddings(updated)
                }
            }
        }

        val refreshed = knowledgeBase.findCandidateChunks(query, limit = 12)
        return refreshed
            .map { it to score(queryEmbedding, it) }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun chunkText(chunk: KnowledgeChunk): String {
        return "${chunk.title}. ${chunk.content}. ${chunk.tags.joinToString(" ")}"
    }

    private fun score(queryEmbedding: List<Float>, chunk: KnowledgeChunk): Double {
        val embedding = chunk.embedding ?: return 0.0
        return cosineSimilarity(queryEmbedding, embedding)
    }

    private fun cosineSimilarity(left: List<Float>, right: List<Float>): Double {
        val size = minOf(left.size, right.size)
        if (size == 0) return 0.0
        var dot = 0.0
        var leftNorm = 0.0
        var rightNorm = 0.0
        for (index in 0 until size) {
            val l = left[index].toDouble()
            val r = right[index].toDouble()
            dot += l * r
            leftNorm += l * l
            rightNorm += r * r
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) return 0.0
        return dot / (sqrt(leftNorm) * sqrt(rightNorm))
    }
}
