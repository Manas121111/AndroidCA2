package com.smarttour360.app.ui.chatbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LocalTrainingExportStore(private val context: Context) {
    private val exportDir: File by lazy {
        File(context.filesDir, "training_exports").apply { mkdirs() }
    }

    fun appendTurn(
        userKey: String,
        userMessage: String,
        assistantReply: String,
        context: ContextBuilder.ChatContext
    ) {
        if (userMessage.isBlank() || assistantReply.isBlank()) return
        pendingFile(userKey).writeText(
            JSONObject().apply {
                put("messages", buildMessages(userMessage, assistantReply, context))
                put("timestamp", System.currentTimeMillis())
            }.toString()
        )
    }

    fun approveLatestTurn(userKey: String) {
        val pending = loadPending(userKey) ?: return
        approvedFile(userKey).appendText(pending.toString() + "\n")
        compactIfNeeded(approvedFile(userKey))
        pendingFile(userKey).delete()
    }

    fun rejectLatestTurn(userKey: String) {
        val pending = loadPending(userKey) ?: return
        rejectedFile(userKey).appendText(pending.toString() + "\n")
        compactIfNeeded(rejectedFile(userKey))
        pendingFile(userKey).delete()
    }

    private fun loadPending(userKey: String): JSONObject? {
        val file = pendingFile(userKey)
        if (!file.exists()) return null
        return runCatching { JSONObject(file.readText()) }.getOrNull()
    }

    private fun compactIfNeeded(file: File) {
        val lines = runCatching { file.readLines() }.getOrDefault(emptyList())
        if (lines.size <= 400) return
        file.writeText(lines.takeLast(400).joinToString("\n", postfix = "\n"))
    }

    private fun buildMessages(
        userMessage: String,
        assistantReply: String,
        context: ContextBuilder.ChatContext
    ): JSONArray {
        return JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", buildTrainingSystemPrompt(context)))
            put(JSONObject().put("role", "user").put("content", userMessage.trim()))
            put(JSONObject().put("role", "assistant").put("content", assistantReply.trim()))
        }
    }

    private fun buildTrainingSystemPrompt(context: ContextBuilder.ChatContext): String {
        return buildString {
            append("You are SmartTour360 Assistant for Indian travel inside the SmartTour360 app. ")
            append("Stay within destinations, trips, transport, stays, bookings, safety, eco score, and profile-aware planning. ")
            append("Current destination: ${context.destinationName ?: "none"}. ")
            append("Trip stops: ${context.tripStops.joinToString(" -> ").ifBlank { "none" }}. ")
            append("User profile: ${context.userProfileSummary.ifBlank { "none" }}.")
        }
    }

    private fun approvedFile(userKey: String): File = fileFor(userKey, "approved")

    private fun rejectedFile(userKey: String): File = fileFor(userKey, "rejected")

    private fun pendingFile(userKey: String): File = fileFor(userKey, "pending")

    private fun fileFor(userKey: String, suffix: String): File {
        val safeName = userKey.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(exportDir, "${safeName}_$suffix.jsonl".removeSuffix(".jsonl") + if (suffix == "pending") ".json" else ".jsonl")
    }
}
