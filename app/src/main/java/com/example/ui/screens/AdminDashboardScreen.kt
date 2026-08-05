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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DriverEntity
import com.example.ui.viewmodel.TaxiViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: TaxiViewModel,
    onLogout: () -> Unit
) {
    val drivers by viewModel.allDrivers.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING_APPROVAL", "APPROVED", "SUSPENDED", "BLOCKED"
    var selectedDriverForDetails by remember { mutableStateOf<DriverEntity?>(null) }

    // Computations for Admin Statistics
    val totalDrivers = drivers.size
    val pendingApprovals = drivers.count { it.status == "PENDING_APPROVAL" }
    val activeSubscriptions = drivers.count { it.status == "APPROVED" && it.subscriptionExpiryDate > System.currentTimeMillis() }
    val blockedDrivers = drivers.count { it.status == "BLOCKED" }

    // Filtered Driver List
    val filteredDrivers = drivers.filter { driver ->
        val matchesSearch = driver.name.contains(searchQuery, ignoreCase = true) || driver.phone.contains(searchQuery)
        val matchesStatus = when (selectedFilter) {
            "ALL" -> true
            "PENDING" -> driver.status == "PENDING_APPROVAL"
            "ACTIVE" -> driver.status == "APPROVED" && driver.subscriptionExpiryDate > System.currentTimeMillis()
            "SUSPENDED" -> driver.status == "SUSPENDED" || (driver.status == "APPROVED" && driver.subscriptionExpiryDate <= System.currentTimeMillis())
            "BLOCKED" -> driver.status == "BLOCKED"
            else -> true
        }
        matchesSearch && matchesStatus
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "لوحة تحكم الإدارة",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Stat Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "السائقين",
                    value = totalDrivers.toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "بانتظار الموافقة",
                    value = pendingApprovals.toString(),
                    icon = Icons.Default.HourglassEmpty,
                    color = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "اشتراكات نشطة",
                    value = activeSubscriptions.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF81C784),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "المحظورين",
                    value = blockedDrivers.toString(),
                    icon = Icons.Default.Block,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث باسم السائق أو رقم الهاتف") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Horizontal Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("الكل") }
                )
                FilterChip(
                    selected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" },
                    label = { Text("بانتظار الموافقة ($pendingApprovals)") }
                )
                FilterChip(
                    selected = selectedFilter == "ACTIVE",
                    onClick = { selectedFilter = "ACTIVE" },
                    label = { Text("مشتركين ($activeSubscriptions)") }
                )
                FilterChip(
                    selected = selectedFilter == "SUSPENDED",
                    onClick = { selectedFilter = "SUSPENDED" },
                    label = { Text("منتهي/متوقف") }
                )
                FilterChip(
                    selected = selectedFilter == "BLOCKED",
                    onClick = { selectedFilter = "BLOCKED" },
                    label = { Text("محظورين") }
                )
            }

            // Driver List Heading
            Text(
                text = "قائمة السائقين المتاحة (${filteredDrivers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (filteredDrivers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا يوجد سائقين يطابقون خيارات البحث حالياً",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDrivers) { driver ->
                        DriverAdminRow(
                            driver = driver,
                            onActionClick = { selectedDriverForDetails = driver },
                            onApprove = { viewModel.approveDriver(driver.phone) },
                            onSuspend = { viewModel.suspendDriverSubscription(driver.phone) },
                            onBlock = { viewModel.blockDriver(driver.phone) },
                            onUnblock = { viewModel.unblockDriver(driver.phone) }
                        )
                    }
                }
            }
        }
    }

    // Driver Detailed Modal
    if (selectedDriverForDetails != null) {
        val driver = selectedDriverForDetails!!
        AlertDialog(
            onDismissRequest = { selectedDriverForDetails = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تفاصيل السائق والسيارة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = { selectedDriverForDetails = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Personal details
                    DetailItem(label = "الاسم الكامل", value = driver.name, icon = Icons.Default.Person)
                    DetailItem(label = "رقم الهاتف", value = driver.phone, icon = Icons.Default.Phone)
                    
                    // Vehicle details
                    DetailItem(label = "نوع السيارة", value = driver.carType, icon = Icons.Default.DirectionsCar)
                    DetailItem(label = "موديل السيارة", value = driver.carModel, icon = Icons.Default.Commute)
                    
                    // Documents details
                    DetailItem(label = "أوراق ومستمسكات السائق", value = driver.nationalDocs, icon = Icons.Default.FilePresent)
                    DetailItem(label = "أوراق السيارة الرسمية", value = driver.carDocs, icon = Icons.Default.FolderOpen)

                    // Subscription details
                    val statusText = when {
                        driver.status == "PENDING_APPROVAL" -> "بانتظار الموافقة"
                        driver.status == "BLOCKED" -> "محظور"
                        driver.status == "SUSPENDED" -> "موقوف الاشتراك"
                        driver.subscriptionExpiryDate > System.currentTimeMillis() -> {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            "نشط (ينتهي: ${sdf.format(Date(driver.subscriptionExpiryDate))})"
                        }
                        else -> "منتهي الاشتراك"
                    }
                    DetailItem(label = "حالة الاشتراك", value = statusText, icon = Icons.Default.CardMembership)
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (driver.status == "PENDING_APPROVAL" || driver.status == "SUSPENDED" || (driver.status == "APPROVED" && driver.subscriptionExpiryDate <= System.currentTimeMillis())) {
                        Button(
                            onClick = {
                                viewModel.approveDriver(driver.phone)
                                selectedDriverForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تفعيل واشتراك", fontWeight = FontWeight.Bold)
                        }
                    } else if (driver.status == "APPROVED") {
                        Button(
                            onClick = {
                                viewModel.suspendDriverSubscription(driver.phone)
                                selectedDriverForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إيقاف الاشتراك", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (driver.status != "BLOCKED") {
                        Button(
                            onClick = {
                                viewModel.blockDriver(driver.phone)
                                selectedDriverForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حظر السائق", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.unblockDriver(driver.phone)
                                selectedDriverForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("فك الحظر", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DriverAdminRow(
    driver: DriverEntity,
    onActionClick: () -> Unit,
    onApprove: () -> Unit,
    onSuspend: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onActionClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = driver.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "هاتف: ${driver.phone}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "السيارة: ${driver.carType} - ${driver.carModel}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Inline Status Pill
                val statusColor = when {
                    driver.status == "PENDING_APPROVAL" -> Color(0xFFFFB74D)
                    driver.status == "BLOCKED" -> MaterialTheme.colorScheme.error
                    driver.status == "SUSPENDED" -> Color(0xFFB0BEC5)
                    driver.subscriptionExpiryDate > System.currentTimeMillis() -> Color(0xFF81C784)
                    else -> Color(0xFFE57373)
                }

                val statusText = when {
                    driver.status == "PENDING_APPROVAL" -> "بانتظار الموافقة"
                    driver.status == "BLOCKED" -> "محظور"
                    driver.status == "SUSPENDED" -> "موقوف"
                    driver.subscriptionExpiryDate > System.currentTimeMillis() -> "مشترك نشط"
                    else -> "منتهي الاشتراك"
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Inline Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (driver.status == "PENDING_APPROVAL" || driver.status == "SUSPENDED" || (driver.status == "APPROVED" && driver.subscriptionExpiryDate <= System.currentTimeMillis())) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text("موافقة وتفعيل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                } else if (driver.status == "APPROVED") {
                    Button(
                        onClick = onSuspend,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text("إيقاف اشتراك", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                if (driver.status != "BLOCKED") {
                    Button(
                        onClick = onBlock,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text("حظر السائق", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onUnblock,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text("فك الحظر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right
            )
        }
    }
}
