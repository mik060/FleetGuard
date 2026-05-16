package com.example.fleet_guard.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class OrsResponse(
    @SerializedName("routes")
    val routes: List<OrsRoute>? = null,
    @SerializedName("features")
    val features: List<OrsFeature>? = null,
    @SerializedName("error")
    val error: OrsError? = null
)

data class OrsRoute(
    @SerializedName("summary")
    val summary: OrsSummary? = null
)

data class OrsFeature(
    @SerializedName("geometry")
    val geometry: OrsGeometry? = null,
    @SerializedName("properties")
    val properties: OrsProperties? = null
)

data class OrsGeometry(
    @SerializedName("coordinates")
    val coordinates: JsonElement
)

data class OrsProperties(
    @SerializedName("summary")
    val summary: OrsSummary? = null,
    @SerializedName("label")
    val label: String? = null
)

data class OrsSummary(
    @SerializedName("distance")
    val distance: Double,
    @SerializedName("duration")
    val duration: Double
)

data class OrsError(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("code")
    val code: Int? = null
)

data class LocationSuggestion(
    val label: String,
    val latitude: Double,
    val longitude: Double
)
