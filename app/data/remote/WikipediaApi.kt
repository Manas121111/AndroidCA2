package com.smarttour360.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface WikipediaApi {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun pageSummary(
        @Path("title", encoded = true) title: String
    ): WikipediaSummaryResponse
}

data class WikipediaSummaryResponse(
    val title: String?,
    val thumbnail: WikipediaThumbnail?,
    val originalimage: WikipediaThumbnail?
)

data class WikipediaThumbnail(
    val source: String?
)
