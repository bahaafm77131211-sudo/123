package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class NotificationType {
    RIDE_REQUESTED, // طلب رحلة جديد
    RIDE_ACCEPTED,  // قبول الرحلة
    APPROACHING,    // اقتراب التكسي من موقع الالتقاء
    ARRIVED,        // وصول التكسي
    IN_PROGRESS,    // بدء الرحلة
    CANCELLED,      // إلغاء الرحلة
    COMPLETED,      // اكتمال الرحلة
    INFO
}

data class InAppNotification(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val timestamp: Long = System.currentTimeMillis()
)
