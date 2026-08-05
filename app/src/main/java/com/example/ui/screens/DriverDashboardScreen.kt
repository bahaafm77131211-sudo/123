package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.BookingEntity
import com.example.data.database.DriverEntity
import com.example.data.model.LocationPreset
import com.example.data.model.TaxiData
import com.example.ui.viewmodel.TaxiViewModel
import java.text.SimpleDateFormat
import java.util.*

fun findLocationByNameOrCoords(name: String): LocationPreset {
    val matched = TaxiData.JABLEH_PRESETS.find { it.name == name }
    if (matched != null) return matched

    try {
        val numbers = Regex("""\d+(\.\d+)?""").findAll(name).map { it.value.toFloat() }.toList()
        if (numbers.size >= 2) {
            val num1 = numbers[0]
            val num2 = numbers[1]
            if (num1 > 30f) {
                val gridY = 50f + (num1 - 35.3600f) / 0.001f
                val gridX = 50f + (num2 - 35.9200f) / 0.001f
                return LocationPreset(name = name, x = gridX.coerceIn(5f, 95f), y = gridY.coerceIn(5f, 95f))
            } else {
                return LocationPreset(name = name, x = num1.coerceIn(5f, 95f), y = num2.coerceIn(5f, 95f))
            }
        }
    } catch (_: Exception) {}

    return LocationPreset(name = name, x = 45f, y = 45f)
}

fun getRealCoordinates(x: Float, y: Float): Pair<Float, Float> {
    val lat = if (y > 20f && y < 40f && y != 50f) y else 32.7489f + (y - 50f) * 0.0015f
    val lng = if (x > 30f && x < 50f && x != 50f) x else 44.6212f + (x - 50f) * 0.0015f
    return Pair(lat, lng)
}

fun openMapNavigation(context: android.content.Context, lat: Float, lng: Float, locationName: String, appType: String) {
    try {
        val intent = when (appType) {
            "waze" -> {
                val uri = android.net.Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes")
                android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.waze")
                }
            }
            "mapsme" -> {
                val uri = android.net.Uri.parse("mapsme://map?v=1&ll=$lat,$lng")
                android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.mapswithme.maps.pro")
                }
            }
            else -> {
                val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(${android.net.Uri.encode(locationName)})")
                android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallbackUri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(${android.net.Uri.encode(locationName)})")
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri))
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "لم يتم العثور على تطبيق خرائط للفتح", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(
    viewModel: TaxiViewModel,
    driver: DriverEntity,
    onLogout: () -> Unit
) {
    // Collect active bookings in the system to simulate real-time ride acceptance
    val bookings by viewModel.history.collectAsState()
    val activeBooking by viewModel.activeBooking.collectAsState()

    // Determine current driver status by fetching from database or keeping track in the view
    // Since admin updates DB, we look up the driver's fresh status from allDrivers list in VM
    val allDrivers by viewModel.allDrivers.collectAsState()
    val currentDriver = allDrivers.find { it.phone == driver.phone } ?: driver

    var isOnline by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("dashboard") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (activeTab == "dashboard") "بوابة الكابتن" else "الملف الشخصي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "تسجيل الخروج",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "dashboard",
                    onClick = { activeTab = "dashboard" },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "الرحلات"
                        )
                    },
                    label = { Text("الرحلات والطلبات", fontSize = 11.sp) },
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
                    label = { Text("الملف الشخصي", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeTab == "profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    role = "DRIVER",
                    onLogout = onLogout
                )
            } else {
                when {
                    currentDriver.status == "PENDING_APPROVAL" -> {
                        PendingApprovalView(driver = currentDriver)
                    }
                    currentDriver.status == "SUSPENDED" || currentDriver.subscriptionExpiryDate <= System.currentTimeMillis() -> {
                        SuspendedSubscriptionView(driver = currentDriver)
                    }
                    else -> {
                        // APPROVED & Subscription is active
                        DriverActiveConsole(
                            driver = currentDriver,
                            isOnline = isOnline,
                            onOnlineToggle = { isOnline = it },
                            bookings = bookings,
                            activeBooking = activeBooking,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingApprovalView(driver: DriverEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFFFB74D).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "طلبك قيد المراجعة والتدقيق",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "أهلاً بك كابتن ${driver.name}!\nتم استلام مستمسكاتك وصور السيارة بنجاح. تقوم إدارة تكسي جبلة حالياً بتدقيق ومراجعة طلبك لتفعيل حسابك مع اشتراك شهري للبدء باستقبال رحلات الزبائن.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "حالة المستندات المرفوعة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                DocumentStatusRow(label = "مستمسكات الهوية والبطاقة الموحدة", isOk = true)
                DocumentStatusRow(label = "أوراق وسنوية السيارة", isOk = true)
                DocumentStatusRow(label = "صورة السيارة الشخصية", isOk = true)
                DocumentStatusRow(label = "الصورة الشخصية للكابتن", isOk = true)
            }
        }
    }
}

