package com.smarttour360.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
    @GET("api/interpreter")
    suspend fun query(
        @Query("data") data: String
    ): OverpassResponse
}

data class OverpassResponse(
    val elements: List<OverpassElement>?
)

data class OverpassElement(
    val id: Long,
    val type: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
)

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)
