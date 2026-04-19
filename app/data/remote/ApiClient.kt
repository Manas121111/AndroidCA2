package com.smarttour360.app.data.remote

import com.smarttour360.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "SmartTour360-Android/1.0")
                .header("Accept-Language", "en-IN,en;q=0.9")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val geocodingApi: OpenMeteoGeocodingApi by lazy {
        retrofit(BuildConfig.OPEN_METEO_GEOCODING_BASE_URL).create(OpenMeteoGeocodingApi::class.java)
    }

    val forecastApi: OpenMeteoForecastApi by lazy {
        retrofit(BuildConfig.OPEN_METEO_FORECAST_BASE_URL).create(OpenMeteoForecastApi::class.java)
    }

    val ticketmasterApi: TicketmasterApi by lazy {
        retrofit(BuildConfig.TICKETMASTER_BASE_URL).create(TicketmasterApi::class.java)
    }

    val wikimediaCommonsApi: WikimediaCommonsApi by lazy {
        retrofit(BuildConfig.WIKIMEDIA_COMMONS_BASE_URL).create(WikimediaCommonsApi::class.java)
    }

    val wikipediaApi: WikipediaApi by lazy {
        retrofit(BuildConfig.WIKIPEDIA_BASE_URL).create(WikipediaApi::class.java)
    }

    val groqApi: GroqApi by lazy {
        retrofit(BuildConfig.GROQ_BASE_URL).create(GroqApi::class.java)
    }

    val trainApi: TrainApi by lazy {
        retrofit("https://erail.in/").create(TrainApi::class.java)
    }

    val overpassApi: OverpassApi by lazy {
        retrofit(BuildConfig.OVERPASS_BASE_URL).create(OverpassApi::class.java)
    }

    val aviationstackApi: AviationstackApi by lazy {
        retrofit(BuildConfig.AVIATIONSTACK_BASE_URL).create(AviationstackApi::class.java)
    }

    val googlePlacesApi: GooglePlacesApi by lazy {
        retrofit(BuildConfig.GOOGLE_PLACES_BASE_URL).create(GooglePlacesApi::class.java)
    }

}
