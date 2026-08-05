package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.TaxiViewModel
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

// Helper function to format phone numbers to international format (+964 for Iraq)
fun formatPhoneNumber(rawPhone: String): String {
    val clean = rawPhone.trim().replace(" ", "").replace("-", "")
    return when {
        clean.startsWith("+") -> clean
        clean.startsWith("00") -> "+" + clean.substring(2)
        clean.startsWith("07") -> "+964" + clean.substring(1) // Iraqi: e.g. 0780 -> +964780
        clean.startsWith("7") -> "+964$clean" // Iraqi: e.g. 780 -> +964780
        else -> if (clean.length >= 10) "+964${clean.takeLast(10)}" else "+964$clean"
    }
}

// Helper function to validate Iraqi phone numbers (must be 11 digits starting with 07 or 10 digits starting with 7)
fun isValidIraqiPhoneNumber(phone: String): Boolean {
    val clean = phone.trim().replace(" ", "").replace("-", "")
    val localPart = when {
        clean.startsWith("+964") -> clean.substring(4)
        clean.startsWith("00964") -> clean.substring(5)
        clean.startsWith("07") -> clean.substring(1)
        clean.startsWith("7") -> clean
        else -> ""
    }
    return localPart.length == 10 && localPart.startsWith("7")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: TaxiViewModel,
    onLoginSuccess: (role: String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }

    var verificationId by remember { mutableStateOf("") }
    var forceResendingToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var registeringRole by remember { mutableStateOf("CUSTOMER") } // "CUSTOMER" or "DRIVER"

    var screenState by remember { mutableStateOf("landing") } // "landing", "customer", "driver", "admin"
    var isRegisterMode by remember { mutableStateOf(false) }

    // Form inputs
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    
    // Driver specific inputs
    var carType by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    
    // Document Upload Real States (using URI)
    var nationalDocsUri by remember { mutableStateOf<Uri?>(null) }
    var carDocsUri by remember { mutableStateOf<Uri?>(null) }
    var carPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var personalPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val nationalDocsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) nationalDocsUri = uri }

    val carDocsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) carDocsUri = uri }

    val carPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) carPhotoUri = uri }

    val personalPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) personalPhotoUri = uri }

    // Error & Loading States
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }

    // Firebase Auth Phone Callbacks
    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                val smsCode = credential.smsCode
                if (!smsCode.isNullOrEmpty()) {
                    otpCode = smsCode
                }
                
                showOtpDialog = false
                isSendingOtp = false
                successMessage = "تم التحقق التلقائي من رقم الهاتف بنجاح!"
                if (registeringRole == "DRIVER") {
                    viewModel.registerDriver(
                        phone = phone,
                        passwordString = password,
                        name = fullName,
                        nationalDocs = nationalDocsUri.toString(),
                        carDocs = carDocsUri.toString(),
                        carType = carType,
                        carModel = carModel,
                        carPhoto = carPhotoUri.toString(),
                        personalPhoto = personalPhotoUri.toString(),
                        onSuccess = {
                            successMessage = "تم تسجيلك كسائق جديد بنجاح! بانتظار موافقة الإدارة."
                            onLoginSuccess("DRIVER")
                        },
                        onError = { errorMessage = it }
                    )
                } else {
                    viewModel.registerCustomer(
                        phone = phone,
                        passwordString = password,
                        name = fullName,
                        onSuccess = { onLoginSuccess("CUSTOMER") },
                        onError = { errorMessage = it }
                    )
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isSendingOtp = false
                showOtpDialog = false
                errorMessage = "تعذر إرسال رمز التحقق OTP عبر Firebase: ${e.localizedMessage ?: e.message}"
                successMessage = ""
            }

            override fun onCodeSent(
                verificationIdSent: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isSendingOtp = false
                verificationId = verificationIdSent
                forceResendingToken = token
                showOtpDialog = true
                successMessage = "تم إرسال رمز التحقق بنجاح إلى الرقم ${formatPhoneNumber(phone)}"
            }
        }
    }

    val scrollState = rememberScrollState()

    val isLanding = screenState == "landing"
    val backgroundColor = if (isLanding) Color(0xFFF4F9FD) else MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .drawBehind {
                if (isLanding) {
                    // Top right decorative circle
                    drawCircle(
                        color = Color(0xFFE1EFFB),
                        radius = 160.dp.toPx(),
                        center = Offset(size.width * 0.95f, size.height * 0.02f)
                    )
                    // Mid-left decorative circle
                    drawCircle(
                        color = Color(0xFFE1EFFB),
                        radius = 140.dp.toPx(),
                        center = Offset(size.width * -0.15f, size.height * 0.26f)
                    )
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header back button
            if (screenState != "landing") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = {
                            screenState = "landing"
                            isRegisterMode = false
                            errorMessage = ""
                            successMessage = ""
                        },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // App Logo and Title
            if (screenState == "landing") {
                // Landing Screen Specific Header (matching the image exactly)
                Spacer(modifier = Modifier.height(32.dp))
                
                // Circular White Logo Card
                Surface(
                    modifier = Modifier
                        .size(130.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.5.dp, Color(0xFFEBF5FF))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "شعار التطبيق",
                            tint = Color(0xFF357EC7),
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "تكسي جبلة",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "خدمة تنقل سهلة ومريحة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)
                )
                
                Text(
                    text = "مرحباً بك",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "اختر نوع حسابك للمتابعة",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )
            } else {
                // Non-landing Compact Header
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "شعار التطبيق",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "تكسي جبلة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Right
                    )
                }
            }

            if (successMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = successMessage,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Right
                    )
                }
            }

            // Render based on state
            AnimatedContent(
                targetState = screenState,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState == "landing") -width else width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState == "landing") width else -width } + fadeOut()
                },
                label = "ScreenStateAnimation"
            ) { state ->
                when (state) {
                    "landing" -> {
                        LandingView(
                            onCustomerClick = {
                                screenState = "customer"
                                phone = ""
                                password = ""
                                confirmPassword = ""
                                fullName = ""
                            },
                            onDriverClick = {
                                screenState = "driver"
                                phone = ""
                                password = ""
                                fullName = ""
                                carType = ""
                                carModel = ""
                                nationalDocsUri = null
                                carDocsUri = null
                                carPhotoUri = null
                                personalPhotoUri = null
                            },
                            onAdminClick = {
                                screenState = "admin"
                                phone = ""
                                password = ""
                            }
                        )
                    }
                    "customer" -> {
                        CustomerView(
                            isRegisterMode = isRegisterMode,
                            phone = phone,
                            password = password,
                            confirmPassword = confirmPassword,
                            fullName = fullName,
                            onPhoneChange = { phone = it },
                            onPasswordChange = { password = it },
                            onConfirmPasswordChange = { confirmPassword = it },
                            onFullNameChange = { fullName = it },
                            onToggleMode = {
                                isRegisterMode = !isRegisterMode
                                errorMessage = ""
                                successMessage = ""
                            },
                            onSubmit = {
                                errorMessage = ""
                                if (phone.isEmpty() || password.isEmpty()) {
                                    errorMessage = "يرجى ملء جميع الحقول المطلوبة."
                                    return@CustomerView
                                }
                                if (isRegisterMode) {
                                    if (fullName.isEmpty()) {
                                        errorMessage = "يرجى إدخال اسمك الكامل."
                                        return@CustomerView
                                    }
                                    if (!isValidIraqiPhoneNumber(phone)) {
                                        errorMessage = "يرجى إدخال رقم هاتف عراقي صحيح (مثال: 07806722599)."
                                        return@CustomerView
                                    }
                                    if (password != confirmPassword) {
                                        errorMessage = "كلمتا المرور غير متطابقتين."
                                        return@CustomerView
                                    }
                                    
                                    val formattedPhone = formatPhoneNumber(phone)
                                    if (activity != null) {
                                        isSendingOtp = true
                                        registeringRole = "CUSTOMER"
                                        errorMessage = ""
                                        successMessage = "جاري إرسال رمز التحقق إلى $formattedPhone..."
                                        otpCode = ""
                                        
                                        val options = PhoneAuthOptions.newBuilder(auth)
                                            .setPhoneNumber(formattedPhone)
                                            .setTimeout(60L, TimeUnit.SECONDS)
                                            .setActivity(activity)
                                            .setCallbacks(callbacks)
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    } else {
                                        errorMessage = "فشل التحقق: لم يتم العثور على نشاط واجهة المستخدم."
                                    }
                                } else {
                                    viewModel.loginCustomer(
                                        phone = phone,
                                        passwordString = password,
                                        onSuccess = { onLoginSuccess("CUSTOMER") },
                                        onError = { errorMessage = it }
                                    )
                                }
                            }
                        )
                    }
                    "driver" -> {
                        DriverView(
                            isRegisterMode = isRegisterMode,
                            phone = phone,
                            password = password,
                            fullName = fullName,
                            carType = carType,
                            carModel = carModel,
                            nationalDocsUri = nationalDocsUri,
                            carDocsUri = carDocsUri,
                            carPhotoUri = carPhotoUri,
                            personalPhotoUri = personalPhotoUri,
                            onPhoneChange = { phone = it },
                            onPasswordChange = { password = it },
                            onFullNameChange = { fullName = it },
                            onCarTypeChange = { carType = it },
                            onCarModelChange = { carModel = it },
                            onNationalDocsUpload = { nationalDocsLauncher.launch("image/*") },
                            onCarDocsUpload = { carDocsLauncher.launch("image/*") },
                            onCarPhotoUpload = { carPhotoLauncher.launch("image/*") },
                            onPersonalPhotoUpload = { personalPhotoLauncher.launch("image/*") },
                            onToggleMode = {
                                isRegisterMode = !isRegisterMode
                                errorMessage = ""
                                successMessage = ""
                            },
                            onSubmit = {
                                errorMessage = ""
                                if (phone.isEmpty() || password.isEmpty()) {
                                    errorMessage = "يرجى ملء جميع الحقول الأساسية."
                                    return@DriverView
                                }
                                if (isRegisterMode) {
                                    if (fullName.isEmpty() || carType.isEmpty() || carModel.isEmpty()) {
                                        errorMessage = "يرجى ملء جميع الحقول وتفاصيل السيارة."
                                        return@DriverView
                                    }
                                    if (nationalDocsUri == null || carDocsUri == null || carPhotoUri == null || personalPhotoUri == null) {
                                        errorMessage = "يرجى رفع جميع المستندات والصور المطلوبة لإكمال التسجيل."
                                        return@DriverView
                                    }
                                    if (!isValidIraqiPhoneNumber(phone)) {
                                        errorMessage = "يرجى إدخال رقم هاتف عراقي صحيح (مثال: 07806722599)."
                                        return@DriverView
                                    }

                                    val formattedPhone = formatPhoneNumber(phone)
                                    if (activity != null) {
                                        isSendingOtp = true
                                        registeringRole = "DRIVER"
                                        errorMessage = ""
                                        successMessage = "جاري إرسال رمز التحقق إلى $formattedPhone..."
                                        otpCode = ""
                                        
                                        val options = PhoneAuthOptions.newBuilder(auth)
                                            .setPhoneNumber(formattedPhone)
                                            .setTimeout(60L, TimeUnit.SECONDS)
                                            .setActivity(activity)
                                            .setCallbacks(callbacks)
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    } else {
                                        errorMessage = "فشل التحقق: لم يتم العثور على نشاط واجهة المستخدم."
                                    }
                                } else {
                                    viewModel.loginDriver(
                                        phone = phone,
                                        passwordString = password,
                                        onSuccess = { onLoginSuccess("DRIVER") },
                                        onError = { errorMessage = it }
                                    )
                                }
                            }
                        )
                    }
                    "admin" -> {
                        AdminLoginView(
                            phone = phone,
                            password = password,
                            onPhoneChange = { phone = it },
                            onPasswordChange = { password = it },
                            onSubmit = {
                                errorMessage = ""
                                viewModel.loginAdmin(
                                    phone = phone,
                                    passwordString = password,
                                    onSuccess = { onLoginSuccess("ADMIN") },
                                    onError = { errorMessage = it }
                                )
                            }
                        )
                    }
                }
            }
        }

        // OTP Dialog for Customer registration verification
        if (showOtpDialog) {
            AlertDialog(
                onDismissRequest = { showOtpDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "OTP",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "التحقق من رقم الهاتف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "تم إرسال رمز التحقق المكون من 6 أرقام عبر SMS إلى الرقم ${formatPhoneNumber(phone)}.\nيرجى إدخال الرمز لإكمال تسجيل الحساب.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("رمز التحقق", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(160.dp)
                        )
                        
                        if (isSendingOtp) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(
                                onClick = {
                                    val formattedPhone = formatPhoneNumber(phone)
                                    if (activity != null && forceResendingToken != null) {
                                        isSendingOtp = true
                                        val options = PhoneAuthOptions.newBuilder(auth)
                                            .setPhoneNumber(formattedPhone)
                                            .setTimeout(60L, TimeUnit.SECONDS)
                                            .setActivity(activity)
                                            .setCallbacks(callbacks)
                                            .setForceResendingToken(forceResendingToken!!)
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    }
                                },
                                enabled = forceResendingToken != null,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("إعادة إرسال الرمز")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (verificationId.isEmpty() || otpCode.length < 6) {
                                errorMessage = "يرجى إدخال رمز التحقق المكون من 6 أرقام."
                                return@Button
                            }
                            isSendingOtp = true
                            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                            auth.signInWithCredential(credential)
                                .addOnCompleteListener { task ->
                                    isSendingOtp = false
                                    if (task.isSuccessful) {
                                        showOtpDialog = false
                                        if (registeringRole == "DRIVER") {
                                            viewModel.registerDriver(
                                                phone = phone,
                                                passwordString = password,
                                                name = fullName,
                                                nationalDocs = nationalDocsUri.toString(),
                                                carDocs = carDocsUri.toString(),
                                                carType = carType,
                                                carModel = carModel,
                                                carPhoto = carPhotoUri.toString(),
                                                personalPhoto = personalPhotoUri.toString(),
                                                onSuccess = {
                                                    successMessage = "تم تسجيلك كسائق جديد بنجاح! بانتظار موافقة الإدارة."
                                                    onLoginSuccess("DRIVER")
                                                },
                                                onError = { errorMessage = it }
                                            )
                                        } else {
                                            viewModel.registerCustomer(
                                                phone = phone,
                                                passwordString = password,
                                                name = fullName,
                                                onSuccess = { onLoginSuccess("CUSTOMER") },
                                                onError = { errorMessage = it }
                                            )
                                        }
                                    } else {
                                        errorMessage = "رمز التحقق غير صحيح: ${task.exception?.localizedMessage ?: "يرجى المحاولة مجدداً."}"
                                        showOtpDialog = false
                                    }
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isSendingOtp && otpCode.length == 6
                    ) {
                        Text("تأكيد الرمز")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOtpDialog = false }) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun LandingView(
    onCustomerClick: () -> Unit,
    onDriverClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Customer Card Button
        Card(
            onClick = onCustomerClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("customer_login_btn"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.5.dp, Color(0xFF1B3B6F)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left circle icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFFEBF5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF357EC7),
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Text Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "دخول الزبون",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اطلب رحلتك بسهولة",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
                
                // Right chevron pointing left
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color(0xFF1B3B6F),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Driver Card Button
        Card(
            onClick = onDriverClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("driver_login_btn"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.5.dp, Color(0xFF0F5A3E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left circle icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFFDCFCE7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF0F7643),
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Text Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "دخول السائق",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اقبل الرحلات في منطقتك",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
                
                // Right chevron pointing left
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color(0xFF0F5A3E),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Admin Link with security/shield check icon
        TextButton(
            onClick = onAdminClick,
            modifier = Modifier.testTag("admin_login_btn")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF357EC7),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "دخول لوحة الإدارة",
                    color = Color(0xFF357EC7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Very bottom footer text
        Text(
            text = "خدمة محلية لمدينة جبلة",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun CustomerView(
    isRegisterMode: Boolean,
    phone: String,
    password: Any, // handled as string inside, but let's use String directly
    confirmPassword: Any,
    fullName: String,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit
) {
    val phoneStr = phone
    val passwordStr = password as String
    val confirmPasswordStr = confirmPassword as String

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isRegisterMode) "تسجيل كزبون جديد" else "تسجيل دخول الزبون",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isRegisterMode) {
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("الاسم الكامل") },
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }

        OutlinedTextField(
            value = phoneStr,
            onValueChange = onPhoneChange,
            label = { Text("رقم الهاتف") },
            leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = passwordStr,
            onValueChange = onPasswordChange,
            label = { Text("كلمة المرور") },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        if (isRegisterMode) {
            var confirmPasswordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = confirmPasswordStr,
                onValueChange = onConfirmPasswordChange,
                label = { Text("تأكيد كلمة المرور") },
                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (isRegisterMode) "تحقق وتسجيل الحساب" else "تسجيل الدخول",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onToggleMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isRegisterMode) "لديك حساب بالفعل؟ تسجيل الدخول هنا" else "ليس لديك حساب؟ سجل كزبون جديد",
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DriverView(
    isRegisterMode: Boolean,
    phone: String,
    password: Any,
    fullName: String,
    carType: String,
    carModel: String,
    nationalDocsUri: Uri?,
    carDocsUri: Uri?,
    carPhotoUri: Uri?,
    personalPhotoUri: Uri?,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onCarTypeChange: (String) -> Unit,
    onCarModelChange: (String) -> Unit,
    onNationalDocsUpload: () -> Unit,
    onCarDocsUpload: () -> Unit,
    onCarPhotoUpload: () -> Unit,
    onPersonalPhotoUpload: () -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit
) {
    val phoneStr = phone
    val passwordStr = password as String

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isRegisterMode) "التسجيل كسائق جديد" else "تسجيل دخول السائق",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isRegisterMode) {
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("الاسم الكامل للسائق") },
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }

        OutlinedTextField(
            value = phoneStr,
            onValueChange = onPhoneChange,
            label = { Text("رقم الهاتف") },
            leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = passwordStr,
            onValueChange = onPasswordChange,
            label = { Text("كلمة المرور") },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        if (isRegisterMode) {
            Text(
                text = "تفاصيل السيارة",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = carType,
                    onValueChange = onCarTypeChange,
                    label = { Text("نوع السيارة (مثلاً: كيا)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = carModel,
                    onValueChange = onCarModelChange,
                    label = { Text("موديل وسنة الصنع") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "المستمسكات والوثائق المطلوبة (اضغط للرفع)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dynamic Real File Upload Slots
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UploadSlot(label = "صورة المستمسكات (الهوية / الموحدة)", uri = nationalDocsUri, onClick = onNationalDocsUpload)
                UploadSlot(label = "صورة سنوية السيارة والوثائق", uri = carDocsUri, onClick = onCarDocsUpload)
                UploadSlot(label = "صورة واضحة للسيارة", uri = carPhotoUri, onClick = onCarPhotoUpload)
                UploadSlot(label = "صورة شخصية حديثة للسائق", uri = personalPhotoUri, onClick = onPersonalPhotoUpload)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (isRegisterMode) "إرسال طلب التسجيل" else "تسجيل الدخول",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onToggleMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isRegisterMode) "لديك حساب بالفعل؟ تسجيل الدخول" else "ليس لديك حساب؟ سجل كسائق جديد الآن",
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UploadSlot(
    label: String,
    uri: Uri?,
    onClick: () -> Unit
) {
    val isUploaded = uri != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = BorderStroke(
            width = 1.dp,
            color = if (isUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUploaded) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (isUploaded) {
                    AsyncImage(
                        model = uri,
                        contentDescription = label,
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isUploaded) "تم اختيار الملف بنجاح" else "اضغط لتحديد صورة حقيقية من الاستوديو",
                        fontSize = 11.sp,
                        color = if (isUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isUploaded) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "رفع ملف",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun AdminLoginView(
    phone: String,
    password: Any,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val phoneStr = phone
    val passwordStr = password as String

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "تسجيل دخول لوحة الإدارة",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneStr,
            onValueChange = onPhoneChange,
            label = { Text("رقم الهاتف للوحة الإدارة") },
            leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = passwordStr,
            onValueChange = onPasswordChange,
            label = { Text("كلمة مرور الإدارة") },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("admin_submit_login"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "الدخول كمدير النظام",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
