package com.smarttour360.app.ui.chatbot

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import com.smarttour360.app.R
import com.smarttour360.app.data.TravelRepository
import com.smarttour360.app.dto.ChatRole
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.MainTab
import com.smarttour360.app.ui.chatbot.brain.BotAction
import com.smarttour360.app.ui.chatbot.brain.BotActionType
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.ui.state.PendingTrainSearch
import kotlinx.coroutines.launch
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

class ChatbotFragment : Fragment() {
    private val viewModel: ChatbotViewModel by viewModels()
    private val travelRepository = TravelRepository()
    private var voiceManager: VoiceManager? = null
    private lateinit var chatContext: ContextBuilder.ChatContext
    private var lastSpokenBotMessage: String? = null
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val root = view ?: return@registerForActivityResult
        if (granted) {
            setupVoice(root)
        } else {
            root.findViewById<ImageButton>(R.id.btnMic)?.isVisible = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatContext = ContextBuilder.fromCurrentState(
            destinationName = arguments?.getString(ARG_DESTINATION),
            safetyFlag = arguments?.getString(ARG_FLAG),
            flagExplanation = arguments?.getString(ARG_FLAG_EXPLANATION),
            ecoScore = arguments?.getInt(ARG_ECO_SCORE)?.takeIf { it > 0 },
            ethicalScore = arguments?.getString(ARG_ETHICAL_SCORE),
            bookingMode = arguments?.getString(ARG_BOOKING_MODE)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_chatbot, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val messagesAdapter = ChatbotAdapter()
        val quickReplyAdapter = QuickReplyAdapter { reply ->
            sendPrompt(reply.prompt, view)
        }
        val messageInput = view.findViewById<EditText>(R.id.etMessage)
        val micButton = view.findViewById<ImageButton>(R.id.btnMic)
        val sendButton = view.findViewById<MaterialButton>(R.id.btnSend)
        val feedbackContainer = view.findViewById<View>(R.id.feedbackContainer)
        val feedbackLabel = view.findViewById<TextView>(R.id.text_feedback_label)
        val positiveButton = view.findViewById<MaterialButton>(R.id.button_feedback_positive)
        val negativeButton = view.findViewById<MaterialButton>(R.id.button_feedback_negative)

        view.findViewById<RecyclerView>(R.id.recycler_chat_messages).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messagesAdapter
        }
        view.findViewById<RecyclerView>(R.id.recycler_quick_replies).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = quickReplyAdapter
        }

        sendButton.setOnClickListener {
            sendCurrentInput(messageInput, view)
        }
        positiveButton.setOnClickListener {
            viewModel.submitFeedback(true)
        }
        negativeButton.setOnClickListener {
            viewModel.submitFeedback(false)
        }
        messageInput.setOnEditorActionListener { _, actionId, event ->
            val shouldSend = actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (shouldSend) {
                sendCurrentInput(messageInput, view)
                true
            } else {
                false
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            setupVoice(view)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        micButton.setOnClickListener {
            voiceManager?.startListening()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                messagesAdapter.submitList(state.messages)
                quickReplyAdapter.submitList(state.quickReplies.ifEmpty { viewModel.quickRepliesFor(chatContext) })
                sendButton.isEnabled = !state.isSending
                micButton.isEnabled = !state.isSending
                feedbackContainer.isVisible = state.showFeedback && !state.isSending
                when (state.feedbackState) {
                    FeedbackState.NONE -> {
                        feedbackLabel.text = "Was this reply helpful?"
                        positiveButton.isEnabled = true
                        negativeButton.isEnabled = true
                    }
                    FeedbackState.POSITIVE -> {
                        feedbackLabel.text = "Thanks. I will reinforce replies like this."
                        positiveButton.isEnabled = false
                        negativeButton.isEnabled = true
                    }
                    FeedbackState.NEGATIVE -> {
                        feedbackLabel.text = "Noted. I will suppress replies like this."
                        positiveButton.isEnabled = true
                        negativeButton.isEnabled = false
                    }
                }
                if (state.pendingActions.isNotEmpty()) {
                    executeActions(state.pendingActions)
                    viewModel.clearPendingActions()
                }
                val recycler = view.findViewById<RecyclerView>(R.id.recycler_chat_messages)
                recycler.post { recycler.scrollToPosition((state.messages.size - 1).coerceAtLeast(0)) }
                val last = state.messages.lastOrNull()
                if (last?.role == ChatRole.BOT && !last.isLoading) {
                    if (last.content != lastSpokenBotMessage) {
                        lastSpokenBotMessage = last.content
                        voiceManager?.speak(last.content)
                    } else {
                        voiceManager?.finishProcessingWithoutSpeech()
                    }
                } else if (!state.isSending) {
                    voiceManager?.finishProcessingWithoutSpeech()
                }
            }
        }
    }

