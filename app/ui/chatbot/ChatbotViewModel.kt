package com.smarttour360.app.ui.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttour360.app.data.remote.ChatbotRepository
import com.smarttour360.app.dto.ChatMessage
import com.smarttour360.app.dto.ChatRole
import com.smarttour360.app.dto.QuickReply
import com.smarttour360.app.ui.chatbot.brain.BotAction
import com.smarttour360.app.ui.state.AppStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val quickReplies: List<QuickReply> = listOf(
        QuickReply("Safe places", "Show me safe places to visit right now."),
        QuickReply("Trip budget", "Help me estimate my trip budget."),
        QuickReply("Rank places", "Why are these places ranked highest right now?")
    ),
    val isSending: Boolean = false,
    val showFeedback: Boolean = false,
    val feedbackState: FeedbackState = FeedbackState.NONE,
    val pendingActions: List<BotAction> = emptyList()
)

enum class FeedbackState {
    NONE,
    POSITIVE,
    NEGATIVE
}

private data class FeedbackTurn(
    val userMessage: String,
    val assistantReply: String,
    val context: ContextBuilder.ChatContext
)

class ChatbotViewModel : ViewModel() {
    private val repository = ChatbotRepository()
    private var lastFeedbackTurn: FeedbackTurn? = null
    private val _uiState = MutableStateFlow(
        ChatbotUiState(
            messages = AppStateStore.chatHistory.map { (role, content) ->
                ChatMessage(
                    role = if (role == "user") ChatRole.USER else ChatRole.BOT,
                    content = content
                )
            }.ifEmpty {
                listOf(ChatMessage(ChatRole.BOT, "Ask about safety, routes, budgets, or your saved trip."))
            }
        )
    )
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    fun sendMessage(
        message: String,
        context: ContextBuilder.ChatContext,
        itineraryMode: Boolean = false
    ) {
        if (message.isBlank()) return
        val currentMessages = _uiState.value.messages
        val stagedMessages = currentMessages +
            ChatMessage(ChatRole.USER, message) +
            ChatMessage(ChatRole.BOT, "Thinking...", isLoading = true)
        _uiState.value = _uiState.value.copy(
            messages = stagedMessages,
            isSending = true,
            showFeedback = false,
            feedbackState = FeedbackState.NONE,
            pendingActions = emptyList()
        )
        AppStateStore.appendChat("user", message)

        viewModelScope.launch {
            val reply = repository.sendMessage(message, currentMessages, context, itineraryMode)
            AppStateStore.appendChat("assistant", reply.displayText)
            lastFeedbackTurn = FeedbackTurn(
                userMessage = message,
                assistantReply = reply.displayText,
                context = context
            )
            _uiState.value = _uiState.value.copy(
                messages = stagedMessages.dropLast(1) + ChatMessage(ChatRole.BOT, reply.displayText),
                isSending = false,
                showFeedback = true,
                feedbackState = FeedbackState.NONE,
                quickReplies = reply.followUpChips.ifEmpty { quickRepliesFor(context) },
                pendingActions = reply.actions
            )
        }
    }

    fun submitFeedback(positive: Boolean) {
        val turn = lastFeedbackTurn ?: return
        repository.submitFeedback(
            userMessage = turn.userMessage,
            assistantReply = turn.assistantReply,
            context = turn.context,
            positive = positive
        )
        _uiState.value = _uiState.value.copy(
            feedbackState = if (positive) FeedbackState.POSITIVE else FeedbackState.NEGATIVE
        )
    }

    fun clearPendingActions() {
        if (_uiState.value.pendingActions.isEmpty()) return
        _uiState.value = _uiState.value.copy(pendingActions = emptyList())
    }

    fun quickRepliesFor(context: ContextBuilder.ChatContext): List<QuickReply> {
        val destination = context.destinationName
        val hasTrip = context.tripStops.isNotEmpty()
        return buildList {
            if (!destination.isNullOrBlank()) {
                add(QuickReply("Safety here", "How safe is $destination right now?"))
                add(QuickReply("When to go", "What is the best season for $destination?"))
                add(QuickReply("Why this", "Why is $destination a strong match for me right now?"))
            } else {
                add(QuickReply("Safe places", "Show me safe places to visit right now."))
                add(QuickReply("Rank places", "Why are these places ranked highest right now?"))
            }
            if (hasTrip) {
                add(QuickReply("Day plan", "Create a concise day-by-day itinerary for my saved trip."))
            } else {
                add(QuickReply("Trip budget", "Help me estimate my trip budget."))
            }
        }
    }
}
