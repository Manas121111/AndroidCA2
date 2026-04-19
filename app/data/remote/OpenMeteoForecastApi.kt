package com.smarttour360.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 4
    ): ForecastResponse
}

data class ForecastResponse(
    val current: CurrentForecast?,
    val daily: DailyForecast?
)

data class CurrentForecast(
    val temperature_2m: Double,
    val weather_code: Int,
    val wind_speed_10m: Double
)

data class DailyForecast(
    val time: List<String>,
    val weather_code: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>
)
