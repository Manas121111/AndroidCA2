package com.smarttour360.app.ui.chatbot

import android.content.Context
import org.json.JSONArray
import com.smarttour360.app.ui.chatbot.brain.UserIntent

object EpisodicLogger {
    private const val PREFS = "assistant_episodes"
    private const val KEY_EPISODES = "episodes"
    private const val MAX_EPISODES = 20

    fun log(context: Context?, userMessage: String, intent: UserIntent, destination: String?) {
        if (context == null) return
        val entry = buildString {
            append(intent.name.lowercase().replace("_", " "))
            destination?.takeIf { it.isNotBlank() }?.let { append(" about $it") }
            if (entryWouldNeedTopic(userMessage)) {
                append(" - ")
                append(userMessage.trim().take(40))
            }
        }
        val episodes = getEpisodes(context).toMutableList()
        episodes.add(0, entry)
        saveEpisodes(context, episodes.take(MAX_EPISODES))
    }

    fun getRecentTopics(context: Context?): List<String> {
        if (context == null) return emptyList()
        return getEpisodes(context).take(5)
    }

    private fun getEpisodes(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EPISODES, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.optString(index))
                }
            }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun saveEpisodes(context: Context, episodes: List<String>) {
        val array = JSONArray()
        episodes.forEach(array::put)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EPISODES, array.toString())
            .apply()
    }

    private fun entryWouldNeedTopic(userMessage: String): Boolean {
        val trimmed = userMessage.trim()
        return trimmed.length > 8 && trimmed.any { it.isLetter() }
    }
}