@Composable
fun DocumentStatusRow(label: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isOk) "مرفوعة ومكتملة" else "غير مكتملة",
                fontSize = 12.sp,
                color = if (isOk) Color(0xFF81C784) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isOk) Color(0xFF81C784) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SuspendedSubscriptionView(driver: DriverEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "الاشتراك الشهري متوقف",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "عزيزي الكابتن ${driver.name}، إن اشتراكك الشهري في نظام تكسي جبلة متوقف حالياً أو انتهت صلاحيته. يرجى مراجعة إدارة تكسي جبلة لتسديد الاشتراك وتجديده لتتمكن من البدء باستقبال طلبات الركاب.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Contact Support */ },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.Phone, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("اتصال بالدعم الفني والإدارة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun DriverActiveConsole(
    driver: DriverEntity,
    isOnline: Boolean,
    onOnlineToggle: (Boolean) -> Unit,
    bookings: List<BookingEntity>,
    activeBooking: BookingEntity?,
    viewModel: TaxiViewModel
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val expiryText = sdf.format(Date(driver.subscriptionExpiryDate))

    // List of pending customer bookings for driver to accept
    // Realistically, any customer request that has EN_ROUTE status can be shown
    val pendingRideRequests = bookings.filter { it.status == "PENDING" || it.status == "EN_ROUTE" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Driver welcome and Info Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "أهلاً بك كابتن ${driver.name} 👋",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "سيارتك: ${driver.carType} - ${driver.carModel}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "صلاحية الاشتراك الشهري:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = expiryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Online / Offline Switch card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) Color(0xFF81C784).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isOnline) Color(0xFF81C784) else Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(if (isOnline) Color(0xFF81C784) else Color(0xFFE57373), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isOnline) "أنت متصل الآن وتستقبل الطلبات" else "أنت غير متصل حالياً",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = isOnline,
                    onCheckedChange = onOnlineToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF81C784)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (activeBooking != null && (activeBooking.status == "EN_ROUTE" || activeBooking.status == "ARRIVED" || activeBooking.status == "IN_PROGRESS")) {
            // Driver Active Trip Panel with Map Canvas showing Pickup & Destination
            Text(
                text = "الرحلة الحالية النشطة",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val pickupLoc = remember(activeBooking.pickupLocation) { findLocationByNameOrCoords(activeBooking.pickupLocation) }
            val destLoc = remember(activeBooking.destinationLocation) { findLocationByNameOrCoords(activeBooking.destinationLocation) }
            val driverPos by viewModel.driverPosition.collectAsState()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Driver Interactive Map displaying both Pickup and Destination pins
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        MapCanvas(
                            modifier = Modifier.fillMaxSize(),
                            pickup = pickupLoc,
                            destination = destLoc,
                            driverPos = driverPos,
                            showRoute = true
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when (activeBooking.status) {
                                "EN_ROUTE" -> Color(0xFF1E88E5)
                                "ARRIVED" -> Color(0xFF0288D1)
                                "IN_PROGRESS" -> Color(0xFF6A1B9A)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = when (activeBooking.status) {
                                    "EN_ROUTE" -> "في الطريق لاستلام الزبون 🚗"
                                    "ARRIVED" -> "وصلت لعنوان الزبون 📍"
                                    "IN_PROGRESS" -> "الرحلة جارية للوجهة 🚀"
                                    else -> "رحلة نشطة"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        var showDriverContactDialog by remember { mutableStateOf(false) }
                        var showMapAppDialog by remember { mutableStateOf(false) }
                        var selectedNavTarget by remember { mutableStateOf<Triple<String, Float, Float>?>(null) } // Triple(Name, Lat, Lng)

                        val context = LocalContext.current
                        val pickupCoords = getRealCoordinates(activeBooking.pickupLat, activeBooking.pickupLng)
                        val destCoords = getRealCoordinates(activeBooking.destLat, activeBooking.destLng)

                        if (showMapAppDialog && selectedNavTarget != null) {
                            val (targetName, targetLat, targetLng) = selectedNavTarget!!
                            AlertDialog(
                                onDismissRequest = { showMapAppDialog = false },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("توجيه الخرائط والملاحة 🗺️", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "الموقع: $targetName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "اختر تطبيق الخرائط والملاحة المفضل لديك:",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Option 1: Waze
                                        OutlinedButton(
                                            onClick = {
                                                showMapAppDialog = false
                                                openMapNavigation(context, targetLat, targetLng, targetName, "waze")
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFF33CCFF), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("فتح في تطبيق ويز (Waze)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Option 2: maps.me
                                        OutlinedButton(
                                            onClick = {
                                                showMapAppDialog = false
                                                openMapNavigation(context, targetLat, targetLng, targetName, "mapsme")
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("فتح في تطبيق maps.me", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Option 3: Google Maps / Default
                                        Button(
                                            onClick = {
                                                showMapAppDialog = false
                                                openMapNavigation(context, targetLat, targetLng, targetName, "google")
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("فتح في خرائط Google / الافتراضي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showMapAppDialog = false }) {
                                        Text("إلغاء", fontSize = 13.sp)
                                    }
                                }
                            )
                        }

                        if (showDriverContactDialog) {
                            AlertDialog(
                                onDismissRequest = { showDriverContactDialog = false },
                                title = {
                                    Text("طريقة التواصل مع الزبون", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("اسم الزبون: ${activeBooking.userName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("رقم الهاتف: ${activeBooking.userPhone}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                                        Spacer(modifier = Modifier.height(4.dp))

                                        OutlinedButton(
                                            onClick = {
                                                showDriverContactDialog = false
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                        data = android.net.Uri.parse("tel:${activeBooking.userPhone}")
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    android.widget.Toast.makeText(context, "رقم الزبون: ${activeBooking.userPhone}", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(46.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("اتصال عبر شريحة الهاتف (SIM)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                showDriverContactDialog = false
                                                viewModel.startCall()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(46.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("اتصال صوتي عبر التطبيق", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                showDriverContactDialog = false
                                                viewModel.startChat()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(46.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("محادثة نصية عبر التطبيق", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showDriverContactDialog = false }) {
                                        Text("إلغاء", fontSize = 13.sp)
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "الزبون: ${activeBooking.userName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text(
                                    text = "هاتف: ${activeBooking.userPhone}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 24.dp)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "${activeBooking.priceEst.toInt()} د.ع", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 16.sp)
                                Text(text = "1,500 + 500 د.ع/كم", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pickup Location Row with Map trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedNavTarget = Triple("نقطة الانطلاق: ${activeBooking.pickupLocation}", pickupCoords.first, pickupCoords.second)
                                    showMapAppDialog = true
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "الانطلاق: ${activeBooking.pickupLocation}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("خارطة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Destination Location Row with Map trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedNavTarget = Triple("نقطة الوصول: ${activeBooking.destinationLocation}", destCoords.first, destCoords.second)
                                    showMapAppDialog = true
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "الوصول: ${activeBooking.destinationLocation}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFEBEE)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("خارطة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                            }
                        }

                        // PROMINENT EXTERNAL MAP NAVIGATION CARD BASED ON TRIP STATUS
                        Spacer(modifier = Modifier.height(10.dp))
                        if (activeBooking.status == "EN_ROUTE") {
                            // Driver accepted ride -> heading to Pickup location
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedNavTarget = Triple("نقطة الانطلاق: ${activeBooking.pickupLocation}", pickupCoords.first, pickupCoords.second)
                                        showMapAppDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "فتح موقع نقطة الانطلاق في الخرائط 🗺️",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "ويز (Waze) • maps.me • خرائط Google",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else if (activeBooking.status == "ARRIVED" || activeBooking.status == "IN_PROGRESS") {
                            // Driver clicked "وصلت للزبون" -> heading to Destination location
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedNavTarget = Triple("نقطة الوصول: ${activeBooking.destinationLocation}", destCoords.first, destCoords.second)
                                        showMapAppDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF2E7D32))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "فتح موقع نقطة الوصول في الخرائط 🗺️",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1B5E20)
                                            )
                                            Text(
                                                text = "ويز (Waze) • maps.me • خرائط Google",
                                                fontSize = 11.sp,
                                                color = Color(0xFF2E7D32).copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2E7D32))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeBooking.status == "EN_ROUTE") {
                                Button(
                                    onClick = { viewModel.boardTaxi() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                                ) {
                                    Text("وصلت للزبون 📍", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (activeBooking.status == "ARRIVED") {
                                Button(
                                    onClick = { viewModel.startRide() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                                ) {
                                    Text("انطلاق للوجهة 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (activeBooking.status == "IN_PROGRESS") {
                                Button(
                                    onClick = { viewModel.completeRide() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("إنهاء الرحلة 🏁", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { showDriverContactDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhoneInTalk, contentDescription = "تواصل مع الزبون", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تواصل مع الزبون", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Pending Requests section
            Text(
                text = "طلبات الركاب القريبة المتاحة",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (!isOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "يرجى تفعيل وضع الاتصال للبدء باستقبال الطلبات",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (pendingRideRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد طلبات رحلات جديدة حالياً في جبلة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pendingRideRequests) { request ->
                        RideRequestCard(
                            request = request,
                            onAccept = {
                                viewModel.acceptRideByDriver(request, driver)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RideRequestCard(
    request: BookingEntity,
    onAccept: () -> Unit
) {
    val pickupLoc = remember(request.pickupLocation) { findLocationByNameOrCoords(request.pickupLocation) }
    val destLoc = remember(request.destinationLocation) { findLocationByNameOrCoords(request.destinationLocation) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Map showing trip route for driver
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            ) {
                MapCanvas(
                    modifier = Modifier.fillMaxSize(),
                    pickup = pickupLoc,
                    destination = destLoc,
                    driverPos = Pair(55f, 48f),
                    showRoute = true
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "صاحب الطلب:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${request.userName} (${request.userPhone})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "موقع الانطلاق:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = request.pickupLocation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "وجهة الوصول:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = request.destinationLocation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الأجرة المقدرة:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = TaxiData.formatIqdPrice(request.priceEst),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("الموافقة على الرحلة وبدء التوصيل 🚕", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
