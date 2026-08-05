package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DriverEntity
import com.example.data.database.UserEntity
import com.example.ui.viewmodel.TaxiViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    viewModel: TaxiViewModel,
    role: String, // "CUSTOMER" or "DRIVER"
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // Gather state
    val customer by viewModel.loggedInCustomer.collectAsState()
    val driver by viewModel.loggedInDriver.collectAsState()

    // Preferences states (Stored locally in remember for settings mock)
    var isDarkMode by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var gpsAccuracy by remember { mutableStateOf("عالية (دقة كاملة)") }

    // Dialog states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Title Header
        Text(
            text = "الملف الشخصي والإعدادات",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. User/Driver Information Header Card
        if (role == "CUSTOMER" && customer != null) {
            CustomerHeaderCard(customer = customer!!) {
                showEditProfileDialog = true
            }
        } else if (role == "DRIVER" && driver != null) {
            DriverHeaderCard(driver = driver!!) {
                showEditProfileDialog = true
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Personal Preferences / Settings Section Title
        Text(
            text = "إعدادات التطبيق",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Settings items
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Dark Mode Switch
                SettingSwitchRow(
                    icon = Icons.Default.DarkMode,
                    iconTint = Color(0xFF90CAF9),
                    title = "المظهر الداكن (الوضع الليلي)",
                    subtitle = "تفعيل المظهر الأسود المريح للعين",
                    checked = isDarkMode,
                    onCheckedChange = {
                        isDarkMode = it
                        Toast.makeText(context, if (it) "تم تفعيل الوضع الداكن" else "تم تفعيل الوضع الفاتح", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                // Sound alerts switch
                SettingSwitchRow(
                    icon = Icons.Default.NotificationsActive,
                    iconTint = Color(0xFFFFB74D),
                    title = "تنبيهات الصوت والاهتزاز",
                    subtitle = "أصوات رنين عند طلب رحلة أو وصول كابتن",
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                // GPS Accuracy Dropdown / Picker
                SettingSelectionRow(
                    icon = Icons.Default.GpsFixed,
                    iconTint = Color(0xFF81C784),
                    title = "دقة تتبع الموقع الجغرافي",
                    subtitle = "تحديد الدقة لتوفير طاقة البطارية",
                    selectedValue = gpsAccuracy,
                    onClick = {
                        gpsAccuracy = when (gpsAccuracy) {
                            "عالية (دقة كاملة)" -> "متوسطة (متوازن)"
                            "متوسطة (متوازن)" -> "منخفضة (حفظ بطارية)"
                            else -> "عالية (دقة كاملة)"
                        }
                        Toast.makeText(context, "تم ضبط دقة التتبع إلى: $gpsAccuracy", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Address presets for Customers
        if (role == "CUSTOMER") {
            Text(
                text = "العناوين المفضلة",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("عنوان المنزل كجبلة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("سوق جبلة المركزي، بالقرب من جامع كوثى الكبير", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { Toast.makeText(context, "تم حفظ عنوان المنزل", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF357EC7), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مكان العمل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("مستشفى كوثى العام (جبلة)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { Toast.makeText(context, "تم حفظ مكان العمل", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. Driver Specific stats & Subscription details
        if (role == "DRIVER" && driver != null) {
            Text(
                text = "تفاصيل الاشتراك السنوي والشهري",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val expiryDateText = if (driver!!.subscriptionExpiryDate > 0) sdf.format(Date(driver!!.subscriptionExpiryDate)) else "غير نشط"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صلاحية الاشتراك:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = expiryDateText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (driver!!.subscriptionExpiryDate > System.currentTimeMillis()) Color(0xFF0F7643) else Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مستندات الهوية الوطنية:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = driver!!.nationalDocs, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مستندات وأوراق السيارة:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = driver!!.carDocs, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. General Support and Legal Section
        Text(
            text = "الدعم والمساعدة",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Support call Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSupportDialog = true }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color(0xFF0F7643), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مركز المساعدة والدعم الفني", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("تواصل مباشرة مع إدارة تكسي جبلة للمشاكل والتفعيل", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                // Privacy and Terms row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyDialog = true }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF90CAF9), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("شروط الاستخدام وسياسة الخصوصية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("سياسات وضوابط استخدام نظام تكسي جبلة", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Danger zone and Logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { showDeleteAccountDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("حذف الحساب", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    // --- Interactive Sheets and Dialogs ---

    // EDIT PROFILE DIALOG
    if (showEditProfileDialog) {
        if (role == "CUSTOMER" && customer != null) {
            EditCustomerProfileDialog(
                customer = customer!!,
                onDismiss = { showEditProfileDialog = false },
                onSave = { name, phone, password ->
                    viewModel.updateCustomerProfile(
                        oldPhone = customer!!.phone,
                        newPhone = phone,
                        newName = name,
                        newPasswordString = password,
                        onSuccess = {
                            showEditProfileDialog = false
                            Toast.makeText(context, "تم حفظ بيانات الملف الشخصي بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        } else if (role == "DRIVER" && driver != null) {
            EditDriverProfileDialog(
                driver = driver!!,
                onDismiss = { showEditProfileDialog = false },
                onSave = { name, phone, password, carType, carModel, carDocs, nationalDocs ->
                    viewModel.updateDriverProfile(
                        oldPhone = driver!!.phone,
                        newPhone = phone,
                        newName = name,
                        newPasswordString = password,
                        newCarType = carType,
                        newCarModel = carModel,
                        newCarDocs = carDocs,
                        newNationalDocs = nationalDocs,
                        onSuccess = {
                            showEditProfileDialog = false
                            Toast.makeText(context, "تم تحديث بيانات الكابتن بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }

    // ACCOUNT DELETION DIALOG (Double Confirmation)
    if (showDeleteAccountDialog) {
        var firstConfirm by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (!firstConfirm) "تنبيه هام جداً" else "تأكيد نهائي لحذف الحساب",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = if (!firstConfirm) {
                        "هل أنت متأكد تماماً من رغبتك في حذف حسابك؟ ستفقد كل بياناتك وسجل الرحلات الخاص بك نهائياً ولا يمكن استرجاع هذا الحساب مجدداً."
                    } else {
                        "هذه هي الخطوة الأخيرة للتأكيد. بحذف الحساب سيتم مسح بياناتك فوراً من خوادم وقواعد بيانات تكسي جبلة كلياً."
                    },
                    color = Color(0xFF475569),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!firstConfirm) {
                            firstConfirm = true
                        } else {
                            val targetPhone = if (role == "CUSTOMER") customer?.phone else driver?.phone
                            if (targetPhone != null) {
                                if (role == "CUSTOMER") {
                                    viewModel.deleteCustomerAccount(targetPhone) {
                                        showDeleteAccountDialog = false
                                        Toast.makeText(context, "تم حذف حساب الزبون بنجاح", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    viewModel.deleteDriverAccount(targetPhone) {
                                        showDeleteAccountDialog = false
                                        Toast.makeText(context, "تم حذف حساب الكابتن بنجاح", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (!firstConfirm) "متابعة الحذف" else "نعم، احذف الحساب كلياً", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            }
        )
    }

    // SUPPORT CONTACT DIALOG
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "الدعم الفني والشكاوى",
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "يمكنك التواصل المباشر مع إدارة تكسي جبلة لطرح استفساراتك، تفعيل الاشتراكات الشهرية، أو تقديم الشكاوى:",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Direct Phone Support Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x0F81C784)),
                        border = BorderStroke(1.dp, Color(0x3381C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF0F7643))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("رقم الهاتف والاتصال المباشر", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text("+964 780 672 2599", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // WhatsApp Support Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x0F357EC7)),
                        border = BorderStroke(1.dp, Color(0x33357EC7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF357EC7))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("رقم واتساب الفني المباشر", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text("+964 770 123 4567", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSupportDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تم")
                }
            }
        )
    }

    // PRIVACY POLICY DIALOG
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "سياسة الخصوصية وشروط الخدمة",
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "مرحباً بك في تطبيق تكسي جبلة كأول منصة توصيل ذكية داخل قضاء كوثى (جبلة) ومحافظة بابل العريقة.\n\n" +
                               "1. جمع البيانات:\nنقوم بجمع وحفظ اسمك ورقم هاتفك وبيانات موقعك الجغرافي لتمكين الكابتن من الوصول إليك بدقة تامة، وحفظ سجل رحلاتك لضمان سلامة وجودة الخدمة.\n\n" +
                               "2. حماية الخصوصية:\nجميع كلمات المرور وبيانات الهوية المسجلة كالكباتن مشفرة بالكامل ولا يتم مشاركتها مطلقاً مع أي جهات خارجية غير مخولة.\n\n" +
                               "3. شروط الكباتن:\nيجب على الكابتن دفع اشتراك شهري مستمر لضمان بقاء تفعيل حسابه واستقبال الطلبات. يُمنع تداول أي بيانات ركاب أو سوء معاملة داخل النظام.\n\n" +
                               "4. مسؤولية الرحلة:\nيتحمل الطرفان مسؤولية الاتفاق المتبادل على الأجرة والدقة التامة للوصول.\n\n" +
                               "شكراً لاستخدامكم تكسي جبلة!",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("موافق")
                }
            }
        )
    }
}

@Composable
fun CustomerHeaderCard(customer: UserEntity, onEditClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit profile click
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = customer.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = customer.phone,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "عضو منذ: ${SimpleDateFormat("yyyy/MM", Locale.getDefault()).format(Date(customer.createdAt))}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Avatar initial
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DriverHeaderCard(driver: DriverEntity, onEditClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F7643).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "كابتن معتمد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F7643)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = driver.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مركبة: ${driver.carType} - ${driver.carModel}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "لوحة: ${driver.carModel}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "4.9",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFFFD54F).copy(alpha = 0.2f), CircleShape)
                    .border(2.dp, Color(0xFFFFD54F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SettingSelectionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    selectedValue: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = selectedValue, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun EditCustomerProfileDialog(
    customer: UserEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var password by remember { mutableStateOf(customer.passwordString) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "تعديل بيانات الحساب",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || password.isBlank()) return@Button
                    onSave(name, phone, password)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ البيانات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
fun EditDriverProfileDialog(
    driver: DriverEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(driver.name) }
    var phone by remember { mutableStateOf(driver.phone) }
    var password by remember { mutableStateOf(driver.passwordString) }
    var carType by remember { mutableStateOf(driver.carType) }
    var carModel by remember { mutableStateOf(driver.carModel) }
    var carDocs by remember { mutableStateOf(driver.carDocs) }
    var nationalDocs by remember { mutableStateOf(driver.nationalDocs) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "تعديل ملف الكابتن",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الكابتن") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = carType,
                    onValueChange = { carType = it },
                    label = { Text("نوع ونوع السيارة (مثال: كيا سيراتو)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = carModel,
                    onValueChange = { carModel = it },
                    label = { Text("سنة الصنع ورقم اللوحة") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = carDocs,
                    onValueChange = { carDocs = it },
                    label = { Text("تفاصيل أوراق السيارة والسنوية") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nationalDocs,
                    onValueChange = { nationalDocs = it },
                    label = { Text("رقم البطاقة الموحدة أو الهوية") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF475569),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || password.isBlank() || carType.isBlank() || carModel.isBlank()) return@Button
                    onSave(name, phone, password, carType, carModel, carDocs, nationalDocs)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تحديث الملف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF64748B))
            }
        }
    )
}
