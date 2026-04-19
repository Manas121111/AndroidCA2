package com.smarttour360.app.ui.recommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.smarttour360.app.R
import com.smarttour360.app.data.TravelRepository
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.state.AssistantRecommendationSnapshot
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.utils.DestinationImagePrefetcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecommendationsFragment : Fragment() {
    private val repository = TravelRepository()
    private var searchJob: Job? = null
    private var refreshToken: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_recommendations, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val queryInput = view.findViewById<EditText>(R.id.input_recommendation_query)
        val status = view.findViewById<TextView>(R.id.text_recommendations_status)
        val navigator = activity as? AppNavigator
        var latestCatalog = repository.getIndianDestinationCatalog()
        val recommendationAdapter = RecommendationAdapter { destinationName ->
            val match = latestCatalog.firstOrNull {
                it.name.equals(destinationName, ignoreCase = true)
            } ?: return@RecommendationAdapter
            AppStateStore.selectDestination(match)
            navigator?.openDestinationDetail()
        }
        val exploreAdapter = ExploreDestinationAdapter {
            AppStateStore.addDestinationToTrip(it)
            Snackbar.make(view, "${it.name} added to Trip Planner", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<RecyclerView>(R.id.recycler_recommendations).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recommendationAdapter
            isNestedScrollingEnabled = false
        }
        view.findViewById<RecyclerView>(R.id.recycler_india_catalog).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exploreAdapter
            isNestedScrollingEnabled = false
        }

        fun loadRecommendations(forceRefresh: Boolean) = viewLifecycleOwner.lifecycleScope.launch {
            if (forceRefresh) refreshToken = System.currentTimeMillis()
            val query = queryInput.text?.toString().orEmpty()
            val prefs = AppStateStore.userPreferences.value
            val userKey = AppStateStore.activeUserKey.value.orEmpty()
            status.text = "Refreshing AI travel brain..."
            val suggestions = repository.getSuggestions(query, refreshToken, prefs, userKey)
            val catalog = runCatching {
                repository.getLiveIndianDestinationCatalog(
                    forceRefresh = forceRefresh,
                    query = query,
                    preferences = prefs
                )
            }.getOrElse {
                repository.getIndianDestinationCatalog(query, prefs)
            }
            latestCatalog = catalog
            AppStateStore.setLiveDestinations(catalog)
            DestinationImagePrefetcher.prefetch(
                requireContext(),
                (catalog.mapNotNull { it.imageUrl } + suggestions.mapNotNull { it.imageUrl })
            )
            AppStateStore.setAssistantRecommendations(
                suggestions.map {
                    AssistantRecommendationSnapshot(
                        destinationName = it.destinationName,
                        region = it.region,
                        safetyFlag = it.safetyFlag,
                        ecoScore = it.ecoScore,
                        rating = it.rating,
                        budgetLabel = it.budgetLabel,
                        rankReason = it.rankReason,
                        liveSignal = it.liveSignal,
                        routeHint = it.routeHint,
                        vibeTags = it.vibeTags
                    )
                }
            )
            recommendationAdapter.submitList(suggestions)
            exploreAdapter.submitList(catalog)
            status.text = if (suggestions.isEmpty()) {
                "No recommendation mix available right now."
            } else {
                "${suggestions.size} personalized picks ready for ${prefs.name.substringBefore(' ').ifBlank { "you" }} from ${prefs.country}."
            }
        }

        queryInput.doAfterTextChanged {
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                loadRecommendations(forceRefresh = true)
            }
        }

        view.findViewById<MaterialButton>(R.id.button_refresh_recommendations).setOnClickListener {
            searchJob?.cancel()
            searchJob = loadRecommendations(forceRefresh = true)
        }

        searchJob = loadRecommendations(forceRefresh = true)
    }
}
