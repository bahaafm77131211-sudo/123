package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InAppNotification
import com.example.data.model.NotificationType
import kotlinx.coroutines.delay

@Composable
fun InAppNotificationBanner(
    notification: InAppNotification?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (notification != null) {
            // Auto dismiss timer
            LaunchedEffect(notification.id) {
                delay(5000)
                onDismiss()
            }

            // Type styling
            val (bgColor, iconColor, icon) = when (notification.type) {
                NotificationType.RIDE_REQUESTED -> Triple(
                    Color(0xFF1E88E5), Color.White, Icons.Default.DirectionsCar
                )
                NotificationType.RIDE_ACCEPTED -> Triple(
                    Color(0xFF2E7D32), Color.White, Icons.Default.CheckCircle
                )
                NotificationType.APPROACHING -> Triple(
                    Color(0xFFFF8F00), Color.White, Icons.Default.NearMe
                )
                NotificationType.ARRIVED -> Triple(
                    Color(0xFF0288D1), Color.White, Icons.Default.Place
                )
                NotificationType.IN_PROGRESS -> Triple(
                    Color(0xFF6A1B9A), Color.White, Icons.Default.Navigation
                )
                NotificationType.CANCELLED -> Triple(
                    Color(0xFFC62828), Color.White, Icons.Default.Cancel
                )
                NotificationType.COMPLETED -> Triple(
                    Color(0xFF00796B), Color.White, Icons.Default.TaskAlt
                )
                NotificationType.INFO -> Triple(
                    MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary, Icons.Default.Notifications
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clickable { onDismiss() },
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                contentColor = Color.White
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = notification.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الآن",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notification.message,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق الإشعار",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Progress bar indicating auto-dismiss count
                    val progress by animateFloatAsState(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 5000, easing = LinearEasing),
                        label = "dismissProgress"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}
