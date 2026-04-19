package com.smarttour360.app.utils

object DestinationImageUrl {
    fun forDestination(name: String): String {
        val query = "${name.trim().lowercase().replace(" ", "+")}+india+travel+landscape"
        return "https://source.unsplash.com/800x600/?$query"
    }

    fun forHotel(hotelName: String, cityName: String): String {
        val query = "${hotelName.trim().lowercase().replace(" ", "+")}+${cityName.trim().lowercase().replace(" ", "+")}+hotel"
        return "https://source.unsplash.com/800x400/?$query"
    }
}
