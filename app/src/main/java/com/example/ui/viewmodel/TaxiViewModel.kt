package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.database.BookingEntity
import com.example.data.database.UserEntity
import com.example.data.database.DriverEntity
import com.example.data.model.ChatMessage
import com.example.data.model.Driver
import com.example.data.model.InAppNotification
import com.example.data.model.LocationPreset
import com.example.data.model.NotificationType
import com.example.data.model.ServiceType
import com.example.data.model.TaxiData
import com.example.data.repository.BookingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.random.Random

class TaxiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BookingRepository
    val history: StateFlow<List<BookingEntity>>
    val allDrivers: StateFlow<List<DriverEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BookingRepository(database.bookingDao(), database.userDriverDao())
        history = repository.allBookings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allDrivers = repository.allDrivers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        createNotificationChannel()
        restoreActiveBooking()
    }

    // Auth States
    private val _currentRole = MutableStateFlow<String?>(null) // "CUSTOMER", "DRIVER", "ADMIN"
    val currentRole = _currentRole.asStateFlow()

    private val _loggedInCustomer = MutableStateFlow<UserEntity?>(null)
    val loggedInCustomer = _loggedInCustomer.asStateFlow()

    private val _loggedInDriver = MutableStateFlow<DriverEntity?>(null)
    val loggedInDriver = _loggedInDriver.asStateFlow()

    // Customer Actions
    fun loginCustomer(phone: String, passwordString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByPhone(phone)
            if (user == null) {
                onError("الحساب غير موجود. يرجى إنشاء حساب جديد.")
            } else if (user.passwordString != passwordString) {
                onError("كلمة المرور غير صحيحة.")
            } else {
                _loggedInCustomer.value = user
                _currentRole.value = "CUSTOMER"
                onSuccess()
            }
        }
    }

    fun registerCustomer(phone: String, passwordString: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getUserByPhone(phone)
            if (existing != null) {
                onError("هذا الرقم مسجل بالفعل كزبون.")
            } else {
                val newUser = UserEntity(phone = phone, passwordString = passwordString, name = name)
                repository.insertUser(newUser)
                _loggedInCustomer.value = newUser
                _currentRole.value = "CUSTOMER"
                onSuccess()
            }
        }
    }

    // Driver Actions
    fun loginDriver(phone: String, passwordString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val driver = repository.getDriverByPhone(phone)
            if (driver == null) {
                onError("حساب السائق غير موجود.")
            } else if (driver.passwordString != passwordString) {
                onError("كلمة المرور غير صحيحة.")
            } else if (driver.status == "BLOCKED") {
                onError("تم حظر هذا الحساب من قبل الإدارة.")
            } else {
                _loggedInDriver.value = driver
                _currentRole.value = "DRIVER"
                onSuccess()
            }
        }
    }

    fun registerDriver(
        phone: String,
        passwordString: String,
        name: String,
        nationalDocs: String,
        carDocs: String,
        carType: String,
        carModel: String,
        carPhoto: String,
        personalPhoto: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.getDriverByPhone(phone)
            if (existing != null) {
                onError("هذا الرقم مسجل بالفعل كسائق.")
            } else {
                val newDriver = DriverEntity(
                    phone = phone,
                    passwordString = passwordString,
                    name = name,
                    nationalDocs = nationalDocs,
                    carDocs = carDocs,
                    carType = carType,
                    carModel = carModel,
                    carPhoto = carPhoto,
                    personalPhoto = personalPhoto,
                    status = "PENDING_APPROVAL",
                    subscriptionExpiryDate = 0L
                )
                repository.insertDriver(newDriver)
                _loggedInDriver.value = newDriver
                _currentRole.value = "DRIVER"
                onSuccess()
            }
        }
    }

    // Admin Actions
    fun loginAdmin(phone: String, passwordString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (phone == "07806722599" && passwordString == "Ba12345haa@") {
            _currentRole.value = "ADMIN"
            onSuccess()
        } else {
            onError("رقم الهاتف أو كلمة المرور غير صحيحة للوحة الإدارة.")
        }
    }

    fun logout() {
        _currentRole.value = null
        _loggedInCustomer.value = null
        _loggedInDriver.value = null
    }

    fun updateCustomerProfile(
        oldPhone: String,
        newPhone: String,
        newName: String,
        newPasswordString: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (newPhone != oldPhone) {
                val existing = repository.getUserByPhone(newPhone)
                if (existing != null) {
                    onError("رقم الهاتف الجديد مسجل مسبقاً لحساب آخر.")
                    return@launch
                }
            }
            val updatedUser = UserEntity(phone = newPhone, passwordString = newPasswordString, name = newName)
            if (newPhone != oldPhone) {
                repository.deleteUserByPhone(oldPhone)
            }
            repository.insertUser(updatedUser)
            _loggedInCustomer.value = updatedUser
            onSuccess()
        }
    }

    fun deleteCustomerAccount(phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteUserByPhone(phone)
            logout()
            onSuccess()
        }
    }

    fun updateDriverProfile(
        oldPhone: String,
        newPhone: String,
        newName: String,
        newPasswordString: String,
        newCarType: String,
        newCarModel: String,
        newCarDocs: String,
        newNationalDocs: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (newPhone != oldPhone) {
                val existing = repository.getDriverByPhone(newPhone)
                if (existing != null) {
                    onError("رقم الهاتف الجديد مسجل مسبقاً لحساب كابتن آخر.")
                    return@launch
                }
            }
            val current = _loggedInDriver.value ?: return@launch
            val updatedDriver = current.copy(
                phone = newPhone,
                passwordString = newPasswordString,
                name = newName,
                carType = newCarType,
                carModel = newCarModel,
                carDocs = newCarDocs,
                nationalDocs = newNationalDocs
            )
            if (newPhone != oldPhone) {
                repository.deleteDriverByPhone(oldPhone)
            }
            repository.insertDriver(updatedDriver)
            _loggedInDriver.value = updatedDriver
            onSuccess()
        }
    }

    fun deleteDriverAccount(phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDriverByPhone(phone)
            logout()
            onSuccess()
        }
    }

    // Admin management functions
    fun approveDriver(phone: String) {
        viewModelScope.launch {
            val oneMonthFromNow = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            repository.updateDriverStatus(phone, "APPROVED")
            repository.updateDriverSubscription(phone, oneMonthFromNow)
            if (_loggedInDriver.value?.phone == phone) {
                val updated = repository.getDriverByPhone(phone)
                _loggedInDriver.value = updated
            }
        }
    }

    fun suspendDriverSubscription(phone: String) {
        viewModelScope.launch {
            repository.updateDriverStatus(phone, "SUSPENDED")
            repository.updateDriverSubscription(phone, System.currentTimeMillis() - 1000L)
            if (_loggedInDriver.value?.phone == phone) {
                val updated = repository.getDriverByPhone(phone)
                _loggedInDriver.value = updated
            }
        }
    }

    fun renewDriverSubscription(phone: String) {
        viewModelScope.launch {
            val oneMonthFromNow = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            repository.updateDriverStatus(phone, "APPROVED")
            repository.updateDriverSubscription(phone, oneMonthFromNow)
            if (_loggedInDriver.value?.phone == phone) {
                val updated = repository.getDriverByPhone(phone)
                _loggedInDriver.value = updated
            }
        }
    }

    fun blockDriver(phone: String) {
        viewModelScope.launch {
            repository.updateDriverStatus(phone, "BLOCKED")
            if (_loggedInDriver.value?.phone == phone) {
                val updated = repository.getDriverByPhone(phone)
                _loggedInDriver.value = updated
            }
        }
    }

    fun unblockDriver(phone: String) {
        viewModelScope.launch {
            repository.updateDriverStatus(phone, "PENDING_APPROVAL")
            if (_loggedInDriver.value?.phone == phone) {
                val updated = repository.getDriverByPhone(phone)
                _loggedInDriver.value = updated
            }
        }
    }

    // State Variables
    private val _pickup = MutableStateFlow<LocationPreset?>(null)
    val pickup = _pickup.asStateFlow()

    private val _destination = MutableStateFlow<LocationPreset?>(null)
    val destination = _destination.asStateFlow()

    private val _selectedService = MutableStateFlow<ServiceType>(TaxiData.SERVICE_TYPES[0])
    val selectedService = _selectedService.asStateFlow()

    private val _activeBooking = MutableStateFlow<BookingEntity?>(null)
    val activeBooking = _activeBooking.asStateFlow()

    private val _driverPosition = MutableStateFlow<Pair<Float, Float>>(Pair(50f, 50f))
    val driverPosition = _driverPosition.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    // Screen navigation helpers
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _isCalling = MutableStateFlow(false)
    val isCalling = _isCalling.asStateFlow()

    private val _isChatting = MutableStateFlow(false)
    val isChatting = _isChatting.asStateFlow()

    // Rating dialog helper for completed trip
    private val _showRatingDialogForId = MutableStateFlow<Int?>(null)
    val showRatingDialogForId = _showRatingDialogForId.asStateFlow()

    // Instant Notification overlay state
    private val _inAppNotification = MutableStateFlow<InAppNotification?>(null)
    val inAppNotification = _inAppNotification.asStateFlow()

    fun dismissInAppNotification() {
        _inAppNotification.value = null
    }

    private var simulationJob: Job? = null

    fun setPickup(location: LocationPreset?) {
        _pickup.value = location
    }

    fun setDestination(location: LocationPreset?) {
        _destination.value = location
    }

    fun selectService(service: ServiceType) {
        _selectedService.value = service
    }

    fun startCalling(calling: Boolean) {
        _isCalling.value = calling
    }

    fun startChatting(chatting: Boolean) {
        _isChatting.value = chatting
    }

    // Send chat message
    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        val updated = _chatMessages.value + ChatMessage("user", text)
        _chatMessages.value = updated

        // Simulated driver auto-response after short delay
        viewModelScope.launch {
            delay(1500)
            val active = _activeBooking.value ?: return@launch
            val response = when (active.status) {
                "EN_ROUTE" -> {
                    val messages = listOf(
                        "أهلاً بك، أنا قادم في الطريق إليك وسأصل قريباً.",
                        "تمام، أنا عند الإشارة الآن وسأصل خلال دقائق.",
                        "قرأت رسالتك، سأكون عندك في الموعد."
                    )
                    messages[Random.nextInt(messages.size)]
                }
                "ARRIVED" -> {
                    val messages = listOf(
                        "أنا بانتظارك في الخارج أمام المدخل تماماً.",
                        "سيارتي واقفة على اليمين بجانب الرصيف.",
                        "أهلاً بك، تفضل أنا بالانتظار."
                    )
                    messages[Random.nextInt(messages.size)]
                }
                "IN_PROGRESS" -> "تمام يا غالي، رحلة سعيدة وموفقة."
                else -> "تمنياتي لك برحلة طيبة."
            }
            _chatMessages.value = _chatMessages.value + ChatMessage("driver", response)
        }
    }

    // Restore active booking if the app restarts
    private fun restoreActiveBooking() {
        viewModelScope.launch {
            val active = repository.getLatestActiveBooking()
            if (active != null) {
                _activeBooking.value = active
                _pickup.value = TaxiData.JABLEH_PRESETS.firstOrNull { it.name == active.pickupLocation }
                _destination.value = TaxiData.JABLEH_PRESETS.firstOrNull { it.name == active.destinationLocation }
                resumeSimulation(active)
            }
        }
    }

    // Accept ride by driver from console
    fun acceptRideByDriver(booking: BookingEntity, driver: DriverEntity) {
        viewModelScope.launch {
            val updated = booking.copy(
                driverName = driver.name,
                driverVehicle = "${driver.carType} ${driver.carModel}",
                driverPhone = driver.phone,
                status = "EN_ROUTE"
            )
            repository.insertOrUpdateBooking(updated)
            _activeBooking.value = updated
            showNotification(
                title = "تم قبول الرحلة! 🚕🎉",
                text = "تم قبول الطلب من قبل الكابتن ${driver.name}. الكابتن في طريقه إلى ${booking.pickupLocation}.",
                type = NotificationType.RIDE_ACCEPTED
            )
            startSimulation(updated)
        }
    }

    // Book Ride
    fun requestRide() {
        val pick = _pickup.value ?: return
        val dest = _destination.value ?: return
        val service = _selectedService.value

        _isSearching.value = true

        showNotification(
            title = "تم طلب رحلة جديدة 🚕",
            text = "جاري البحث عن كابتن قريب للوصول من ${pick.name} إلى ${dest.name}...",
            type = NotificationType.RIDE_REQUESTED
        )

        viewModelScope.launch {
            delay(3000) // Simulated driver search

            val driver = TaxiData.DRIVERS[Random.nextInt(TaxiData.DRIVERS.size)]
            val distance = calculateDistance(pick, dest)
            val rawPrice = 1500.0 + (distance * 500.0)
            val price = TaxiData.roundIqdPrice(rawPrice.toInt()).toDouble()
            val duration = (distance * 1.5).toInt().coerceAtLeast(1)

            val customer = _loggedInCustomer.value
            val custName = customer?.name ?: "زبون تكسي جبلة"
            val custPhone = customer?.phone ?: "+964 780 000 0000"

            val booking = BookingEntity(
                pickupLocation = pick.name,
                destinationLocation = dest.name,
                pickupLat = pick.x,
                pickupLng = pick.y,
                destLat = dest.x,
                destLng = dest.y,
                priceEst = price,
                distanceKm = distance,
                durationMin = duration,
                driverName = driver.name,
                driverVehicle = driver.vehicle,
                driverPhone = driver.phone,
                driverRating = driver.rating,
                userName = custName,
                userPhone = custPhone,
                status = "EN_ROUTE"
            )

            val id = repository.insertOrUpdateBooking(booking)
            val savedBooking = booking.copy(id = id.toInt())

            _activeBooking.value = savedBooking
            _isSearching.value = false

            // Set initial driver position offset from pickup
            val offsetX = if (Random.nextBoolean()) 20f else -20f
            val offsetY = if (Random.nextBoolean()) 20f else -20f
            _driverPosition.value = Pair(pick.x + offsetX, pick.y + offsetY)

            _chatMessages.value = listOf(
                ChatMessage("driver", "مرحباً! أنا سائقك ${driver.name}. سأصل إليك خلال دقائق بسيارتي ${driver.vehicle}.")
            )

            showNotification(
                title = "تم قبول الرحلة! 🎉",
                text = "قام الكابتن ${driver.name} بقبول طلبك وسيصلك بسيارة ${driver.vehicle}.",
                type = NotificationType.RIDE_ACCEPTED
            )
            startSimulation(savedBooking)
        }
    }

    // Start Simulation of driver moving
    private fun startSimulation(booking: BookingEntity) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            val pickX = booking.pickupLat
            val pickY = booking.pickupLng

            // 1. Simulating driver on the way (EN_ROUTE)
            var currentX = _driverPosition.value.first
            var currentY = _driverPosition.value.second
            var notifiedApproaching = false

            while (isActive && hypot(pickX - currentX, pickY - currentY) > 1.5f) {
                val dx = pickX - currentX
                val dy = pickY - currentY
                val dist = hypot(dx, dy)

                if (dist <= 0.001f) break

                // Trigger approaching alert when driver is close to pickup (e.g. dist <= 8.0)
                if (!notifiedApproaching && dist <= 8.0f) {
                    notifiedApproaching = true
                    showNotification(
                        title = "التكسي يقترب من موقعك! 🚕⚡",
                        text = "الكابتن ${booking.driverName} أصبح على بعد مسافة قصيرة جداً من موقع الالتقاء في ${booking.pickupLocation}. يرجى التوجه لموقع اللقاء.",
                        type = NotificationType.APPROACHING
                    )
                }

                val step = 1.5f // Speed
                currentX += (dx / dist) * step
                currentY += (dy / dist) * step
                _driverPosition.value = Pair(currentX, currentY)
                delay(150)
            }

            // Driver Arrived!
            _driverPosition.value = Pair(pickX, pickY)
            val arrivedBooking = booking.copy(status = "ARRIVED")
            repository.insertOrUpdateBooking(arrivedBooking)
            _activeBooking.value = arrivedBooking

            _chatMessages.value = _chatMessages.value + ChatMessage(
                "driver", "لقد وصلت إلى موقعك! أنا بالانتظار في الخارج."
            )
            showNotification(
                title = "وصل التكسي! 📍",
                text = "سائقك ${booking.driverName} بانتظارك الآن في موقع الالتقاء (${booking.pickupLocation}).",
                type = NotificationType.ARRIVED
            )
        }
    }

    // Transition from EN_ROUTE / ARRIVED to IN_PROGRESS (Started Ride)
    fun boardTaxi() {
        val active = _activeBooking.value ?: return

        viewModelScope.launch {
            val currentBooking = if (active.status == "EN_ROUTE") {
                val arrived = active.copy(status = "ARRIVED")
                repository.insertOrUpdateBooking(arrived)
                _activeBooking.value = arrived
                arrived
            } else {
                active
            }

            if (currentBooking.status == "ARRIVED" || currentBooking.status == "EN_ROUTE" || currentBooking.status == "IN_PROGRESS") {
                val updated = currentBooking.copy(status = "IN_PROGRESS")
                repository.insertOrUpdateBooking(updated)
                _activeBooking.value = updated

                showNotification(
                    title = "بدء الرحلة 🚀",
                    text = "انطلقت الرحلة الآن باتجاه ${updated.destinationLocation}.",
                    type = NotificationType.IN_PROGRESS
                )

                // Resume simulation towards destination
                simulationJob?.cancel()
                simulationJob = viewModelScope.launch {
                    val destX = updated.destLat
                    val destY = updated.destLng
                    var currentX = _driverPosition.value.first
                    var currentY = _driverPosition.value.second

                    while (isActive && hypot(destX - currentX, destY - currentY) > 1.5f) {
                        val dx = destX - currentX
                        val dy = destY - currentY
                        val dist = hypot(dx, dy)
                        if (dist <= 0.001f) break
                        val step = 2.0f // Faster speed for trip
                        currentX += (dx / dist) * step
                        currentY += (dy / dist) * step
                        _driverPosition.value = Pair(currentX, currentY)
                        delay(150)
                    }

                    // Arrived at destination (COMPLETED)
                    _driverPosition.value = Pair(destX, destY)
                    val completedBooking = updated.copy(status = "COMPLETED")
                    repository.insertOrUpdateBooking(completedBooking)

                    showNotification(
                        title = "الوصول إلى الوجهة 🏁",
                        text = "لقد وصلت إلى وجهتك بسلام! شكراً لاختيارك تكسي جبلة.",
                        type = NotificationType.COMPLETED
                    )

                    _showRatingDialogForId.value = completedBooking.id
                    _activeBooking.value = null
                    _pickup.value = null
                    _destination.value = null
                }
            }
        }
    }

    fun startChat() { startChatting(true) }
    fun startCall() { startCalling(true) }
    fun startRide() { boardTaxi() }

    fun completeRide() {
        val active = _activeBooking.value ?: return
        viewModelScope.launch {
            simulationJob?.cancel()
            val completedBooking = active.copy(status = "COMPLETED")
            repository.insertOrUpdateBooking(completedBooking)
            _activeBooking.value = null
            _pickup.value = null
            _destination.value = null
            _isChatting.value = false
            _isCalling.value = false
            showNotification(
                title = "تم إنهاء الرحلة 🏁",
                text = "تم إنهاء الرحلة وتسديد المبالغ بنجاح.",
                type = NotificationType.COMPLETED
            )
        }
    }

    // Cancel Active Ride
    fun cancelRide() {
        val active = _activeBooking.value ?: return
        viewModelScope.launch {
            simulationJob?.cancel()
            val cancelled = active.copy(status = "CANCELLED")
            repository.insertOrUpdateBooking(cancelled)
            _activeBooking.value = null
            _pickup.value = null
            _destination.value = null
            _isChatting.value = false
            _isCalling.value = false
            showNotification(
                title = "تم إلغاء الرحلة ❌",
                text = "تم إلغاء رحلتك الحالية بنجاح.",
                type = NotificationType.CANCELLED
            )
        }
    }

    // Rate completed ride
    fun submitRating(bookingId: Int, rating: Int) {
        viewModelScope.launch {
            repository.updateBookingRating(bookingId, rating)
            _showRatingDialogForId.value = null
        }
    }

    fun dismissRating() {
        _showRatingDialogForId.value = null
    }

    // Helper to resume simulation if restored from state
    private fun resumeSimulation(booking: BookingEntity) {
        if (booking.status == "EN_ROUTE") {
            val offsetX = if (Random.nextBoolean()) 20f else -20f
            val offsetY = if (Random.nextBoolean()) 20f else -20f
            _driverPosition.value = Pair(booking.pickupLat + offsetX, booking.pickupLng + offsetY)
            startSimulation(booking)
        } else if (booking.status == "ARRIVED") {
            _driverPosition.value = Pair(booking.pickupLat, booking.pickupLng)
        } else if (booking.status == "IN_PROGRESS") {
            _driverPosition.value = Pair(booking.pickupLat, booking.pickupLng)
            // Start simulation directly from pickup to destination
            viewModelScope.launch {
                val updated = booking
                _activeBooking.value = updated

                simulationJob?.cancel()
                simulationJob = viewModelScope.launch {
                    val destX = updated.destLat
                    val destY = updated.destLng
                    var currentX = _driverPosition.value.first
                    var currentY = _driverPosition.value.second

                    while (isActive && hypot(destX - currentX, destY - currentY) > 1.5f) {
                        val dx = destX - currentX
                        val dy = destY - currentY
                        val dist = hypot(dx, dy)
                        if (dist <= 0.001f) break
                        val step = 2.0f
                        currentX += (dx / dist) * step
                        currentY += (dy / dist) * step
                        _driverPosition.value = Pair(currentX, currentY)
                        delay(150)
                    }

                    _driverPosition.value = Pair(destX, destY)
                    val completedBooking = updated.copy(status = "COMPLETED")
                    repository.insertOrUpdateBooking(completedBooking)
                    showNotification(
                        title = "الوصول إلى الوجهة 🏁",
                        text = "لقد وصلت إلى وجهتك بسلام! شكراً لاختيارك تكسي جبلة.",
                        type = NotificationType.COMPLETED
                    )
                    _showRatingDialogForId.value = completedBooking.id
                    _activeBooking.value = null
                    _pickup.value = null
                    _destination.value = null
                }
            }
        }
    }

    private fun calculateDistance(pick: LocationPreset, dest: LocationPreset): Double {
        val gridDistance = hypot(pick.x - dest.x, pick.y - dest.y)
        // Convert grid distance to simulated kilometers (e.g. 100 units = 10km)
        return String.format("%.1f", gridDistance / 10f).toDouble().coerceAtLeast(0.5)
    }

    // Android Status Bar Notifications & In-App Heads-up Overlay
    fun showNotification(title: String, text: String, type: NotificationType = NotificationType.INFO) {
        // 1. In-App Heads-Up Notification Overlay State
        _inAppNotification.value = InAppNotification(
            title = title,
            message = text,
            type = type
        )

        // 2. Android System Notification with Heads-Up (PRIORITY_MAX)
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage(getApplication<Application>().packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            getApplication(),
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(getApplication(), "taxi_jableh_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات تكسي جبلة الفورية"
            val descriptionText = "إشعارات تتبع الرحلات، اقتراب التكسي، والوصول المباشر"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("taxi_jableh_channel", name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
