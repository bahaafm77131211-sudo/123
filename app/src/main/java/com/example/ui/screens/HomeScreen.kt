package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.database.BookingEntity
import com.example.data.model.LocationPreset
import com.example.data.model.ServiceType
import com.example.data.model.TaxiData

fun getNearestPresetOrCustom(gridX: Float, gridY: Float): LocationPreset {
    var nearest: LocationPreset? = null
    var minDist = Float.MAX_VALUE
    for (preset in TaxiData.ALL_LOCATIONS) {
        val dx = preset.x - gridX
        val dy = preset.y - gridY
        val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist < minDist) {
            minDist = dist
            nearest = preset
        }
    }
    // If within 10 units, snap to named preset or governorate address, else output location coordinates
    return if (nearest != null && minDist < 10f) {
        nearest
    } else {
        val lat = String.format(java.util.Locale.ENGLISH, "%.4f", 30.0000 + (gridY / 100.0) * 7.0)
        val lng = String.format(java.util.Locale.ENGLISH, "%.4f", 42.0000 + (gridX / 100.0) * 6.0)
        LocationPreset(
            name = "موقع مخصص في العراق ($lat, $lng)",
            x = gridX,
            y = gridY
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    pickup: LocationPreset?,
    destination: LocationPreset?,
    selectedService: ServiceType,
    activeBooking: BookingEntity?,
    driverPos: Pair<Float, Float>,
    isSearching: Boolean,
    onPickupSelect: (LocationPreset?) -> Unit,
    onDestinationSelect: (LocationPreset?) -> Unit,
    onServiceSelect: (ServiceType) -> Unit,
    onBookClick: () -> Unit,
    onBoardClick: () -> Unit,
    onCancelClick: () -> Unit,
    onCallClick: () -> Unit,
    onChatClick: () -> Unit
) {
    var rideStep by remember { mutableStateOf(1) } // 1: Pickup selection, 2: Destination selection
    var isPickupDropdownExpanded by remember { mutableStateOf(false) }
    var isDestDropdownExpanded by remember { mutableStateOf(false) }
    var isMapFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Reset rideStep to 1 when active booking clears or is empty
    LaunchedEffect(activeBooking) {
        if (activeBooking == null && pickup == null && destination == null) {
            rideStep = 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (activeBooking == null) {
                // --- INACTIVE BOOKING STATE (Interactive Stepped Selection) ---
                // 1. Map Canvas Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isMapFullscreen) {
                                Modifier.weight(1f)
                            } else {
                                Modifier.height(460.dp)
                            }
                        )
                ) {
                    MapCanvas(
                        modifier = Modifier.fillMaxSize(),
                        pickup = pickup,
                        destination = destination,
                        driverPos = Pair(55f, 48f), // Nearby taxi icon
                        showRoute = (rideStep == 2 && pickup != null && destination != null),
                        onMapClick = { x, y ->
                            val loc = getNearestPresetOrCustom(x, y)
                            if (rideStep == 1) {
                                onPickupSelect(loc)
                                android.widget.Toast.makeText(context, "تم تحديد موقع الانطلاق: ${loc.name}", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onDestinationSelect(loc)
                                android.widget.Toast.makeText(context, "تم تحديد وجهة الوصول: ${loc.name}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    
                    // Floating Selected Points Badges
                    if (pickup != null || destination != null) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopStart),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pickup != null) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF2E7D32),
                                    contentColor = Color.White,
                                    shadowElevation = 6.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "الانطلاق: ${pickup.name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (destination != null && rideStep == 2) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFC62828),
                                    contentColor = Color.White,
                                    shadowElevation = 6.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "الوصول: ${destination.name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Floating Fullscreen Toggle Button
                    IconButton(
                        onClick = { isMapFullscreen = !isMapFullscreen },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMapFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isMapFullscreen) "تصغير الخريطة" else "تكبير الخريطة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    val mapTipText = if (rideStep == 1) {
                        "📍 انقر على الخريطة لتحديد موقع الانطلاق"
                    } else {
                        "📍 انقر على الخريطة لتحديد وجهة الوصول"
                    }

                    // Floating interactive tip
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mapTipText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Booking Controls Section
                if (!isMapFullscreen) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        ScrollViewWithBookingControls(
                            rideStep = rideStep,
                            pickup = pickup,
                            destination = destination,
                            selectedService = selectedService,
                            isPickupDropdownExpanded = isPickupDropdownExpanded,
                            isDestDropdownExpanded = isDestDropdownExpanded,
                            onPickupExpandedChange = { isPickupDropdownExpanded = it },
                            onDestExpandedChange = { isDestDropdownExpanded = it },
                            onPickupSelect = onPickupSelect,
                            onDestinationSelect = onDestinationSelect,
                            onServiceSelect = onServiceSelect,
                            onConfirmPickup = {
                                rideStep = 2
                            },
                            onChangePickup = {
                                rideStep = 1
                            },
                            onBookClick = onBookClick
                        )
                    }
                } else {
                    // Floating bottom card for quick action when map is fullscreen
                    Card(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (rideStep == 1) "الخطوة 1: تحديد الانطلاق" else "الخطوة 2: تحديد الوصول",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (rideStep == 1) (pickup?.name ?: "انقر على الخريطة لتحديد الانطلاق") else (destination?.name ?: "انقر على الخريطة لتحديد الوصول"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { isMapFullscreen = false },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("متابعة الحجز", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // --- ACTIVE TRACKING RIDE STATE ---
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Live Tracking Animated Map
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        MapCanvas(
                            modifier = Modifier.fillMaxSize(),
                            pickup = pickup,
                            destination = destination,
                            driverPos = driverPos,
                            showRoute = true
                        )

                        // Floating Cancel Button on Map
                        IconButton(
                            onClick = onCancelClick,
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopStart)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إلغاء الرحلة",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 2. Interactive Status Card Overlay at the bottom
                    ActiveRideDetailsCard(
                        activeBooking = activeBooking,
                        onBoardClick = onBoardClick,
                        onCallClick = onCallClick,
                        onChatClick = onChatClick,
                        onCancelClick = onCancelClick
                    )
                }
            }
        }

        // --- SEARCHING OVERLAY STATE ---
        if (isSearching) {
            SearchingOverlay(onCancelClick = onCancelClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollViewWithBookingControls(
    rideStep: Int,
    pickup: LocationPreset?,
    destination: LocationPreset?,
    selectedService: ServiceType,
    isPickupDropdownExpanded: Boolean,
    isDestDropdownExpanded: Boolean,
    onPickupExpandedChange: (Boolean) -> Unit,
    onDestExpandedChange: (Boolean) -> Unit,
    onPickupSelect: (LocationPreset?) -> Unit,
    onDestinationSelect: (LocationPreset?) -> Unit,
    onServiceSelect: (ServiceType) -> Unit,
    onConfirmPickup: () -> Unit,
    onChangePickup: () -> Unit,
    onBookClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var pickupSearchQuery by remember { mutableStateOf(pickup?.name ?: "") }
    LaunchedEffect(pickup) {
        pickupSearchQuery = pickup?.name ?: ""
    }

    var destSearchQuery by remember { mutableStateOf(destination?.name ?: "") }
    LaunchedEffect(destination) {
        destSearchQuery = destination?.name ?: ""
    }

    val filteredPickupPresets = remember(pickupSearchQuery) {
        if (pickupSearchQuery.isEmpty() || pickupSearchQuery == pickup?.name) {
            TaxiData.ALL_LOCATIONS
        } else {
            TaxiData.ALL_LOCATIONS.filter { it.name.contains(pickupSearchQuery, ignoreCase = true) }
        }
    }

    val filteredDestPresets = remember(destSearchQuery) {
        if (destSearchQuery.isEmpty() || destSearchQuery == destination?.name) {
            TaxiData.ALL_LOCATIONS
        } else {
            TaxiData.ALL_LOCATIONS.filter { it.name.contains(destSearchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        if (rideStep == 1) {
            // ==================== STEP 1: PICKUP SELECTION ====================
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تحديد موقع الانطلاق 📍",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Pickup Search Input Box (Plain text address input)
                    OutlinedTextField(
                        value = pickupSearchQuery,
                        onValueChange = { query ->
                            pickupSearchQuery = query
                            if (query.isNotBlank()) {
                                val currentX = pickup?.x ?: 50f
                                val currentY = pickup?.y ?: 50f
                                onPickupSelect(LocationPreset(name = query, x = currentX, y = currentY))
                            } else {
                                onPickupSelect(null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("اكتب عنوان نقطة الانطلاق هنا أو انقر الخريطة…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (pickupSearchQuery.isNotEmpty() || pickup != null) {
                                IconButton(onClick = {
                                    pickupSearchQuery = ""
                                    onPickupSelect(null)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "تفريغ",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.secondary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.secondary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Big Primary Confirmation Button (Matching reference image)
            Button(
                onClick = {
                    val queryText = pickupSearchQuery.trim()
                    if (pickup != null) {
                        onConfirmPickup()
                    } else if (queryText.isNotBlank()) {
                        onPickupSelect(LocationPreset(name = queryText, x = 50f, y = 50f))
                        onConfirmPickup()
                    } else {
                        android.widget.Toast.makeText(context, "الرجاء كتابة عنوان نقطة الانطلاق أو النقر على الخريطة لتحديدها", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0040FF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "تأكيد نقطة الإطلاق",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // ==================== STEP 2: DESTINATION SELECTION ====================
            // Confirmed Pickup Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "موقع الانطلاق المؤكد:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = pickup?.name ?: "لم يتم التحديد",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onChangePickup,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32))
                    ) {
                        Text("تغيير الانطلاق", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تحديد وجهة الوصول 🏁",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Destination Search Input Box
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = destSearchQuery,
                            onValueChange = { 
                                destSearchQuery = it
                                onDestExpandedChange(true)
                                if (it != destination?.name) {
                                    onDestinationSelect(null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("ابحث أو انقر الخريطة لتحديد الوصول…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "وجهة",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (destSearchQuery.isNotEmpty() || destination != null) {
                                    IconButton(onClick = {
                                        destSearchQuery = ""
                                        onDestinationSelect(null)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "تفريغ",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { onDestExpandedChange(true) }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "عرض الكل",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.secondary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.secondary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        DropdownMenu(
                            expanded = isDestDropdownExpanded,
                            onDismissRequest = { onDestExpandedChange(false) },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(MaterialTheme.colorScheme.secondary)
                        ) {
                            if (filteredDestPresets.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("لا توجد نتائج، انقر الخريطة للتحديد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                                    onClick = { onDestExpandedChange(false) }
                                )
                            } else {
                                filteredDestPresets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp) },
                                        onClick = {
                                            onDestinationSelect(preset)
                                            destSearchQuery = preset.name
                                            onDestExpandedChange(false)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Presets Chips for Destination
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(TaxiData.ALL_LOCATIONS) { preset ->
                            val isSelected = destination == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onDestinationSelect(preset)
                                    destSearchQuery = preset.name
                                },
                                label = { Text(preset.name, fontSize = 11.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Estimated Fare Card
                    if (pickup != null && (destination != null || destSearchQuery.isNotBlank())) {
                        val activeDest = destination ?: filteredDestPresets.firstOrNull() ?: TaxiData.JABLEH_PRESETS.last()
                        val dx = activeDest.x - pickup.x
                        val dy = activeDest.y - pickup.y
                        val distKm = (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) * 0.15).coerceAtLeast(1.0)
                        val estimatedFare = (1500.0 + (distKm * 500.0)).toInt()

                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "سعر التكلفة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = TaxiData.formatIqdPrice(estimatedFare),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "عدد الكيلومترات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = String.format(java.util.Locale.ENGLISH, "%.1f كم", distKm),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "الوقت التقريبي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "${(distKm * 2.5).toInt().coerceAtLeast(3)} دقائق",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Big Primary Ride Request Button
            val isValidDest = destination != null || destSearchQuery.isNotBlank()
            val isIdentical = pickup != null && destination != null && pickup == destination

            if (isIdentical) {
                Text(
                    text = "⚠️ موقع الانطلاق والوصول متطابقين تماماً!",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (destination == null && destSearchQuery.isNotBlank()) {
                        val matched = filteredDestPresets.firstOrNull() ?: TaxiData.JABLEH_PRESETS.last()
                        onDestinationSelect(matched)
                    }
                    onBookClick()
                },
                enabled = isValidDest && !isIdentical,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0040FF),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF0040FF).copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "تأكيد وطلب التكسي 🚕",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveRideDetailsCard(
    activeBooking: BookingEntity,
    onBoardClick: () -> Unit,
    onCallClick: () -> Unit,
    onChatClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current
    var showContactOptions by remember { mutableStateOf(false) }

    if (showContactOptions) {
        AlertDialog(
            onDismissRequest = { showContactOptions = false },
            title = {
                Text(
                    text = "طريقة التواصل مع الكابتن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "الكابتن: ${activeBooking.driverName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "رقم الهاتف: ${activeBooking.driverPhone}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Direct phone call
                    OutlinedButton(
                        onClick = {
                            showContactOptions = false
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:${activeBooking.driverPhone}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "رقم الكابتن: ${activeBooking.driverPhone}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اتصال عبر شريحة الهاتف (SIM)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Option 2: In-App Voice Call
                    Button(
                        onClick = {
                            showContactOptions = false
                            onCallClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اتصال صوتي عبر التطبيق", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Option 3: In-App Text Chat
                    OutlinedButton(
                        onClick = {
                            showContactOptions = false
                            onChatClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
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
                TextButton(onClick = { showContactOptions = false }) {
                    Text("إلغاء", fontSize = 13.sp)
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // 1. Status Indicator / Progress Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusLabel = when (activeBooking.status) {
                    "EN_ROUTE" -> "الكابتن في الطريق إليك 🚕"
                    "ARRIVED" -> "وصل الكابتن إلى موقعك 📍"
                    "IN_PROGRESS" -> "الرحلة مستمرة إلى الوجهة 🚀"
                    else -> "جاري الرحلة"
                }
                Text(
                    text = statusLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = TaxiData.formatIqdPrice(activeBooking.priceEst),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Timeline status indicators (Visual Stepper)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val activeStep = when (activeBooking.status) {
                    "EN_ROUTE" -> 1
                    "ARRIVED" -> 2
                    "IN_PROGRESS" -> 3
                    else -> 1
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (activeStep >= 1) MaterialTheme.colorScheme.primary else Color.DarkGray)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (activeStep >= 2) MaterialTheme.colorScheme.primary else Color.DarkGray)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (activeStep >= 3) MaterialTheme.colorScheme.primary else Color.DarkGray)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Driver Detail Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Driver Avatar Icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "سائق",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Driver details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeBooking.driverName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "تقييم",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = " ${activeBooking.driverRating}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "السيارة: ${activeBooking.driverVehicle}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "رقم هاتف الكابتن: ${activeBooking.driverPhone}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Car license Plate tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text(
                        text = activeBooking.driverVehicle.substringAfter(" - ", "بابل"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Choice of Communication Button Section
            Button(
                onClick = { showContactOptions = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "خيارات الاتصال بالكابتن",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "خيارات التواصل والاتصال بالكابتن", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Big context-aware action triggers
            if (activeBooking.status == "ARRIVED") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onBoardClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.EmeraldGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "ركبت السيارة - ابدأ الرحلة 🚀",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cancel Button
            Text(
                text = "إلغاء الطلب",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCancelClick() }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SearchingOverlay(onCancelClick: () -> Unit) {
    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sizeScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alphaScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Radar pulse graphics
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Expanding wave
                Box(
                    modifier = Modifier
                        .size((130 * sizeScale).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alphaScale * 0.3f))
                )
                Box(
                    modifier = Modifier
                        .size((170 * sizeScale).dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = alphaScale * 0.2f), CircleShape)
                )

                // Central Core Taxi Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "سيارة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "جاري البحث عن سائق تكسي قريب…",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "يرجى الانتظار، نتواصل مع كباتن تكسي جبلة حالاً",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onCancelClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "إلغاء البحث", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
    }
}
