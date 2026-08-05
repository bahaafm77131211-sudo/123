package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val phone: String,
    val passwordString: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey val phone: String,
    val passwordString: String,
    val name: String,
    val nationalDocs: String,
    val carDocs: String,
    val carType: String,
    val carModel: String,
    val carPhoto: String,
    val personalPhoto: String,
    val status: String, // "PENDING_APPROVAL", "APPROVED", "SUSPENDED", "BLOCKED"
    val subscriptionExpiryDate: Long, // timestamp
    val rating: Float = 4.8f,
    val createdAt: Long = System.currentTimeMillis()
)
