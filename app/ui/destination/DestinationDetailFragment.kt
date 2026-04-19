package com.smarttour360.app.ui.destination

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.smarttour360.app.R
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.common.HotelSummary
import com.smarttour360.app.ui.state.AppStateStore
import com.smarttour360.app.utils.BarometerReader
import com.smarttour360.app.utils.DestinationImageUrl
import coil.load
import coil.request.ErrorResult
import coil.request.SuccessResult
import com.smarttour360.app.utils.ShakeDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DestinationDetailFragment : Fragment() {

    private val viewModel: DestinationDetailViewModel by viewModels()
    private val forecastAdapter = ForecastAdapter()
    private val stayAdapter = StayAdapter()
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private var barometerJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_destination_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerForecast = view.findViewById<RecyclerView>(R.id.recycler_forecast)
        val recyclerStays = view.findViewById<RecyclerView>(R.id.recycler_stays)
        val buttonWhyFlag = view.findViewById<MaterialButton>(R.id.button_why_flag)
        val buttonAskAi = view.findViewById<MaterialButton>(R.id.button_ask_ai_destination)
        val chipVerification = view.findViewById<TextView>(R.id.chip_verification)
        val buttonBack = view.findViewById<ImageButton>(R.id.button_back)
        val buttonShare = view.findViewById<ImageButton>(R.id.button_share)
        val fabTrip = view.findViewById<FloatingActionButton>(R.id.fab_trip)
        val textSeeAll = view.findViewById<TextView>(R.id.text_see_all_stays)
        val sensorBadge = view.findViewById<View>(R.id.sensorBadge)
        val tvPressureValue = view.findViewById<TextView>(R.id.tvPressureValue)
        val tvSensorWarning = view.findViewById<TextView>(R.id.tvSensorWarning)
        val navigator = activity as? AppNavigator

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector {
            view.post {
                Snackbar.make(view, "🔄 Refreshing safety flags...", Snackbar.LENGTH_SHORT).show()
                viewModel.refreshSafetyData()
            }
        }

        recyclerForecast.layoutManager = LinearLayoutManager(requireContext())
        recyclerForecast.adapter = forecastAdapter

        recyclerStays.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerStays.adapter = stayAdapter
        stayAdapter.setOnClickListener {
            AppStateStore.selectHotel(
                HotelSummary(
                    id = it.name,
                    name = it.name,
                    subtitle = it.meta,
                    priceText = it.price.substringBefore("/"),
                    ecoScore = 78,
                    trend = "RISING",
                    hasHiddenFees = false,
                    ecoCertified = true
                )
            )
            navigator?.openHotelDetail()
        }

        buttonWhyFlag.setOnClickListener {
            val state = viewModel.uiState.value ?: return@setOnClickListener
            XaiExplanationBottomSheet.newInstance(
                title = "Why this ${state.flagLabel}?",
                riskScore = state.riskScore,
                explanation = state.explanation,
                dominantFactor = state.dominantFactor,
                structural = state.structuralScore,
                situational = state.situationalScore,
                environmental = state.environmentalScore,
                blockchainRef = state.blockchainRef
            ).show(childFragmentManager, "xai_sheet")
        }

        buttonAskAi.setOnClickListener {
            val state = viewModel.uiState.value ?: return@setOnClickListener
            val selectedDestination = AppStateStore.selectedDestination.value
            navigator?.openChatbot(
                destinationName = state.destinationName,
                safetyFlag = state.flagDisplay,
                flagExplanation = state.explanation,
                ecoScore = state.ecoScoreText.filter { it.isDigit() }.toIntOrNull(),
                ethicalScore = selectedDestination?.ethicalScore,
                bookingMode = null
            )
        }

        chipVerification.setOnClickListener {
            val state = viewModel.uiState.value ?: return@setOnClickListener
            BlockchainVerifyBottomSheet.newInstance(
                flag = state.flagLabel,
                blockchainRef = state.blockchainRef,
                timestamp = state.blockchainTimestamp
            ).show(childFragmentManager, "verify_sheet")
        }

        buttonBack.setOnClickListener {
            Toast.makeText(requireContext(), "Back navigation hook", Toast.LENGTH_SHORT).show()
        }

        buttonShare.setOnClickListener {
            Snackbar.make(view, "Share flow can connect next", Snackbar.LENGTH_SHORT).show()
        }

        fabTrip.setOnClickListener {
            AppStateStore.addCurrentDestinationToTrip()
            Snackbar.make(view, "Added to Trip Planner", Snackbar.LENGTH_SHORT).show()
        }

        textSeeAll.setOnClickListener {
            navigator?.openHotelList()
        }

        startBarometerReading(sensorBadge, tvPressureValue, tvSensorWarning)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val textTitle = view.findViewById<TextView>(R.id.text_title)
            val chipFlag = view.findViewById<TextView>(R.id.chip_flag)
            val textSafetyValue = view.findViewById<TextView>(R.id.text_safety_value)
            val textEcoValue = view.findViewById<TextView>(R.id.text_eco_value)
            val textCarbonValue = view.findViewById<TextView>(R.id.text_carbon_value)
            val textTemperature = view.findViewById<TextView>(R.id.text_temperature)
            val textWeatherSummary = view.findViewById<TextView>(R.id.text_weather_summary)
            val textUpdatedLabel = view.findViewById<TextView>(R.id.text_updated_label)
            val textReviewerInitials = view.findViewById<TextView>(R.id.text_reviewer_initials)
            val textReviewerName = view.findViewById<TextView>(R.id.text_reviewer_name)
            val textReviewerMeta = view.findViewById<TextView>(R.id.text_reviewer_meta)
            val textReviewBody = view.findViewById<TextView>(R.id.text_review_body)
            val weatherCard = view.findViewById<MaterialCardView>(R.id.text_weather_card)
            val heroImage = view.findViewById<ImageView>(R.id.image_destination_hero)
            val heroCredit = view.findViewById<TextView>(R.id.text_destination_hero_credit)

            textTitle.text = state.destinationName
            chipFlag.text = state.flagDisplay
            textSafetyValue.text = state.safetyText
            textEcoValue.text = state.ecoScoreText
            textCarbonValue.text = state.carbonText
            textTemperature.text = state.temperatureText
            textWeatherSummary.text = state.weatherSummary
            textUpdatedLabel.text = state.updatedText
            textReviewerInitials.text = state.reviewInitials
            textReviewerName.text = state.reviewerName
            textReviewerMeta.text = state.reviewerMeta
            textReviewBody.text = state.reviewBody

            chipFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), state.flagBackgroundColor)
            chipFlag.setTextColor(ContextCompat.getColor(requireContext(), state.flagTextColor))
            weatherCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            val imageUrl = state.heroImageUrl ?: DestinationImageUrl.forDestination(state.destinationName.substringBefore(","))
            Log.d("SmartTourImage", "Detail hero load url=$imageUrl place=${state.destinationName}")
            heroImage.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_hero)
                error(R.drawable.bg_hero)
                listener(
                    onSuccess = { _, result: SuccessResult ->
                        Log.d("SmartTourImage", "Detail hero success place=${state.destinationName} source=${result.dataSource}")
                    },
                    onError = { _, result: ErrorResult ->
                        Log.e("SmartTourImage", "Detail hero failed place=${state.destinationName} url=$imageUrl throwable=${result.throwable}")
                    }
                )
            }
            heroCredit.text = state.heroImageAttribution
            heroCredit.isVisible = !state.heroImageAttribution.isNullOrBlank()

            forecastAdapter.submitList(state.forecast)
            stayAdapter.submitList(state.stays)
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::shakeDetector.isInitialized) {
            shakeDetector.start(sensorManager)
        }
    }

    override fun onPause() {
        if (this::shakeDetector.isInitialized) {
            shakeDetector.stop(sensorManager)
        }
        super.onPause()
    }

    override fun onDestroyView() {
        barometerJob?.cancel()
        super.onDestroyView()
    }

    private fun startBarometerReading(
        sensorBadge: View,
        pressureValue: TextView,
        sensorWarning: TextView
    ) {
        barometerJob?.cancel()
        barometerJob = viewLifecycleOwner.lifecycleScope.launch {
            BarometerReader.readPressure(requireContext()).collect { reading ->
                if (reading == null) {
                    sensorBadge.isVisible = false
                    AppStateStore.barometerAvailable = false
                    AppStateStore.localPressureLabel = ""
                    viewModel.refreshSafetyData()
                    return@collect
                }

                AppStateStore.localPressureHpa = reading.pressureHpa
                AppStateStore.localPressureRisk = reading.riskContribution
                AppStateStore.localPressureLabel = reading.label
                AppStateStore.barometerAvailable = true

                sensorBadge.isVisible = true
                pressureValue.text = "📡 ${reading.pressureHpa.toInt()} hPa · ${reading.label}"
                if (reading.riskContribution >= 0.5f) {
                    sensorWarning.isVisible = true
                    sensorWarning.text = "⚠️ Live device sensor detects ${reading.label.lowercase()}"
                } else {
                    sensorWarning.isVisible = false
                }

                viewModel.refreshSafetyData()
            }
        }
    }

    companion object {
        fun newInstance() = DestinationDetailFragment()
    }
}
