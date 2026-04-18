package com.example.fleet_guard.data

data class TripRecord(
    val id: String = "",
    val driver: String = "",
    val route: String = "",
    val destination: String = "",
    val mileage: String = "",
    val date: String = "",
    val vehicle: String = "Unknown",
    val status: String = "Returning", // "Returning", "Returned"
    val userId: String = "",
    val adminId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
