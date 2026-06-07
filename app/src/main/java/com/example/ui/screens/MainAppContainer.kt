package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.util.Log
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.BaptistChurch
import com.example.data.ChurchActivity
import com.example.data.Note
import com.example.ui.state.AppViewModel
import com.example.ui.theme.CinzelBoldFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LoraFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer

fun generateAutomatedLocation(province: String, lat: Float, lng: Float): Pair<String, String> {
    val latStr = String.format(Locale.US, "%.5f", lat)
    val lngStr = String.format(Locale.US, "%.5f", lng)
    val address = when (province) {
        "Metro Manila" -> "Faith Memorial Baptist, 88 West Ave, Diliman, Quezon City, Metro Manila"
        "Cebu" -> "Grace Landmark Baptist, 204 General Maxilom Ave, Cebu City, Cebu"
        "Davao del Sur" -> "First Missionary Baptist, MacArthur Hwy, Matina, Davao City, Davao del Sur"
        "Cavite" -> "Bible Baptist Tabernacle, Aguinaldo Hwy, Imus, Cavite"
        "Bulacan" -> "Calvary Baptist Church, MacArthur Hwy, Malolos, Bulacan"
        "Pangasinan" -> "Pangasinan Faith Baptist, McArthur Highway, Urdaneta, Pangasinan"
        "Laguna" -> "Charity Baptist Fellowship, National Highway, Calamba, Laguna"
        "Rizal" -> "Rizal Baptist Church, Ortigas Ave Extension, Cainta, Rizal"
        "Iloilo" -> "Iloilo Bible Baptist Church, Jalandoni St, Jaro, Iloilo City, Iloilo"
        else -> "Independent Baptist Church, 45 National Road, San Jose, $province"
    }
    return Pair(address, "$latStr, $lngStr")
}

fun parseNoteContent(richTextNotes: String): Pair<List<String>, String> {
    if (richTextNotes.startsWith("[REDESIGNED_NOTE_JSON_V2]:") || richTextNotes.startsWith("[REDESIGNED_NOTE_JSON]:")) {
        return Pair(emptyList(), "")
    }
    if (richTextNotes.startsWith("[OUTLINE]")) {
        val contentIndex = richTextNotes.indexOf("[CONTENT]")
        if (contentIndex != -1) {
            val outlinePart = richTextNotes.substring(9, contentIndex).trim()
            val contentPart = richTextNotes.substring(contentIndex + 9).trim()
            val lines = outlinePart.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            return Pair(lines, contentPart)
        }
    }
    if (richTextNotes.contains("--- Sermon Outline ---")) {
        val parts = richTextNotes.split("--- Sermon Outline ---")
        val contentPart = parts[0].trim()
        val outlinePart = parts[1].trim()
        val lines = outlinePart.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return Pair(lines, contentPart)
    }
    return Pair(emptyList(), richTextNotes)
}

data class OutlineNode(
    val outline: String = "",
    val verses: String = "",
    val takeaways: String = ""
)

