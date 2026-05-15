package com.example.fleet_guard.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Encapsulation & Polymorphism: Specific implementation of FleetRepository using Firebase Firestore.
 */
class FirestoreRepositoryImpl(private val firestore: FirebaseFirestore) : FleetRepository {

    override suspend fun updateVehicleStatus(vehicleId: String, status: String) {
        firestore.collection("vehicles").document(vehicleId)
            .update("status", status)
            .await()
    }

    override suspend fun approveSchedule(scheduleId: String, vehicleId: String) {
        firestore.collection("schedules").document(scheduleId)
            .update("status", "APPROVED")
            .await()
        updateVehicleStatus(vehicleId, "In Use")
    }

    override suspend fun rejectSchedule(scheduleId: String) {
        firestore.collection("schedules").document(scheduleId)
            .update("status", "REJECTED")
            .await()
    }

    override suspend fun completeTrip(tripId: String, vehicleId: String) {
        firestore.collection("trips").document(tripId)
            .update(
                "status", "Returned",
                "returnTimestamp", System.currentTimeMillis()
            ).await()
        updateVehicleStatus(vehicleId, "Available")
    }
}
