package com.smarttour360.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface TicketmasterApi {
    @GET("discovery/v2/events.json")
    suspend fun events(
        @Query("apikey") apiKey: String,
        @Query("keyword") keyword: String,
        @Query("size") size: Int = 8,
        @Query("sort") sort: String = "date,asc"
    ): TicketmasterResponse
}

data class TicketmasterResponse(
    val _embedded: EmbeddedEvents?
)

data class EmbeddedEvents(
    val events: List<TicketmasterEvent>?
)

data class TicketmasterEvent(
    val id: String,
    val name: String,
    val dates: EventDates?,
    val _embedded: EventEmbedded?
)

data class EventDates(
    val start: EventStart?
)

data class EventStart(
    val localDate: String?,
    val localTime: String?
)

data class EventEmbedded(
    val venues: List<EventVenue>?
)

data class EventVenue(
    val name: String?,
    val city: EventCity?,
    val country: EventCountry?
)

data class EventCity(
    val name: String?
)

data class EventCountry(
    val name: String?
)