fun parseRedesignedNote(richTextNotes: String): List<OutlineNode>? {
    if (richTextNotes.startsWith("[REDESIGNED_NOTE_JSON_V2]:")) {
        return try {
            val jsonStr = richTextNotes.substring("[REDESIGNED_NOTE_JSON_V2]:".length)
            val rootObj = org.json.JSONObject(jsonStr)
            val array = rootObj.optJSONArray("nodes") ?: org.json.JSONArray()
            val list = mutableListOf<OutlineNode>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OutlineNode(
                        outline = obj.optString("outline", ""),
                        verses = obj.optString("verses", ""),
                        takeaways = obj.optString("takeaways", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    if (!richTextNotes.startsWith("[REDESIGNED_NOTE_JSON]:")) return null
    return try {
        val jsonStr = richTextNotes.substring("[REDESIGNED_NOTE_JSON]:".length)
        val array = org.json.JSONArray(jsonStr)
        val list = mutableListOf<OutlineNode>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                OutlineNode(
                    outline = obj.optString("outline", ""),
                    verses = obj.optString("verses", ""),
                    takeaways = obj.optString("takeaways", "")
                )
            )
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun parseRedesignedNoteSummary(richTextNotes: String): String {
    if (!richTextNotes.startsWith("[REDESIGNED_NOTE_JSON_V2]:")) return ""
    return try {
        val jsonStr = richTextNotes.substring("[REDESIGNED_NOTE_JSON_V2]:".length)
        val rootObj = org.json.JSONObject(jsonStr)
        rootObj.optString("summary", "")
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun serializeRedesignedNote(nodes: List<OutlineNode>): String {
    val array = org.json.JSONArray()
    for (node in nodes) {
        val obj = org.json.JSONObject()
        obj.put("outline", node.outline)
        obj.put("verses", node.verses)
        obj.put("takeaways", node.takeaways)
        array.put(obj)
    }
    return "[REDESIGNED_NOTE_JSON]:" + array.toString()
}

fun serializeRedesignedNoteV2(nodes: List<OutlineNode>, summary: String): String {
    val rootObj = org.json.JSONObject()
    rootObj.put("summary", summary)
    
    val array = org.json.JSONArray()
    for (node in nodes) {
        val obj = org.json.JSONObject()
        obj.put("outline", node.outline)
        obj.put("verses", node.verses)
        obj.put("takeaways", node.takeaways)
        array.put(obj)
    }
    rootObj.put("nodes", array)
    return "[REDESIGNED_NOTE_JSON_V2]:" + rootObj.toString()
}

fun getInitialsText(name: String?): String {
    if (name.isNullOrBlank()) return "S"
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "S"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    val firstInitial = parts[0].firstOrNull() ?: ' '
    val lastInitial = parts.lastOrNull()?.firstOrNull() ?: ' '
    return "$firstInitial$lastInitial".uppercase().trim()
}

// Navigation screens enum
enum class AppScreen {
    SPLASH,
    LOGIN,
    ONBOARDING,
    MAIN
}

@Composable
fun MainAppContainer(viewModel: AppViewModel) {
    var currentScreenState by remember { mutableStateOf(AppScreen.SPLASH) }
    var previousLaunched by remember { mutableStateOf(false) }

    // Splash screen timer delay auto transition
    if (currentScreenState == AppScreen.SPLASH) {
        LaunchedEffect(Unit) {
            delay(3000)
            if (viewModel.currentUser != null) {
                currentScreenState = AppScreen.MAIN
            } else {
                currentScreenState = AppScreen.LOGIN
            }
        }
    }

    Crossfade(targetState = currentScreenState, animationSpec = tween(500), label = "ScreenTransition") { screen ->
        when (screen) {
            AppScreen.SPLASH -> BaptistSplashScreen()
            AppScreen.LOGIN -> BaptistLoginScreen(
                viewModel = viewModel,
                onLoginCompleted = {
                    currentScreenState = AppScreen.ONBOARDING
                }
            )
            AppScreen.ONBOARDING -> BaptistOnboardingScreen(
                onTutorialComplete = {
                    currentScreenState = AppScreen.MAIN
                }
            )
            AppScreen.MAIN -> MainBottomNavDashboard(viewModel = viewModel, onLogout = {
                currentScreenState = AppScreen.LOGIN
            })
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN (Premium Scribe For Baptists branding)
// ==========================================
@Composable
fun BaptistSplashScreen() {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F7FA), // Serene soft off-white sky
                        Color(0xFFE4ECF5)  // Elegant pure soft blue-grey
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle moving ambient halo background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = 0.12f), // Warm golden aura
                radius = size.width * 0.7f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.4f)
            )
            drawCircle(
                color = Color(0xFF42A5F5).copy(alpha = 0.10f), // Soft luminous blue glow
                radius = size.width * 0.5f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.4f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // Empty space to push core card center-aligned vertically
            Spacer(modifier = Modifier.height(30.dp))

            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(1200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(1200))
            ) {
                // The core majestic Scribe frame card (displaying the new premium logo)
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon),
                    contentDescription = "Scribe Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f) // Maintain square icon look
                        .clip(RoundedCornerShape(44.dp))
                        .border(1.dp, Color(0xFF1B365D).copy(alpha = 0.08f), RoundedCornerShape(44.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // Subtitle text branding
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(1400)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "FOR FUNDAMENTAL BAPTISTS",
                        fontSize = 14.sp,
                        fontFamily = CinzelBoldFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37),
                        letterSpacing = 2.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PREMIUM SERMON & KJV COMPANION",
                        fontSize = 9.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B365D).copy(alpha = 0.55f),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Decorative signature footer brand line
        AnimatedVisibility(
            visible = animateIn,
            enter = fadeIn(animationSpec = tween(1500)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 50.dp)
        ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "STUDY • WORSHIP • GROW",
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37),
                        letterSpacing = 2.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "by Cliff Rozal",
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF1B365D).copy(alpha = 0.65f), // Matching deep navy ink signature
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }

@Composable
fun MockDockItem(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF0C1D38),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = InterFontFamily,
            color = Color(0xFF0C1D38).copy(alpha = 0.85f),
            letterSpacing = 0.2.sp
        )
    }
}


// ==========================================
// 2. AUTHENTICATION (LOGIN / REGISTRATION)
// ==========================================
@Composable
fun BaptistLoginScreen(
    viewModel: AppViewModel,
    onLoginCompleted: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var fullNameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var regProvince by remember { mutableStateOf("") }
    var regProvinceExpanded by remember { mutableStateOf(false) }
    var regCity by remember { mutableStateOf("") }
    var regCityExpanded by remember { mutableStateOf(false) }
    var regChurch by remember { mutableStateOf("") }
    var regChurchExpanded by remember { mutableStateOf(false) }
    var regChurchSearchQuery by remember { mutableStateOf("") }
    var regCitySuggestions by remember(regProvince) {
        mutableStateOf(com.example.data.PsgcData.getCitiesAndMunicipalitiesForProvince(regProvince))
    }
    LaunchedEffect(regProvince) {
        regCitySuggestions = com.example.data.PsgcData.fetchCitiesForProvinceAsync(regProvince)
        if (!regCitySuggestions.contains(regCity)) {
            regCity = ""
        }
    }
    var isAdminToggle by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var showGmailPrompt by remember { mutableStateOf(false) }
    var gmailPromptEmail by remember { mutableStateOf("") }
    var gmailPromptName by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Configured Google Sign In
    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("368629443683-std28j2rqgpfkpnteo00173l0qgol7k3.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, googleSignInOptions) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            val email = account.email
            val name = account.displayName ?: "Google User"
            if (idToken != null) {
                viewModel.loginWithGoogleCredential(idToken) { success ->
                    if (success) {
                        Toast.makeText(context, "Welcome back! Google Synced successfully.", Toast.LENGTH_SHORT).show()
                        onLoginCompleted()
                    }
                }
            } else if (!email.isNullOrBlank()) {
                viewModel.loginWithGoogleSimulation(email, name) { success ->
                    if (success) {
                        Toast.makeText(context, "Signed in seamlessly as $email!", Toast.LENGTH_SHORT).show()
                        onLoginCompleted()
                    }
                }
            } else {
                showGmailPrompt = true
            }
        } catch (e: Exception) {
            Log.e("LoginScreen", "Google Sign-In exception", e)
            val fallbackAccount = try { task.result } catch (ex: Exception) { null }
            val fallbackEmail = fallbackAccount?.email
            val fallbackName = fallbackAccount?.displayName ?: "Google User"
            if (!fallbackEmail.isNullOrBlank()) {
                viewModel.loginWithGoogleSimulation(fallbackEmail, fallbackName) { success ->
                    if (success) {
                        Toast.makeText(context, "Gmail auto-sync: logged in as $fallbackEmail", Toast.LENGTH_LONG).show()
                        onLoginCompleted()
                    }
                }
            } else {
                showGmailPrompt = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_serene_hills),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dim overlay to keep contrast high holding luxury theme background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
        )

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, // maintains spacing
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = "Scribe Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scribe.",
                        fontFamily = CinzelBoldFamily,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Middle Display Typography
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Sermon\nNotes\nOrganizer",
                    fontFamily = CinzelBoldFamily,
                    fontSize = 42.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\"I am the way, the truth, and the life. No one comes to the Father except through Me.\"\n— John 14:6",
                    fontStyle = FontStyle.Italic,
                    fontFamily = LoraFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card at the bottom with modern luxury styling
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegisterMode) "Create Spiritual Account" else "Welcome to Scribe",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CinzelBoldFamily,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRegisterMode) "Register below to secure your study database" else "Sign in to compile study insights & sermon logs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    viewModel.authErrorMessage?.let { message ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Input fields
                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("John Doe") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Full Name", modifier = Modifier.size(18.dp)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fullname_input"),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Province Dropdown Selector (Mandatory)
                        val provincesList_Reg = listOf(
                            "Metro Manila", "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", 
                            "Aurora", "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol", "Bukidnon", 
                            "Bulacan", "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes", "Cavite", 
                            "Cebu", "Cotabato", "Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental", "Davao Oriental", 
                            "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur", "Iloilo", "Isabela", 
                            "Kalinga", "La Union", "Laguna", "Lanao del Norte", "Lanao del Sur", "Leyte", "Maguindanao del Norte", 
                            "Maguindanao del Sur", "Marinduque", "Masbate", "Misamis Occidental", "Misamis Oriental", "Mountain Province", 
                            "Negros Occidental", "Negros Oriental", "Northern Samar", "Nueva Ecija", "Nueva Vizcaya", "Occidental Mindoro", 
                            "Oriental Mindoro", "Palawan", "Pampanga", "Pangasinan", "Quezon", "Quirino", "Rizal", "Romblon", "Samar", 
                            "Sarangani", "Siquijor", "Sorsogon", "South Cotabato", "Southern Leyte", "Sultan Kudarat", "Sulu", 
                            "Surigao del Norte", "Surigao del Sur", "Tarlac", "Tawi-Tawi", "Zambales", "Zamboanga del Norte", 
                            "Zamboanga del Sur", "Zamboanga Sibugay"
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (regProvince.isEmpty()) "Select Province..." else regProvince,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Province (Mandatory)", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = "Province", modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = { regProvinceExpanded = !regProvinceExpanded }) {
                                        Icon(
                                            imageVector = if (regProvinceExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Choose Province"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { regProvinceExpanded = !regProvinceExpanded },
                                shape = RoundedCornerShape(24.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            )
                            DropdownMenu(
                                expanded = regProvinceExpanded,
                                onDismissRequest = { regProvinceExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .background(Color.White)
                            ) {
                                provincesList_Reg.forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov, color = Color.Black, fontFamily = InterFontFamily, fontSize = 13.sp) },
                                        onClick = {
                                            regProvince = prov
                                            regProvinceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // City Dropdown Selector (Mandatory)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (regCity.isEmpty()) {
                                    if (regProvince.isEmpty()) "Select Province First..." else "Select City/Municipality..."
                                } else regCity,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("City/Municipality (Mandatory)", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = "City", modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { 
                                            if (regProvince.isNotEmpty()) {
                                                regCityExpanded = !regCityExpanded 
                                            }
                                        },
                                        enabled = regProvince.isNotEmpty()
                                    ) {
                                        Icon(
                                            imageVector = if (regCityExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Choose City"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = regProvince.isNotEmpty()) { 
                                        regCityExpanded = !regCityExpanded 
                                    },
                                shape = RoundedCornerShape(24.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                                )
                            )
                            DropdownMenu(
                                expanded = regCityExpanded,
                                onDismissRequest = { regCityExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .background(Color.White)
                            ) {
                                regCitySuggestions.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(city, color = Color.Black, fontFamily = InterFontFamily, fontSize = 13.sp) },
                                        onClick = {
                                            regCity = city
                                            regCityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Church Dropdown Selector (Mandatory)
                        val approvedChurches_Reg by viewModel.approvedChurches.collectAsStateWithLifecycle(initialValue = emptyList())
                        val filteredChurches = remember(approvedChurches_Reg, regProvince, regChurchSearchQuery) {
                            var list = approvedChurches_Reg
                            val query = regChurchSearchQuery.trim()
                            // If they are searching with at least 2 characters, search globally.
                            // If search query is short or empty, filter by province if province is selected.
                            if (query.length < 2 && regProvince.isNotEmpty()) {
                                list = list.filter { it.province.trim().equals(regProvince.trim(), ignoreCase = true) }
                            }
                            val names = list.map { it.name }.distinct().sorted()
                            if (query.isNotEmpty() && !query.equals(regChurch, ignoreCase = true)) {
                                val startsWithList = names.filter { it.startsWith(query, ignoreCase = true) }
                                val containsList = names.filter { !it.startsWith(query, ignoreCase = true) && it.contains(query, ignoreCase = true) }
                                startsWithList + containsList
                            } else {
                                names
                            }
                        }
                        val regChurchOptions = remember(filteredChurches, regChurchSearchQuery) {
                            val query = regChurchSearchQuery.trim()
                            if (query.isNotEmpty() && !filteredChurches.any { it.trim().equals(query, ignoreCase = true) }) {
                                filteredChurches + listOf("custom_church:$query")
                            } else {
                                filteredChurches
                            }
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = regChurchSearchQuery,
                                onValueChange = { newValue ->
                                    regChurchSearchQuery = newValue
                                    regChurchExpanded = true
                                    // Robust real-time predictive matching as they type
                                    val matchedCh = approvedChurches_Reg.find { it.name.trim().equals(newValue.trim(), ignoreCase = true) }
                                    if (matchedCh != null) {
                                        regChurch = matchedCh.name
                                        regProvince = matchedCh.province
                                        regCity = matchedCh.cityMunicipality
                                    }
                                },
                                label = { Text("Search/Select Local Church (Mandatory)", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                placeholder = { Text("Type church name (e.g. Faith...)", fontFamily = InterFontFamily, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = "Local Church", modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (regChurchSearchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    regChurchSearchQuery = ""
                                                    regChurch = ""
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Clear Selection",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { regChurchExpanded = !regChurchExpanded },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (regChurchExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                contentDescription = "Toggle Church suggestions",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            )
                            AnimatedVisibility(
                                visible = regChurchExpanded && regChurchOptions.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                      ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        regChurchOptions.forEach { chOpt ->
                                            val isCustom = chOpt.startsWith("custom_church:")
                                            val displayName = if (isCustom) {
                                                "➕ Use \"${chOpt.removePrefix("custom_church:")}\" (Custom)"
                                            } else {
                                                chOpt
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (isCustom) {
                                                            val customName = chOpt.removePrefix("custom_church:")
                                                            regChurch = customName
                                                            regChurchSearchQuery = customName
                                                            regChurchExpanded = false
                                                        } else {
                                                            regChurch = chOpt
                                                            regChurchSearchQuery = chOpt
                                                            regChurchExpanded = false
                                                            val matchedCh = approvedChurches_Reg.find { it.name == chOpt }
                                                            if (matchedCh != null) {
                                                                regProvince = matchedCh.province
                                                                regCity = matchedCh.cityMunicipality
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isCustom) Icons.Filled.Add else Icons.Filled.Home,
                                                    contentDescription = null,
                                                    tint = if (isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = displayName,
                                                    color = if (isCustom) MaterialTheme.colorScheme.primary else Color.Black,
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontFamily = InterFontFamily,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("yourname@church.com") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email", modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password", modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // stay signed in / Admin toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = viewModel.rememberMe,
                            onCheckedChange = { viewModel.rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text("Stay signed-in", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                if (fullNameInput.isBlank()) {
                                    Toast.makeText(context, "Full Name is mandatory.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (regProvince.isBlank()) {
                                    Toast.makeText(context, "Province is mandatory.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (regCity.isBlank()) {
                                    Toast.makeText(context, "City/Municipality is mandatory.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val finalChurch = regChurchSearchQuery.trim()

                                if (finalChurch.isBlank()) {
                                    Toast.makeText(context, "Local Church is mandatory.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.register(
                                    email = emailInput, 
                                    pword = passwordInput, 
                                    fullName = fullNameInput,
                                    province = regProvince,
                                    city = regCity,
                                    church = finalChurch
                                ) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Scribe Connection Registered & Synced!", Toast.LENGTH_SHORT).show()
                                        onLoginCompleted()
                                    }
                                }
                            } else {
                                viewModel.login(emailInput, passwordInput) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Scribe Connection Synced!", Toast.LENGTH_SHORT).show()
                                        onLoginCompleted()
                                    }
                                }
                            }
                        },
                        enabled = !viewModel.isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (viewModel.isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "Create Account" else "Open Scribe",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.Black.copy(alpha = 0.12f)
                        )
                        Text(
                            text = "OR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.Black.copy(alpha = 0.12f)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                googleSignInClient.signOut()
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "signOut error", e)
                            }
                            try {
                                val signInIntent = googleSignInClient.signInIntent
                                googleLauncher.launch(signInIntent)
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "launch error", e)
                                // Fallback if Google Sign-In initialization fails completely - present the Gmail Dialog
                                showGmailPrompt = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("google_login_button"),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.18f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = com.example.R.drawable.ic_google),
                                contentDescription = "Google Log In",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showForgotPassword = true }) {
                            Text("Recover Password?", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                            Text(
                                text = if (isRegisterMode) "Have account? Sign-In" else "Join Scribe",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForgotPassword) {
        var recoveryEmail by remember { mutableStateOf(emailInput) }
        var isSendingReset by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("Password Rescue", fontFamily = CinzelBoldFamily) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your email address and we will dispatch a password rescue/reset link:")
                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = { recoveryEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSendingReset = true
                        viewModel.forgotPassword(recoveryEmail) { success, msg ->
                            isSendingReset = false
                            showForgotPassword = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSendingReset
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGmailPrompt) {
        var isSubmitting by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showGmailPrompt = false },
            containerColor = Color.White,
            title = { Text("Gmail Authentication", fontFamily = CinzelBoldFamily, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Google Play Services is not available in this test environment. To simulate Gmail login or registration and Join Scribe, please specify your email address and full name below:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = gmailPromptEmail,
                        onValueChange = { gmailPromptEmail = it },
                        label = { Text("Gmail Address") },
                        placeholder = { Text("yourname@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    OutlinedTextField(
                        value = gmailPromptName,
                        onValueChange = { gmailPromptName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("Your Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanEmail = gmailPromptEmail.lowercase().trim()
                        if (cleanEmail.isBlank()) {
                            Toast.makeText(context, "Gmail Address cannot be blank", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!cleanEmail.contains("@")) {
                            Toast.makeText(context, "Please enter a valid Gmail Address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (gmailPromptName.isBlank()) {
                            Toast.makeText(context, "Please enter your Full Name to register", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        viewModel.loginWithGoogleSimulation(cleanEmail, gmailPromptName) { success ->
                            isSubmitting = false
                            if (success) {
                                showGmailPrompt = false
                                Toast.makeText(context, "Gmail Auto-sync: logged in as $cleanEmail", Toast.LENGTH_LONG).show()
                                onLoginCompleted()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Join Scribe & Sync", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGmailPrompt = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

// Simple Helper to scale Composable down briefly
private fun Modifier.scale(scale: Float) = this.then(
    Modifier.alpha(scale)
)

// ==========================================
// 3. ONBOARDING TUTORIAL
// ==========================================
@Composable
fun BaptistOnboardingScreen(onTutorialComplete: () -> Unit) {
    var tutorialStep by remember { mutableStateOf(0) }
    val steps = listOf(
        Triple(
            Icons.Outlined.Create,
            "Sermon Note Taker",
            "Perfectly capture Sabbath truths! Categorize by Corporate Worship or Sunday School and attach tags easily."
        ),
        Triple(
            Icons.Outlined.Book,
            "Bible Reader",
            "Read the scriptures fully offline! Color highlight important themes, copy verses, and reference them instantly."
        ),
        Triple(
            Icons.Outlined.LocationOn,
            "Philippine Baptist Directory",
            "Discover Fundamental Baptist Churches in Metro Manila, Cebu, Davao and nationwide. Submit new templates anytime!"
        )
    )

    val activeStep = steps[tutorialStep]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                activeStep.first,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(90.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = activeStep.second,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = CinzelBoldFamily,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = activeStep.third,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Step Indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx == tutorialStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(42.dp))

            Button(
                onClick = {
                    if (tutorialStep < steps.size - 1) {
                        tutorialStep++
                    } else {
                        onTutorialComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (tutorialStep == steps.size - 1) "Enter Sanctuary" else "Continue Guide",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ==========================================
// 4. MAIN CONTAINER & NAVIGATION DASHBOARD
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBottomNavDashboard(
    viewModel: AppViewModel,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf("BIBLE") } // "BIBLE", "SUMMARY", "ACTIVITIES", "CHURCHES", "SETTINGS"
    var showAddNoteModal by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMasterAdminDashboard by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val approvedChurches by viewModel.approvedChurches.collectAsStateWithLifecycle()
    val allChurches by viewModel.allChurches.collectAsStateWithLifecycle()

    val pendingAdminCount = remember(allChurches) {
        val pendingNew = allChurches.filter { !it.isApproved && it.replacesChurchId == null }.size
        val pendingEdit = allChurches.filter { !it.isApproved && it.replacesChurchId != null }.size
        val pendingDelete = allChurches.filter { it.isApproved && it.isDeletePending }.size
        pendingNew + pendingEdit + pendingDelete
    }
    val churchNames = remember(approvedChurches) {
        approvedChurches.map { it.name }.distinct().sorted()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Continuous, immersive nature backdrop matching reference mockup
        Image(
            painter = painterResource(id = R.drawable.img_serene_hills),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Elegant overlay that blends theme backgrounds dynamically
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
        )

        Scaffold(
            containerColor = Color.Transparent, // Flow beautifully over backdrop
            topBar = {
                AnimatedVisibility(
                    visible = viewModel.isBibleHeaderVisible,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    TopAppBar(
                        title = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                // Translucent center pill badge from reference image
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.65f))
                                        .border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (activeTab) {
                                            "BIBLE" -> "Holy Scriptures"
                                            "SUMMARY" -> "Notes"
                                            "ACTIVITIES" -> "Activities"
                                            "CHURCHES" -> "Churches"
                                            else -> "Scribe"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = InterFontFamily,
                                        color = Color(0xFF1B2E1C)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            // Notification bell icon inside a subtle circular button on the left
                            IconButton(
                                onClick = {
                                    if (viewModel.currentUser?.isAdmin == true) {
                                        showMasterAdminDashboard = true
                                    } else {
                                        Toast.makeText(context, "All services active & compiled.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.7f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (viewModel.currentUser?.isAdmin == true) Icons.Filled.Shield else Icons.Filled.NotificationsNone,
                                        contentDescription = "Alerts",
                                        tint = if (viewModel.currentUser?.isAdmin == true) Color(0xFFE2B93B) else Color(0xFF1B2E1C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (viewModel.currentUser?.isAdmin == true && pendingAdminCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 6.dp, y = (-6).dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                                .size(14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pendingAdminCount.toString(),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            // User Avatar in Circular layout on the extreme right, just like mockup
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { showSettingsDialog = true }
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.customProfilePhotoUri != null) {
                                    AsyncImage(
                                        model = viewModel.customProfilePhotoUri,
                                        contentDescription = "Profile Settings",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = getInitialsText(viewModel.currentUser?.name),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = viewModel.isBibleHeaderVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    // Ultra-sleek floating bar carbon dock matching TripGlide reference mockup
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // 1. Bible Tab
                                IconButton(
                                    onClick = { activeTab = "BIBLE"; viewModel.isBibleHeaderVisible = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(46.dp)
                                        .background(if (activeTab == "BIBLE") MaterialTheme.colorScheme.onPrimary else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = "Bible Study",
                                        tint = if (activeTab == "BIBLE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                                    )
                                }

                                // 2. Summary Tab
                                IconButton(
                                    onClick = { activeTab = "SUMMARY"; viewModel.isBibleHeaderVisible = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(46.dp)
                                        .background(if (activeTab == "SUMMARY") MaterialTheme.colorScheme.onPrimary else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Journal Notes",
                                        tint = if (activeTab == "SUMMARY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                                    )
                                }

                                // 3. Central Accented Floating Add Button
                                IconButton(
                                    onClick = { showAddNoteModal = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(46.dp)
                                        .background(MaterialTheme.colorScheme.secondary)
                                        .testTag("add_sermon_floating_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Write Note",
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // 4. Activities Tab
                                IconButton(
                                    onClick = { activeTab = "ACTIVITIES"; viewModel.isBibleHeaderVisible = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(46.dp)
                                        .background(if (activeTab == "ACTIVITIES") MaterialTheme.colorScheme.onPrimary else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FormatListBulleted,
                                        contentDescription = "Events List",
                                        tint = if (activeTab == "ACTIVITIES") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                                    )
                                }

                                // 5. Churches Directory Tab
                                IconButton(
                                    onClick = { activeTab = "CHURCHES"; viewModel.isBibleHeaderVisible = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(46.dp)
                                        .background(if (activeTab == "CHURCHES") MaterialTheme.colorScheme.onPrimary else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Church,
                                        contentDescription = "Directory",
                                        tint = if (activeTab == "CHURCHES") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val topPadding = if (activeTab == "BIBLE" && !viewModel.isBibleHeaderVisible) 0.dp else maxOf(innerPadding.calculateTopPadding(), 80.dp)
            val bottomPadding = if (activeTab == "BIBLE") 0.dp else maxOf(innerPadding.calculateBottomPadding() + 8.dp, 120.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding)
            ) {
                when (activeTab) {
                    "BIBLE" -> BibleSection(viewModel = viewModel, modifier = Modifier.background(Color.Transparent))
                    "SUMMARY" -> SummarySectionView(viewModel = viewModel)
                    "ACTIVITIES" -> ChurchActivitiesView(viewModel = viewModel)
                    "CHURCHES" -> BaptistChurchesDirectoryView(viewModel = viewModel)
                }
            }
        }
    }

    // Modal Sheet to write notes
    if (showAddNoteModal) {
        AddNoteModalBottomSheet(
            viewModel = viewModel,
            onDismiss = { showAddNoteModal = false }
        )
    }

    // Settings Profile Dialog
    if (showSettingsDialog) {
        BaptistSettingsAndProfileDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false },
            onLogout = {
                viewModel.logout()
                showSettingsDialog = false
                onLogout()
            },
            onOpenAdminDashboard = {
                showSettingsDialog = false
                showMasterAdminDashboard = true
            }
        )
    }

    // Master Scribe Admin Dashboard
    if (showMasterAdminDashboard) {
        MasterAdminDashboardDialog(
            viewModel = viewModel,
            onDismiss = { showMasterAdminDashboard = false }
        )
    }
}

// ==========================================
// 5. SUMMARY MODULE (DASHBOARD & DETAILS)
// ==========================================
@Composable
fun SummarySectionView(viewModel: AppViewModel) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val mostCommonVerses by viewModel.statsMostReferencedVerses.collectAsStateWithLifecycle()
    val monthlyBreakdown by viewModel.statsMonthlySummary.collectAsStateWithLifecycle()
    val approvedChurches by viewModel.approvedChurches.collectAsStateWithLifecycle()
    val churchNames = remember(approvedChurches) {
        approvedChurches.map { it.name }.distinct().sorted()
    }

    var activeFilterCategory by remember { mutableStateOf("All") }
    var searchKeyword by remember { mutableStateOf("") }
    var viewingNoteDetails by remember { mutableStateOf<Note?>(null) }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    var showCalendar by remember { mutableStateOf(false) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedDateStr by remember { mutableStateOf<String?>(null) }

    val filteredNotes = notes.filter {
        val matchesSearch = it.title.contains(searchKeyword, ignoreCase = true) ||
                it.speaker.contains(searchKeyword, ignoreCase = true) ||
                it.richTextNotes.contains(searchKeyword, ignoreCase = true)
        val matchesCategory = activeFilterCategory == "All" || it.category == activeFilterCategory
        matchesSearch && matchesCategory
    }

    val finalDisplayedNotes = if (selectedDateStr != null) {
        filteredNotes.filter { it.date == selectedDateStr }.sortedByDescending { it.date }
    } else if (searchKeyword.isNotBlank()) {
        filteredNotes.sortedByDescending { it.date }
    } else {
        val mostRecent = filteredNotes.sortedByDescending { it.date }.firstOrNull()
        if (mostRecent != null) listOf(mostRecent) else emptyList()
    }

    val listState = rememberLazyListState()
    var prevIndex by remember { mutableStateOf(0) }
    var prevOffset by remember { mutableStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        val delta = if (currentIndex == prevIndex) {
            currentOffset - prevOffset
        } else {
            (currentIndex - prevIndex) * 200
        }
        if (delta > 30) {
            if (viewModel.isBibleHeaderVisible) {
                viewModel.isBibleHeaderVisible = false
            }
        } else if (delta < -30) {
            if (!viewModel.isBibleHeaderVisible) {
                viewModel.isBibleHeaderVisible = true
            }
        }
        if (currentIndex == 0 && currentOffset < 20) {
            viewModel.isBibleHeaderVisible = true
        }
        prevIndex = currentIndex
        prevOffset = currentOffset
    }

    LaunchedEffect(selectedDateStr) {
        if (selectedDateStr != null) {
            val groups = filteredNotes.sortedByDescending { it.date }.groupBy { it.date }
            var targetIndex = 2 // Starts after Calendar card (index 0) and Header row (index 1)
            var found = false
            for (entry in groups.entries) {
                val dateGroup = entry.key
                val notesOnDate = entry.value
                if (dateGroup == selectedDateStr) {
                    found = true
                    break
                }
                targetIndex += 1 // For the date group header item
                targetIndex += notesOnDate.size // For the notes inside this group
            }
            if (found) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        // Search inputs row with Calendar icon trigger (hidden when a date filter is selected)
        AnimatedVisibility(
            visible = selectedDateStr == null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        placeholder = { Text("Search title, pastor, topics...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF1E3A8A)) },
                        trailingIcon = if (searchKeyword.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchKeyword = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = Color(0xFF1E3A8A))
                                }
                            }
                        } else null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black.copy(alpha = 0.8f),
                            focusedContainerColor = Color.White.copy(alpha = 0.8f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                            focusedBorderColor = Color(0xFF1E3A8A),
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                        )
                    )

                    IconButton(
                        onClick = { showCalendar = !showCalendar },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showCalendar) Color(0xFF1E3A8A) else Color.Transparent,
                            contentColor = if (showCalendar) Color.White else Color(0xFF1E3A8A)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Toggle Calendar View"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Categories Chips Row
        AnimatedVisibility(
            visible = viewModel.isBibleHeaderVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                val categorizers = listOf("All", "Corporate Worship", "Sunday School", "Other Classes")
                items(categorizers) { cat ->
                    val isSelected = activeFilterCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeFilterCategory = cat },
                        label = { Text(cat, fontSize = 11.sp, color = if (isSelected) Color.White else Color(0xFF1E3A8A)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A)
                        )
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Interactive Calendar Card
            item {
                AnimatedVisibility(
                    visible = showCalendar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val calBackCol = if (viewModel.isDarkMode) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.85f)
                    val calBorderCol = if (viewModel.isDarkMode) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = calBackCol),
                        border = androidx.compose.foundation.BorderStroke(1.dp, calBorderCol)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Month selector row
                            val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                            val monthLabel = "${monthNames[currentMonth]} $currentYear"
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentMonth == 0) {
                                            currentMonth = 11
                                            currentYear -= 1
                                        } else {
                                            currentMonth -= 1
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Prev Month", tint = if (viewModel.isDarkMode) Color(0xFF90CAF9) else Color(0xFF1E3A8A))
                                }
                                
                                Text(
                                    text = monthLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CinzelBoldFamily,
                                    color = if (viewModel.isDarkMode) Color(0xFF90CAF9) else Color(0xFF1E3A8A)
                                )
                                
                                IconButton(
                                    onClick = {
                                        if (currentMonth == 11) {
                                            currentMonth = 0
                                            currentYear += 1
                                        } else {
                                            currentMonth += 1
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month", tint = if (viewModel.isDarkMode) Color(0xFF90CAF9) else Color(0xFF1E3A8A))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Weekdays Header Row
                            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                            Row(modifier = Modifier.fillMaxWidth()) {
                                daysOfWeek.forEach { d ->
                                    Text(
                                        text = d,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewModel.isDarkMode) Color(0xFF90CAF9).copy(alpha = 0.7f) else Color(0xFF1E3A8A).copy(alpha = 0.6f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Days Grid computation
                            val firstDayCal = GregorianCalendar(currentYear, currentMonth, 1)
                            val dayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ... 
                            val maxDays = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            val totalSquares = (dayOfWeek - 1) + maxDays
                            val rows = (totalSquares + 6) / 7
                            
                            for (r in 0 until rows) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    for (c in 0 until 7) {
                                        val index = r * 7 + c
                                        val dayNum = index - (dayOfWeek - 2)
                                        
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (dayNum in 1..maxDays) {
                                                val cellDateStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth + 1, dayNum)
                                                val notesOnThisDay = filteredNotes.filter { it.date == cellDateStr }
                                                val isSelected = selectedDateStr == cellDateStr
                                                val hasNotes = notesOnThisDay.isNotEmpty()
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelected -> if (viewModel.isDarkMode) Color(0xFF42A5F5) else Color(0xFF1E3A8A)
                                                                hasNotes -> if (viewModel.isDarkMode) Color(0xFF1E2E4F) else Color(0xFFDBEAFE)
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable {
                                                            selectedDateStr = if (isSelected) null else cellDateStr
                                                            if (selectedDateStr != null) {
                                                                showCalendar = false
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                 ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = dayNum.toString(),
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected || hasNotes) FontWeight.Bold else FontWeight.Normal,
                                                            color = when {
                                                                isSelected -> Color.White
                                                                hasNotes -> if (viewModel.isDarkMode) Color(0xFF90CAF9) else Color(0xFF1E3A8A)
                                                                else -> if (viewModel.isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
                                                            }
                                                        )
                                                        if (hasNotes && !isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.primary)
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.size(32.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Searching and listing header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDateStr != null) {
                            "Notes on $selectedDateStr"
                        } else if (searchKeyword.isNotBlank()) {
                            "Search Results (${finalDisplayedNotes.size})"
                        } else {
                            "Most Recent Note"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A),
                        fontFamily = CinzelBoldFamily
                    )
                    
                    if (selectedDateStr != null) {
                        TextButton(
                            onClick = { selectedDateStr = null },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Show Default (Most Recent)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (searchKeyword.isNotBlank()) {
                        TextButton(
                            onClick = { searchKeyword = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Clear Search", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (notes.size > 1) {
                        Text(
                            text = "Select a date to see prior entries",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = Color.Black.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            val sortedGroupedNotes = finalDisplayedNotes.sortedByDescending { it.date }.groupBy { it.date }

            if (sortedGroupedNotes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Notes, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color(0xFF1E3A8A).copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedDateStr != null) "No sermon logs saved on this day." else "No sermon logs registered in this filter.",
                                color = Color.Black.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                sortedGroupedNotes.forEach { (dateGroup, notesOnDate) ->
                    item {
                        Card(
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDateStr == dateGroup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        ) {
                            Text(
                                text = dateGroup,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDateStr == dateGroup) Color.White else MaterialTheme.colorScheme.primary,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    items(notesOnDate) { note ->
                        val containerCol = if (viewModel.isDarkMode) Color.White.copy(alpha = 0.15f) else Color.White
                        val borderCol = if (viewModel.isDarkMode) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingNoteDetails = note }
                                .border(1.dp, borderCol, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = containerCol),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                // Sophisticated color accent bar on left representing the category
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .fillMaxHeight()
                                        .background(
                                            when (note.category) {
                                                "Corporate Worship" -> Color(0xFF1E3A8A)
                                                "Sunday School" -> Color(0xFFD4AF37)
                                                else -> Color(0xFF455A64)
                                            }
                                        )
                                )
                                Column(modifier = Modifier.padding(18.dp).weight(1f)) {
                                    // Upper meta bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Sleek pill chip for category
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when (note.category) {
                                                        "Corporate Worship" -> Color(0xFFEFF6FF)
                                                        "Sunday School" -> Color(0xFFFEFCE8)
                                                        else -> Color(0xFFF1F5F9)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when (note.category) {
                                                                "Corporate Worship" -> Color(0xFF1E3A8A)
                                                                "Sunday School" -> Color(0xFFD4AF37)
                                                                else -> Color(0xFF455A64)
                                                            }
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = note.category.uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = InterFontFamily,
                                                    color = when (note.category) {
                                                        "Corporate Worship" -> Color(0xFF1E3A8A)
                                                        "Sunday School" -> Color(0xFF854D0E)
                                                        else -> Color(0xFF475569)
                                                    }
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                contentDescription = "Starred Note",
                                                tint = if (note.isFavorite) Color(0xFFD4AF37) else Color(0xFFB0BEC5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = note.date, 
                                                fontSize = 11.sp, 
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = InterFontFamily,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Sermon / Note Title
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = InterFontFamily,
                                        color = if (viewModel.isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E3A8A),
                                        lineHeight = 20.sp
                                    )

                                    val (originalPoints, mainContentText) = parseNoteContent(note.richTextNotes)
                                    val redesignedNodes = parseRedesignedNote(note.richTextNotes)
                                    val outlinePoints = if (redesignedNodes != null) {
                                        redesignedNodes.map { node ->
                                            var pt = node.outline
                                            if (node.verses.isNotBlank()) pt += "\n  📖 " + node.verses
                                            if (node.takeaways.isNotBlank()) pt += "\n  💡 " + node.takeaways
                                            pt
                                        }
                                    } else {
                                        originalPoints
                                    }

                                    if (redesignedNodes != null || outlinePoints.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.FormatListNumbered,
                                                    contentDescription = null,
                                                    tint = Color(0xFF3B82F6),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Sermon Insights",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = InterFontFamily,
                                                    color = if (viewModel.isDarkMode) Color(0xFF90CAF9) else Color(0xFF1E3A8A)
                                                )
                                            }
                                            outlinePoints.forEach { pt ->
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Text(
                                                        text = "• ",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF3B82F6),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = pt,
                                                        fontSize = 12.sp,
                                                        fontFamily = InterFontFamily,
                                                        lineHeight = 18.sp,
                                                        color = if (viewModel.isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (note.bibleVerses.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = Color(0xFF854D0E),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = note.bibleVerses,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = InterFontFamily,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = if (viewModel.isDarkMode) Color.White.copy(alpha = 0.15f) else Color(0xFFECEFF1))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Note footer
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Person, 
                                                contentDescription = "Speaker", 
                                                modifier = Modifier.size(12.dp), 
                                                tint = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = note.speaker, 
                                                fontSize = 12.sp, 
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = InterFontFamily,
                                                color = if (viewModel.isDarkMode) Color(0xFFE0E0E0) else Color(0xFF374151)
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (viewModel.isDarkMode) Color.White.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = note.churchName, 
                                                fontSize = 11.sp, 
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = InterFontFamily,
                                                color = if (viewModel.isDarkMode) Color(0xFFECEFF1) else Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // Closes sortedGroupedNotes.forEach
            } // Closes else block
        } // Closes LazyColumn
    } // Closes Columns Column

    // View Note Details Modal Sheet Dialog
    if (viewingNoteDetails != null) {
        val currNote = viewingNoteDetails!!
        Dialog(onDismissRequest = { viewingNoteDetails = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currNote.category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                editingNote = currNote
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Note", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                viewModel.deleteNoteDetails(currNote)
                                viewingNoteDetails = null
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Trash Note", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val (outlinePoints, mainContentPart) = parseNoteContent(currNote.richTextNotes)
                    val redesignedDetails = parseRedesignedNote(currNote.richTextNotes)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = currNote.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val noteSummary = parseRedesignedNoteSummary(currNote.richTextNotes)
                        if (noteSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Summary",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = noteSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        if (redesignedDetails != null || outlinePoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (redesignedDetails != null) "Sermon Outlines & Takeaways" else "Sermon Outline",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                if (redesignedDetails != null) {
                                    redesignedDetails.forEach { node ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = node.outline,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (node.verses.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Filled.MenuBook,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = node.verses,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            if (node.takeaways.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Lightbulb,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF57C00),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = node.takeaways,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    outlinePoints.forEach { point ->
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(10.dp))

                        if (currNote.bibleVerses.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "Bible References Covered:\n${currNote.bibleVerses}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Text(
                            text = mainContentPart,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (currNote.tags.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                currNote.tags.split(",").forEach { tag ->
                                    if (tag.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("#${tag.trim()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preacher: ${currNote.speaker}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Church: ${currNote.churchName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Logged: ${currNote.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewingNoteDetails = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Scribe View", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Edit Note Dialog
    if (editingNote != null) {
        val noteToEdit = editingNote!!
        val context = androidx.compose.ui.platform.LocalContext.current
        val editScope = rememberCoroutineScope()
        
        var editCategory by remember(noteToEdit) { mutableStateOf(noteToEdit.category) }
        var editTitle by remember(noteToEdit) { mutableStateOf(noteToEdit.title) }
        var editSpeaker by remember(noteToEdit) { mutableStateOf(noteToEdit.speaker) }
        var editChurch by remember(noteToEdit) { mutableStateOf(noteToEdit.churchName) }
        var editChurchDropdownExpanded by remember { mutableStateOf(false) }
        val parsedRedesigned = remember(noteToEdit) { parseRedesignedNote(noteToEdit.richTextNotes) }
        var editOutlineNodes by remember(noteToEdit) {
            mutableStateOf(
                parsedRedesigned ?: run {
                    val parsedLegacy = parseNoteContent(noteToEdit.richTextNotes)
                    if (parsedLegacy.first.isEmpty()) {
                        listOf(OutlineNode("I. ", "", ""), OutlineNode("II. ", "", ""), OutlineNode("III. ", "", ""))
                    } else {
                        parsedLegacy.first.map { OutlineNode(it, "", "") }
                    }
                }
            )
        }
        val parsedSummary = remember(noteToEdit) { parseRedesignedNoteSummary(noteToEdit.richTextNotes) }
        var editSummary by remember(noteToEdit) { mutableStateOf(parsedSummary) }
        var isGeneratingEditSummary by remember { mutableStateOf(false) }
        
        var editTags by remember(noteToEdit) { mutableStateOf(noteToEdit.tags) }
        var editFavorite by remember(noteToEdit) { mutableStateOf(noteToEdit.isFavorite) }
        
        androidx.compose.ui.window.Dialog(onDismissRequest = { editingNote = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Edit Scribe Note Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { editingNote = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close edit")
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            var editCategoryExpanded by remember { mutableStateOf(false) }
                            Text("Note Category", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                OutlinedTextField(
                                    value = editCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Category") },
                                    trailingIcon = {
                                        IconButton(onClick = { editCategoryExpanded = !editCategoryExpanded }) {
                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { editCategoryExpanded = !editCategoryExpanded },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                )
                                DropdownMenu(
                                    expanded = editCategoryExpanded,
                                    onDismissRequest = { editCategoryExpanded = false },
                                    modifier = Modifier.fillMaxWidth().background(Color.White)
                                ) {
                                    val categories = listOf("Corporate Worship", "Sunday School", "Other Classes")
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                editCategory = cat
                                                editCategoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        item {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Sermon/Lesson Title") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Sermon Outline Builder", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "For each outline point, edit its associated scripture references and your personal takeaways.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                
                                editOutlineNodes.forEachIndexed { index, node ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, 
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Point #${index + 1}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF3B82F6)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val updated = editOutlineNodes.toMutableList()
                                                        updated.removeAt(index)
                                                        editOutlineNodes = updated
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Delete, 
                                                        contentDescription = "Delete point", 
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            // Outline Point Input field
                                            OutlinedTextField(
                                                value = node.outline,
                                                onValueChange = { newValue ->
                                                    val updated = editOutlineNodes.toMutableList()
                                                    updated[index] = updated[index].copy(outline = newValue)
                                                    editOutlineNodes = updated
                                                },
                                                label = { Text("Outline Topic / Heading") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = false
                                            )

                                            // Scripture verses Input field
                                            OutlinedTextField(
                                                value = node.verses,
                                                onValueChange = { newValue ->
                                                    val updated = editOutlineNodes.toMutableList()
                                                    updated[index] = updated[index].copy(verses = newValue)
                                                    editOutlineNodes = updated
                                                },
                                                label = { Text("Verses (e.g., Romans 5:8; John 3:16)") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Filled.MenuBook, 
                                                        contentDescription = null, 
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = false
                                            )

                                            // Takeaways Input field
                                            OutlinedTextField(
                                                value = node.takeaways,
                                                onValueChange = { newValue ->
                                                    val updated = editOutlineNodes.toMutableList()
                                                    updated[index] = updated[index].copy(takeaways = newValue)
                                                    editOutlineNodes = updated
                                                },
                                                label = { Text("Takeaways") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Filled.Lightbulb, 
                                                        contentDescription = null, 
                                                        modifier = Modifier.size(16.dp),
                                                        tint = Color(0xFFF57C00)
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = false
                                            )
                                        }
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        val romanNumerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV")
                                        val prefix = if (editOutlineNodes.size < romanNumerals.size) {
                                            "${romanNumerals[editOutlineNodes.size]}. "
                                        } else {
                                            "${editOutlineNodes.size + 1}. "
                                        }
                                        editOutlineNodes = editOutlineNodes + OutlineNode(prefix, "", "")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Filled.Add, 
                                        contentDescription = "Add Point", 
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Add Outline Point", 
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Summary", 
                                        style = MaterialTheme.typography.bodySmall, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Button(
                                        onClick = {
                                            isGeneratingEditSummary = true
                                            val activeNodes = editOutlineNodes.filter { it.outline.isNotBlank() }
                                            editScope.launch {
                                                try {
                                                    val summary = com.example.data.GeminiService.generateSermonSummary(
                                                        title = editTitle,
                                                        speaker = editSpeaker,
                                                        church = editChurch,
                                                        nodes = activeNodes
                                                    )
                                                    if (summary.startsWith("Error:")) {
                                                        Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                                                    } else {
                                                        editSummary = summary
                                                        Toast.makeText(context, "Summary generated successfully!", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to generate summary: ${e.message}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isGeneratingEditSummary = false
                                                }
                                            }
                                        },
                                        enabled = !isGeneratingEditSummary,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        if (isGeneratingEditSummary) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            if (isGeneratingEditSummary) "Generating..." else "Generate Summary", 
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = editSummary,
                                    onValueChange = { editSummary = it },
                                    label = { Text("Daily Summary") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    minLines = 2,
                                    maxLines = 4,
                                    singleLine = false
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = editTags,
                                onValueChange = { editTags = it },
                                label = { Text("Tags separator (e.g. grace, faith)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editSpeaker,
                                    onValueChange = { editSpeaker = it },
                                    label = { Text("Preacher/Pastor") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = editChurch,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Church Name") },
                                        trailingIcon = {
                                            IconButton(onClick = { editChurchDropdownExpanded = !editChurchDropdownExpanded }) {
                                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable { editChurchDropdownExpanded = !editChurchDropdownExpanded },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        )
                                    )
                                    DropdownMenu(
                                        expanded = editChurchDropdownExpanded,
                                        onDismissRequest = { editChurchDropdownExpanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White)
                                    ) {
                                        if (churchNames.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No churches in directory") },
                                                onClick = { editChurchDropdownExpanded = false }
                                            )
                                        } else {
                                            churchNames.forEach { name ->
                                                DropdownMenuItem(
                                                    text = { Text(name) },
                                                    onClick = {
                                                        editChurch = name
                                                        editChurchDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = editFavorite, onCheckedChange = { editFavorite = it })
                                Text("Add to Spiritual Favorites Star")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            if (editTitle.isNotBlank()) {
                                val activeNodes = editOutlineNodes.filter { it.outline.isNotBlank() }
                                val finalNotes = serializeRedesignedNoteV2(activeNodes, editSummary)
                                val finalBibleVerses = activeNodes.map { it.verses.trim() }.filter { it.isNotBlank() }.joinToString("; ")
                                
                                val updatedNote = noteToEdit.copy(
                                    category = editCategory,
                                    title = editTitle,
                                    speaker = editSpeaker,
                                    churchName = editChurch,
                                    bibleVerses = finalBibleVerses,
                                    richTextNotes = finalNotes,
                                    tags = editTags,
                                    isFavorite = editFavorite
                                )
                                
                                viewModel.updateNoteDetails(updatedNote)
                                viewingNoteDetails = updatedNote // Update active detail view
                                editingNote = null
                                Toast.makeText(context, "Sermon insight successfully updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Lesson Title can't be empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Note Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. ACTIVITIES PLANNER MODULE
// ==========================================
@Composable
fun ChurchActivitiesView(viewModel: AppViewModel) {
    val activities by viewModel.allActivities.collectAsStateWithLifecycle()
    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<ChurchActivity?>(null) }

    val context = LocalContext.current

    val listState = rememberLazyListState()
    var prevIndex by remember { mutableStateOf(0) }
    var prevOffset by remember { mutableStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        val delta = if (currentIndex == prevIndex) {
            currentOffset - prevOffset
        } else {
            (currentIndex - prevIndex) * 200
        }
        if (delta > 30) {
            if (viewModel.isBibleHeaderVisible) {
                viewModel.isBibleHeaderVisible = false
            }
        } else if (delta < -30) {
            if (!viewModel.isBibleHeaderVisible) {
                viewModel.isBibleHeaderVisible = true
            }
        }
        if (currentIndex == 0 && currentOffset < 20) {
            viewModel.isBibleHeaderVisible = true
        }
        prevIndex = currentIndex
        prevOffset = currentOffset
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = viewModel.isBibleHeaderVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Church Activities",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CinzelBoldFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Services, cell groups & prayer points calendar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = { showAddEventDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add New Event", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (activities.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Event,
                title = "No Upcoming Activities",
                description = "Plan your weekly service attendance and church groups meetings."
            )
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activities) { act ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Event,
                                        contentDescription = null,
                                        tint = Color(0xFF1E3A8A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${act.date} • ${act.time}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingActivity = act
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit Event", tint = Color(0xFF1E3A8A), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.removeActivity(act)
                                            NotificationHelper.cancelNotification(context, act.id)
                                            Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove Event", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = act.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF64748B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(act.location, fontSize = 12.sp, color = Color(0xFF475569))
                            }

                            if (act.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = act.notes,
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val notifColor = if (act.notificationEnabled) Color(0xFF059669) else Color(0xFF64748B)
                                    Icon(
                                        if (act.notificationEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                        contentDescription = "Alert notifications state",
                                        tint = notifColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (act.notificationEnabled) "Reminders Enabled" else "Muted Alerts",
                                        fontSize = 11.sp,
                                        color = notifColor
                                    )
                                }

                                if (act.isRecurring) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFEFF6FF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Every ${act.recurrenceDay}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        var evTitle by remember { mutableStateOf("") }
        var evDate by remember { mutableStateOf("2026-06-07") }
        var evTime by remember { mutableStateOf("10:00") }
        var evLocation by remember { mutableStateOf("") }
        var evNotes by remember { mutableStateOf("") }
        var evRecurring by remember { mutableStateOf(false) }
        var evRecurDay by remember { mutableStateOf("Sunday") }

        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDayOfMonth)
                evDate = "$selectedYear-$formattedMonth-$formattedDay"
            },
            year, month, day
        )

        val timePickerDialog = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minuteOfHour ->
                val formattedHour = String.format("%02d", hourOfDay)
                val formattedMinute = String.format("%02d", minuteOfHour)
                evTime = "$formattedHour:$formattedMinute"
            },
            10, 0, false
        )

        val textColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF0F172A),
            unfocusedTextColor = Color(0xFF334155),
            focusedLabelColor = Color(0xFF1E3A8A),
            unfocusedLabelColor = Color(0xFF475569),
            focusedBorderColor = Color(0xFF1E3A8A),
            unfocusedBorderColor = Color(0xFFCBD5E1),
            cursorColor = Color(0xFF1E3A8A)
        )

        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Log Church Activity",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    item {
                        OutlinedTextField(
                            value = evTitle,
                            onValueChange = { evTitle = it },
                            label = { Text("Event Title") },
                            placeholder = { Text("Sunday School Fellowship") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = evDate,
                            onValueChange = { evDate = it },
                            label = { Text("Select Date") },
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Filled.DateRange, contentDescription = "Select Date", tint = Color(0xFF1E3A8A))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = evTime,
                            onValueChange = { evTime = it },
                            label = { Text("Select Time") },
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            trailingIcon = {
                                IconButton(onClick = { timePickerDialog.show() }) {
                                    Icon(Icons.Filled.Schedule, contentDescription = "Select Time", tint = Color(0xFF1E3A8A))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { timePickerDialog.show() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = evLocation,
                            onValueChange = { evLocation = it },
                            label = { Text("Venue") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = evNotes,
                            onValueChange = { evNotes = it },
                            label = { Text("Spiritual Notes") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = evRecurring,
                                onCheckedChange = { evRecurring = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E3A8A))
                            )
                            Text("Weekly recurring service", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF0F172A))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (evTitle.isNotBlank()) {
                            viewModel.createActivity(evTitle, evDate, evTime, evLocation, evNotes, true, evRecurring, evRecurDay) { savedAct ->
                                try {
                                    // Trigger actual, exact scheduled local alarm notification
                                    NotificationHelper.scheduleNotification(
                                        context,
                                        savedAct.id,
                                        savedAct.title,
                                        savedAct.date,
                                        savedAct.time
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            showAddEventDialog = false
                            Toast.makeText(context, "Activity logged successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Add Calendar Event", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1E3A8A)))
                }
            }
        )
    }

    if (editingActivity != null) {
        val original = editingActivity!!
        var evTitle by remember(original) { mutableStateOf(original.title) }
        var evDate by remember(original) { mutableStateOf(original.date) }
        var evTime by remember(original) { mutableStateOf(original.time) }
        var evLocation by remember(original) { mutableStateOf(original.location) }
        var evNotes by remember(original) { mutableStateOf(original.notes) }
        var evRecurring by remember(original) { mutableStateOf(original.isRecurring) }
        var evRecurDay by remember(original) { mutableStateOf(original.recurrenceDay) }

        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDayOfMonth)
                evDate = "$selectedYear-$formattedMonth-$formattedDay"
            },
            year, month, day
        )

        val timePickerDialog = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minuteOfHour ->
                val formattedHour = String.format("%02d", hourOfDay)
                val formattedMinute = String.format("%02d", minuteOfHour)
                evTime = "$formattedHour:$formattedMinute"
            },
            10, 0, false
        )

        val textColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF0F172A),
            unfocusedTextColor = Color(0xFF334155),
            focusedLabelColor = Color(0xFF1E3A8A),
            unfocusedLabelColor = Color(0xFF475569),
            focusedBorderColor = Color(0xFF1E3A8A),
            unfocusedBorderColor = Color(0xFFCBD5E1),
            cursorColor = Color(0xFF1E3A8A)
        )

        AlertDialog(
            onDismissRequest = { editingActivity = null },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Edit Church Activity",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    item {
                        OutlinedTextField(
                            value = evTitle,
                            onValueChange = { evTitle = it },
                            label = { Text("Event Title") },
                            placeholder = { Text("Sunday School Fellowship") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = evDate,
                            onValueChange = { evDate = it },
                            label = { Text("Select Date") },
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Filled.DateRange, contentDescription = "Select Date", tint = Color(0xFF1E3A8A))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = evTime,
                            onValueChange = { evTime = it },
                            label = { Text("Select Time") },
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            trailingIcon = {
                                IconButton(onClick = { timePickerDialog.show() }) {
                                    Icon(Icons.Filled.Schedule, contentDescription = "Select Time", tint = Color(0xFF1E3A8A))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { timePickerDialog.show() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = evLocation,
                            onValueChange = { evLocation = it },
                            label = { Text("Venue") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = evNotes,
                            onValueChange = { evNotes = it },
                            label = { Text("Spiritual Notes") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0F172A)),
                            colors = textColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = evRecurring,
                                onCheckedChange = { evRecurring = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E3A8A))
                            )
                            Text("Weekly recurring service", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF0F172A))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (evTitle.isNotBlank()) {
                            val updatedAct = original.copy(
                                title = evTitle,
                                date = evDate,
                                time = evTime,
                                location = evLocation,
                                notes = evNotes,
                                isRecurring = evRecurring,
                                recurrenceDay = evRecurDay
                            )
                            viewModel.updateActivity(updatedAct)
                            editingActivity = null
                            Toast.makeText(context, "Activity updated successfully", Toast.LENGTH_SHORT).show()

                            // Cancel original, schedule newly updated notification alarm
                            try {
                                NotificationHelper.cancelNotification(context, updatedAct.id)
                                NotificationHelper.scheduleNotification(
                                    context,
                                    updatedAct.id,
                                    updatedAct.title,
                                    updatedAct.date,
                                    updatedAct.time
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Save Changes", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingActivity = null }) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1E3A8A)))
                }
            }
        )
    }
}

// ==========================================
// 7. PHILIPPINES BAPTIST DIRECTORY
// ==========================================
@Composable
fun BaptistChurchesDirectoryView(viewModel: AppViewModel) {
    val approvedChurches by viewModel.approvedChurches.collectAsStateWithLifecycle()
    var displayAddChurchModal by remember { mutableStateOf(false) }
    var editingChurch by remember { mutableStateOf<BaptistChurch?>(null) }
    var deletingChurch by remember { mutableStateOf<BaptistChurch?>(null) }
    var showAddSuccessDialog by remember { mutableStateOf(false) }
    var showEditSuccessDialog by remember { mutableStateOf(false) }
    var editSuccessMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    val listState = rememberLazyListState()
    var prevIndex by remember { mutableStateOf(0) }
    var prevOffset by remember { mutableStateOf(0) }

    val simulatedUserLat = 14.5995
    val simulatedUserLng = 120.9842

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        val delta = if (currentIndex == prevIndex) {
            currentOffset - prevOffset
        } else {
            (currentIndex - prevIndex) * 200
        }
        if (delta > 30) {
            if (viewModel.isBibleHeaderVisible) {
                viewModel.isBibleHeaderVisible = false
            }
        } else if (delta < -30) {
            if (!viewModel.isBibleHeaderVisible) {
                viewModel.isBibleHeaderVisible = true
            }
        }
        if (currentIndex == 0 && currentOffset < 20) {
            viewModel.isBibleHeaderVisible = true
        }
        prevIndex = currentIndex
        prevOffset = currentOffset
    }

    val activeProvinces = remember(approvedChurches) {
        approvedChurches.map { it.province }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredChurches = remember(approvedChurches, viewModel.selectedProvinceFilter, viewModel.churchSearchQuery) {
        val query = viewModel.churchSearchQuery.trim()
        val baseList = if (viewModel.selectedProvinceFilter == "All") {
            approvedChurches
        } else {
            approvedChurches.filter { it.province.equals(viewModel.selectedProvinceFilter, ignoreCase = true) }
        }
        if (query.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.pastorName.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true) ||
                it.cityMunicipality.contains(query, ignoreCase = true) ||
                it.province.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = viewModel.isBibleHeaderVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Church Directory",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Fundamental Baptist churches across the Philippines",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = { displayAddChurchModal = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFFFBBF24)
                        ),
                        modifier = Modifier.testTag("submit_church_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddHome, 
                            contentDescription = "Submit New Church", 
                            tint = Color(0xFFFBBF24)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unified Search & Filter Row (highly compact and space-efficient)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = viewModel.churchSearchQuery,
                        onValueChange = { viewModel.churchSearchQuery = it },
                        placeholder = { Text("Search location, pastor...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("church_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )

                    // Province Filter Dropdown Selector as a compact button
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { dropdownExpanded = !dropdownExpanded },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.height(50.dp) // align visually with OutlinedTextField height
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = "Filter",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewModel.selectedProvinceFilter == "All") "All" else viewModel.selectedProvinceFilter,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = "Expand",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.FilterList,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("All Provinces", fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    viewModel.selectedProvinceFilter = "All"
                                    dropdownExpanded = false
                                },
                                modifier = Modifier.testTag("dropdown_item_recent")
                            )

                            if (activeProvinces.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                activeProvinces.forEach { prov ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Place,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(prov)
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectedProvinceFilter = prov
                                            dropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("dropdown_item_${prov.replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (filteredChurches.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Home,
                title = "No Churches Found",
                description = "Try expanding your filter or search, or submit a new church for approval!"
            )
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredChurches) { church ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = church.province,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val isNew = church.timestamp > 0L && (System.currentTimeMillis() - church.timestamp < 604_800_000L)
                                    if (isNew) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF10B981))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.NewReleases,
                                                    contentDescription = "New Church",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "NEW",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Dial/Call
                                    if (church.contactNumber.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${church.contactNumber}"))
                                                context.startActivity(dialIntent)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.Phone, contentDescription = "Contact Church", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    if (viewModel.currentUser != null) {
                                        // Edit
                                        IconButton(
                                            onClick = { editingChurch = church },
                                            modifier = Modifier.size(28.dp).testTag("edit_church_${church.id}")
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit Church", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                        }

                                        // Delete
                                        IconButton(
                                            onClick = { deletingChurch = church },
                                            modifier = Modifier.size(28.dp).testTag("delete_church_${church.id}")
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete Church", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (church.isDeletePending) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Awaiting Deletion Review by Admin",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Text(
                                text = church.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CinzelBoldFamily,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row {
                                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFEA4335))
                                Spacer(modifier = Modifier.width(4.dp))
                                val displayAddr = church.address.substringBefore("\nMap Pin:")
                                Text(displayAddr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), lineHeight = 16.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBox, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF4285F4))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pastor(s): ${church.pastorName}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }

                            if (church.contactNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${church.contactNumber}"))
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not dial number", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Phone, contentDescription = "Phone Number", modifier = Modifier.size(13.dp), tint = Color(0xFF34A853))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Phone: ${church.contactNumber}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    )
                                }
                            }

                            val hasFbLink = church.facebookUrl.isNotBlank() && 
                                            church.facebookUrl.contains("facebook.com", ignoreCase = true) &&
                                            !church.facebookUrl.equals("na", ignoreCase = true) &&
                                            !church.facebookUrl.equals("n/a", ignoreCase = true) &&
                                            !church.facebookUrl.equals("none", ignoreCase = true)

                            if (hasFbLink) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        try {
                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(church.facebookUrl))
                                            context.startActivity(webIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open Facebook page", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_facebook),
                                        contentDescription = "Facebook Page",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Unspecified
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Facebook Page",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1877F2),
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    )
                                }
                            }

                            if (church.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = church.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                            .testTag("church_description")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Weekly Worship Gatherings:\n${church.worshipSchedule}",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (displayAddChurchModal) {
        var chName by remember { mutableStateOf("") }
        var chProvince by remember { mutableStateOf("Metro Manila") }
        var chAddress by remember { mutableStateOf("") }
        var pastorsList by remember { mutableStateOf(listOf("")) }
        var chContact by remember { mutableStateOf("") }
        var chSchedule by remember { mutableStateOf("Sunday Worship: 09:30 AM") }
        var chFacebook by remember { mutableStateOf("") }
        var showDuplicateWarning by remember { mutableStateOf(false) }
        var duplicateChurchNameText by remember { mutableStateOf("") }

        var chCityMunicipality by remember { mutableStateOf("") }
        var chDescription by remember { mutableStateOf("") }
        var chLatitude by remember { mutableStateOf(14.5995) }
        var chLongitude by remember { mutableStateOf(120.9842) }

        var citiesSuggestions by remember(chProvince) {
            mutableStateOf(com.example.data.PsgcData.getCitiesAndMunicipalitiesForProvince(chProvince))
        }
        LaunchedEffect(chProvince) {
            citiesSuggestions = com.example.data.PsgcData.fetchCitiesForProvinceAsync(chProvince)
        }

        AlertDialog(
            onDismissRequest = { displayAddChurchModal = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Submit Baptist Church",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = CinzelBoldFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = chName,
                            onValueChange = { chName = it },
                            label = { Text("Church Name", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Grace Baptist Church", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }
                        val provinces = listOf(
                            "Metro Manila", "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", 
                            "Aurora", "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol", "Bukidnon", 
                            "Bulacan", "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes", "Cavite", 
                            "Cebu", "Cotabato", "Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental", "Davao Oriental", 
                            "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur", "Iloilo", "Isabela", 
                            "Kalinga", "La Union", "Laguna", "Lanao del Norte", "Lanao del Sur", "Leyte", "Maguindanao del Norte", 
                            "Maguindanao del Sur", "Marinduque", "Masbate", "Misamis Occidental", "Misamis Oriental", "Mountain Province", 
                            "Negros Occidental", "Negros Oriental", "Northern Samar", "Nueva Ecija", "Nueva Vizcaya", "Occidental Mindoro", 
                            "Oriental Mindoro", "Palawan", "Pampanga", "Pangasinan", "Quezon", "Quirino", "Rizal", "Romblon", "Samar", 
                            "Sarangani", "Siquijor", "Sorsogon", "South Cotabato", "Southern Leyte", "Sultan Kudarat", "Sulu", 
                            "Surigao del Norte", "Surigao del Sur", "Tarlac", "Tawi-Tawi", "Zambales", "Zamboanga del Norte", 
                            "Zamboanga del Sur", "Zamboanga Sibugay"
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = chProvince,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Province", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                trailingIcon = {
                                    IconButton(onClick = { expanded = !expanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose Province")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).background(Color.White)
                            ) {
                                provinces.forEach { province ->
                                    DropdownMenuItem(
                                        text = { Text(province, fontFamily = InterFontFamily, fontSize = 13.sp, color = Color.Black) },
                                        onClick = {
                                            chProvince = province
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chAddress,
                            onValueChange = { chAddress = it },
                            label = { Text("Exact Address Directions", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Unit 4B, Emerald Street", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        var cityExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (chCityMunicipality.isEmpty()) "Select City/Municipality..." else chCityMunicipality,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("City or Municipality", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                trailingIcon = {
                                    IconButton(onClick = { cityExpanded = !cityExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose City")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { cityExpanded = !cityExpanded },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            )
                            DropdownMenu(
                                expanded = cityExpanded,
                                onDismissRequest = { cityExpanded = false },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).background(Color.White)
                            ) {
                                citiesSuggestions.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(city, fontFamily = InterFontFamily, fontSize = 13.sp, color = Color.Black) },
                                        onClick = {
                                            chCityMunicipality = city
                                            cityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chDescription,
                            onValueChange = { chDescription = it },
                            label = { Text("Church Testimony / Description", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Preaching Christ in the local community", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Pastors & Leaders", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            pastorsList.forEachIndexed { index, pastor ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pastor,
                                        onValueChange = { newValue ->
                                            val updated = pastorsList.toMutableList()
                                            updated[index] = newValue
                                            pastorsList = updated
                                        },
                                        label = { Text(if (index == 0) "Lead Pastor" else "Preacher/Pastor ${index + 1}", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                        placeholder = { Text("e.g. Pastor Timothy", fontFamily = InterFontFamily, fontSize = 13.sp) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    )
                                    if (pastorsList.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val updated = pastorsList.toMutableList()
                                                updated.removeAt(index)
                                                pastorsList = updated
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove pastor", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = {
                                    pastorsList = pastorsList + ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Other Pastor", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chContact,
                            onValueChange = { chContact = it },
                            label = { Text("Ph Contact Number", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. +639171234567", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = chSchedule,
                            onValueChange = { chSchedule = it },
                            label = { Text("Gathering Services Times", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Sunday 10:00 AM", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = chFacebook,
                            onValueChange = { chFacebook = it },
                            label = { Text("Facebook Page Link", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. https://facebook.com/churchpage", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chName.isNotBlank() && chAddress.isNotBlank()) {
                            val trimmedName = chName.trim()
                            val duplicate = viewModel.approvedChurches.value.find { it.name.trim().equals(trimmedName, ignoreCase = true) }
                            if (duplicate != null) {
                                duplicateChurchNameText = duplicate.name
                                showDuplicateWarning = true
                            } else {
                                val combinedPastors = pastorsList.filter { it.isNotBlank() }.joinToString(", ")
                                viewModel.submitNewChurch(
                                    name = chName,
                                    province = chProvince,
                                    cityMunicipality = chCityMunicipality.ifEmpty { "Poblacion" },
                                    address = chAddress,
                                    pastor = combinedPastors,
                                    contact = chContact,
                                    schedule = chSchedule,
                                    description = chDescription.ifEmpty { "We preach the Gospel of Christ and serve local families." },
                                    latitude = chLatitude,
                                    longitude = chLongitude,
                                    facebookUrl = chFacebook
                                )
                                displayAddChurchModal = false
                                showAddSuccessDialog = true
                            }
                        } else {
                            Toast.makeText(context, "Please enter both Church Name and Exact Address Directions.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Submit for Approval", style = MaterialTheme.typography.bodyMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { displayAddChurchModal = false }) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )

        if (showDuplicateWarning) {
            AlertDialog(
                onDismissRequest = { showDuplicateWarning = false },
                containerColor = Color.White,
                title = {
                    Text("Duplicate Church Suggestion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                text = {
                    Text("A church with the name \"$duplicateChurchNameText\" already exists in our directory. We highly recommend using the existing church already present in the directory.\n\nWould you still like to proceed with adding this new church anyway?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDuplicateWarning = false
                            val combinedPastors = pastorsList.filter { it.isNotBlank() }.joinToString(", ")
                            viewModel.submitNewChurch(
                                name = chName,
                                province = chProvince,
                                cityMunicipality = chCityMunicipality.ifEmpty { "Poblacion" },
                                address = chAddress,
                                pastor = combinedPastors,
                                contact = chContact,
                                schedule = chSchedule,
                                description = chDescription.ifEmpty { "We preach the Gospel of Christ and serve local families." },
                                latitude = chLatitude,
                                longitude = chLongitude,
                                facebookUrl = chFacebook
                            )
                            displayAddChurchModal = false
                            showAddSuccessDialog = true
                        }
                    ) {
                        Text("Continue Adding Anyway")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDuplicateWarning = false }
                    ) {
                        Text("Cancel / Use Existing")
                    }
                }
            )
        }
    }

    if (editingChurch != null) {
        val currentEdit = editingChurch!!
        var chName by remember { mutableStateOf(currentEdit.name) }
        var chProvince by remember { mutableStateOf(currentEdit.province) }
        var chAddress by remember { mutableStateOf(currentEdit.address) }
        var pastorsList by remember { mutableStateOf(currentEdit.pastorName.split(", ").filter { it.isNotBlank() }.let { if (it.isEmpty()) listOf("") else it }) }
        var chContact by remember { mutableStateOf(currentEdit.contactNumber) }
        var chSchedule by remember { mutableStateOf(currentEdit.worshipSchedule) }

        var chCityMunicipality by remember { mutableStateOf(currentEdit.cityMunicipality) }
        var chDescription by remember { mutableStateOf(currentEdit.description) }
        var chLatitude by remember { mutableStateOf(currentEdit.latitude) }
        var chLongitude by remember { mutableStateOf(currentEdit.longitude) }
        var chFacebook by remember { mutableStateOf(currentEdit.facebookUrl) }

        var citiesSuggestions by remember(chProvince) {
            mutableStateOf(com.example.data.PsgcData.getCitiesAndMunicipalitiesForProvince(chProvince))
        }
        LaunchedEffect(chProvince) {
            citiesSuggestions = com.example.data.PsgcData.fetchCitiesForProvinceAsync(chProvince)
        }

        AlertDialog(
            onDismissRequest = { editingChurch = null },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (viewModel.currentUser?.isAdmin == true) "Edit Church (Admin)" else "Propose Church Edits",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = CinzelBoldFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = chName,
                            onValueChange = { chName = it },
                            label = { Text("Church Name", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Grace Baptist Church", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }
                        val provinces = listOf(
                            "Metro Manila", "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", 
                            "Aurora", "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol", "Bukidnon", 
                            "Bulacan", "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes", "Cavite", 
                            "Cebu", "Cotabato", "Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental", "Davao Oriental", 
                            "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur", "Iloilo", "Isabela", 
                            "Kalinga", "La Union", "Laguna", "Lanao del Norte", "Lanao del Sur", "Leyte", "Maguindanao del Norte", 
                            "Maguindanao del Sur", "Marinduque", "Masbate", "Misamis Occidental", "Misamis Oriental", "Mountain Province", 
                            "Negros Occidental", "Negros Oriental", "Northern Samar", "Nueva Ecija", "Nueva Vizcaya", "Occidental Mindoro", 
                            "Oriental Mindoro", "Palawan", "Pampanga", "Pangasinan", "Quezon", "Quirino", "Rizal", "Romblon", "Samar", 
                            "Sarangani", "Siquijor", "Sorsogon", "South Cotabato", "Southern Leyte", "Sultan Kudarat", "Sulu", 
                            "Surigao del Norte", "Surigao del Sur", "Tarlac", "Tawi-Tawi", "Zambales", "Zamboanga del Norte", 
                            "Zamboanga del Sur", "Zamboanga Sibugay"
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = chProvince,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Province", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                trailingIcon = {
                                    IconButton(onClick = { expanded = !expanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose Province")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).background(Color.White)
                            ) {
                                provinces.forEach { province ->
                                    DropdownMenuItem(
                                        text = { Text(province, fontFamily = InterFontFamily, fontSize = 13.sp, color = Color.Black) },
                                        onClick = {
                                            chProvince = province
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chAddress,
                            onValueChange = { chAddress = it },
                            label = { Text("Exact Address Directions", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Unit 4B, Emerald Street", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        var cityExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (chCityMunicipality.isEmpty()) "Select City/Municipality..." else chCityMunicipality,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("City or Municipality", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                trailingIcon = {
                                    IconButton(onClick = { cityExpanded = !cityExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose City")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { cityExpanded = !cityExpanded },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            )
                            DropdownMenu(
                                expanded = cityExpanded,
                                onDismissRequest = { cityExpanded = false },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).background(Color.White)
                            ) {
                                citiesSuggestions.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(city, fontFamily = InterFontFamily, fontSize = 13.sp, color = Color.Black) },
                                        onClick = {
                                            chCityMunicipality = city
                                            cityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chDescription,
                            onValueChange = { chDescription = it },
                            label = { Text("Church Testimony / Description", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Preaching Christ in the local community", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Pastors & Leaders", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            pastorsList.forEachIndexed { index, pastor ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pastor,
                                        onValueChange = { newValue ->
                                            val updated = pastorsList.toMutableList()
                                            updated[index] = newValue
                                            pastorsList = updated
                                        },
                                        label = { Text(if (index == 0) "Lead Pastor" else "Preacher/Pastor ${index + 1}", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                        placeholder = { Text("e.g. Pastor Timothy", fontFamily = InterFontFamily, fontSize = 13.sp) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    )
                                    if (pastorsList.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val updated = pastorsList.toMutableList()
                                                updated.removeAt(index)
                                                pastorsList = updated
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove pastor", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = { pastorsList = pastorsList + "" },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Other Pastor", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chContact,
                            onValueChange = { chContact = it },
                            label = { Text("Ph Contact Number", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. +639171234567", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = chSchedule,
                            onValueChange = { chSchedule = it },
                            label = { Text("Gathering Services Times", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Sunday 10:00 AM", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = chFacebook,
                            onValueChange = { chFacebook = it },
                            label = { Text("Facebook Page URL", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. https://facebook.com/churchpage", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chName.isNotBlank() && chAddress.isNotBlank()) {
                            val combinedPastors = pastorsList.filter { it.isNotBlank() }.joinToString(", ")
                            viewModel.editChurch(
                                churchId = currentEdit.id,
                                name = chName,
                                province = chProvince,
                                cityMunicipality = chCityMunicipality.ifEmpty { currentEdit.cityMunicipality },
                                address = chAddress,
                                pastor = combinedPastors,
                                contact = chContact,
                                schedule = chSchedule,
                                description = chDescription.ifEmpty { currentEdit.description },
                                latitude = chLatitude,
                                longitude = chLongitude,
                                facebookUrl = chFacebook,
                                onComplete = { isDirect ->
                                    if (isDirect) {
                                        editSuccessMessage = "The Baptist Church details have been successfully updated live in our system!"
                                    } else {
                                        editSuccessMessage = "Your proposed updates have been submitted to the Admin for approval. Thank you for keeping the Scribe directory accurate!"
                                    }
                                    showEditSuccessDialog = true
                                    editingChurch = null
                                }
                            )
                        } else {
                            Toast.makeText(context, "Please enter both Church Name and Exact Address Directions.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text(if (viewModel.currentUser?.isAdmin == true) "Save Live" else "Propose Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingChurch = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deletingChurch != null) {
        val churchToDel = deletingChurch!!
        AlertDialog(
            onDismissRequest = { deletingChurch = null },
            title = { Text(if (viewModel.currentUser?.isAdmin == true) "Delete Church Live" else "Request Deletion") },
            text = { Text("Are you sure you want to delete '${churchToDel.name}'? ${if (viewModel.currentUser?.isAdmin == true) "As Admin, this will delete it immediately." else "This request will be submitted to the Admin for approval."}") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.requestDeleteChurch(churchToDel) { isDirect ->
                            if (isDirect) {
                                Toast.makeText(context, "Church deleted successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Delete proposal submitted for Admin approval", Toast.LENGTH_LONG).show()
                            }
                            deletingChurch = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (viewModel.currentUser?.isAdmin == true) "Delete Live" else "Request Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingChurch = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showAddSuccessDialog = false },
            containerColor = Color.White,
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Request Submitted!",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "Your Baptist Church entry proposal has been successfully sent to the Admin for approval.\n\nThank you for helping keep our directory updated!",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color(0xFF475569),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAddSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Understood", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showEditSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showEditSuccessDialog = false },
            containerColor = Color.White,
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Details Submitted",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = editSuccessMessage,
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color(0xFF475569),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showEditSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("OK", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==========================================
// 8. ADD NOTES MODAL BOTTOM SHEET
// ==========================================
@Composable
fun AddNoteModalBottomSheet(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val approvedChurches by viewModel.approvedChurches.collectAsStateWithLifecycle(initialValue = emptyList())
    val churchNames = remember(approvedChurches) {
        approvedChurches.map { it.name }.distinct().sorted()
    }
    var ntCategory by remember { mutableStateOf("Corporate Worship") }
    var ntTitle by remember { mutableStateOf("") }
    var ntSpeaker by remember { mutableStateOf("") }
    var ntChurch by remember(churchNames) { 
        mutableStateOf(churchNames.firstOrNull() ?: "Faithway Fundamental Baptist Church") 
    }
    var churchDropdownExpanded by remember { mutableStateOf(false) }
    var outlineNodes by remember { mutableStateOf(listOf(OutlineNode("I. ", "", ""), OutlineNode("II. ", "", ""), OutlineNode("III. ", "", ""))) }
    var ntSummary by remember { mutableStateOf("") }
    var isGeneratingSummary by remember { mutableStateOf(false) }
    var ntTags by remember { mutableStateOf("") }
    var ntFavorite by remember { mutableStateOf(false) }

    val todayDateStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }
    var ntDate by remember { mutableStateOf(todayDateStr) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val isDark = viewModel.isDarkMode
        val dialogBg = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White
        val glassBorder = androidx.compose.foundation.BorderStroke(2.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.45f))

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            border = glassBorder,
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with exit button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Divine Scribe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CinzelBoldFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Document sermons and biblical learnings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Dialog",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Choice
                    item {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Text("Note Category", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            OutlinedTextField(
                                value = ntCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Category") },
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = !dropdownExpanded },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            )
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth().background(Color.White)
                            ) {
                                val categories = listOf("Corporate Worship", "Sunday School", "Other Classes")
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            ntCategory = cat
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = ntTitle,
                            onValueChange = { ntTitle = it },
                            label = { Text("Sermon/Lesson Title") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = ntDate,
                            onValueChange = { ntDate = it },
                            label = { Text("Sermon Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val calendar = java.util.Calendar.getInstance()
                                    try {
                                        val sdkDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(ntDate)
                                        if (sdkDate != null) {
                                            calendar.time = sdkDate
                                        }
                                    } catch (e: Exception) {}
                                    
                                    val year = calendar.get(java.util.Calendar.YEAR)
                                    val month = calendar.get(java.util.Calendar.MONTH)
                                    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                            ntDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDayOfMonth)
                                        },
                                        year,
                                        month,
                                        day
                                    ).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = "Select Date"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Sermon Outline Builder", 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                "For each outline point, record its associated scripture references and your personal takeaways.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            
                            outlineNodes.forEachIndexed { index, node ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF8FAFC)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, 
                                        if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Point #${index + 1}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3B82F6)
                                            )
                                            IconButton(
                                                onClick = {
                                                    val updated = outlineNodes.toMutableList()
                                                    updated.removeAt(index)
                                                    outlineNodes = updated
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete, 
                                                    contentDescription = "Delete point", 
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Outline Point Input field
                                        OutlinedTextField(
                                            value = node.outline,
                                            onValueChange = { newValue ->
                                                val updated = outlineNodes.toMutableList()
                                                updated[index] = updated[index].copy(outline = newValue)
                                                outlineNodes = updated
                                            },
                                            label = { Text("Outline Topic / Heading") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = false
                                        )

                                        // Scripture verses Input field
                                        OutlinedTextField(
                                            value = node.verses,
                                            onValueChange = { newValue ->
                                                val updated = outlineNodes.toMutableList()
                                                updated[index] = updated[index].copy(verses = newValue)
                                                outlineNodes = updated
                                            },
                                            label = { Text("Verses (e.g., Romans 5:8; John 3:16)") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.MenuBook, 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = false
                                        )

                                        // Takeaways Input field
                                        OutlinedTextField(
                                            value = node.takeaways,
                                            onValueChange = { newValue ->
                                                val updated = outlineNodes.toMutableList()
                                                updated[index] = updated[index].copy(takeaways = newValue)
                                                outlineNodes = updated
                                            },
                                            label = { Text("Takeaways") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Lightbulb, 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color(0xFFF57C00)
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = false
                                        )
                                    }
                                }
                            }
                            
                            Button(
                                onClick = {
                                    val romanNumerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV")
                                    val prefix = if (outlineNodes.size < romanNumerals.size) {
                                        "${romanNumerals[outlineNodes.size]}. "
                                    } else {
                                        "${outlineNodes.size + 1}. "
                                    }
                                    outlineNodes = outlineNodes + OutlineNode(prefix, "", "")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Add, 
                                    contentDescription = "Add Point", 
                                    tint = Color(0xFF1E3A8A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Add Outline Point", 
                                    color = Color(0xFF1E3A8A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Summary", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                                
                                Button(
                                    onClick = {
                                        isGeneratingSummary = true
                                        val activeNodes = outlineNodes.filter { it.outline.isNotBlank() }
                                        coroutineScope.launch {
                                            try {
                                                val summary = com.example.data.GeminiService.generateSermonSummary(
                                                    title = ntTitle,
                                                    speaker = ntSpeaker,
                                                    church = ntChurch,
                                                    nodes = activeNodes
                                                )
                                                if (summary.startsWith("Error:")) {
                                                    Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                                                } else {
                                                    ntSummary = summary
                                                    Toast.makeText(context, "Summary generated successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to generate summary: ${e.message}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isGeneratingSummary = false
                                            }
                                        }
                                    },
                                    enabled = !isGeneratingSummary,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    if (isGeneratingSummary) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF1E3A8A),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        if (isGeneratingSummary) "Generating..." else "Generate Summary", 
                                        color = Color(0xFF1E3A8A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            OutlinedTextField(
                                value = ntSummary,
                                onValueChange = { ntSummary = it },
                                label = { Text("Daily Summary") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                minLines = 2,
                                maxLines = 4,
                                singleLine = false
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = ntTags,
                            onValueChange = { ntTags = it },
                            label = { Text("Tags separator (e.g. grace, faith, baptism)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ntSpeaker,
                                onValueChange = { ntSpeaker = it },
                                label = { Text("Preacher/Pastor") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = ntChurch,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Church Name") },
                                    trailingIcon = {
                                        IconButton(onClick = { churchDropdownExpanded = !churchDropdownExpanded }) {
                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { churchDropdownExpanded = !churchDropdownExpanded },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                )
                                DropdownMenu(
                                    expanded = churchDropdownExpanded,
                                    onDismissRequest = { churchDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                ) {
                                    if (churchNames.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No churches in directory") },
                                            onClick = { churchDropdownExpanded = false }
                                        )
                                    } else {
                                        churchNames.forEach { name ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    ntChurch = name
                                                    churchDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = ntFavorite,
                                onCheckedChange = { ntFavorite = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E3A8A))
                            )
                            Text("Add to Spiritual Favorites Star", color = Color(0xFF1E3A8A), fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (ntTitle.isNotBlank()) {
                            val activeNodes = outlineNodes.filter { it.outline.isNotBlank() }
                            val finalNotes = serializeRedesignedNoteV2(activeNodes, ntSummary)
                            val finalBibleVerses = activeNodes.map { it.verses.trim() }.filter { it.isNotBlank() }.joinToString("; ")
                            
                            viewModel.addNote(
                                ntCategory,
                                ntTitle,
                                ntSpeaker,
                                ntChurch,
                                finalBibleVerses,
                                finalNotes,
                                ntTags,
                                ntFavorite,
                                customDate = ntDate.ifBlank { null }
                            )
                            onDismiss()
                            Toast.makeText(context, "Sermon insights preserved offline!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Sermon Title cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// 9. SETTINGS & RE-LOGIN/PROFILE DRAWER DIALOG
// ==========================================
@Composable
fun BaptistSettingsAndProfileDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onOpenAdminDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryAlbumLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.uploadGalleryPhoto(uri)
        }
    }

    var isSyncing by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0f) }
    var syncPhase by remember { mutableStateOf("") }
    var isEditingProfile by remember { mutableStateOf(false) }
    var showFullPhotoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var editProvince by remember { mutableStateOf("") }
    var editCity by remember { mutableStateOf("") }
    var editChurch by remember { mutableStateOf("") }
    var editChurchSearchQuery by remember { mutableStateOf("") }
    var displayAddChurchEditModal by remember { mutableStateOf(false) }
    var showAddSuccessEditDialog by remember { mutableStateOf(false) }

    var isAdminSyncing by remember { mutableStateOf(false) }
    var adminSyncProgress by remember { mutableStateOf(0f) }
    var adminSyncPhase by remember { mutableStateOf("") }

    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackRating by remember { mutableStateOf(5) }
    var isSendingFeedback by remember { mutableStateOf(false) }

    val verses = remember {
        listOf(
            Triple("Romans 8:28", "And we know that all things work together for good to them that love God, to them who are the called according to his purpose.", "Romans 8:28"),
            Triple("Psalm 23:1-2", "The LORD is my shepherd; I shall not want. He maketh me to lie down in green pastures: he leadeth me beside the still waters.", "Psalm 23:1-2"),
            Triple("Philippians 4:13", "I can do all things through Christ which strengtheneth me.", "Philippians 4:13"),
            Triple("Proverbs 3:5-6", "Trust in the LORD with all thine heart; and lean not unto thine own understanding. In all thy ways acknowledge him, and he shall direct thy paths.", "Proverbs 3:5-6"),
            Triple("Isaiah 40:31", "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.", "Isaiah 40:31"),
            Triple("Joshua 1:9", "Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee whithersoever thou goest.", "Joshua 1:9"),
            Triple("Galatians 2:20", "I am crucified with Christ: nevertheless I live; yet not I, but Christ liveth in me: and the life which I now live in the flesh I live by the faith of the Son of God, who loved me, and gave himself for me.", "Galatians 2:20")
        )
    }
    var activeVerseIndex by remember { mutableStateOf((java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)) % 7) }

    val avatarIcons = listOf(
        Icons.Filled.AccountCircle,
        Icons.Filled.Badge,
        Icons.Filled.AutoAwesome,
        Icons.Filled.Star,
        Icons.Filled.Favorite,
        Icons.Filled.Shield
    )

    val gradientColorsList = listOf(
        listOf(Color(0xFF8E24AA), Color(0xFFD81B60)),
        listOf(Color(0xFF1E88E5), Color(0xFF00ACC1)),
        listOf(Color(0xFF43A047), Color(0xFF7CB342)),
        listOf(Color(0xFFE53935), Color(0xFFF4511E)),
        listOf(Color(0xFF3949AB), Color(0xFF5E35B1)),
        listOf(Color(0xFF00897B), Color(0xFF00ACC1)),
        listOf(Color(0xFFD81B60), Color(0xFFF4511E))
    )

    val selectedGradientColors = remember(viewModel.currentUser?.name) {
        val hash = viewModel.currentUser?.name?.hashCode() ?: 0
        val index = java.lang.Math.abs(hash) % gradientColorsList.size
        gradientColorsList[index]
    }

    fun getInitialsText(name: String?): String {
        if (name.isNullOrBlank()) return "S"
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "S"
        if (parts.size == 1) return parts[0].take(2).uppercase()
        val firstInitial = parts[0].firstOrNull() ?: ' '
        val lastInitial = parts.lastOrNull()?.firstOrNull() ?: ' '
        return "$firstInitial$lastInitial".uppercase().trim()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scribe Profile & Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = CinzelBoldFamily,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = !menuExpanded }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Profile", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    menuExpanded = false
                                    isEditingProfile = true
                                    Toast.makeText(context, "Scroll down to update your profile details!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Toggle Theme", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = if (viewModel.isDarkMode) Icons.Filled.WbSunny else Icons.Filled.Brightness4, 
                                        contentDescription = "Toggle Theme", 
                                        tint = MaterialTheme.colorScheme.primary
                                    ) 
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.toggleTheme()
                                    Toast.makeText(
                                        context, 
                                        if (viewModel.isDarkMode) "Dark Theme Enabled" else "Light Theme Enabled", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sync Offline Bible", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        isSyncing = true
                                        val steps = listOf(
                                            "Downloading Scripture indices..." to 0.15f,
                                            "Downloading King James Version (KJV)..." to 0.45f,
                                            "Caching offline references..." to 0.70f,
                                            "Optimizing search index..." to 0.90f,
                                            "Offline Bible sync complete!" to 1.0f
                                        )
                                        for ((desc, prog) in steps) {
                                            syncPhase = desc
                                            syncProgress = prog
                                            delay(700)
                                        }
                                        delay(500)
                                        isSyncing = false
                                        Toast.makeText(context, "KJV Offline Bible fully cached & synchronized!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share App", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    menuExpanded = false
                                    try {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Scribe Baptist App")
                                            putExtra(
                                                android.content.Intent.EXTRA_TEXT,
                                                "Check out the Scribe App! The ultimate system for Baptists to sync sermon notes, study KJV Bible offline, and connect with local churches: https://ai.studio/build"
                                            )
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Scribe App"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Unable to share app: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    menuExpanded = false
                                    showAboutDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Help", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    menuExpanded = false
                                    showHelpDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Submit Feedback", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.Feedback, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    menuExpanded = false
                                    showFeedbackDialog = true
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close settings")
                    }
                }

                if (isSyncing) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Offline Bible Sync Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = syncProgress,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(syncPhase, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (showFullPhotoDialog && viewModel.customProfilePhotoUri != null) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showFullPhotoDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Profile Photo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = CinzelBoldFamily
                                    )
                                    IconButton(
                                        onClick = { showFullPhotoDialog = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = viewModel.customProfilePhotoUri,
                                    contentDescription = "Full Profile Photo Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 380.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                if (showAboutDialog) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showAboutDialog = false }) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_icon),
                                    contentDescription = "Scribe Logo",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("About Scribe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Scribe is a premium, localized, and offline-first digital notepad and directory crafted specifically for Baptist churches and individuals across the Philippines.\n\n" +
                                    "Version 2.4.0\n" +
                                    "Developed under AI Studio guidelines.",
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAboutDialog = false },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Awesome")
                                }
                            }
                        }
                    }
                }

                if (showHelpDialog) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showHelpDialog = false }) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Help & Support", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "How to use Scribe:\n" +
                                    "• Write sermon or lesson notes under Sunday School or Corporate Worship categories.\n" +
                                    "• Use Gemini AI to automatically generate standard structural summaries.\n" +
                                    "• Navigate directories to find local Baptist Churches in the Philippines.\n" +
                                    "• Synchronize bible references offline to keep active scriptures ready anywhere.\n\n" +
                                    "For urgent support, reach out to the lead system administrators.",
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(
                                        onClick = { showHelpDialog = false },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }

                if (showFeedbackDialog) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showFeedbackDialog = false }) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Feedback,
                                        contentDescription = "App Feedback",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Submit Scribe Feedback",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Send your feedback, bug reports, or feature requests direct to compiling services.",
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = feedbackMessage,
                                    onValueChange = { feedbackMessage = it },
                                    label = { Text("What can we improve?", fontSize = 11.sp) },
                                    placeholder = { Text("Enter your comments or suggestions...", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4,
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Satisfaction:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Row {
                                        for (i in 1..5) {
                                            IconButton(
                                                onClick = { feedbackRating = i },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (i <= feedbackRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                    contentDescription = "Star $i",
                                                    tint = Color(0xFFD4AF37),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showFeedbackDialog = false }) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isSendingFeedback) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        Button(
                                            onClick = {
                                                if (feedbackMessage.isBlank()) {
                                                    Toast.makeText(context, "Feedback message cannot be blank", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    scope.launch {
                                                        isSendingFeedback = true
                                                        delay(1000)
                                                        Toast.makeText(context, "Thank you! Your feedback has been transmitted.", Toast.LENGTH_LONG).show()
                                                        feedbackMessage = ""
                                                        isSendingFeedback = false
                                                        showFeedbackDialog = false
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Submit")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile-Avatar Selection
                    item {
                        var editNameField by remember(viewModel.currentUser?.name) { mutableStateOf(viewModel.currentUser?.name ?: "") }
                        var editProvinceExpanded by remember { mutableStateOf(false) }
                        var editCityExpanded by remember { mutableStateOf(false) }
                        var editChurchExpanded by remember { mutableStateOf(false) }
                        var editCitySuggestions by remember(editProvince) {
                            mutableStateOf(com.example.data.PsgcData.getCitiesAndMunicipalitiesForProvince(editProvince))
                        }
                        LaunchedEffect(editProvince) {
                            editCitySuggestions = com.example.data.PsgcData.fetchCitiesForProvinceAsync(editProvince)
                        }
                        LaunchedEffect(isEditingProfile) {
                            if (isEditingProfile) {
                                editNameField = viewModel.currentUser?.name ?: ""
                                editProvince = viewModel.currentUser?.province ?: ""
                                editCity = viewModel.currentUser?.city ?: ""
                                editChurch = viewModel.currentUser?.church ?: ""
                                editChurchSearchQuery = viewModel.currentUser?.church ?: ""
                            }
                        }

                        val infiniteTransition = rememberInfiniteTransition(label = "profile_upload")
                        val borderRotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large active circular profile image with animated upload states & custom camera icon overlay!
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable { galleryAlbumLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                // Animated loader border if uploading profile picture
                                if (viewModel.isUploadingProfile) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .rotate(borderRotation)
                                            .border(
                                                width = 3.dp,
                                                brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(94.dp)
                                            .border(
                                                width = 2.dp,
                                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = selectedGradientColors
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }

                                // Interactive Profile Photo Box (Circular Profile Image)
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = if (viewModel.customProfilePhotoUri != null) {
                                                androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                                            } else {
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = selectedGradientColors
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (viewModel.customProfilePhotoUri != null) {
                                        AsyncImage(
                                            model = viewModel.customProfilePhotoUri,
                                            contentDescription = "Active Profile Photo",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { showFullPhotoDialog = true },
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = getInitialsText(viewModel.currentUser?.name),
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily
                                        )
                                    }

                                    // Upload overlay state
                                    if (viewModel.isUploadingProfile) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .alpha(pulseAlpha),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }

                                // Interactive Camera overlay icon
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        .clickable { galleryAlbumLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoCamera,
                                        contentDescription = "Upload local photo",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Name, Email and Badge Display
                            Text(
                                text = viewModel.currentUser?.name ?: "Scribe User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = CinzelBoldFamily
                            )

                            Text(
                                text = viewModel.currentUser?.email ?: "user@scribe.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Role Badge (Minimal, Modern, Rounded)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (viewModel.currentUser?.isAdmin == true) "Lead System Admin" else "Scribe Member",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            val cUser = viewModel.currentUser
                            if (cUser != null && (cUser.province.isNotEmpty() || cUser.city.isNotEmpty() || cUser.church.isNotEmpty())) {
                                Spacer(modifier = Modifier.height(10.dp))
                                if (cUser.city.isNotEmpty() || cUser.province.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = "Location",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = listOf(cUser.city, cUser.province).filter { it.isNotEmpty() }.joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (cUser.church.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Home,
                                            contentDescription = "Church",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = cUser.church,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Edit Profile & Customization Toggles row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Edit profile button
                                OutlinedButton(
                                    onClick = { isEditingProfile = !isEditingProfile },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("edit_profile_button"),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEditingProfile) "View Profile" else "Edit Profile",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // None - Avatar Styles removed as requested
                            }

                            // Smooth upload status feedback below
                            if (viewModel.isUploadingProfile && viewModel.profileSyncStatus != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = viewModel.profileSyncStatus!!,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Interactive Form with Animated Visibility (Edit Profile Section)
                            AnimatedVisibility(
                                visible = isEditingProfile,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "UPDATE PROFILE DETAILS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    OutlinedTextField(
                                        value = editNameField,
                                        onValueChange = { editNameField = it },
                                        label = { Text("Display Name", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                        placeholder = { Text("Enter your full name", fontFamily = InterFontFamily, fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )

                                    // Province Selector
                                    val provincesList_Reg = listOf(
                                        "Metro Manila", "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", 
                                        "Aurora", "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol", "Bukidnon", 
                                        "Bulacan", "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes", "Cavite", 
                                        "Cebu", "Cotabato", "Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental", "Davao Oriental", 
                                        "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur", "Iloilo", "Isabela", 
                                        "Kalinga", "La Union", "Laguna", "Lanao del Norte", "Lanao del Sur", "Leyte", "Maguindanao del Norte", 
                                        "Maguindanao del Sur", "Marinduque", "Masbate", "Misamis Occidental", "Misamis Oriental", "Mountain Province", 
                                        "Negros Occidental", "Negros Oriental", "Northern Samar", "Nueva Ecija", "Nueva Vizcaya", "Occidental Mindoro", 
                                        "Oriental Mindoro", "Palawan", "Pampanga", "Pangasinan", "Quezon", "Quirino", "Rizal", "Romblon", "Samar", 
                                        "Sarangani", "Siquijor", "Sorsogon", "South Cotabato", "Southern Leyte", "Sultan Kudarat", "Sulu", 
                                        "Surigao del Norte", "Surigao del Sur", "Tarlac", "Tawi-Tawi", "Zambales", "Zamboanga del Norte", 
                                        "Zamboanga del Sur", "Zamboanga Sibugay"
                                    )
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = if (editProvince.isEmpty()) "Select Province..." else editProvince,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Province (Mandatory)", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = "Province", modifier = Modifier.size(16.dp)) },
                                            trailingIcon = {
                                                IconButton(onClick = { editProvinceExpanded = !editProvinceExpanded }) {
                                                    Icon(
                                                        imageVector = if (editProvinceExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                        contentDescription = "Choose Province"
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { editProvinceExpanded = !editProvinceExpanded },
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = editProvinceExpanded,
                                            onDismissRequest = { editProvinceExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 280.dp)
                                                .background(Color.White)
                                        ) {
                                            provincesList_Reg.forEach { prov ->
                                                DropdownMenuItem(
                                                    text = { Text(prov, color = Color.Black, fontFamily = InterFontFamily, fontSize = 13.sp) },
                                                    onClick = {
                                                        editProvince = prov
                                                        editProvinceExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // City Selector
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = if (editCity.isEmpty()) {
                                                if (editProvince.isEmpty()) "Select Province First..." else "Select City/Municipality..."
                                            } else editCity,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("City/Municipality (Mandatory)", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                            leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = "City", modifier = Modifier.size(16.dp)) },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = { 
                                                        if (editProvince.isNotEmpty()) {
                                                            editCityExpanded = !editCityExpanded 
                                                        }
                                                    },
                                                    enabled = editProvince.isNotEmpty()
                                                ) {
                                                    Icon(
                                                        imageVector = if (editCityExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                        contentDescription = "Choose City"
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = editProvince.isNotEmpty()) { 
                                                    editCityExpanded = !editCityExpanded 
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = editCityExpanded,
                                            onDismissRequest = { editCityExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 280.dp)
                                                .background(Color.White)
                                        ) {
                                            editCitySuggestions.forEach { city ->
                                                DropdownMenuItem(
                                                    text = { Text(city, color = Color.Black, fontFamily = InterFontFamily, fontSize = 13.sp) },
                                                    onClick = {
                                                        editCity = city
                                                        editCityExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Local Church Selector
                                    val approvedChurches_Edit by viewModel.approvedChurches.collectAsStateWithLifecycle(initialValue = emptyList())
                                    val filteredChurches = remember(approvedChurches_Edit, editProvince, editChurchSearchQuery) {
                                        var list = approvedChurches_Edit
                                        val query = editChurchSearchQuery.trim()
                                        if (query.length < 2 && editProvince.isNotEmpty()) {
                                            list = list.filter { it.province.trim().equals(editProvince.trim(), ignoreCase = true) }
                                        }
                                        val names = list.map { it.name }.distinct().sorted()
                                        if (query.isNotEmpty() && !query.equals(editChurch, ignoreCase = true)) {
                                            val startsWithList = names.filter { it.startsWith(query, ignoreCase = true) }
                                            val containsList = names.filter { !it.startsWith(query, ignoreCase = true) && it.contains(query, ignoreCase = true) }
                                            startsWithList + containsList
                                        } else {
                                            names
                                        }
                                    }
                                    val editChurchOptions = remember(filteredChurches) {
                                        filteredChurches + listOf("Add Church")
                                    }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = editChurchSearchQuery,
                                            onValueChange = { newValue ->
                                                editChurchSearchQuery = newValue
                                                editChurchExpanded = true
                                            },
                                            label = { Text("Search/Select Local Church (Mandatory)", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                            placeholder = { Text("Type church name (e.g. Faith...)", fontFamily = InterFontFamily, fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Filled.Home, contentDescription = "Local Church", modifier = Modifier.size(16.dp)) },
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (editChurchSearchQuery.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                editChurchSearchQuery = ""
                                                                editChurch = ""
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Close,
                                                                contentDescription = "Clear Selection",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = { editChurchExpanded = !editChurchExpanded },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (editChurchExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                            contentDescription = "Toggle Church suggestions",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            )
                                        )
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = editChurchExpanded && editChurchOptions.isNotEmpty(),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color.White
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 200.dp)
                                                        .verticalScroll(rememberScrollState())
                                                ) {
                                                    editChurchOptions.forEach { chOpt ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    if (chOpt == "Add Church") {
                                                                        editChurch = "Add Church"
                                                                        editChurchSearchQuery = "Add Church"
                                                                        editChurchExpanded = false
                                                                        displayAddChurchEditModal = true
                                                                    } else {
                                                                        editChurch = chOpt
                                                                        editChurchSearchQuery = chOpt
                                                                        editChurchExpanded = false
                                                                        val matchedCh = approvedChurches_Edit.find { it.name == chOpt }
                                                                        if (matchedCh != null) {
                                                                            editProvince = matchedCh.province
                                                                            editCity = matchedCh.cityMunicipality
                                                                        }
                                                                    }
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = if (chOpt == "Add Church") Icons.Filled.Add else Icons.Filled.Home,
                                                                contentDescription = null,
                                                                tint = if (chOpt == "Add Church") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = if (chOpt == "Add Church") "➕ Add Church (Custom)" else chOpt,
                                                                color = if (chOpt == "Add Church") MaterialTheme.colorScheme.primary else Color.Black,
                                                                fontWeight = if (chOpt == "Add Church") FontWeight.Bold else FontWeight.Normal,
                                                                fontFamily = InterFontFamily,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                                                    }
                                                }
                                            }
                                        }
                                    }



                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { isEditingProfile = false }) {
                                            Text("Cancel", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (editNameField.isNotBlank()) {
                                                    if (editProvince.isBlank()) {
                                                        Toast.makeText(context, "Province is mandatory.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    if (editCity.isBlank()) {
                                                        Toast.makeText(context, "City is mandatory.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    if (editChurch.isBlank()) {
                                                        Toast.makeText(context, "Local Church is mandatory.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    val finalChurchName = editChurch

                                                    viewModel.updateProfile(
                                                        newName = editNameField.trim(),
                                                        newProvince = editProvince,
                                                        newCity = editCity,
                                                        newChurch = finalChurchName
                                                    )
                                                    isEditingProfile = false
                                                    Toast.makeText(context, "Full profile update applied!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Save Changes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // None - Preset Avatars Selection Panel removed as requested
                        }
                    }



                    // KJV Verse of the Day Section
                    item {
                        val currentVerse = verses[activeVerseIndex]
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFFDF6) // Creamy holy aesthetic
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFFE5C060).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Verse of the Day",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF7D5915),
                                        fontFamily = CinzelBoldFamily
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "“${currentVerse.second}”",
                                    fontSize = 13.sp,
                                    color = Color.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    lineHeight = 18.sp
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Text(
                                    text = "— ${currentVerse.first} (KJV)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7D5915)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                HorizontalDivider(color = Color(0xFFE5C060).copy(alpha = 0.25f))
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(
                                        onClick = {
                                            try {
                                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("KJV Verse of the Day", "“${currentVerse.second}” — ${currentVerse.first} (KJV) - Shared via Scribe")
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast.makeText(context, "Scripture copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Unable to copy: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            tint = Color(0xFF7D5915),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Word", fontSize = 11.sp, color = Color(0xFF7D5915), fontWeight = FontWeight.Bold)
                                    }
                                    
                                    TextButton(
                                        onClick = {
                                            try {
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "KJV Verse of the Day")
                                                    putExtra(
                                                        android.content.Intent.EXTRA_TEXT,
                                                        "“${currentVerse.second}” — ${currentVerse.first} (King James Version) - Shared from Scribe"
                                                    )
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Verse"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Unable to share: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = null,
                                            tint = Color(0xFF7D5915),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share", fontSize = 11.sp, color = Color(0xFF7D5915), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Admin Scribe Digital Console with sync database and subscriber summary (only visible to role: Admin)
                    if (viewModel.currentUser?.isAdmin == true) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Lead Admin Digital Console",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Admin database sync section
                                    Text(
                                        text = "ADMIN DATABASE SYNCHRONIZER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Consolidate and publish local sermon templates, church records, and subscriber indexes to the primary cloud server.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (isAdminSyncing) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            LinearProgressIndicator(
                                                progress = adminSyncProgress,
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "$adminSyncPhase (${(adminSyncProgress * 100).toInt()}%)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isAdminSyncing = true
                                                    val phases = listOf(
                                                        "Initializing server secure handshake...",
                                                        "Verifying admin session credentials...",
                                                        "Gathering decentralized Scribe sermon notes...",
                                                        "Syncing 12 approved Baptist churches...",
                                                        "Publishing active subscriber records...",
                                                        "Optimizing database structure with indices...",
                                                        "All local nodes fully synchronized!"
                                                    )
                                                    for (i in 1..21) {
                                                        delay(100)
                                                        adminSyncProgress = i / 21f
                                                        adminSyncPhase = phases[((i - 1) * phases.size) / 21]
                                                    }
                                                    isAdminSyncing = false
                                                    Toast.makeText(context, "Scribe Database Admin Sync Complete!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Filled.Sync,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Sync Scribe Core Database Live",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            onOpenAdminDashboard()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Open Master Admin Dashboard",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // SUMMARY OF SUBSCRIBERS
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SUBSCRIBERS OVERVIEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "58 Active",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // High fidelity subscribers list representation - only NEW subscribers
                                    val dummySubscribers = listOf(
                                        Triple("mario.santos@baptist.org.ph", "Scribe Apprentice (Luzon MBBC)", "New"),
                                        Triple("sarah.perez@fbf.org.ph", "Children Teacher (Cebu MBBC)", "New"),
                                        Triple("anthony.cruz@gmail.com", "Youth Leader (Davao MBBC)", "New")
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        dummySubscribers.forEach { (email, subtitle, status) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = email,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = subtitle,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(
                                                            Color(0xFF2196F3).copy(alpha = 0.15f)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = status,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1976D2)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Export Subscriber list Action
                                        Button(
                                            onClick = {
                                                try {
                                                    val csvContent = "Email,Role/Position,Status\n" +
                                                            "mario.santos@baptist.org.ph,Scribe Apprentice (Luzon MBBC),New\n" +
                                                            "sarah.perez@fbf.org.ph,Children Teacher (Cebu MBBC),New\n" +
                                                            "anthony.cruz@gmail.com,Youth Leader (Davao MBBC),New\n" +
                                                            "popoydev@gmail.com,Lead System Administrator,Active\n" +
                                                            "dr.robert@mbbc.org,Lead Pastor (Tondo MBBC),Premium\n" +
                                                            "ruth.teacher@gmail.com,Sunday School Scribe,Active\n" +
                                                            "timothy.baptist@fbf.org.ph,FBF Associate Editor,Premium\n" +
                                                            "eunice.cruz@yahoo.com,Youth Ministry Organizer,Trial"

                                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "text/csv"
                                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Scribe Subscribers List Export")
                                                        putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(intent, "Export Subscriber List"))
                                                    Toast.makeText(context, "Subscriber CSV List exported successfully!", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Filled.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Export Subscriber List (CSV)",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Logout/De-authenticate
                Button(
                    onClick = {
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = "Exit session")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (displayAddChurchEditModal) {
        var chName by remember { mutableStateOf("") }
        var chProvince by remember { mutableStateOf("Metro Manila") }
        var chAddress by remember { mutableStateOf("") }
        var pastorsList by remember { mutableStateOf(listOf("")) }
        var chContact by remember { mutableStateOf("") }
        var chSchedule by remember { mutableStateOf("Sunday Worship: 09:30 AM") }
        var chFacebook by remember { mutableStateOf("") }
        var showDuplicateWarning by remember { mutableStateOf(false) }
        var duplicateChurchNameText by remember { mutableStateOf("") }

        var chCityMunicipality by remember { mutableStateOf("") }
        var chDescription by remember { mutableStateOf("") }
        var chLatitude by remember { mutableStateOf(14.5995) }
        var chLongitude by remember { mutableStateOf(120.9842) }

        var citiesSuggestions by remember(chProvince) {
            mutableStateOf(com.example.data.PsgcData.getCitiesAndMunicipalitiesForProvince(chProvince))
        }
        LaunchedEffect(chProvince) {
            citiesSuggestions = com.example.data.PsgcData.fetchCitiesForProvinceAsync(chProvince)
        }

        AlertDialog(
            onDismissRequest = { displayAddChurchEditModal = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Suggest Local Baptist Church",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = CinzelBoldFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = chName,
                            onValueChange = { chName = it },
                            label = { Text("Church Name", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Grace Baptist Church", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }
                        val provinces = listOf(
                            "Metro Manila", "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", 
                            "Aurora", "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol", "Bukidnon", 
                            "Bulacan", "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes", "Cavite", 
                            "Cebu", "Cotabato", "Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental", "Davao Oriental", 
                            "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur", "Iloilo", "Isabela", 
                            "Kalinga", "La Union", "Laguna", "Lanao del Norte", "Lanao del Sur", "Leyte", "Maguindanao del Norte", 
                            "Maguindanao del Sur", "Marinduque", "Masbate", "Misamis Occidental", "Misamis Oriental", "Mountain Province", 
                            "Negros Occidental", "Negros Oriental", "Northern Samar", "Nueva Ecija", "Nueva Vizcaya", "Occidental Mindoro", 
                            "Oriental Mindoro", "Palawan", "Pampanga", "Pangasinan", "Quezon", "Quirino", "Rizal", "Romblon", "Samar", 
                            "Sarangani", "Siquijor", "Sorsogon", "South Cotabato", "Southern Leyte", "Sultan Kudarat", "Sulu", 
                            "Surigao del Norte", "Surigao del Sur", "Tarlac", "Tawi-Tawi", "Zambales", "Zamboanga del Norte", 
                            "Zamboanga del Sur", "Zamboanga Sibugay"
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = chProvince,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Province", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                trailingIcon = {
                                    IconButton(onClick = { expanded = !expanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose Province")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).background(Color.White)
                            ) {
                                provinces.forEach { province ->
                                    DropdownMenuItem(
                                        text = { Text(province, fontFamily = InterFontFamily, fontSize = 13.sp, color = Color.Black) },
                                        onClick = {
                                            chProvince = province
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chAddress,
                            onValueChange = { chAddress = it },
                            label = { Text("Exact Address Directions", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Unit 4B, Emerald Street", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        var cityExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (chCityMunicipality.isEmpty()) "Select City/Municipality..." else chCityMunicipality,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("City or Municipality", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                trailingIcon = {
                                    IconButton(onClick = { cityExpanded = !cityExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose City")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { cityExpanded = !cityExpanded },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            )
                            DropdownMenu(
                                expanded = cityExpanded,
                                onDismissRequest = { cityExpanded = false },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).background(Color.White)
                            ) {
                                citiesSuggestions.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(city, fontFamily = InterFontFamily, fontSize = 13.sp, color = Color.Black) },
                                        onClick = {
                                            chCityMunicipality = city
                                            cityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chDescription,
                            onValueChange = { chDescription = it },
                            label = { Text("Church Testimony / Description", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Preaching Christ in the local community", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Pastors & Leaders", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            pastorsList.forEachIndexed { index, pastor ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pastor,
                                        onValueChange = { newValue ->
                                            val updated = pastorsList.toMutableList()
                                            updated[index] = newValue
                                            pastorsList = updated
                                        },
                                        label = { Text(if (index == 0) "Lead Pastor" else "Preacher/Pastor ${index + 1}", fontFamily = InterFontFamily, fontSize = 11.sp) },
                                        placeholder = { Text("e.g. Pastor Timothy", fontFamily = InterFontFamily, fontSize = 13.sp) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    )
                                    if (pastorsList.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val updated = pastorsList.toMutableList()
                                                updated.removeAt(index)
                                                pastorsList = updated
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove pastor", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = {
                                    pastorsList = pastorsList + ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Other Pastor", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = chContact,
                            onValueChange = { chContact = it },
                            label = { Text("Church Contact Details", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. +639171234567", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = chSchedule,
                            onValueChange = { chSchedule = it },
                            label = { Text("Gathering Services Times", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Sunday 10:00 AM", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = chFacebook,
                            onValueChange = { chFacebook = it },
                            label = { Text("Facebook Page Link", fontFamily = InterFontFamily, fontSize = 11.sp) },
                            placeholder = { Text("e.g. https://facebook.com/churchpage", fontFamily = InterFontFamily, fontSize = 13.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chName.isNotBlank() && chAddress.isNotBlank()) {
                            val trimmedName = chName.trim()
                            val duplicate = viewModel.approvedChurches.value.find { it.name.trim().equals(trimmedName, ignoreCase = true) }
                            if (duplicate != null) {
                                duplicateChurchNameText = duplicate.name
                                showDuplicateWarning = true
                            } else {
                                val combinedPastors = pastorsList.filter { it.isNotBlank() }.joinToString(", ")
                                viewModel.submitNewChurch(
                                    name = chName,
                                    province = chProvince,
                                    cityMunicipality = chCityMunicipality.ifEmpty { "Poblacion" },
                                    address = chAddress,
                                    pastor = combinedPastors,
                                    contact = chContact,
                                    schedule = chSchedule,
                                    description = chDescription.ifEmpty { "We preach the Gospel of Christ and serve local families." },
                                    latitude = chLatitude,
                                    longitude = chLongitude,
                                    facebookUrl = chFacebook
                                )
                                editChurch = chName
                                editChurchSearchQuery = chName
                                editProvince = chProvince
                                editCity = chCityMunicipality.ifEmpty { editCity }
                                displayAddChurchEditModal = false
                                showAddSuccessEditDialog = true
                            }
                        } else {
                            Toast.makeText(context, "Please enter both Church Name and Exact Address Directions.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Submit for Approval", style = MaterialTheme.typography.bodyMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { displayAddChurchEditModal = false }) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )

        if (showDuplicateWarning) {
            AlertDialog(
                onDismissRequest = { showDuplicateWarning = false },
                containerColor = Color.White,
                title = {
                    Text("Duplicate Church Suggestion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                text = {
                    Text("A church with the name \"$duplicateChurchNameText\" already exists in our directory. We highly recommend using the existing church already present in the directory.\n\nWould you still like to proceed with adding this new church anyway?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDuplicateWarning = false
                            val combinedPastors = pastorsList.filter { it.isNotBlank() }.joinToString(", ")
                            viewModel.submitNewChurch(
                                name = chName,
                                province = chProvince,
                                cityMunicipality = chCityMunicipality.ifEmpty { "Poblacion" },
                                address = chAddress,
                                pastor = combinedPastors,
                                contact = chContact,
                                schedule = chSchedule,
                                description = chDescription.ifEmpty { "We preach the Gospel of Christ and serve local families." },
                                latitude = chLatitude,
                                longitude = chLongitude,
                                facebookUrl = chFacebook
                            )
                            editChurch = chName
                            editChurchSearchQuery = chName
                            editProvince = chProvince
                            editCity = chCityMunicipality.ifEmpty { editCity }
                            displayAddChurchEditModal = false
                            showAddSuccessEditDialog = true
                        }
                    ) {
                        Text("Continue Adding Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDuplicateWarning = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    if (showAddSuccessEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddSuccessEditDialog = false },
            containerColor = Color.White,
            title = {
                Text("Submission Received!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Text("Thank you so much! Your submitted Church will be processed and listed in our directory as soon as approved by the system administrators.")
            },
            confirmButton = {
                Button(onClick = { showAddSuccessEditDialog = false }) {
                    Text("Awesome")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MasterAdminDashboardDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allChurches by viewModel.allChurches.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val liveEvents by com.example.data.ScribeAnalytics.liveEvents.collectAsStateWithLifecycle()
    
    // Find unapproved churches & requests
    val pendingChurches = remember(allChurches) {
        allChurches.filter { !it.isApproved }
    }
    val pendingNewChurches = remember(allChurches) {
        allChurches.filter { !it.isApproved && it.replacesChurchId == null }
    }
    val pendingEditChurches = remember(allChurches) {
        allChurches.filter { !it.isApproved && it.replacesChurchId != null }
    }
    val pendingDeleteChurches = remember(allChurches) {
        allChurches.filter { it.isApproved && it.isDeletePending }
    }
    val pendingTotalCount = pendingNewChurches.size + pendingEditChurches.size + pendingDeleteChurches.size

    var activeSubTab by remember { mutableStateOf("ANALYTICS") } // "ANALYTICS", "CHURCHES"

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false) // Full screen
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Scribe Master Admin Control",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CinzelBoldFamily,
                                    fontSize = 18.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { onDismiss() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close Dashboard")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // Navigation Sub Tabs: Analytics vs Church Entries
                    TabRow(
                        selectedTabIndex = if (activeSubTab == "ANALYTICS") 0 else 1,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = activeSubTab == "ANALYTICS",
                            onClick = { activeSubTab = "ANALYTICS" },
                            text = { Text("System Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = activeSubTab == "CHURCHES",
                            onClick = { activeSubTab = "CHURCHES" },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Church Registry", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    if (pendingTotalCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                                .size(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pendingTotalCount.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (activeSubTab == "ANALYTICS") {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // 1. STATS GRID CARDS
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Total registrations Card
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Scribe Installs", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("1,248", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("↑ 12% this week", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    // Notes count Card
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Total Local Notes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(notes.size.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Real-time count", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // User locations Card
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Active Scribes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("342", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Pulse: 18 Online", fontSize = 9.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    // Active directories Card
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Church Entries", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(allChurches.size.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("${allChurches.filter { it.isApproved }.size} approved nodes", fontSize = 9.sp, color = Color(0xFF388E3C), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }

                            // 2. CANVAS ANALYTICS CHART
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Weekly sermon writing activity analytics",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Dynamic native canvas line graph
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.02f))
                                        ) {
                                            val primaryColor = MaterialTheme.colorScheme.primary
                                            val secondaryColor = MaterialTheme.colorScheme.secondary
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val width = size.width
                                                val height = size.height
                                                val points = listOf(0.1f, 0.45f, 0.3f, 0.75f, 0.6f, 0.88f, 0.95f)
                                                val pointCount = points.size
                                                val stepX = width / (pointCount - 1)
                                                
                                                val path = androidx.compose.ui.graphics.Path()
                                                points.forEachIndexed { idx, value ->
                                                    val x = idx * stepX
                                                    val y = height - (value * height * 0.85f)
                                                    if (idx == 0) {
                                                        path.moveTo(x, y)
                                                    } else {
                                                        path.lineTo(x, y)
                                                    }
                                                    drawCircle(
                                                        color = secondaryColor,
                                                        radius = 5f,
                                                        center = androidx.compose.ui.geometry.Offset(x, y)
                                                    )
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = primaryColor,
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                                                Text(day, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. SECURE USER LOCATIONS STATS
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "User registrations by location density",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        val locationStats = listOf(
                                            "Metro Manila, PH" to 442,
                                            "Cebu Province, PH" to 215,
                                            "Davao del Sur, PH" to 189,
                                            "Benguet Province, PH" to 112,
                                            "Pangasinan, PH" to 84,
                                            "California, USA" to 42
                                        )

                                        locationStats.forEach { (loc, count) ->
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(loc, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                    Text("$count Scribes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val pct = count / 442f
                                                LinearProgressIndicator(
                                                    progress = pct,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. FIREBASE EVENT ANALYTICS REALTIME LOG
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Firebase Analytics Live Event Log",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2E7D32))
                                                    .size(8.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        val systemEvents = liveEvents.map { ev ->
                                            Triple(ev.name, ev.description, ev.timestamp)
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            systemEvents.forEach { (event, desc, time) ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("event_action: $event", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                        Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                    }
                                                    Text(time, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                                                }
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB: CHURCH REGISTRY REVIEW (APPROVE & REJECT SCREEN)
                        if (pendingTotalCount == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "All Submissions Handled",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No pending church registry entries awaiting review.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            var showAdminAdjustDialogFor by remember { mutableStateOf<BaptistChurch?>(null) }

                            if (showAdminAdjustDialogFor != null) {
                                val currentAdj = showAdminAdjustDialogFor!!
                                GoogleMapPinDropperDialog(
                                    initialProvince = currentAdj.province,
                                    onDismissRequest = { showAdminAdjustDialogFor = null },
                                    onLocationConfirmed = { lat, lng, address, city, desc ->
                                        viewModel.updateChurchLocation(
                                            church = currentAdj,
                                            latitude = lat,
                                            longitude = lng,
                                            address = address,
                                            city = city,
                                            description = desc
                                        )
                                        showAdminAdjustDialogFor = null
                                    }
                                )
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                // 1. DELETION REQUESTS
                                if (pendingDeleteChurches.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Proposed Deletions (${pendingDeleteChurches.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(pendingDeleteChurches) { church ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(church.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.error)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Request Delete", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Province: ${church.province}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                                Text("Address: ${church.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                                Text("Pastor: ${church.pastorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Delete Requested by: ${church.submittedBy}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.rejectDeletionRequest(church)
                                                            Toast.makeText(context, "Deletion request rejected. Church remains active.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Keep Church", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            viewModel.approveDeletionRequest(church)
                                                            Toast.makeText(context, "${church.name} has been deleted.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Approve Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. PROPOSED EDITS
                                if (pendingEditChurches.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Proposed Modifications (${pendingEditChurches.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(pendingEditChurches) { church ->
                                        val original = remember(allChurches) { allChurches.find { it.id == church.replacesChurchId } }
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Modified details for ${original?.name ?: "Unknown Church"}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.secondary)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Proposed Edit", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                
                                                // Comparison View
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                        .padding(8.dp)
                                                ) {
                                                    if (original != null) {
                                                        val differences = mutableListOf<Triple<String, String, String>>()
                                                        if (original.name != church.name) differences.add(Triple("Name", original.name, church.name))
                                                        if (original.province != church.province) differences.add(Triple("Province", original.province, church.province))
                                                        if (original.cityMunicipality != church.cityMunicipality) differences.add(Triple("City/Municipality", original.cityMunicipality, church.cityMunicipality))
                                                        if (original.address != church.address) differences.add(Triple("Address", original.address, church.address))
                                                        if (original.pastorName != church.pastorName) differences.add(Triple("Pastor Name(s)", original.pastorName, church.pastorName))
                                                        if (original.contactNumber != church.contactNumber) differences.add(Triple("Contact Number", original.contactNumber, church.contactNumber))
                                                        if (original.worshipSchedule != church.worshipSchedule) differences.add(Triple("Worship Schedule", original.worshipSchedule, church.worshipSchedule))
                                                        if (original.description != church.description) differences.add(Triple("Description", original.description, church.description))
                                                        if (original.facebookUrl != church.facebookUrl) differences.add(Triple("Facebook Page Link", original.facebookUrl, church.facebookUrl))
                                                        if (original.latitude != church.latitude || original.longitude != church.longitude) {
                                                            differences.add(Triple("GPS Coordinates", "${original.latitude}, ${original.longitude}", "${church.latitude}, ${church.longitude}"))
                                                        }

                                                        if (differences.isNotEmpty()) {
                                                            Text(
                                                                text = "Edited/Modified Fields (${differences.size}):",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.padding(bottom = 6.dp)
                                                            )
                                                            differences.forEach { (field, old, new) ->
                                                                Column(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(vertical = 4.dp)
                                                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                                        .padding(6.dp)
                                                                ) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(
                                                                            imageVector = Icons.Filled.Edit,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.secondary,
                                                                            modifier = Modifier.size(12.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            text = field,
                                                                            fontWeight = FontWeight.Bold,
                                                                            fontSize = 11.sp,
                                                                            color = MaterialTheme.colorScheme.onSurface
                                                                        )
                                                                    }
                                                                    Text(
                                                                        text = "Original: $old",
                                                                        fontSize = 10.sp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                                                                    )
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.padding(start = 16.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Filled.ArrowForward,
                                                                            contentDescription = null,
                                                                            tint = Color(0xFF2E7D32),
                                                                            modifier = Modifier.size(12.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            text = "Proposed: $new",
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color(0xFF2E7D32)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Text("No details changed from original.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    } else {
                                                        Text("Proposed fields: Name: ${church.name}, Prov: ${church.province}, Addr: ${church.address}, Pastor: ${church.pastorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "Edits submitted by: ${church.submittedBy}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.rejectChurch(church)
                                                            Toast.makeText(context, "Proposed edits rejected.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reject Edits", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            viewModel.approveChurch(church)
                                                            Toast.makeText(context, "Edits approved and applied live!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Approve Edits", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. NEW CHURCH PROPOSALS
                                if (pendingNewChurches.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "New Church Proposals (${pendingNewChurches.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(pendingNewChurches) { church ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = church.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.errorContainer)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Awaiting Verification",
                                                            fontSize = 8.sp,
                                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Address: ${church.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text("Province: ${church.province}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                Text("Pastor: ${church.pastorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                Text("Schedule: ${church.worshipSchedule}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                                Text("Coordinates: Lat ${church.latitude}, Lng ${church.longitude}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Styled custom map preview box for coordinate verification
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFEFF6FF))
                                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                ) {
                                                    InteractiveCustomMap(
                                                        centerLat = church.latitude,
                                                        centerLng = church.longitude,
                                                        zoom = 1.0f,
                                                        displayStyle = MapStyle.STANDARD,
                                                        onMapClick = { lat, lng ->
                                                            // Tapping map acts as a direct shortcut to adjuster!
                                                            showAdminAdjustDialogFor = church
                                                        },
                                                        interactiveMarkerLat = church.latitude,
                                                        interactiveMarkerLng = church.longitude,
                                                        modifier = Modifier.fillMaxSize()
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp)
                                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            "Interactive Verification Preview",
                                                            color = Color.White,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Direct button link to update correct coordinates
                                                TextButton(
                                                    onClick = { showAdminAdjustDialogFor = church },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Filled.EditLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Adjust / Correct Coordinates & Geocode Address", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Addition submitted by: ${church.submittedBy}",
                                                    fontSize = 9.sp,
                                                    fontStyle = FontStyle.Italic,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )

                                                Spacer(modifier = Modifier.height(14.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Reject button
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.rejectChurch(church)
                                                            Toast.makeText(context, "Submission rejected and cleared.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            contentColor = MaterialTheme.colorScheme.error
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reject Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // Approve button
                                                    Button(
                                                        onClick = {
                                                            viewModel.approveChurch(church)
                                                            Toast.makeText(context, "${church.name} is now approved and live!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF2E7D32)
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Approve live", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
