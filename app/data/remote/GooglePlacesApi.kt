package com.smarttour360.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesApi {
    @GET("maps/api/place/textsearch/json")
    suspend fun textSearch(
        @Query("query") query: String,
        @Query("key") key: String
    ): GooglePlacesTextSearchResponse
}

data class GooglePlacesTextSearchResponse(
    val results: List<GooglePlacesTextSearchItem> = emptyList()
)

data class GooglePlacesTextSearchItem(
    val name: String? = null,
    val price_level: Int? = null
)
