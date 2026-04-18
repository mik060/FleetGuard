package com.example.fleet_guard.data

import com.google.firebase.firestore.PropertyName

data class ScheduleData(
    val id: String = "",
    val date: String = "",
    val route: String = "",
    val destination: String = "",
    val driver: String = "",
    val time: String = "",
    val vehicle: String = "Not Assigned",
    val reason: String = "",
    val userId: String = "",
    val adminId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isReached")
    @set:PropertyName("isReached")
    var isReached: Boolean = false,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
)
