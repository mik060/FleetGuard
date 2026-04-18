package com.example.fleet_guard.data

data class Vehicle(
    val id: String = "",
    val model: String = "",
    val plateNumber: String = "",
    val status: String = "Available",
    val type: String = "Van",
    val latitude: Double = 14.5995, // Default to Manila
    val longitude: Double = 120.9842,
    val maintenanceReason: String? = null,
    val adminId: String? = null
)
