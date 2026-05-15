package com.example.fleet_guard.data

/**
 * Abstraction & Interface: Defines the contract for fleet operations.
 * This demonstrates Abstraction and Encapsulation by hiding Firestore implementation details.
 */
interface FleetRepository {
    suspend fun updateVehicleStatus(vehicleId: String, status: String)
    suspend fun approveSchedule(scheduleId: String, vehicleId: String)
    suspend fun rejectSchedule(scheduleId: String)
    suspend fun completeTrip(tripId: String, vehicleId: String)
}
