package com.example.fleet_guard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE (email = :identifier OR fullName = :identifier) AND password = :password")
    suspend fun login(identifier: String, password: String): User?

    @Query("SELECT * FROM users WHERE email = :email OR fullName = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE adminId = :adminId AND isAdmin = 1")
    suspend fun getAdminById(adminId: String): User?

    @Query("SELECT * FROM users WHERE adminId = :adminId AND connectionStatus = 'PENDING'")
    fun getPendingUsersForAdmin(adminId: String): Flow<List<User>>

    @Query("UPDATE users SET connectionStatus = :status WHERE id = :userId")
    suspend fun updateConnectionStatus(userId: Int, status: String)

    @Query("SELECT * FROM users WHERE isAdmin = 1 AND (fullName LIKE '%' || :query || '%' OR adminId LIKE '%' || :query || '%')")
    suspend fun searchAdmins(query: String): List<User>
}
