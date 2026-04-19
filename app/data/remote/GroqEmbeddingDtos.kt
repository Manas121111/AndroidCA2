package com.smarttour360.app.data.remote

data class GroqEmbeddingRequest(
    val model: String,
    val input: List<String>
)

data class GroqEmbeddingResponse(
    val data: List<GroqEmbeddingData> = emptyList()
)

data class GroqEmbeddingData(
    val embedding: List<Float> = emptyList(),
    val index: Int = 0
)
