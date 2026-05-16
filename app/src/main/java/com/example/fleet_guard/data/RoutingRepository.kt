package com.example.fleet_guard.data

import android.util.Log
import com.example.fleet_guard.BuildConfig
import com.google.gson.JsonArray

/**
 * Repository responsible for fetching road routing data and location suggestions
 */
class RoutingRepository(private val apiService: OrsApiService) {

    /**
     * Fetches the road distance and duration between two points.
     * Checks both 'routes' (standard JSON) and 'features' (GeoJSON) for data.
     */
    suspend fun getRoadDistance(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): OrsSummary? {
        return try {
            val startCoords = "$startLng,$startLat"
            val endCoords = "$endLng,$endLat"

            Log.d("RoutingRepository", "Fetching route: Start($startCoords) End($endCoords)")

            val response = apiService.getDirections(
                apiKey = BuildConfig.ORS_API_KEY,
                start = startCoords,
                end = endCoords
            )

            // 1. Try to get summary from standard JSON structure
            val routeSummary = response.routes?.firstOrNull()?.summary
            if (routeSummary != null) return routeSummary

            // 2. Try to get summary from GeoJSON structure
            val featureSummary = response.features?.firstOrNull()?.properties?.summary
            if (featureSummary != null) return featureSummary

            // 3. If no summary found, check if server returned an error message
            if (response.error != null) {
                Log.e("RoutingRepository", "API Error: ${response.error.message}")
            } else {
                Log.e("RoutingRepository", "No route summary found in response")
            }
            null
        } catch (e: Exception) {
            Log.e("RoutingRepository", "Network or Parsing Error: ${e.message}")
            null
        }
    }

    /**
     * Fetches location suggestions as the user types
     */
    suspend fun getAutocompleteSuggestions(text: String): List<LocationSuggestion> {
        if (text.length < 3) return emptyList()

        return try {
            val response = apiService.autocomplete(
                apiKey = BuildConfig.ORS_API_KEY,
                text = text
            )

            response.features?.mapNotNull { feature ->
                val label = feature.properties?.label
                val geometry = feature.geometry
                
                if (label != null && geometry != null && geometry.coordinates.isJsonArray) {
                    val coordsArray = geometry.coordinates.asJsonArray
                    if (coordsArray.size() >= 2) {
                        LocationSuggestion(
                            label = label,
                            longitude = coordsArray[0].asDouble,
                            latitude = coordsArray[1].asDouble
                        )
                    } else null
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("RoutingRepository", "Autocomplete Error: ${e.message}")
            emptyList()
        }
    }
}
