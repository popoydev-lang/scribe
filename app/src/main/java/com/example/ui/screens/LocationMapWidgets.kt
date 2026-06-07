package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.CinzelBoldFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LoraFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BaptistChurch
import java.util.Locale
import kotlin.math.*

// ==========================================
// 1. DATA MODELS & PHILIPPINES GEO DATA
// ==========================================

data class MapCheckLocation(
    val address: String,
    val province: String,
    val cityMunicipality: String,
    val description: String
)

data class AutocompleteSuggestion(
    val label: String,
    val province: String,
    val cityMunicipality: String,
    val latitude: Double,
    val longitude: Double,
    val district: String = ""
)

enum class MapDisplayStyle {
    STANDARD,
    SATELLITE
}

// Preset dynamic Philippine location suggestions for Search Autocomplete
val PH_AUTOCOMPLETE_SUGGESTIONS = listOf(
    AutocompleteSuggestion("Tondo, Manila", "Metro Manila", "Manila", 14.6125, 120.9723, "District I"),
    AutocompleteSuggestion("Diliman, Quezon City", "Metro Manila", "Quezon City", 14.6549, 121.0492, "Diliman"),
    AutocompleteSuggestion("Ecoland, Davao City", "Davao del Sur", "Davao City", 7.0545, 125.5941, "Talomo"),
    AutocompleteSuggestion("Matina, Davao City", "Davao del Sur", "Davao City", 7.0707, 125.6087, "Matina"),
    AutocompleteSuggestion("Mandaue City, Cebu", "Cebu", "Mandaue", 10.3446, 123.9390, "Centro"),
    AutocompleteSuggestion("Jones Avenue, Cebu City", "Cebu", "Cebu City", 10.3157, 123.8854, "Jones"),
    AutocompleteSuggestion("General Luna, Iloilo City", "Iloilo", "Iloilo City", 10.7202, 122.5621, "City Proper"),
    AutocompleteSuggestion("Leonard Wood, Baguio City", "Benguet", "Baguio", 16.4116, 120.6083, "Baguio Proper"),
    AutocompleteSuggestion("Subic Bay, Zambales", "Zambales", "Subic", 14.8189, 120.2798, "Freeport Zone"),
    AutocompleteSuggestion("Aguinaldo Hwy, Imus Cavite", "Cavite", "Imus", 14.4167, 120.9333, "Poblacion"),
    AutocompleteSuggestion("Malolos, Bulacan", "Bulacan", "Malolos", 14.8433, 120.8122, "San Vicente"),
    AutocompleteSuggestion("Calamba, Laguna", "Laguna", "Calamba", 14.2125, 121.1625, "Real"),
    AutocompleteSuggestion("Antipolo, Rizal", "Rizal", "Antipolo", 14.5847, 121.1764, "Poblacion Central"),
    AutocompleteSuggestion("Urdaneta, Pangasinan", "Pangasinan", "Urdaneta", 15.9761, 120.5711, "Anonas")
)

// ==========================================
// 2. GIS MATH PROJECTIONS (LAT/LNG <-> PIXELS)
// ==========================================

object MapProjection {
    // Standard projection helper to map GPS coords to interactive local canvas pixels
    fun toPixelOffset(
        lat: Double,
        lng: Double,
        centerLat: Double,
        centerLng: Double,
        zoom: Float,
        widthPx: Float,
        heightPx: Float,
        scaleFactor: Float = 250f
    ): Offset {
        val deltaLng = lng - centerLng
        val deltaLat = lat - centerLat
        
        // standard simple Mercator projection simulation
        val x = (widthPx / 2f) + (deltaLng * scaleFactor * zoom).toFloat()
        val y = (heightPx / 2f) - (deltaLat * scaleFactor * zoom).toFloat() // Y goes down, Latitude goes up
        return Offset(x, y)
    }

    // Convert local canvas click pixel coordinates back to exact geographic Lat/Lng
    fun toCoordinates(
        offset: Offset,
        centerLat: Double,
        centerLng: Double,
        zoom: Float,
        widthPx: Float,
        heightPx: Float,
        scaleFactor: Float = 250f
    ): Pair<Double, Double> {
        val deltaX = offset.x - (widthPx / 2f)
        val deltaY = offset.y - (heightPx / 2f)
        
        val deltaLng = deltaX / (scaleFactor * zoom)
        val deltaLat = -deltaY / (scaleFactor * zoom)
        
        val targetLng = centerLng + deltaLng
        val targetLat = centerLat + deltaLat
        return Pair(targetLat, targetLng)
    }

