package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.screens.DriverCallScreen
import com.example.ui.screens.DriverChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DriverDashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TaxiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: TaxiViewModel = viewModel()

                // Auth states
                val currentRole by viewModel.currentRole.collectAsState()
                val loggedInCustomer by viewModel.loggedInCustomer.collectAsState()
                val loggedInDriver by viewModel.loggedInDriver.collectAsState()

                // State flows
                val pickup by viewModel.pickup.collectAsState()
                val destination by viewModel.destination.collectAsState()
                val selectedService by viewModel.selectedService.collectAsState()
                val activeBooking by viewModel.activeBooking.collectAsState()
                val driverPos by viewModel.driverPosition.collectAsState()
                val chatMessages by viewModel.chatMessages.collectAsState()
                val isSearching by viewModel.isSearching.collectAsState()
                val isCalling by viewModel.isCalling.collectAsState()
                val isChatting by viewModel.isChatting.collectAsState()
                val showRatingDialogForId by viewModel.showRatingDialogForId.collectAsState()
                val history by viewModel.history.collectAsState()
                val inAppNotification by viewModel.inAppNotification.collectAsState()

                // Bottom Tab Navigation State
                var activeTab by remember { mutableStateOf("home") }

                // Runtime Notification Permission Request
                val context = LocalContext.current
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }
                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Main Root Screen Switcher
                    when {
                        currentRole == null -> {
                        // Unauthenticated - Landing and Login
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = { /* Handle routing or state updates inside VM */ }
                        )
                    }
                    currentRole == "ADMIN" -> {
                        // Admin Dashboard
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            onLogout = { viewModel.logout() }
                        )
                    }
                    currentRole == "DRIVER" && loggedInDriver != null -> {
                        // Driver Dashboard
                        DriverDashboardScreen(
                            viewModel = viewModel,
                            driver = loggedInDriver!!,
                            onLogout = { viewModel.logout() }
                        )
                    }
                    isCalling && activeBooking != null -> {
                        // Immersive Calling View
                        DriverCallScreen(
                            driverName = activeBooking!!.driverName,
                            driverVehicle = activeBooking!!.driverVehicle,
                            onHangUp = { viewModel.startCalling(false) }
                        )
                    }
                    isChatting && activeBooking != null -> {
                        // Immersive Chat View
                        DriverChatScreen(
                            driverName = activeBooking!!.driverName,
                            driverVehicle = activeBooking!!.driverVehicle,
                            messages = chatMessages,
                            onSendMessage = { viewModel.sendUserMessage(it) },
                            onBack = { viewModel.startChatting(false) }
                        )
                    }
                    else -> {
                        // Standard Application Layout with Tab Navigation
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBarItem(
                                        selected = activeTab == "home",
                                        onClick = { activeTab = "home" },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = "الرئيسية"
                                            )
                                        },
                                        label = { Text("الرئيسية", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == "history",
                                        onClick = { activeTab = "history" },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "سجل الرحلات"
                                            )
                                        },
                                        label = { Text("سجل الرحلات", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == "profile",
                                        onClick = { activeTab = "profile" },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "الملف الشخصي"
                                            )
                                        },
                                        label = { Text("حسابي", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            },
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                // Tab Switcher Content
                                when (activeTab) {
                                    "home" -> {
                                        HomeScreen(
                                            pickup = pickup,
                                            destination = destination,
                                            selectedService = selectedService,
                                            activeBooking = activeBooking,
                                            driverPos = driverPos,
                                            isSearching = isSearching,
                                            onPickupSelect = { viewModel.setPickup(it) },
                                            onDestinationSelect = { viewModel.setDestination(it) },
                                            onServiceSelect = { viewModel.selectService(it) },
                                            onBookClick = { viewModel.requestRide() },
                                            onBoardClick = { viewModel.boardTaxi() },
                                            onCancelClick = { viewModel.cancelRide() },
                                            onCallClick = { viewModel.startCalling(true) },
                                            onChatClick = { viewModel.startChatting(true) }
                                        )
                                    }
                                    "history" -> {
                                        HistoryScreen(
                                            bookings = history,
                                            onRateBooking = { id, rating -> viewModel.submitRating(id, rating) }
                                        )
                                    }
                                    "profile" -> {
                                        ProfileScreen(
                                            viewModel = viewModel,
                                            role = "CUSTOMER",
                                            onLogout = { viewModel.logout() }
                                        )
                                    }
                                }

                                // Overlay rating feedback dialog on arrival
                                if (showRatingDialogForId != null) {
                                    // Fetch completed trip details for rating view
                                    val completedTrip = history.firstOrNull { it.id == showRatingDialogForId }
                                    if (completedTrip != null) {
                                        RatingDialog(
                                            bookingId = completedTrip.id,
                                            driverName = completedTrip.driverName,
                                            onRateSubmit = { stars ->
                                                viewModel.submitRating(completedTrip.id, stars)
                                            },
                                            onDismiss = { viewModel.dismissRating() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating In-App Heads-up Notification Overlay over all screens
                    InAppNotificationBanner(
                        notification = inAppNotification,
                        onDismiss = { viewModel.dismissInAppNotification() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .zIndex(999f)
                    )
                }
            }
        }
    }
}

@Composable
fun RatingDialog(
    bookingId: Int,
    driverName: String,
    onRateSubmit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "تم الوصول",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "الحمد لله على السلامة!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "كيف كانت رحلتك مع الكابتن $driverName؟",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    (1..5).forEach { index ->
                        val isSelected = index <= rating
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "تقييم $index نجوم",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRateSubmit(rating) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إرسال التقييم", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("تخطي", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
