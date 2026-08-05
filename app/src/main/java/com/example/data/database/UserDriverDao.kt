package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDriverDao {
    // User operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    // Driver operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DriverEntity): Long

    @Query("SELECT * FROM drivers WHERE phone = :phone LIMIT 1")
    suspend fun getDriverByPhone(phone: String): DriverEntity?

    @Query("SELECT * FROM drivers ORDER BY createdAt DESC")
    fun getAllDriversFlow(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers ORDER BY createdAt DESC")
    suspend fun getAllDriversList(): List<DriverEntity>

    @Update
    suspend fun updateDriver(driver: DriverEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE phone = :phone")
    suspend fun deleteUserByPhone(phone: String)

    @Query("DELETE FROM drivers WHERE phone = :phone")
    suspend fun deleteDriverByPhone(phone: String)

    @Query("UPDATE drivers SET status = :status WHERE phone = :phone")
    suspend fun updateDriverStatus(phone: String, status: String)

    @Query("UPDATE drivers SET subscriptionExpiryDate = :expiryDate WHERE phone = :phone")
    suspend fun updateDriverSubscription(phone: String, expiryDate: Long)
}