    // Haversine formula for exact distance between two GPS coordinates in kilometers
    fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

// ==========================================
// 3. PHILIPPINES SIMULATED REVERSE-GEOCODER
// ==========================================

fun geocodeLocationSimulator(province: String, lat: Double, lng: Double): MapCheckLocation {
    val hash = (province.hashCode() + (lat * 1000).toInt() + (lng * 1000).toInt()).absoluteValue
    val localMuni = when (province) {
        "Metro Manila" -> listOf("Manila", "Quezon City", "Pasay", "Caloocan", "Makati", "Taguig").let { it[hash % it.size] }
        "Cebu" -> listOf("Cebu City", "Mandaue City", "Lapu-Lapu City", "Talisay", "Consolacion").let { it[hash % it.size] }
        "Davao del Sur" -> listOf("Davao City", "Digos City", "Santa Cruz", "Bansalan").let { it[hash % it.size] }
        "Benguet" -> listOf("Baguio City", "La Trinidad", "Itogon", "Tuba").let { it[hash % it.size] }
        "Iloilo" -> listOf("Iloilo City", "Jaro", "Oton", "Pavia", "Santa Barbara").let { it[hash % it.size] }
        "Zambales" -> listOf("Subic", "Olongapo City", "San Marcelino", "Castillejos").let { it[hash % it.size] }
        "Cavite" -> listOf("Imus", "Bacoor City", "Dasmarinas", "Tagaytay City", "General Trias").let { it[hash % it.size] }
        "Bulacan" -> listOf("Malolos City", "Meycauayan", "Marilao", "Bocaue", "Plaridel").let { it[hash % it.size] }
        "Laguna" -> listOf("Calamba City", "Santa Rosa", "San Pedro", "Biñan", "Los Baños").let { it[hash % it.size] }
        "Rizal" -> listOf("Antipolo City", "Cainta", "Taytay", "San Mateo", "Angono").let { it[hash % it.size] }
        else -> "Poblacion District"
    }

    val streetNames = listOf("Rizal St.", "Magsaysay Boulevard", "Bonifacio Highway", "Quezon Ave.", "Del Pilar St.", "Aguinaldo Drive", "Mataas Road")
    val selectedStreet = streetNames[hash % streetNames.size]
    val streetNo = (hash % 220) + 12
    
    val fullAddress = "$streetNo $selectedStreet, Brgy. Central, $localMuni, $province, Philippines"
    val description = "A faithful missionary Baptist assembly centered in the $localMuni congregation, serving Visayas, Luzon, and Mindanao souls with Bible-based teachings of grace."
    
    return MapCheckLocation(
        address = fullAddress,
        province = province,
        cityMunicipality = localMuni,
        description = description
    )
}

// ==========================================
// 4. INTERACTIVE CUSTOM MAP CANVAS
// ==========================================

@Composable
fun InteractiveCustomMap(
    modifier: Modifier = Modifier,
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    displayStyle: MapStyle = MapStyle.STANDARD,
    approvedChurches: List<BaptistChurch> = emptyList(),
    selectedChurch: BaptistChurch? = null,
    onMapClick: ((Double, Double) -> Unit)? = null,
    onMarkerDragEnd: ((Double, Double) -> Unit)? = null,
    userLocationLat: Double? = null,
    userLocationLng: Double? = null,
    userRadarPulsing: Boolean = false,
    interactiveMarkerLat: Double? = null,
    interactiveMarkerLng: Double? = null,
    onSelectChurch: ((BaptistChurch) -> Unit)? = null
) {
    var mapCenterLatState by remember(centerLat) { mutableStateOf(centerLat) }
    var mapCenterLngState by remember(centerLng) { mutableStateOf(centerLng) }
    var mapZoomState by remember(zoom) { mutableStateOf(zoom) }
    
    var localMapWidth by remember { mutableStateOf(500f) }
    var localMapHeight by remember { mutableStateOf(300f) }
    
    val pulseTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    Box(
        modifier = modifier
            .background(
                if (displayStyle == MapStyle.SATELLITE) Color(0xFF0F172A) else Color(0xFFE0F2FE)
            )
            .pointerInput(mapZoomState) {
                detectTapGestures { offset ->
                    val coords = MapProjection.toCoordinates(
                        offset = offset,
                        centerLat = mapCenterLatState,
                        centerLng = mapCenterLngState,
                        zoom = mapZoomState,
                        widthPx = localMapWidth,
                        heightPx = localMapHeight
                    )
                    onMapClick?.invoke(coords.first, coords.second)
                }
            }
            .pointerInput(mapZoomState) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Adjust map center geographical coordinates based on drag pixels
                    val degreesPerPixel = 1f / (250f * mapZoomState)
                    mapCenterLngState -= dragAmount.x * degreesPerPixel
                    mapCenterLatState += dragAmount.y * degreesPerPixel
                    
                    if (onMarkerDragEnd != null && interactiveMarkerLat != null && interactiveMarkerLng != null) {
                        // Dragging adjusts the interactive target coordinates
                        onMarkerDragEnd.invoke(mapCenterLatState, mapCenterLngState)
                    }
                }
            }
            .onSizeChanged { size: androidx.compose.ui.unit.IntSize ->
                localMapWidth = size.width.toFloat()
                localMapHeight = size.height.toFloat()
            }
    ) {
        // Draw Map Terrain Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (displayStyle == MapStyle.SATELLITE) {
                // SATELLITE WATER & TOPO SHADING
                drawRect(Color(0xFF0F172A)) // ocean background
                
                // Draw land topography vectors
                drawCircle(
                    color = Color(0xFF065F46).copy(alpha = 0.5f),
                    radius = size.width * 0.9f * mapZoomState,
                    center = Offset(size.width * 0.8f, size.height * 0.3f)
                )
                drawCircle(
                    color = Color(0xFF1E3A8A).copy(alpha = 0.3f), // reef depth shading
                    radius = size.width * 0.4f * mapZoomState,
                    center = Offset(size.width * 0.2f, size.height * 0.9f)
                )
                
                // Satellite terrain details
                drawRect(
                    color = Color(0xFF14532D).copy(alpha = 0.6f),
                    topLeft = Offset(size.width * 0.3f, size.height * 0.4f),
                    size = Size(size.width * 0.5f * mapZoomState, size.height * 0.4f * mapZoomState)
                )
            } else {
                // STANDARD ROAD & CIVIC OVERLAYS
                drawRect(Color(0xFFF1F5F9)) // Standard land color
                
                // Draw water bodies (e.g. Manila Bay beach curvature)
                val beachPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    cubicTo(
                        size.width * 0.15f, size.height * 0.3f,
                        size.width * 0.05f, size.height * 0.7f,
                        0f, size.height
                    )
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(beachPath, Color(0xFFB9E6FF))
                
                // Draw regional park vectors
                drawRect(
                    color = Color(0xFFDCFCE7), // Rizal Park Green
                    topLeft = Offset(size.width * 0.55f, size.height * 0.22f),
                    size = Size(100f * mapZoomState, 50f * mapZoomState)
                )
            }
            
            // Render Arterial Road Lines
            drawRoads(displayStyle, mapZoomState)
        }

        // Pulse Radar Sweep for 'Churches Near Me' GPS Search
        if (userRadarPulsing && userLocationLat != null && userLocationLng != null) {
            val pulsingPos = MapProjection.toPixelOffset(
                lat = userLocationLat,
                lng = userLocationLng,
                centerLat = mapCenterLatState,
                centerLng = mapCenterLngState,
                zoom = mapZoomState,
                widthPx = localMapWidth,
                heightPx = localMapHeight
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = pulsingPos.x - 150f
                        translationY = pulsingPos.y - 150f
                    }
            ) {
                Canvas(modifier = Modifier.size(300.dp)) {
                    // Sweeping radar scanning line
                    val radius = strokeWidthToRadius(120.dp.toPx(), pulseProgress)
                    drawCircle(
                        color = Color(0xFF1E3A8A).copy(alpha = 0.12f * (1f - pulseProgress)),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF1E3A8A).copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Render User Location Pointer Blue Dot
        if (userLocationLat != null && userLocationLng != null) {
            val userPx = MapProjection.toPixelOffset(
                lat = userLocationLat,
                lng = userLocationLng,
                centerLat = mapCenterLatState,
                centerLng = mapCenterLngState,
                zoom = mapZoomState,
                widthPx = localMapWidth,
                heightPx = localMapHeight
            )
            
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = userPx.x - 12f
                        translationY = userPx.y - 12f
                    }
                    .size(24.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color(0xFF1976D2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF1976D2), CircleShape)
                )
            }
        }

        // Render Approved Baptist Church Markers
        approvedChurches.forEach { church ->
            val markerPx = MapProjection.toPixelOffset(
                lat = church.latitude,
                lng = church.longitude,
                centerLat = mapCenterLatState,
                centerLng = mapCenterLngState,
                zoom = mapZoomState,
                widthPx = localMapWidth,
                heightPx = localMapHeight
            )

            val isSelectedMarker = selectedChurch?.name == church.name
            
            IconButton(
                onClick = { onSelectChurch?.invoke(church) },
                modifier = Modifier
                    .testTag("map_marker_${church.id}")
                    .graphicsLayer {
                        translationX = markerPx.x - 18f
                        translationY = markerPx.y - 36f
                    }
                    .size(36.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelectedMarker) 32.dp else 24.dp)
                            .background(
                                if (isSelectedMarker) Color(0xFF1E3A8A) else Color(0xFF1B2E1C),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.5.dp, 
                                Color.White, 
                                RoundedCornerShape(8.dp)
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Church,
                            contentDescription = church.name,
                            tint = Color.White,
                            modifier = Modifier.size(if (isSelectedMarker) 18.dp else 14.dp)
                        )
                    }
                    // Marker small pointer arrow below the box
                    Box(
                        modifier = Modifier
                            .size(6.dp, 4.dp)
                            .background(
                                if (isSelectedMarker) Color(0xFF1E3A8A) else Color(0xFF1B2E1C),
                                RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)
                            )
                    )
                }
            }
        }

        // Render Interactive Position Pin (Drag and drop representation)
        if (interactiveMarkerLat != null && interactiveMarkerLng != null) {
            val interactPx = MapProjection.toPixelOffset(
                lat = interactiveMarkerLat,
                lng = interactiveMarkerLng,
                centerLat = mapCenterLatState,
                centerLng = mapCenterLngState,
                zoom = mapZoomState,
                widthPx = localMapWidth,
                heightPx = localMapHeight
            )

            // Dynamic float action bounce animation
            val bounceTransition = rememberInfiniteTransition(label = "MarkerBounce")
            val bounceOffset by bounceTransition.animateFloat(
                initialValue = -6f,
                targetValue = 2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BounceOffset"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = interactPx.x - 20f
                        translationY = interactPx.y - 40f + bounceOffset
                    }
                    .size(40.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Interactive Dropped GPS Marker",
                    tint = Color.Red,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

private fun strokeWidthToRadius(maxRadius: Float, progress: Float): Float {
    return maxRadius * progress
}

private fun DrawScope.drawRoads(style: MapDisplayStyle, zoom: Float) {
    val roadColor = if (style == MapStyle.SATELLITE) Color(0xFF475569) else Color(0xFFFFFFFF)
    val widthMajor = 10f * zoom
    val widthMinor = 5f * zoom

    // Draw main highways
    drawLine(
        roadColor,
        start = Offset(0f, size.height * 0.4f),
        end = Offset(size.width, size.height * 0.45f),
        strokeWidth = widthMajor,
        cap = StrokeCap.Round
    )

    drawLine(
        roadColor,
        start = Offset(0f, size.height * 0.75f),
        end = Offset(size.width, size.height * 0.72f),
        strokeWidth = widthMajor,
        cap = StrokeCap.Round
    )

    // Avenues or crossings
    drawLine(
        roadColor,
        start = Offset(size.width * 0.25f, 0f),
        end = Offset(size.width * 0.35f, size.height),
        strokeWidth = widthMinor,
        cap = StrokeCap.Round
    )

    drawLine(
        roadColor,
        start = Offset(size.width * 0.7f, 0f),
        end = Offset(size.width * 0.65f, size.height),
        strokeWidth = widthMajor,
        cap = StrokeCap.Round
    )
}

// Convert MapDisplayStyle to standard helper type for flexibility
typealias MapStyle = MapDisplayStyle

// ==========================================
// 5. REUSABLE PREMIUM MAP PIN DROPPER DIALOG
// ==========================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GoogleMapPinDropperDialog(
    initialProvince: String,
    onDismissRequest: () -> Unit,
    onLocationConfirmed: (lat: Double, lng: Double, address: String, city: String, description: String) -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    var chosenProvince by remember { mutableStateOf(initialProvince) }
    var selectedLat by remember { mutableStateOf(14.5995) } // Default Manila
    var selectedLng by remember { mutableStateOf(120.9842) }
    
    var currentStyle by remember { mutableStateOf(MapDisplayStyle.STANDARD) }
    var showSuggestions by remember { mutableStateOf(false) }
    
    val reverseGeocode = geocodeLocationSimulator(chosenProvince, selectedLat, selectedLng)
    var customAddressText by remember { mutableStateOf(reverseGeocode.address) }
    var activeMarkerCity by remember { mutableStateOf(reverseGeocode.cityMunicipality) }
    var activeMarkerDesc by remember { mutableStateOf(reverseGeocode.description) }

    // Synchronize values when panned, dragged or loaded
    LaunchedEffect(selectedLat, selectedLng, chosenProvince) {
        val geo = geocodeLocationSimulator(chosenProvince, selectedLat, selectedLng)
        customAddressText = geo.address
        activeMarkerCity = geo.cityMunicipality
        activeMarkerDesc = geo.description
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 Baptist Maps Pin Dropper",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CinzelBoldFamily,
                        color = Color(0xFF1E3A8A)
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Maps UI", modifier = Modifier.size(18.dp))
                    }
                }

                // Autocomplete Google Maps Place Search Bar
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { 
                            searchInput = it
                            showSuggestions = it.isNotBlank()
                        },
                        placeholder = { Text("Search location in the Philippines...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchInput = ""
                                    showSuggestions = false
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear Search", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Autocomplete Suggestions Dropdown Panel
                    if (showSuggestions) {
                        val filteredSuggestions = PH_AUTOCOMPLETE_SUGGESTIONS.filter {
                            it.label.contains(searchInput, ignoreCase = true) ||
                            it.province.contains(searchInput, ignoreCase = true)
                        }

                        if (filteredSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 56.dp)
                                    .shadow(6.dp, RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .heightIn(max = 180.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(filteredSuggestions) { sug ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedLat = sug.latitude
                                                    selectedLng = sug.longitude
                                                    chosenProvince = sug.province
                                                    searchInput = sug.label
                                                    showSuggestions = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Place,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(sug.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(sug.district.ifEmpty { sug.province }, fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Custom Map View Box with Draggable marker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Color(0xFF1E3A8A).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                ) {
                    InteractiveCustomMap(
                        modifier = Modifier.fillMaxSize(),
                        centerLat = selectedLat,
                        centerLng = selectedLng,
                        zoom = 1.3f,
                        displayStyle = currentStyle,
                        onMapClick = { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                        },
                        onMarkerDragEnd = { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                        },
                        interactiveMarkerLat = selectedLat,
                        interactiveMarkerLng = selectedLng
                    )

                    // Overlay Map style and location controllers at Top-Right
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Toggle Map Style: Satellite vs Standard
                        IconButton(
                            onClick = { 
                                currentStyle = if (currentStyle == MapDisplayStyle.STANDARD) 
                                    MapDisplayStyle.SATELLITE else MapDisplayStyle.STANDARD 
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                .border(0.5.dp, Color.LightGray, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (currentStyle == MapDisplayStyle.STANDARD) Icons.Filled.Satellite else Icons.Filled.Navigation,
                                contentDescription = "Satellite Toggle",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Adjust/Reset GPS home button
                        IconButton(
                            onClick = { 
                                selectedLat = 14.5995
                                selectedLng = 120.9842
                                chosenProvince = "Metro Manila"
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                .border(0.5.dp, Color.LightGray, CircleShape)
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Center", tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Capturing location info display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AUTOMATION COORDINATES SAVES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    }
                    Text(
                        text = "GPS: ${String.format(Locale.US, "%.5f", selectedLat)}, ${String.format(Locale.US, "%.5f", selectedLng)} (Drag map/markers to adjust)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Address: $customAddressText",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "City/Municipality: $activeMarkerCity | Prov: $chosenProvince",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Optional preview description builder
                OutlinedTextField(
                    value = activeMarkerDesc,
                    onValueChange = { activeMarkerDesc = it },
                    label = { Text("Pre-saving Biblical Description") },
                    maxLines = 2,
                    textStyle = TextStyle(fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onLocationConfirmed(
                        selectedLat,
                        selectedLng,
                        customAddressText,
                        activeMarkerCity,
                        activeMarkerDesc
                    )
                },
                modifier = Modifier.testTag("apply_pinn_location_confirm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Text("Confirm & Apply Geocoding")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

// Custom style override for OutlinedTextField to allow compact text styles
private fun TextStyle(fontSize: androidx.compose.ui.unit.TextUnit): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(fontSize = fontSize)
}

// ==========================================
// 6. CHOREOGRAPHED "CHURCHES NEAR ME" MAP DISCOVERY
// ==========================================

@Composable
fun ChurchesNearMeRadarView(
    userLat: Double,
    userLng: Double,
    approvedChurches: List<BaptistChurch>,
    onDirectionClick: (BaptistChurch) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var selectedNearbyState by remember { mutableStateOf<BaptistChurch?>(null) }
    
    val scope = rememberCoroutineScope()

    // Filtered and sorted churches with their relative calculated distances in km using the projection helper
    val nearbyChurchesWithDistances = remember(approvedChurches, isScanning) {
        approvedChurches.map { church ->
            val dist = MapProjection.calculateDistanceInKm(userLat, userLng, church.latitude, church.longitude)
            Pair(church, dist)
        }.sortedBy { it.second }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Radar Map Radar Screen Widget
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, Color(0xFFEFF6FF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
        ) {
            // Embedded Custom Map Radar Canvas
            InteractiveCustomMap(
                modifier = Modifier.fillMaxSize(),
                centerLat = userLat,
                centerLng = userLng,
                zoom = 1.0f,
                displayStyle = MapDisplayStyle.SATELLITE,
                approvedChurches = approvedChurches,
                selectedChurch = selectedNearbyState,
                onSelectChurch = { nearby ->
                    selectedNearbyState = nearby
                },
                userLocationLat = userLat,
                userLocationLng = userLng,
                userRadarPulsing = isScanning
            )

            // Scanning Status indicators overlay at Top-Left
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isScanning) Color(0xFFEF4444) else Color(0xFF10B981), CircleShape)
                )
                Text(
                    text = if (isScanning) "SCANNING RADAR CHANNELS..." else "ONLINE: FIXED GPS POINT",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Command Trigger for radar pulsing on the Bottom-Right
            Button(
                onClick = {
                    isScanning = true
                    Toast.makeText(context, "Scanning local GPS coordinates...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .testTag("churches_near_me_trigger_button")
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Radar, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sweep Search", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Selected marker details popup overlay if active
        AnimatedVisibility(
            visible = selectedNearbyState != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            selectedNearbyState?.let { activeCh ->
                val dist = MapProjection.calculateDistanceInKm(userLat, userLng, activeCh.latitude, activeCh.longitude)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = activeCh.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", dist)} km",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                            }
                            Text(
                                text = "Services: ${activeCh.worshipSchedule.substringBefore("\n")}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { onDirectionClick(activeCh) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Directions", fontSize = 8.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Ordered Nearby list entries
        Text("📍 Nearby Churches (Nearest first)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(nearbyChurchesWithDistances) { (ch, dist) ->
                val isSelected = selectedNearbyState?.name == ch.name
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFEFF6FF) else Color.White)
                        .border(
                            0.5.dp, 
                            if (isSelected) Color(0xFF1E3A8A) else Color.LightGray.copy(alpha = 0.5f), 
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedNearbyState = ch }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF1E3A8A).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Church, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ch.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(ch.address.substringBefore("\n"), fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", dist)} km away",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }
            }
        }
    }
}
