package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.LocationPreset
import com.example.data.model.TaxiData

// Defined representation for procedural streets and houses in Jableh town
data class MapHouse(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val colorIndex: Int
)

data class MapStreet(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

enum class OsmMapStyle(val title: String, val attribution: String) {
    STANDARD("OpenStreetMap القياسي", "© OpenStreetMap contributors"),
    HOT("OpenStreetMap الإنساني (HOT)", "© OpenStreetMap / Humanitarian OpenStreetMap Team"),
    DARK("OpenStreetMap الليلي", "© OpenStreetMap / CartoDB")
}

@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    pickup: LocationPreset?,
    destination: LocationPreset?,
    driverPos: Pair<Float, Float>,
    showRoute: Boolean = true,
    onMapClick: ((Float, Float) -> Unit)? = null
) {
    // Zoom & Pan state (starts zoomed-in on current location)
    var scale by remember { mutableStateOf(2.2f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var mapStyle by remember { mutableStateOf(OsmMapStyle.STANDARD) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var hasAutoCentered by remember { mutableStateOf(false) }

    // Text Measurer for Arabic landmark names & pre-cached layout map to avoid 60fps allocations
    val textMeasurer = rememberTextMeasurer()

    // Pulse animations for markers (glowing rings)
    val infiniteTransition = rememberInfiniteTransition(label = "map_pulsar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Flowing route dash animation
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "route_dash"
    )

    // Authentic OpenStreetMap (OSM) Carto Palette
    val isDark = mapStyle == OsmMapStyle.DARK
    val mapBgColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFFF2EFE9) // OSM Standard light beige land
        OsmMapStyle.HOT -> Color(0xFFF4F3F0)      // OSM HOT soft background
        OsmMapStyle.DARK -> Color(0xFF19212C)     // OSM Dark night canvas
    }
    val canalColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFFAAD3DF) // OSM standard water blue
        OsmMapStyle.HOT -> Color(0xFF92C5DE)      // OSM HOT turquoise water
        OsmMapStyle.DARK -> Color(0xFF0F3045)     // OSM Dark deep sea
    }
    val riverColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFF1976D2) // Deep blue for Tigris & Euphrates
        OsmMapStyle.HOT -> Color(0xFF0288D1)
        OsmMapStyle.DARK -> Color(0xFF0277BD)
    }
    val roadColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFFFFFFFF) // OSM Standard white local roads
        OsmMapStyle.HOT -> Color(0xFFFFFFFF)
        OsmMapStyle.DARK -> Color(0xFF2C3848)
    }
    val roadBorderColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFFCFD8DC) // OSM casing grey
        OsmMapStyle.HOT -> Color(0xFFD3D3D3)
        OsmMapStyle.DARK -> Color(0xFF1E2836)
    }
    val highwayColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFFFCD6A4) // OSM standard orange highway
        OsmMapStyle.HOT -> Color(0xFFE27B73)      // OSM HOT primary red route
        OsmMapStyle.DARK -> Color(0xFF3B4D63)
    }
    val parkColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFFC8E6C9) // OSM Standard green forest/park
        OsmMapStyle.HOT -> Color(0xFFD9E8D3)
        OsmMapStyle.DARK -> Color(0xFF132B1D)
    }
    val gridColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFFE5E0D8)
        OsmMapStyle.HOT -> Color(0xFFE2DFD8)
        OsmMapStyle.DARK -> Color(0xFF202B3A)
    }
    val textColor = when (mapStyle) {
        OsmMapStyle.STANDARD -> Color(0xFF1A237E)
        OsmMapStyle.HOT -> Color(0xFF212121)
        OsmMapStyle.DARK -> Color(0xFFECEFF1)
    }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Pre-measure all preset label texts across Iraq to prevent GC allocations during 60fps animations
    val presetTextLayouts = remember(textColor) {
        TaxiData.ALL_LOCATIONS.associateWith { preset ->
            textMeasurer.measure(
                text = preset.name,
                style = TextStyle(
                    color = textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    // Generate a rich set of deterministic houses
    val houses = remember {
        val list = mutableListOf<MapHouse>()
        val neighborhoods = listOf(
            Pair(30f, 40f), // Al-Askari
            Pair(50f, 52f), // Central Jableh
            Pair(70f, 45f), // Al-Wahda
            Pair(65f, 30f), // Hospital area
            Pair(45f, 62f), // Central Market area
            Pair(55f, 35f)  // Municipality area
        )
        
        neighborhoods.forEach { (centerX, centerY) ->
            val seed = (centerX * 17 + centerY * 43).toInt()
            val random = java.util.Random(seed.toLong())
            for (row in -3..3) {
                for (col in -3..3) {
                    // organic layout variations
                    if (random.nextFloat() > 0.85f) continue
                    
                    val hx = centerX + col * 2.8f + (random.nextFloat() - 0.5f) * 0.7f
                    val hy = centerY + row * 2.8f + (random.nextFloat() - 0.5f) * 0.7f
                    
                    // Exclude river/canal path
                    val riverYAtX = if (hx < 40f) {
                        20f + (hx / 40f) * 10f
                    } else if (hx < 60f) {
                        30f + ((hx - 40f) / 20f) * 40f
                    } else {
                        70f + ((hx - 60f) / 40f) * 20f
                    }
                    if (Math.abs(hy - riverYAtX) < 4.5f) continue
                    
                    // Exclude highways/main roads
                    if (Math.abs(hx - 80f) < 4f) continue
                    if (Math.abs(hx - 48f) < 3.5f) continue
                    if (Math.abs(hy - 50f) < 3f) continue
                    if (Math.abs(hy - 35f) < 3f) continue
                    
                    list.add(
                        MapHouse(
                            x = hx,
                            y = hy,
                            width = 1.3f + random.nextFloat() * 0.7f,
                            height = 1.3f + random.nextFloat() * 0.7f,
                            colorIndex = random.nextInt(4)
                        )
                    )
                }
            }
        }
        
        // Add extra random residential blocks outside the neighborhood centers
        val randomOutside = java.util.Random(12345L)
        for (i in 0..120) {
            val hx = randomOutside.nextFloat() * 100f
            val hy = randomOutside.nextFloat() * 100f
            
            val riverYAtX = if (hx < 40f) {
                20f + (hx / 40f) * 10f
            } else if (hx < 60f) {
                30f + ((hx - 40f) / 20f) * 40f
            } else {
                70f + ((hx - 60f) / 40f) * 20f
            }
            if (Math.abs(hy - riverYAtX) < 4.5f) continue
            if (Math.abs(hx - 80f) < 4f) continue
            if (Math.abs(hx - 48f) < 3.5f) continue
            if (Math.abs(hy - 50f) < 3f) continue
            if (Math.abs(hy - 35f) < 3f) continue
            
            var tooCloseToHub = false
            neighborhoods.forEach { (cx, cy) ->
                if (Math.abs(hx - cx) < 9f && Math.abs(hy - cy) < 9f) {
                    tooCloseToHub = true
                }
            }
            if (tooCloseToHub) continue
            
            list.add(
                MapHouse(
                    x = hx,
                    y = hy,
                    width = 1.2f + randomOutside.nextFloat() * 0.6f,
                    height = 1.2f + randomOutside.nextFloat() * 0.6f,
                    colorIndex = randomOutside.nextInt(4)
                )
            )
        }
        list
    }

    // Generate secondary streets network to represent minor block divisions
    val secondaryStreets = remember {
        val list = mutableListOf<MapStreet>()
        val neighborhoods = listOf(
            Pair(30f, 40f), // Al-Askari
            Pair(50f, 52f), // Central
            Pair(70f, 45f), // Al-Wahda
            Pair(65f, 30f), // Hospital area
            Pair(45f, 62f), // Central Market area
            Pair(55f, 35f)  // Municipality area
        )
        
        neighborhoods.forEach { (cx, cy) ->
            // Sub-grid lines for each neighborhood
            list.add(MapStreet(cx - 9f, cy - 4f, cx + 9f, cy - 4f))
            list.add(MapStreet(cx - 9f, cy + 4f, cx + 9f, cy + 4f))
            list.add(MapStreet(cx - 4f, cy - 9f, cx - 4f, cy + 9f))
            list.add(MapStreet(cx + 4f, cy - 9f, cx + 4f, cy + 9f))
        }
        
        // Add nice scenic alleyways and secondary roads
        list.add(MapStreet(10f, 10f, 40f, 10f))
        list.add(MapStreet(40f, 10f, 40f, 30f))
        list.add(MapStreet(10f, 80f, 45f, 80f))
        list.add(MapStreet(25f, 50f, 25f, 80f))
        list.add(MapStreet(60f, 10f, 75f, 10f))
        list.add(MapStreet(75f, 10f, 75f, 35f))
        list.add(MapStreet(90f, 20f, 90f, 60f))
        list.add(MapStreet(60f, 90f, 95f, 90f))
        list.add(MapStreet(90f, 60f, 90f, 90f))
        list.add(MapStreet(60f, 70f, 60f, 90f))
        list.add(MapStreet(5f, 30f, 25f, 30f))
        
        list
    }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .background(mapBgColor)
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Auto-center on user/pickup or driver position on launch
        LaunchedEffect(pickup, driverPos, containerWidth, containerHeight) {
            if (!hasAutoCentered && containerWidth > 0 && containerHeight > 0) {
                val targetX = pickup?.x ?: driverPos.first
                val targetY = pickup?.y ?: driverPos.second
                val pxX = (targetX / 100f) * containerWidth
                val pxY = (targetY / 100f) * containerHeight

                offset = Offset(
                    x = (containerWidth / 2f) - (pxX * scale),
                    y = (containerHeight / 2f) - (pxY * scale)
                )
                hasAutoCentered = true
            }
        }
        // Main Interactive Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Custom Gestures handling: zoom with pinch, drag to pan, tap to select coordinates
                    detectTapGestures(
                        onDoubleTap = {
                            // Double tap to reset
                            scale = 1f
                            offset = Offset.Zero
                        },
                        onTap = { pressOffset ->
                            if (onMapClick != null) {
                                // Translate screen coordinates back to map grid (0..100) taking scale & offset into account
                                val untransformedX = (pressOffset.x - offset.x) / scale
                                val untransformedY = (pressOffset.y - offset.y) / scale
                                val gridX = (untransformedX / size.width) * 100f
                                val gridY = (untransformedY / size.height) * 100f
                                onMapClick(gridX.coerceIn(0f, 100f), gridY.coerceIn(0f, 100f))
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.2f, 8f)
                        // Allow panning across the full canvas
                        offset = Offset(
                            x = offset.x + pan.x,
                            y = offset.y + pan.y
                        )
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // Local converter to project 0..100 grid to Canvas pixels
            fun gridXToPx(gridX: Float): Float = (gridX / 100f) * width
            fun gridYToPx(gridY: Float): Float = (gridY / 100f) * height

            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale)
            }) {
                // 1. Draw Grid Lines
                val gridLinesCount = 10
                for (i in 0..gridLinesCount) {
                    val x = (i.toFloat() / gridLinesCount) * width
                    val y = (i.toFloat() / gridLinesCount) * height
                    // Vertical grid line
                    drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
                    // Horizontal grid line
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                }

                // 2. Draw Parks and Farms (Green spots in Jableh town layout)
                drawRoundRect(
                    color = parkColor,
                    topLeft = Offset(gridXToPx(25f), gridYToPx(35f)),
                    size = Size(gridXToPx(15f), gridYToPx(15f)),
                    cornerRadius = CornerRadius(20f, 20f)
                )
                drawRoundRect(
                    color = parkColor,
                    topLeft = Offset(gridXToPx(60f), gridYToPx(55f)),
                    size = Size(gridXToPx(18f), gridYToPx(12f)),
                    cornerRadius = CornerRadius(20f, 20f)
                )

                // 3. Draw Water Canal (Kutha historical river branch)
                val riverPath = Path().apply {
                    moveTo(0f, gridYToPx(20f))
                    quadraticTo(
                        gridXToPx(40f), gridYToPx(30f),
                        gridXToPx(60f), gridYToPx(70f)
                    )
                    lineTo(width, gridYToPx(90f))
                }
                drawPath(
                    path = riverPath,
                    color = canalColor,
                    style = Stroke(width = 24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 3b. Draw secondary streets (drawn BEFORE houses and BEFORE main roads to layer correctly)
                secondaryStreets.forEach { street ->
                    val start = Offset(gridXToPx(street.startX), gridYToPx(street.startY))
                    val end = Offset(gridXToPx(street.endX), gridYToPx(street.endY))
                    drawLine(
                        color = roadBorderColor,
                        start = start,
                        end = end,
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                }
                secondaryStreets.forEach { street ->
                    val start = Offset(gridXToPx(street.startX), gridYToPx(street.startY))
                    val end = Offset(gridXToPx(street.endX), gridYToPx(street.endY))
                    drawLine(
                        color = roadColor,
                        start = start,
                        end = end,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                // 3c. Draw Houses/Buildings with high-fidelity 3D depth styling
                val houseColors = if (isDark) {
                    listOf(
                        Color(0xFF2E2620), // Soft sandy dark clay
                        Color(0xFF222C38), // Deep slate-blue
                        Color(0xFF292E36), // Cozy warm grey
                        Color(0xFF352C28)  // Dark Terracotta
                    )
                } else {
                    listOf(
                        Color(0xFFFFF1DC), // Warm golden sand
                        Color(0xFFEFF3F6), // Light cool grey-white
                        Color(0xFFF1EAE4), // Soft clay beige
                        Color(0xFFFFEAD2)  // Soft sunset terracotta
                    )
                }
                val houseBorderColor = if (isDark) Color(0xFF1E2B38) else Color(0xFFCFD8DC)

                houses.forEach { house ->
                    val hx = gridXToPx(house.x)
                    val hy = gridYToPx(house.y)
                    val hw = gridXToPx(house.width)
                    val hh = gridYToPx(house.height)
                    
                    // Draw tiny building shadow for realistic 3D map depth
                    drawRoundRect(
                        color = if (isDark) Color(0x22000000) else Color(0x0C000000),
                        topLeft = Offset(hx + 1.5f, hy + 1.5f),
                        size = Size(hw, hh),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    
                    // Main Building structure
                    drawRoundRect(
                        color = houseColors[house.colorIndex % houseColors.size],
                        topLeft = Offset(hx, hy),
                        size = Size(hw, hh),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    
                    // Delicate borders for high-fidelity rendering
                    drawRoundRect(
                        color = houseBorderColor,
                        topLeft = Offset(hx, hy),
                        size = Size(hw, hh),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1f)
                    )
                    
                    // Accent details on roofs (e.g. typical water tanks or secondary levels) when zoomed in
                    if (scale > 2.5f) {
                        drawRect(
                            color = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                            topLeft = Offset(hx + hw * 0.25f, hy + hh * 0.25f),
                            size = Size(hw * 0.4f, hh * 0.4f)
                        )
                    }
                }

                // 4. Draw Streets and Roads Grid
                // We draw standard streets connecting major nodes
                val streets = listOf(
                    // Horizontal major road
                    Pair(Offset(0f, gridYToPx(50f)), Offset(width, gridYToPx(50f))),
                    // Secondary horizontal
                    Pair(Offset(0f, gridYToPx(35f)), Offset(width, gridYToPx(35f))),
                    // Vertical major road
                    Pair(Offset(gridXToPx(48f), 0f), Offset(gridXToPx(48f), height)),
                    // Diagonal road connecting entry route
                    Pair(Offset(gridXToPx(30f), gridYToPx(40f)), Offset(gridXToPx(80f), gridYToPx(75f)))
                )

                // Draw roads with background border for high quality contrast
                streets.forEach { road ->
                    drawLine(color = roadBorderColor, start = road.first, end = road.second, strokeWidth = 16f, cap = StrokeCap.Round)
                }
                streets.forEach { road ->
                    drawLine(color = roadColor, start = road.first, end = road.second, strokeWidth = 10f, cap = StrokeCap.Round)
                }

                // Main Baghdad - Hilla Highway (thick golden/yellow road on the right entry)
                val highwayPoints = Pair(Offset(gridXToPx(80f), 0f), Offset(gridXToPx(80f), height))
                drawLine(color = roadBorderColor, start = highwayPoints.first, end = highwayPoints.second, strokeWidth = 24f)
                drawLine(color = highwayColor, start = highwayPoints.first, end = highwayPoints.second, strokeWidth = 16f)

                // 5. Draw Faint Preset Labels/Points for all Iraq Locations and Governorates
                val visiblePresets = if (scale <= 1.2f) TaxiData.IRAQ_GOVERNORATES else TaxiData.ALL_LOCATIONS
                visiblePresets.forEach { preset ->
                    val px = gridXToPx(preset.x)
                    val py = gridYToPx(preset.y)

                    // Draw a prominent pin/dot for governorate or preset
                    val isGovernorate = TaxiData.IRAQ_GOVERNORATES.contains(preset)
                    val dotColor = if (isGovernorate) Color(0xFFD32F2F) else primaryColor.copy(alpha = 0.6f)
                    
                    drawCircle(
                        color = dotColor.copy(alpha = 0.3f),
                        radius = if (isGovernorate) 12f else 6f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = dotColor,
                        radius = if (isGovernorate) 6f else 4f,
                        center = Offset(px, py)
                    )

                    // Draw pre-measured Arabic text name
                    presetTextLayouts[preset]?.let { textLayoutResult ->
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(px - textLayoutResult.size.width / 2f, py + (if (isGovernorate) 10f else 6f))
                        )
                    }
                }

                // 6. Draw Animated Dotted Route path from Pickup to Destination
                if (showRoute && pickup != null && destination != null) {
                    val startOffset = Offset(gridXToPx(pickup.x), gridYToPx(pickup.y))
                    val endOffset = Offset(gridXToPx(destination.x), gridYToPx(destination.y))

                    // Draw background route line
                    drawLine(
                        color = primaryColor.copy(alpha = 0.3f),
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 12f,
                        cap = StrokeCap.Round
                    )

                    // Draw animated dashing progress line
                    drawLine(
                        color = primaryColor,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 6f,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(25f, 20f),
                            phase = -dashOffset
                        ),
                        cap = StrokeCap.Round
                    )
                }

                // 7. Draw Pickup Marker (Green Pin with glowing aura)
                if (pickup != null) {
                    val px = gridXToPx(pickup.x)
                    val py = gridYToPx(pickup.y)

                    // Pulse outer circle
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = pulseAlpha),
                        radius = 25f * pulseScale,
                        center = Offset(px, py)
                    )

                    // Solid outer ring
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = Offset(px, py)
                    )

                    // Solid inner marker
                    drawCircle(
                        color = Color(0xFF2E7D32),
                        radius = 9f,
                        center = Offset(px, py)
                    )
                }

                // 8. Draw Destination Marker (Red Flag Pin with glowing aura)
                if (destination != null) {
                    val dx = gridXToPx(destination.x)
                    val dy = gridYToPx(destination.y)

                    // Pulse outer circle
                    drawCircle(
                        color = Color(0xFFE53935).copy(alpha = pulseAlpha),
                        radius = 25f * pulseScale,
                        center = Offset(dx, dy)
                    )

                    // Solid outer ring
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = Offset(dx, dy)
                    )

                    // Solid inner marker
                    drawCircle(
                        color = Color(0xFFC62828),
                        radius = 9f,
                        center = Offset(dx, dy)
                    )
                }

                // 9. Draw Live Driver Location Badge
                val drX = gridXToPx(driverPos.first)
                val drY = gridYToPx(driverPos.second)

                // Pulse outer circle
                drawCircle(
                    color = Color(0xFFFFB300).copy(alpha = pulseAlpha),
                    radius = 30f * pulseScale,
                    center = Offset(drX, drY)
                )

                // Draw Driver's Taxi marker ring
                drawCircle(
                    color = Color.White,
                    radius = 14f,
                    center = Offset(drX, drY)
                )
                drawCircle(
                    color = Color(0xFFFFC107),
                    radius = 11f,
                    center = Offset(drX, drY)
                )
                // Draw tiny center core for driver
                drawCircle(
                    color = Color(0xFFE65100),
                    radius = 5f,
                    center = Offset(drX, drY)
                )
            }
        }

        // OpenStreetMap Badge & Watermark Attribution (Bottom-Start)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "OpenStreetMap",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "OpenStreetMap 🗺️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = mapStyle.attribution,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Map View Level Switcher (Top-Start)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (scale <= 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = if (scale <= 1.0f) Color.White else MaterialTheme.colorScheme.onSurface,
                shadowElevation = 3.dp,
                modifier = Modifier.clickable {
                    scale = 0.5f
                    offset = Offset.Zero
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🇮🇶 خارطة العراق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (scale > 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = if (scale > 1.0f) Color.White else MaterialTheme.colorScheme.onSurface,
                shadowElevation = 3.dp,
                modifier = Modifier.clickable {
                    scale = 1.8f
                    offset = Offset.Zero
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("📍 جبلة / بابل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // OpenStreetMap Layer / Style Selector Button (Top-End)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            FloatingActionButton(
                onClick = { showStyleMenu = !showStyleMenu },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "طبقات OpenStreetMap", modifier = Modifier.size(20.dp))
            }

            DropdownMenu(
                expanded = showStyleMenu,
                onDismissRequest = { showStyleMenu = false }
            ) {
                OsmMapStyle.values().forEach { style ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = style.title,
                                    fontWeight = if (mapStyle == style) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (mapStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            mapStyle = style
                            showStyleMenu = false
                        }
                    )
                }
            }
        }

        // Floating On-Screen Map Zoom & Reset Controls in the corner
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .width(44.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = (scale + 0.4f).coerceIn(0.2f, 8f) },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
            }

            FloatingActionButton(
                onClick = { scale = (scale - 0.4f).coerceIn(0.2f, 8f) },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
            }

            FloatingActionButton(
                onClick = {
                    scale = 2.2f
                    val targetX = pickup?.x ?: driverPos.first
                    val targetY = pickup?.y ?: driverPos.second
                    val pxX = (targetX / 100f) * containerWidth
                    val pxY = (targetY / 100f) * containerHeight
                    offset = Offset(
                        x = (containerWidth / 2f) - (pxX * scale),
                        y = (containerHeight / 2f) - (pxY * scale)
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(16.dp))
            }
        }
    }
}

