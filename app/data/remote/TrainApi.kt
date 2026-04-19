package com.smarttour360.app.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface TrainApi {
    @GET("rail/getTrains.aspx")
    suspend fun getTrainsBetweenStations(
        @Query("Station_From") fromCode: String,
        @Query("Station_To") toCode: String,
        @Query("DataType") dataType: Int = 1,
        @Query("srid") searchRouteId: Int = 1,
        @Query("MedType") medType: Int = 1
    ): ResponseBody

    @GET("js/cmp/stations.js?v=092f8")
    suspend fun getStationsCatalog(): ResponseBody

    @GET
    suspend fun getTrainRoutePage(
        @Url url: String
    ): ResponseBody
}