    private fun setupVoice(root: View) {
        val messageInput = root.findViewById<EditText>(R.id.etMessage)
        voiceManager = VoiceManager(
            context = requireContext(),
            onSpeechResult = { recognisedText ->
                messageInput.setText(recognisedText)
                messageInput.postDelayed({
                    sendPrompt(recognisedText, root)
                }, 400)
            },
            onStateChange = { state -> updateMicUi(root, state) }
        )
    }

    private fun updateMicUi(root: View, state: VoiceManager.VoiceState) {
        val micButton = root.findViewById<ImageButton>(R.id.btnMic)
        val micPulse = root.findViewById<View>(R.id.micPulse)
        requireActivity().runOnUiThread {
            when (state) {
                VoiceManager.VoiceState.IDLE -> {
                    micButton.setImageResource(R.drawable.ic_mic)
                    micButton.imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.text_secondary))
                    micPulse.isVisible = false
                }
                VoiceManager.VoiceState.LISTENING -> {
                    micButton.setImageResource(R.drawable.ic_mic)
                    micButton.imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.primary))
                    micPulse.isVisible = true
                }
                VoiceManager.VoiceState.PROCESSING -> {
                    micButton.setImageResource(R.drawable.ic_send)
                    micPulse.isVisible = false
                }
                VoiceManager.VoiceState.SPEAKING -> {
                    micButton.setImageResource(R.drawable.ic_volume_up)
                    micButton.imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.primary))
                    micPulse.isVisible = false
                }
            }
        }
    }

    override fun onDestroyView() {
        voiceManager?.destroy()
        voiceManager = null
        super.onDestroyView()
    }

    private fun sendCurrentInput(messageInput: EditText, root: View) {
        val text = messageInput.text?.toString().orEmpty()
        sendPrompt(text, root)
        messageInput.text?.clear()
    }

    private fun sendPrompt(text: String, root: View) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        chatContext = ContextBuilder.fromCurrentState(
            destinationName = arguments?.getString(ARG_DESTINATION),
            safetyFlag = arguments?.getString(ARG_FLAG),
            flagExplanation = arguments?.getString(ARG_FLAG_EXPLANATION),
            ecoScore = arguments?.getInt(ARG_ECO_SCORE)?.takeIf { it > 0 },
            ethicalScore = arguments?.getString(ARG_ETHICAL_SCORE),
            bookingMode = arguments?.getString(ARG_BOOKING_MODE)
        )
        val itineraryMode = trimmed.contains("day-by-day", true) ||
            trimmed.contains("itinerary", true) ||
            trimmed.contains("day plan", true)
        viewModel.sendMessage(trimmed, chatContext, itineraryMode)
        root.findViewById<EditText>(R.id.etMessage).text?.clear()
    }

    private fun executeActions(actions: List<BotAction>) {
        val navigator = activity as? AppNavigator ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            actions.forEach { action ->
                when (action.type) {
                    BotActionType.OPEN_RECOMMENDATIONS -> navigator.openRecommendations()
                    BotActionType.OPEN_BOOKINGS -> navigator.openBookings()
                    BotActionType.OPEN_HOTELS -> {
                        resolveDestinationTarget(action.target)?.let(AppStateStore::selectDestination)
                        navigator.openHotelList()
                    }
                    BotActionType.OPEN_TRIPS -> navigator.enterApp(MainTab.TRIPS)
                    BotActionType.OPEN_BOOKING_PORTAL -> navigator.enterApp(MainTab.SEARCH)
                    BotActionType.SHOW_TRAINS -> {
                        val request = parseTrainSearchTarget(action.target)
                            ?: buildDefaultTrainSearchTarget()
                        if (request != null) {
                            AppStateStore.queueTrainSearch(
                                fromQuery = request.fromQuery,
                                toQuery = request.toQuery,
                                travelDate = request.travelDate
                            )
                        }
                        navigator.enterApp(MainTab.SEARCH)
                    }
                    BotActionType.ADD_TO_TRIP -> {
                        val selected = resolveDestinationTarget(action.target)
                        if (selected != null) {
                            AppStateStore.selectDestination(selected)
                            AppStateStore.addDestinationToTrip(selected)
                            Toast.makeText(requireContext(), "${selected.name} added to Trip Planner", Toast.LENGTH_SHORT).show()
                        }
                    }
                    BotActionType.OPEN_DESTINATION_DETAIL -> {
                        val selected = resolveDestinationTarget(action.target)
                        if (selected != null) {
                            AppStateStore.selectDestination(selected)
                            navigator.openDestinationDetail()
                        }
                    }
                    BotActionType.OPEN_CART -> navigator.openCart()
                    BotActionType.NONE -> Unit
                }
            }
        }
    }

    private suspend fun resolveDestinationTarget(target: String?): com.smarttour360.app.ui.common.DestinationSummary? {
        val query = target?.trim().orEmpty()
        val selected = AppStateStore.selectedDestination.value
        if (query.isBlank()) {
            return selected
        }
        if (selected != null && matchesDestination(query, selected)) {
            return selected
        }

        val preferences = AppStateStore.userPreferences.value
        val candidates = mutableListOf<com.smarttour360.app.ui.common.DestinationSummary>()
        candidates += travelRepository.getIndianDestinationCatalog(query = query, preferences = preferences)
        candidates += travelRepository.getLiveIndianDestinationCatalog(
            forceRefresh = false,
            query = query,
            preferences = preferences
        )
        if (candidates.isEmpty()) {
            candidates += travelRepository.searchDestinations(query)
        }

        return candidates
            .distinctBy { "${it.name}-${it.subtitle}" }
            .maxByOrNull { scoreDestinationMatch(query, it) }
    }

    private fun matchesDestination(
        query: String,
        destination: com.smarttour360.app.ui.common.DestinationSummary
    ): Boolean {
        val normalized = query.lowercase(Locale.getDefault())
        return destination.name.lowercase(Locale.getDefault()).contains(normalized) ||
            destination.subtitle.lowercase(Locale.getDefault()).contains(normalized)
    }

    private fun scoreDestinationMatch(
        query: String,
        destination: com.smarttour360.app.ui.common.DestinationSummary
    ): Int {
        val normalized = query.lowercase(Locale.getDefault())
        val name = destination.name.lowercase(Locale.getDefault())
        val subtitle = destination.subtitle.lowercase(Locale.getDefault())
        return when {
            name == normalized -> 120
            subtitle == normalized -> 110
            name.startsWith(normalized) -> 100
            subtitle.startsWith(normalized) -> 90
            name.contains(normalized) -> 80
            subtitle.contains(normalized) -> 70
            else -> 0
        }
    }

    private fun buildDefaultTrainSearchTarget(): PendingTrainSearch? {
        val selectedDestination = AppStateStore.selectedDestination.value?.name
        val homeCity = AppStateStore.userPreferences.value.homeCity
        if (homeCity.isBlank() || selectedDestination.isNullOrBlank()) return null
        return PendingTrainSearch(fromQuery = homeCity, toQuery = selectedDestination)
    }

    private fun parseTrainSearchTarget(target: String?): PendingTrainSearch? {
        val raw = target?.trim()?.trimEnd('.', '!', '?').orEmpty()
        if (raw.isBlank()) return null

        val parts = Regex("\\s+on\\s+", RegexOption.IGNORE_CASE).split(raw, limit = 2)
        val routePart = parts.firstOrNull().orEmpty()
            .removePrefix("from ")
            .trim()
        val date = parts.getOrNull(1)?.let(::normalizeTrainDate)
        val match = Regex("(.+?)\\s+(?:to|->|–|-)\\s+(.+)", RegexOption.IGNORE_CASE).matchEntire(routePart)
            ?: return null
        val from = match.groupValues[1].trim().trim(',', '.')
        val to = match.groupValues[2].trim().trim(',', '.')
        if (from.isBlank() || to.isBlank()) return null
        return PendingTrainSearch(fromQuery = from, toQuery = to, travelDate = date)
    }

    private fun normalizeTrainDate(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.matches(Regex("\\d{8}"))) return trimmed

        val formats = listOf(
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "dd MMM yyyy",
            "d MMM yyyy",
            "dd MMMM yyyy",
            "d MMMM yyyy"
        )
        val output = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        formats.forEach { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
            val parsed = parser.parse(trimmed, ParsePosition(0))
            if (parsed != null) {
                return output.format(parsed)
            }
        }
        return null
    }

    companion object {
        private const val ARG_DESTINATION = "destination"
        private const val ARG_FLAG = "flag"
        private const val ARG_FLAG_EXPLANATION = "flag_explanation"
        private const val ARG_ECO_SCORE = "eco_score"
        private const val ARG_ETHICAL_SCORE = "ethical_score"
        private const val ARG_BOOKING_MODE = "booking_mode"

        fun newInstance(
            destinationName: String? = null,
            safetyFlag: String? = null,
            flagExplanation: String? = null,
            ecoScore: Int? = null,
            ethicalScore: String? = null,
            bookingMode: String? = null
        ): ChatbotFragment {
            return ChatbotFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DESTINATION, destinationName)
                    putString(ARG_FLAG, safetyFlag)
                    putString(ARG_FLAG_EXPLANATION, flagExplanation)
                    putInt(ARG_ECO_SCORE, ecoScore ?: 0)
                    putString(ARG_ETHICAL_SCORE, ethicalScore)
                    putString(ARG_BOOKING_MODE, bookingMode)
                }
            }
        }
    }
}
