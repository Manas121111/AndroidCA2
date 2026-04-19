package com.smarttour360.app.ui

import com.smarttour360.app.ui.train.TrainResult

interface AppNavigator {
    fun openOnboarding()
    fun openLogin()
    fun enterApp(tab: MainTab = MainTab.HOME)
    fun openDestinationDetail()
    fun openHotelList()
    fun openHotelDetail()
    fun openCart()
    fun openOrderConfirmation()
    fun openBookings()
    fun openRecommendations()
    fun openChatbot(
        destinationName: String? = null,
        safetyFlag: String? = null,
        flagExplanation: String? = null,
        ecoScore: Int? = null,
        ethicalScore: String? = null,
        bookingMode: String? = null
    )
    fun openTrainDetail(train: TrainResult)
    fun goBack()
}

enum class MainTab {
    HOME,
    SEARCH,
    RECOMMENDATIONS,
    TRIPS,
    PROFILE
}
