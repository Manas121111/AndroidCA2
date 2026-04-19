package com.smarttour360.app.ui.chatbot

import com.smarttour360.app.dto.ChatMessage
import com.smarttour360.app.dto.ChatRole

class ChatMemoryManager(
    private val maxRecentMessages: Int = 8
) {
    fun summarizedHistory(history: List<ChatMessage>): Pair<String?, List<ChatMessage>> {
        if (history.size <= maxRecentMessages) {
            return null to history
        }

        val older = history.dropLast(maxRecentMessages)
        val recent = history.takeLast(maxRecentMessages)

        val summary = buildString {
            val userTopics = older
                .filter { it.role == ChatRole.USER }
                .map { it.content.trim() }
                .filter { it.isNotBlank() }
                .takeLast(4)

            val assistantReplies = older
                .filter { it.role == ChatRole.BOT && !it.isLoading }
                .map { it.content.trim() }
                .filter { it.isNotBlank() }
                .takeLast(3)

            if (userTopics.isNotEmpty()) {
                append("Earlier user requests: ")
                append(userTopics.joinToString(" | "))
                append(". ")
            }
            if (assistantReplies.isNotEmpty()) {
                append("Earlier assistant guidance: ")
                append(assistantReplies.joinToString(" | "))
                append(".")
            }
        }.trim().ifBlank { null }

        return summary to recent
    }
}
