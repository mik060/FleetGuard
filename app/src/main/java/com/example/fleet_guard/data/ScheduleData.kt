package com.example.fleet_guard.data

import com.google.firebase.firestore.PropertyName

data class ScheduleData(
    override val id: String = "",
    val date: String = "",
    val route: String = "",
    val destination: String = "",
    val driver: String = "",
    val time: String = "",
    val vehicle: String = "Not Assigned",
    val reason: String = "",
    val startLat: Double = 14.5995,
    val startLng: Double = 120.9842,
    val destLat: Double = 14.6091,
    val destLng: Double = 121.0223,
    val estimatedTime: String = "",
    val userId: String = "",
    val adminId: String? = null,
    override val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isReached")
    @set:PropertyName("isReached")
    var isReached: Boolean = false,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
) : FirestoreEntity()
