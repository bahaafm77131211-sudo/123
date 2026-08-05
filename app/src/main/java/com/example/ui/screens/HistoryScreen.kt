package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.BookingEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    bookings: List<BookingEntity>,
    onRateBooking: (Int, Int) -> Unit
) {
    // Computations
    val completedBookings = bookings.filter { it.status == "COMPLETED" }
    val totalSpent = completedBookings.sumOf { it.priceEst }
    val totalTrips = bookings.size

    val avgRating = remember(bookings) {
        val rated = bookings.filter { it.userRating > 0 }
        if (rated.isEmpty()) 0f else rated.map { it.userRating }.average().toFloat()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Title Header
        Text(
            text = "سجل الرحلات",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 16.dp)
        )
 
        // Stats Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Spent
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "المصروف", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%,.0f", totalSpent)} د.ع",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF357EC7)
                    )
                }
 
                // Divider
                Box(
                    modifier = Modifier
                        .height(35.dp)
                        .width(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
 
                // Column 2: Total Trips
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "الرحلات", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalTrips مشاوير",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
 
                // Divider
                Box(
                    modifier = Modifier
                        .height(35.dp)
                        .width(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
 
                // Column 3: Rating
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "التقييم", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (avgRating > 0) String.format("%.1f", avgRating) else "--",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "تقييم",
                            tint = Color(0xFF357EC7),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // List View of past rides
        if (bookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "سجل فارغ",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد رحلات سابقة حتى الآن",
                        fontSize = 15.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ستظهر تفاصيل رحلاتك هنا فور حجز أول تكسي.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(bookings) { booking ->
                    HistoryCard(booking = booking, onRate = { onRateBooking(booking.id, it) })
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    booking: BookingEntity,
    onRate: (Int) -> Unit
) {
    val dateString = remember(booking.timestamp) {
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
        sdf.format(Date(booking.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Status Badge & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                // Status Badge
                val (statusText, statusBg, statusColor) = when (booking.status) {
                    "COMPLETED" -> Triple("مكتملة", Color(0xFFDCFCE7), Color(0xFF0F7643))
                    "CANCELLED" -> Triple("ملغية", Color(0xFFFEE2E2), Color(0xFFDC2626))
                    else -> Triple("نشطة", Color(0xFFE0F2FE), Color(0xFF0369A1))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg,
                    contentColor = statusColor
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Route Information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Route Icon Timeline Line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF0F7643), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(2.dp)
                            .background(Color(0xFFCBD5E1))
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFDC2626), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Location text details
                Column {
                    Text(
                        text = booking.pickupLocation,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = booking.destinationLocation,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE2E8F0))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Section: Price & Driver name & Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Price details
                Column {
                    Text(text = "الأجرة المدفوعة", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = com.example.data.model.TaxiData.formatIqdPrice(booking.priceEst),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                // Right: Driver Info & Stars
                Column(horizontalAlignment = Alignment.End) {
                    if (booking.status == "COMPLETED") {
                        Text(
                            text = "السائق: ${booking.driverName}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Rating stars
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (booking.userRating > 0) {
                                (1..5).forEach { starIndex ->
                                    Icon(
                                        imageVector = if (starIndex <= booking.userRating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "نجمية",
                                        tint = Color(0xFF357EC7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                // Dynamic editable star ratings for past unrated trips
                                Text(
                                    text = "اضغط للتقييم: ",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Row {
                                    (1..5).forEach { starIndex ->
                                        Icon(
                                            imageVector = Icons.Default.StarBorder,
                                            contentDescription = "نجمة فارغة",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { onRate(starIndex) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Cancelled Ride Details
                        Text(
                            text = "مشوار ملغي",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
