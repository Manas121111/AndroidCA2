package com.smarttour360.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface AviationstackApi {
    @GET("v1/flights")
    suspend fun searchFlights(
        @Query("access_key") accessKey: String,
        @Query("dep_iata") departureIata: String,
        @Query("arr_iata") arrivalIata: String,
        @Query("flight_date") flightDate: String
    ): AviationstackFlightResponse
}

data class AviationstackFlightResponse(
    val data: List<AviationstackFlightItem> = emptyList()
)

data class AviationstackFlightItem(
    val flight_status: String? = null,
    val departure: AviationstackAirportInfo? = null,
    val arrival: AviationstackAirportInfo? = null,
    val airline: AviationstackAirlineInfo? = null,
    val flight: AviationstackFlightCodeInfo? = null
)

data class AviationstackAirportInfo(
    val airport: String? = null,
    val iata: String? = null,
    val scheduled: String? = null
)

data class AviationstackAirlineInfo(
    val name: String? = null
)

data class AviationstackFlightCodeInfo(
    val iata: String? = null,
    val number: String? = null
)
