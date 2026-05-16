package com.example.fleet_guard.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OrsApiService {
    @GET("v2/directions/driving-car")
    suspend fun getDirections(
        @Query("api_key") apiKey: String,
        @Query("start") start: String, // format: "lng,lat"
        @Query("end") end: String      // format: "lng,lat"
    ): OrsResponse

    @GET("geocode/autocomplete")
    suspend fun autocomplete(
        @Query("api_key") apiKey: String,
        @Query("text") text: String,
        @Query("boundary.country") country: String = "PH", // Restricted to Philippines
        @Query("size") size: Int = 5
    ): OrsResponse

    companion object {
        private const val BASE_URL = "https://api.openrouteservice.org/"

        fun create(): OrsApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OrsApiService::class.java)
        }
    }
}
