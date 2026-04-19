package com.smarttour360.app.ui.chatbot.rag

data class KnowledgeChunk(
    val id: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val source: String,
    val embedding: List<Float>? = null
)
