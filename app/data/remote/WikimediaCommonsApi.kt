package com.smarttour360.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface WikimediaCommonsApi {
    @GET("w/api.php")
    suspend fun searchFiles(
        @Query("action") action: String = "query",
        @Query("format") format: String = "json",
        @Query("list") list: String = "search",
        @Query("srsearch") search: String,
        @Query("srnamespace") namespace: Int = 6,
        @Query("srlimit") limit: Int = 5
    ): CommonsSearchResponse

    @GET("w/api.php")
    suspend fun imageInfo(
        @Query("action") action: String = "query",
        @Query("format") format: String = "json",
        @Query("prop") prop: String = "imageinfo",
        @Query("titles") titles: String,
        @Query("iiprop") imageInfoProps: String = "url|user|extmetadata",
        @Query("iiurlwidth") imageWidth: Int = 900
    ): CommonsImageInfoResponse
}

data class CommonsSearchResponse(
    val query: CommonsSearchQuery?
)

data class CommonsSearchQuery(
    val search: List<CommonsSearchHit> = emptyList()
)

data class CommonsSearchHit(
    val title: String
)

data class CommonsImageInfoResponse(
    val query: CommonsImageInfoQuery?
)

data class CommonsImageInfoQuery(
    val pages: Map<String, CommonsImagePage> = emptyMap()
)

data class CommonsImagePage(
    val title: String?,
    @SerializedName("imageinfo")
    val imageInfo: List<CommonsImageInfo> = emptyList()
)

data class CommonsImageInfo(
    val url: String?,
    val thumburl: String?,
    val user: String?,
    @SerializedName("extmetadata")
    val extMetadata: CommonsExtMetadata?
)

data class CommonsExtMetadata(
    @SerializedName("Artist")
    val artist: CommonsMetadataValue? = null,
    @SerializedName("LicenseShortName")
    val licenseShortName: CommonsMetadataValue? = null,
    @SerializedName("Credit")
    val credit: CommonsMetadataValue? = null
)

data class CommonsMetadataValue(
    val value: String?
)
