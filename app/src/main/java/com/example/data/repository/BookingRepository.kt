package com.example.data.repository

import com.example.data.database.BookingDao
import com.example.data.database.BookingEntity
import com.example.data.database.UserDriverDao
import com.example.data.database.UserEntity
import com.example.data.database.DriverEntity
import kotlinx.coroutines.flow.Flow

class BookingRepository(
    private val bookingDao: BookingDao,
    private val userDriverDao: UserDriverDao
) {
    val allBookings: Flow<List<BookingEntity>> = bookingDao.getAllBookings()

    // User authentication/registration
    suspend fun insertUser(user: UserEntity): Long {
        return userDriverDao.insertUser(user)
    }

    suspend fun getUserByPhone(phone: String): UserEntity? {
        return userDriverDao.getUserByPhone(phone)
    }

    // Driver authentication/registration
    suspend fun insertDriver(driver: DriverEntity): Long {
        return userDriverDao.insertDriver(driver)
    }

    suspend fun getDriverByPhone(phone: String): DriverEntity? {
        return userDriverDao.getDriverByPhone(phone)
    }

    val allDrivers: Flow<List<DriverEntity>> = userDriverDao.getAllDriversFlow()

    suspend fun getAllDriversList(): List<DriverEntity> {
        return userDriverDao.getAllDriversList()
    }

    suspend fun updateDriver(driver: DriverEntity) {
        userDriverDao.updateDriver(driver)
    }

    suspend fun updateUser(user: UserEntity) {
        userDriverDao.updateUser(user)
    }

    suspend fun deleteUserByPhone(phone: String) {
        userDriverDao.deleteUserByPhone(phone)
    }

    suspend fun deleteDriverByPhone(phone: String) {
        userDriverDao.deleteDriverByPhone(phone)
    }

    suspend fun updateDriverStatus(phone: String, status: String) {
        userDriverDao.updateDriverStatus(phone, status)
    }

    suspend fun updateDriverSubscription(phone: String, expiryDate: Long) {
        userDriverDao.updateDriverSubscription(phone, expiryDate)
    }

    // Booking actions
    suspend fun getBookingById(id: Int): BookingEntity? {
        return bookingDao.getBookingById(id)
    }

    suspend fun insertOrUpdateBooking(booking: BookingEntity): Long {
        return bookingDao.insertOrUpdateBooking(booking)
    }

    suspend fun updateBookingStatus(id: Int, status: String) {
        bookingDao.updateBookingStatus(id, status)
    }

    suspend fun updateBookingRating(id: Int, rating: Int) {
        bookingDao.updateBookingRating(id, rating)
    }

    suspend fun deleteBookingById(id: Int) {
        bookingDao.deleteBookingById(id)
    }

    suspend fun getLatestActiveBooking(): BookingEntity? {
        return bookingDao.getLatestActiveBooking()
    }
}
