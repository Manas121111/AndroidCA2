package com.smarttour360.app.dto

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val isLoading: Boolean = false
)

enum class ChatRole {
    USER,
    BOT
}

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.5,
    val max_tokens: Int = 500
)

data class GroqChatResponse(
    val choices: List<GroqChoice> = emptyList()
)

data class GroqChoice(
    val message: GroqMessage
)

data class QuickReply(
    val label: String,
    val prompt: String
)
