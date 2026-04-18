package com.example.fleet_guard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val password: String,
    val isAdmin: Boolean = false,
    val adminId: String? = null,
    val connectionStatus: String? = null // null, "PENDING", "ACCEPTED"
)
