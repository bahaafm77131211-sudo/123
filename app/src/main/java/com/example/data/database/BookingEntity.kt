package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pickupLocation: String,
    val destinationLocation: String,
    val pickupLat: Float,
    val pickupLng: Float,
    val destLat: Float,
    val destLng: Float,
    val priceEst: Double,
    val distanceKm: Double,
    val durationMin: Int,
    val driverName: String,
    val driverVehicle: String,
    val driverPhone: String,
    val driverRating: Float,
    val status: String, // PENDING, EN_ROUTE, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED
    val timestamp: Long = System.currentTimeMillis(),
    val userRating: Int = 0, // 0 means unrated
    val userName: String = "زبون كوثى",
    val userPhone: String = "07801234567"
)
