package com.example.data.model

data class Driver(
    val name: String,
    val vehicle: String,
    val licensePlate: String,
    val phone: String,
    val rating: Float,
    val avatarId: Int // index of mock avatars
)

data class LocationPreset(
    val name: String,
    val x: Float, // Grid coordinates (0..100) for custom Canvas drawing
    val y: Float
)

data class ServiceType(
    val id: String,
    val name: String,
    val pricePerKm: Double,
    val etaMins: Int,
    val description: String
)

data class ChatMessage(
    val sender: String, // "driver" or "user"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object TaxiData {
    val JABLEH_PRESETS = listOf(
        LocationPreset("قضاء كوثى (مركز جبلة)", 50f, 50f),
        LocationPreset("سوق جبلة المركزي (بابل)", 45f, 62f),
        LocationPreset("مستشفى كوثى العام (جبلة)", 65f, 30f),
        LocationPreset("حي العسكري (جبلة كوثى)", 30f, 40f),
        LocationPreset("حي الوحدة (المشروع)", 70f, 45f),
        LocationPreset("جامع كوثى الكبير", 48f, 54f),
        LocationPreset("تقاطع المشروع الرئيسي (بابل)", 50f, 35f),
        LocationPreset("بلدية جبلة وبناية الجنسية", 55f, 35f),
        LocationPreset("مدخل جبلة (طريق حلة - بغداد)", 80f, 75f)
    )

    val IRAQ_GOVERNORATES = listOf(
        LocationPreset("محافظة بغداد (العاصمة 🇮🇶)", 48f, 38f),
        LocationPreset("محافظة بابل (الحلة / جبلة)", 50f, 50f),
        LocationPreset("محافظة النجف الأشرف 🕌", 40f, 65f),
        LocationPreset("محافظة كربلاء المقدسة 🕌", 38f, 52f),
        LocationPreset("محافظة البصرة (الفاو / شط العرب)", 85f, 90f),
        LocationPreset("محافظة أربيل (كردستان 🏰)", 65f, 12f),
        LocationPreset("محافظة نينوى (الموصل)", 50f, 10f),
        LocationPreset("محافظة ذي قار (الناصرية / أور)", 72f, 75f),
        LocationPreset("محافظة واسط (الكوت)", 62f, 50f),
        LocationPreset("محافظة القادسية (الديوانية)", 50f, 62f),
        LocationPreset("محافظة المثنى (السماوة)", 55f, 78f),
        LocationPreset("محافظة ميسان (العمارة)", 78f, 65f),
        LocationPreset("محافظة صلاح الدين (تكريت / سامراء)", 46f, 25f),
        LocationPreset("محافظة الأنبار (الرمادي / الفلوجة)", 30f, 35f),
        LocationPreset("محافظة كركوك", 58f, 20f),
        LocationPreset("محافظة ديالى (بعقوبة)", 56f, 32f),
        LocationPreset("محافظة دهوك", 48f, 5f),
        LocationPreset("محافظة السليمانية", 72f, 18f)
    )

    val ALL_LOCATIONS = JABLEH_PRESETS + IRAQ_GOVERNORATES

    fun roundIqdPrice(price: Int): Int {
        if (price <= 0) return 0
        val thousands = (price / 1000) * 1000
        val remainder = price % 1000
        val roundedRemainder = when {
            remainder < 125 -> 0
            remainder < 375 -> 250
            remainder < 625 -> 500
            remainder < 875 -> 750
            else -> 1000
        }
        return thousands + roundedRemainder
    }

    fun formatIqdPrice(rawPrice: Number): String {
        val rounded = roundIqdPrice(rawPrice.toInt())
        return String.format(java.util.Locale.ENGLISH, "%,d د.ع", rounded)
    }

    val DRIVERS = listOf(
        Driver("أبو أحمد", "كيا سيراتو صفراء (تكسي)", "بابل - 48293", "+964 780 111 2222", 4.9f, 0),
        Driver("علي يوسف", "هيونداي إلنترا صفراء", "بابل - 90283", "+964 770 333 4444", 4.8f, 1),
        Driver("سامر شعبان", "سايبا صفراء (تكسي)", "بابل - 12847", "+964 750 555 6666", 4.7f, 2),
        Driver("محمد إسماعيل", "شيري أريزو صفراء", "بابل - 78219", "+964 790 777 8888", 4.9f, 3)
    )

    val SERVICE_TYPES = listOf(
        ServiceType("economy", "توفير (سايبا)", 1200.0, 3, "رحلة مريحة وموفرة للمشاوير اليومية"),
        ServiceType("vip", "جبلة VIP (كيا/هيونداي)", 2000.0, 5, "سيارات حديثة ومكيفة لراحة قصوى"),
        ServiceType("express", "تكسي إكسبرس (سريع)", 1600.0, 2, "أقرب سيارة متوفرة لتصلك بأسرع وقت")
    )

    val CHAT_SUGGESTIONS = listOf(
        "أنا بانتظارك في الخارج",
        "أنا عند الإشارة وسأصل خلال دقيقة",
        "هل يمكنك تحديد موقعك بدقة أكثر؟",
        "تمام، أنا قادم في الطريق",
        "لقد وصلت إلى نقطة الانطلاق"
    )
}
