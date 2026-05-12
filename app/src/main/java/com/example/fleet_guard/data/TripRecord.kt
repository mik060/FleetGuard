package com.example.fleet_guard.data

data class TripRecord(
    val id: String = "",
    val driver: String = "",
    val route: String = "",
    val destination: String = "",
    val mileage: String = "",
    val date: String = "",
    val vehicle: String = "Unknown",
    val startLat: Double = 14.5995,
    val startLng: Double = 120.9842,
    val destLat: Double = 14.6091,
    val destLng: Double = 121.0223,
    val status: String = "Returning", // "Returning", "Returned"
    val userId: String = "",
    val adminId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
