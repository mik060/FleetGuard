package com.example.fleet_guard.data

/**
 * Abstraction: Base class for all entities stored in Firestore.
 * This demonstrates Inheritance and Abstraction.
 */
abstract class FirestoreEntity {
    abstract val id: String
    abstract val timestamp: Long
}
