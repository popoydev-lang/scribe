package com.example.ui.state

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BaptistNotesApplication
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Auth Model
data class UserProfile(
    val email: String,
    val isAdmin: Boolean = false,
    val joinedDate: String = "May 2026",
    val name: String = "",
    val province: String = "",
    val city: String = "",
    val church: String = ""
)

fun getInitialsFromName(fullName: String): String {
    val clean = fullName.trim()
    if (clean.isEmpty()) return "U"
    val parts = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size >= 2) {
        val first = parts[0].firstOrNull()?.uppercaseChar() ?: ' '
        val last = parts[parts.size - 1].firstOrNull()?.uppercaseChar() ?: ' '
        return "$first$last".trim()
    } else if (parts.size == 1) {
        val word = parts[0]
        return if (word.length >= 2) {
            word.substring(0, 2).uppercase()
        } else {
            word.uppercase()
        }
    }
    return "U"
}

class AppViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // ==========================================
    // 1. AUTHENTICATION STATE
    // ==========================================
    var currentUser by mutableStateOf<UserProfile?>(null)
        private set

    var authErrorMessage by mutableStateOf<String?>(null)
        private set

    var isAuthenticating by mutableStateOf(false)
        private set

    var rememberMe by mutableStateOf(true)
    var isDarkMode by mutableStateOf(true)
    var profilePhotoIndex by mutableStateOf(0)
    var customProfilePhotoUri by mutableStateOf<String?>(null)
    var isUploadingProfile by mutableStateOf(false)
    var profileSyncStatus by mutableStateOf<String?>(null)
    var isBsbInstalled by mutableStateOf(false)
    var isBibleHeaderVisible by mutableStateOf(true)

    var adminEmails by mutableStateOf(setOf("popoydev@gmail.com"))
        private set

    init {
        com.example.data.ScribeAnalytics.initialize(application)
        com.example.data.ScribeAnalytics.logInstallDownload(application)
        val sharedPrefs = application.getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        isDarkMode = sharedPrefs.getBoolean("dark_theme", true)
        profilePhotoIndex = sharedPrefs.getInt("profile_photo_index", 0)
        customProfilePhotoUri = sharedPrefs.getString("custom_profile_photo_uri", null)
        isBsbInstalled = sharedPrefs.getBoolean("bsb_installed", false)
        val isRemembered = sharedPrefs.getBoolean("remember_me", false)
        rememberMe = isRemembered

        val savedAdmins = sharedPrefs.getStringSet("admin_emails", setOf("popoydev@gmail.com")) ?: setOf("popoydev@gmail.com")
        adminEmails = savedAdmins.toMutableSet().apply { add("popoydev@gmail.com") }

        val firebaseUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }

        if (firebaseUser != null && isRemembered) {
            val savedEmail = firebaseUser.email ?: ""
            val actualIsAdmin = adminEmails.contains(savedEmail.lowercase().trim())
            val savedName = sharedPrefs.getString("saved_name", "") ?: ""
            val savedProvince = sharedPrefs.getString("saved_province", "") ?: ""
            val savedCity = sharedPrefs.getString("saved_city", "") ?: ""
            val savedChurch = sharedPrefs.getString("saved_church", "") ?: ""
            currentUser = UserProfile(
                email = savedEmail,
                isAdmin = actualIsAdmin,
                name = savedName,
                province = savedProvince,
                city = savedCity,
                church = savedChurch
            )
        } else if (isRemembered && sharedPrefs.contains("saved_email")) {
            val savedEmail = sharedPrefs.getString("saved_email", "") ?: ""
            if (savedEmail.isNotEmpty()) {
                val actualIsAdmin = adminEmails.contains(savedEmail.lowercase().trim())
                val savedName = sharedPrefs.getString("saved_name", "") ?: ""
                val savedProvince = sharedPrefs.getString("saved_province", "") ?: ""
                val savedCity = sharedPrefs.getString("saved_city", "") ?: ""
                val savedChurch = sharedPrefs.getString("saved_church", "") ?: ""
                currentUser = UserProfile(
                    email = savedEmail,
                    isAdmin = actualIsAdmin,
                    name = savedName,
                    province = savedProvince,
                    city = savedCity,
                    church = savedChurch
                )
            } else {
                currentUser = null
            }
        } else {
            currentUser = null
        }
        
        // Ensure Bible database is initialized and prepopulated with key indicators immediately 
        viewModelScope.launch {
            repository.checkAndPrepopulateBibleIfNeeded()
            val savedBook = sharedPrefs.getString("last_selected_book", "John") ?: "John"
            val savedChapter = sharedPrefs.getInt("last_selected_chapter", 1)
            loadRealVersesIfNeeded(savedBook, savedChapter)

            // Watch for changes and load/cache KJV or BSB when selected
            combine(
                _selectedBookFlow,
                _selectedChapterFlow,
                snapshotFlow { selectedTranslation }
            ) { b, c, t ->
                Triple(b, c, t)
            }.collect { (b, c, t) ->
                loadRealVersesIfNeeded(b, c)
            }
        }
    }

    fun loginWithGoogleCredential(idToken: String, onComplete: (Boolean) -> Unit) {
        isAuthenticating = true
        authErrorMessage = null
        try {
            val auth = FirebaseAuth.getInstance()
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    isAuthenticating = false
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        val email = firebaseUser?.email ?: ""
                        val name = firebaseUser?.displayName ?: ""
                        val actualIsAdmin = adminEmails.contains(email.lowercase().trim())
                        
                        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
                        val sProv = sharedPrefs.getString("saved_province", "") ?: ""
                        val sCity = sharedPrefs.getString("saved_city", "") ?: ""
                        val sChurch = sharedPrefs.getString("saved_church", "") ?: ""
                        
                        currentUser = UserProfile(
                            email = email,
                            isAdmin = actualIsAdmin,
                            name = name,
                            province = sProv,
                            city = sCity,
                            church = sChurch
                        )
                        
                        com.example.data.ScribeAnalytics.logUserLogin(
                            context = getApplication(),
                            email = email,
                            role = if (actualIsAdmin) "Admin" else "User"
                        )
                        
                        val editor = sharedPrefs.edit()
                        editor.putString("saved_name", name)
                        editor.putBoolean("remember_me", rememberMe)
                        if (rememberMe) {
                            editor.putString("saved_email", email)
                            editor.putBoolean("saved_is_admin", actualIsAdmin)
                        } else {
                            editor.remove("saved_email")
                            editor.remove("saved_is_admin")
                        }
                        editor.apply()
                        
                        com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                            email = email,
                            fullName = name,
                            photoUrl = customProfilePhotoUri,
                            initials = getInitialsFromName(name),
                            context = getApplication()
                        ) { success ->
                            Log.d("AppViewModel", "Cloud Profile updated from Google sign-in: $success")
                        }
                        
                        authErrorMessage = null
                        onComplete(true)
                    } else {
                        val err = task.exception?.localizedMessage ?: "Google Sign-In failed"
                        authErrorMessage = err
                        onComplete(false)
                    }
                }
        } catch (e: Exception) {
            isAuthenticating = false
            authErrorMessage = "Google Auth requires Play Services configured: ${e.localizedMessage}"
            onComplete(false)
        }
    }

    fun loginWithGoogleSimulation(email: String, name: String, onComplete: (Boolean) -> Unit) {
        isAuthenticating = true
        authErrorMessage = null
        val actualIsAdmin = adminEmails.contains(email.lowercase().trim())
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        val sProv = sharedPrefs.getString("saved_province", "") ?: ""
        val sCity = sharedPrefs.getString("saved_city", "") ?: ""
        val sChurch = sharedPrefs.getString("saved_church", "") ?: ""
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            isAuthenticating = false
            currentUser = UserProfile(
                email = email,
                isAdmin = actualIsAdmin,
                name = name,
                province = sProv,
                city = sCity,
                church = sChurch
            )
            com.example.data.ScribeAnalytics.logUserLogin(
                context = getApplication(),
                email = email,
                role = if (actualIsAdmin) "Admin" else "User"
            )
            val editor = sharedPrefs.edit()
            editor.putString("saved_name", name)
            editor.putBoolean("remember_me", rememberMe)
            if (rememberMe) {
                editor.putString("saved_email", email)
                editor.putBoolean("saved_is_admin", actualIsAdmin)
            } else {
                editor.remove("saved_email")
                editor.remove("saved_is_admin")
            }
            editor.apply()
            
            com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                email = email,
                fullName = name,
                photoUrl = customProfilePhotoUri,
                initials = getInitialsFromName(name),
                context = getApplication()
            ) { success ->
                Log.d("AppViewModel", "Simulation Cloud Profile sync: $success")
            }
            
            authErrorMessage = null
            onComplete(true)
        }
    }

    fun login(email: String, pword: String, onComplete: (Boolean) -> Unit) {
        if (email.isEmpty() || pword.isEmpty()) {
            authErrorMessage = "Email and Password cannot be empty."
            onComplete(false)
            return
        }
        if (!email.contains("@")) {
            authErrorMessage = "Please enter a valid email address."
            onComplete(false)
            return
        }
        isAuthenticating = true
        authErrorMessage = null
        
        val actualIsAdmin = adminEmails.contains(email.lowercase().trim())
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        val savedName = sharedPrefs.getString("saved_name", "") ?: ""
        val sProv = sharedPrefs.getString("saved_province", "") ?: ""
        val sCity = sharedPrefs.getString("saved_city", "") ?: ""
        val sChurch = sharedPrefs.getString("saved_church", "") ?: ""
        
        try {
            val auth = FirebaseAuth.getInstance()
            auth.signInWithEmailAndPassword(email, pword)
                .addOnCompleteListener { task ->
                    isAuthenticating = false
                    if (task.isSuccessful) {
                        currentUser = UserProfile(
                            email = email,
                            isAdmin = actualIsAdmin,
                            name = savedName,
                            province = sProv,
                            city = sCity,
                            church = sChurch
                        )
                        com.example.data.ScribeAnalytics.logUserLogin(
                            context = getApplication(),
                            email = email,
                            role = if (actualIsAdmin) "Admin" else "User"
                        )
                        val editor = sharedPrefs.edit()
                        editor.putBoolean("remember_me", rememberMe)
                        if (rememberMe) {
                            editor.putString("saved_email", email)
                            editor.putBoolean("saved_is_admin", actualIsAdmin)
                        } else {
                            editor.remove("saved_email")
                            editor.remove("saved_is_admin")
                        }
                        editor.apply()
                        authErrorMessage = null
                        onComplete(true)
                    } else {
                        val exception = task.exception
                        val err = exception?.localizedMessage ?: "Sign-In failed."
                        val isWrongPassword = exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException || 
                                              err.contains("password", ignoreCase = true) || 
                                              err.contains("wrong", ignoreCase = true) || 
                                              err.contains("invalid", ignoreCase = true)
                        authErrorMessage = if (isWrongPassword) "Incorrect password." else err
                        onComplete(false)
                    }
                }
        } catch (e: Exception) {
            // Local Database fallback simulation when Firebase is unconfigured or offline
            isAuthenticating = false
            
            val savedLocalPword = sharedPrefs.getString("saved_pword_${email.lowercase().trim()}", "") ?: ""
            if (savedLocalPword.isNotEmpty() && savedLocalPword != pword) {
                authErrorMessage = "Incorrect password."
                onComplete(false)
                return
            }

            currentUser = UserProfile(
                email = email,
                isAdmin = actualIsAdmin,
                name = savedName,
                province = sProv,
                city = sCity,
                church = sChurch
            )
            com.example.data.ScribeAnalytics.logUserLogin(
                context = getApplication(),
                email = email,
                role = if (actualIsAdmin) "Admin" else "User"
            )
            val editor = sharedPrefs.edit()
            editor.putBoolean("remember_me", rememberMe)
            if (rememberMe) {
                editor.putString("saved_email", email)
                editor.putBoolean("saved_is_admin", actualIsAdmin)
            } else {
                editor.remove("saved_email")
                editor.remove("saved_is_admin")
            }
            editor.apply()
            authErrorMessage = null
            onComplete(true)
        }
    }

    fun register(
        email: String,
        pword: String,
        fullName: String,
        province: String = "",
        city: String = "",
        church: String = "",
        onComplete: (Boolean) -> Unit
    ) {
        if (email.isEmpty() || pword.isEmpty() || fullName.isEmpty()) {
            authErrorMessage = "Fields cannot be empty."
            onComplete(false)
            return
        }
        if (pword.length < 6) {
            authErrorMessage = "Password must be at least 6 characters."
            onComplete(false)
            return
        }
        isAuthenticating = true
        authErrorMessage = null
        
        val actualIsAdmin = adminEmails.contains(email.lowercase().trim())
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)

        try {
            val auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, pword)
                .addOnCompleteListener { task ->
                    isAuthenticating = false
                    if (task.isSuccessful) {
                        val editor = sharedPrefs.edit()
                        editor.putString("saved_name", fullName)
                        editor.putString("saved_province", province)
                        editor.putString("saved_city", city)
                        editor.putString("saved_church", church)
                        editor.putString("saved_pword_${email.lowercase().trim()}", pword)
                        editor.putBoolean("remember_me", rememberMe)
                        if (rememberMe) {
                            editor.putString("saved_email", email)
                            editor.putBoolean("saved_is_admin", actualIsAdmin)
                        } else {
                            editor.remove("saved_email")
                            editor.remove("saved_is_admin")
                        }
                        editor.apply()

                         currentUser = UserProfile(
                             email = email,
                             isAdmin = actualIsAdmin,
                             name = fullName,
                             province = province,
                             city = city,
                             church = church
                         )
                        com.example.data.ScribeAnalytics.logUserRegistration(
                            context = getApplication(),
                            email = email,
                            role = if (actualIsAdmin) "Admin" else "User"
                        )
                        
                        // Cloud sync profile to Firestore
                        com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                            email = email,
                            fullName = fullName,
                            photoUrl = customProfilePhotoUri,
                            initials = getInitialsFromName(fullName),
                            province = province,
                            city = city,
                            church = church,
                            context = getApplication()
                        ) { success ->
                            Log.d("AppViewModel", "Cloud Profile updated: $success")
                        }
                        
                        authErrorMessage = null
                        onComplete(true)
                    } else {
                        val err = task.exception?.localizedMessage ?: "Registration failed."
                        authErrorMessage = err
                        onComplete(false)
                    }
                }
        } catch (e: Exception) {
            isAuthenticating = false
            val editor = sharedPrefs.edit()
            editor.putString("saved_name", fullName)
            editor.putString("saved_province", province)
            editor.putString("saved_city", city)
            editor.putString("saved_church", church)
            editor.putString("saved_pword_${email.lowercase().trim()}", pword)
            editor.putBoolean("remember_me", rememberMe)
            if (rememberMe) {
                editor.putString("saved_email", email)
                editor.putBoolean("saved_is_admin", actualIsAdmin)
            } else {
                editor.remove("saved_email")
                editor.remove("saved_is_admin")
            }
            editor.apply()

            currentUser = UserProfile(
                email = email,
                isAdmin = actualIsAdmin,
                name = fullName,
                province = province,
                city = city,
                church = church
            )
            com.example.data.ScribeAnalytics.logUserRegistration(
                context = getApplication(),
                email = email,
                role = if (actualIsAdmin) "Admin" else "User"
            )
            authErrorMessage = null
            onComplete(true)
        }
    }

    fun forgotPassword(email: String, onComplete: (Boolean, String) -> Unit) {
        if (email.isEmpty()) {
            onComplete(false, "Email cannot be empty.")
            return
        }
        try {
            val auth = FirebaseAuth.getInstance()
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        com.example.data.ScribeAnalytics.logForgotPassword(
                            context = getApplication(),
                            email = email
                        )
                        onComplete(true, "Reset link dispatched successfully to $email!")
                    } else {
                        onComplete(false, task.exception?.localizedMessage ?: "Failed to dispatch reset link.")
                    }
                }
        } catch (e: Exception) {
            com.example.data.ScribeAnalytics.logForgotPassword(
                context = getApplication(),
                email = email
            )
            onComplete(true, "Simulated reset email dispatched to $email (Firebase offline).")
        }
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w("AppViewModel", "Firebase sign out error: ${e.message}")
        }
        currentUser = null
        customProfilePhotoUri = null
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean("remember_me", false)
        editor.remove("saved_email")
        editor.remove("saved_is_admin")
        editor.remove("custom_profile_photo_uri")
        editor.apply()
    }

    fun unsubscribeUser() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w("AppViewModel", "Firebase auth unsubscribe error: ${e.message}")
        }
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean("remember_me", false)
        editor.remove("saved_email")
        editor.remove("saved_is_admin")
        editor.remove("saved_name")
        editor.remove("custom_profile_photo_uri")
        editor.apply()
        currentUser = null
        customProfilePhotoUri = null
    }

    fun addAdminEmail(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) return false
        val updated = adminEmails.toMutableSet()
        updated.add(cleanEmail)
        adminEmails = updated
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putStringSet("admin_emails", adminEmails).apply()
        
        // Update active user state if email matches
        val current = currentUser
        if (current != null && current.email.lowercase().trim() == cleanEmail) {
            currentUser = current.copy(isAdmin = true)
        }
        return true
    }

    fun toggleAdminRole() {
        val current = currentUser ?: return
        val updatedAdmin = !current.isAdmin
        currentUser = current.copy(isAdmin = updatedAdmin)
        
        val cleanEmail = current.email.lowercase().trim()
        val updatedEmails = adminEmails.toMutableSet()
        if (updatedAdmin) {
            updatedEmails.add(cleanEmail)
        } else {
            // Keep popoydev@gmail.com ALWAYS
            if (cleanEmail != "popoydev@gmail.com") {
                updatedEmails.remove(cleanEmail)
            }
        }
        adminEmails = updatedEmails
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putStringSet("admin_emails", adminEmails).apply()
    }

    fun toggleTheme() {
        isDarkMode = !isDarkMode
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("dark_theme", isDarkMode).apply()
    }

    fun updateProfilePhoto(index: Int) {
        profilePhotoIndex = index
        // When setting static avatar, clear custom photo if desired, or keep it as alternative.
        // Let's clear customProfilePhotoUri so display changes immediately to chosen index avatar.
        customProfilePhotoUri = null
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("profile_photo_index", index)
            .remove("custom_profile_photo_uri")
            .apply()
    }

    fun setCustomProfilePhoto(uri: String?) {
        customProfilePhotoUri = uri
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        if (uri == null) {
            editor.remove("custom_profile_photo_uri")
        } else {
            editor.putString("custom_profile_photo_uri", uri)
        }
        editor.apply()
    }

    fun updateProfileName(newName: String) {
        val user = currentUser ?: return
        val initials = getInitialsFromName(newName)
        currentUser = user.copy(name = newName)
        
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("saved_name", newName).apply()
        
        viewModelScope.launch {
            com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                email = user.email,
                fullName = newName,
                photoUrl = customProfilePhotoUri,
                initials = initials,
                province = user.province,
                city = user.city,
                church = user.church,
                context = getApplication()
            ) { success ->
                profileSyncStatus = if (success) "Profile sync with cloud complete." else "Saved locally (Firebase offline)"
            }
        }
    }

    fun updateProfile(newName: String, newProvince: String, newCity: String, newChurch: String) {
        val user = currentUser ?: return
        val initials = getInitialsFromName(newName)
        currentUser = user.copy(name = newName, province = newProvince, city = newCity, church = newChurch)
        
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("saved_name", newName)
            .putString("saved_province", newProvince)
            .putString("saved_city", newCity)
            .putString("saved_church", newChurch)
            .apply()
        
        viewModelScope.launch {
            com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                email = user.email,
                fullName = newName,
                photoUrl = customProfilePhotoUri,
                initials = initials,
                province = newProvince,
                city = newCity,
                church = newChurch,
                context = getApplication()
            ) { success ->
                profileSyncStatus = if (success) "Profile sync with cloud complete." else "Saved locally (Firebase offline)"
            }
        }
    }

    fun uploadGalleryPhoto(uri: android.net.Uri) {
        val context = getApplication<Application>()
        val user = currentUser ?: return
        isUploadingProfile = true
        profileSyncStatus = "Persisting image locally..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Copy selected content URI to internal storage file to avoid transient permission loss
                val profileDir = java.io.File(context.filesDir, "profiles")
                if (!profileDir.exists()) {
                    profileDir.mkdirs()
                }
                val localFile = java.io.File(profileDir, "profile_${user.email.replace("@", "_")}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Stable local file URI
                val localFileUri = android.net.Uri.fromFile(localFile).toString()
                
                withContext(Dispatchers.Main) {
                    customProfilePhotoUri = localFileUri
                    val sharedPrefs = context.getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString("custom_profile_photo_uri", localFileUri).apply()
                    
                    // Trigger Firestore update for local file uri so it refreshes immediately offline too
                    val initials = getInitialsFromName(user.name)
                    com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                        email = user.email,
                        fullName = user.name,
                        photoUrl = localFileUri,
                        initials = initials,
                        context = context
                    ) { }
                }

                // Now attempt upload to online storage
                com.example.data.FirebaseProfileHelper.uploadProfilePhoto(
                    uri = uri,
                    context = context,
                    email = user.email,
                    onSuccess = { downloadUrl ->
                        viewModelScope.launch(Dispatchers.Main) {
                            customProfilePhotoUri = downloadUrl
                            val sharedPrefs = context.getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
                            sharedPrefs.edit().putString("custom_profile_photo_uri", downloadUrl).apply()
                            
                            val initials = getInitialsFromName(user.name)
                            com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                                email = user.email,
                                fullName = user.name,
                                photoUrl = downloadUrl,
                                initials = initials,
                                context = context
                            ) { success ->
                                isUploadingProfile = false
                                profileSyncStatus = if (success) "Upload & cloud sync successful!" else "Saved successfully"
                            }
                        }
                    },
                    onFailure = { error ->
                        viewModelScope.launch(Dispatchers.Main) {
                            // Keep the local file photo working, but show upload failure warning nicely
                            isUploadingProfile = false
                            profileSyncStatus = "Saved locally. Cloud sync optional: ${error.localizedMessage}"
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isUploadingProfile = false
                    profileSyncStatus = "Error saving image: ${e.localizedMessage}"
                    Log.e("AppViewModel", "Failed to save local profile file", e)
                }
            }
        }
    }

    fun uploadCameraPhoto(bitmap: android.graphics.Bitmap) {
        val context = getApplication<Application>()
        val user = currentUser ?: return
        isUploadingProfile = true
        profileSyncStatus = "Compressing & saving image..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Save bitmap locally
                val profileDir = java.io.File(context.filesDir, "profiles")
                if (!profileDir.exists()) {
                    profileDir.mkdirs()
                }
                val localFile = java.io.File(profileDir, "profile_${user.email.replace("@", "_")}.jpg")
                localFile.outputStream().use { output ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)
                }
                
                val localFileUri = android.net.Uri.fromFile(localFile).toString()
                
                withContext(Dispatchers.Main) {
                    customProfilePhotoUri = localFileUri
                    val sharedPrefs = context.getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString("custom_profile_photo_uri", localFileUri).apply()
                    
                    val initials = getInitialsFromName(user.name)
                    com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                        email = user.email,
                        fullName = user.name,
                        photoUrl = localFileUri,
                        initials = initials,
                        context = context
                    ) { }
                }

                // Attempt to upload online
                com.example.data.FirebaseProfileHelper.uploadProfileBitmap(
                    bitmap = bitmap,
                    context = context,
                    email = user.email,
                    onSuccess = { downloadUrl ->
                        viewModelScope.launch(Dispatchers.Main) {
                            customProfilePhotoUri = downloadUrl
                            val sharedPrefs = context.getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
                            sharedPrefs.edit().putString("custom_profile_photo_uri", downloadUrl).apply()
                            
                            val initials = getInitialsFromName(user.name)
                            com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                                email = user.email,
                                fullName = user.name,
                                photoUrl = downloadUrl,
                                initials = initials,
                                context = context
                            ) { success ->
                                isUploadingProfile = false
                                profileSyncStatus = if (success) "Upload & cloud sync successful!" else "Saved successfully"
                            }
                        }
                    },
                    onFailure = { error ->
                        viewModelScope.launch(Dispatchers.Main) {
                            isUploadingProfile = false
                            profileSyncStatus = "Saved locally. Cloud sync optional: ${error.localizedMessage}"
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isUploadingProfile = false
                    profileSyncStatus = "Error compressing image: ${e.localizedMessage}"
                    Log.e("AppViewModel", "Failed to save camera photo", e)
                }
            }
        }
    }

    fun deleteProfilePhoto() {
        val user = currentUser ?: return
        customProfilePhotoUri = null
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("custom_profile_photo_uri").apply()
        
        viewModelScope.launch {
            val initials = getInitialsFromName(user.name)
            com.example.data.FirebaseProfileHelper.saveProfileToFirestore(
                email = user.email,
                fullName = user.name,
                photoUrl = null,
                initials = initials,
                context = getApplication()
            ) { success ->
                profileSyncStatus = if (success) "Profile cleared & synced on cloud." else "Cleared local profile image reference."
            }
        }
    }

    fun clearProfileSyncStatus() {
        profileSyncStatus = null
    }

    fun updateBsbInstalled(installed: Boolean) {
        isBsbInstalled = installed
        val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("bsb_installed", installed).apply()
    }

    // ==========================================
    // 2. BIBLE NAV & DATA STATE
    // ==========================================
    // 2. BIBLE NAV & DATA STATE
    // ==========================================
    var lastBibleScrollIndex by mutableStateOf(0)
    var lastBibleScrollOffset by mutableStateOf(1)

    var selectedTranslation by mutableStateOf("KJV") // "BSB" or "KJV"
    var isFetchingKjv by mutableStateOf(false)
    var kjvFetchError by mutableStateOf<String?>(null)

    private val _selectedBookFlow = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
            .getString("last_selected_book", "John") ?: "John"
    )
    val selectedBookFlow: StateFlow<String> get() = _selectedBookFlow
    var selectedBook: String
        get() = _selectedBookFlow.value
        set(value) { 
            _selectedBookFlow.value = value 
            val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("last_selected_book", value).apply()
            lastBibleScrollIndex = 0
            lastBibleScrollOffset = 0
            loadRealVersesIfNeeded(value, selectedChapter)
        }

    private val _selectedChapterFlow = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
            .getInt("last_selected_chapter", 1)
    )
    val selectedChapterFlow: StateFlow<Int> get() = _selectedChapterFlow
    var selectedChapter: Int
        get() = _selectedChapterFlow.value
        set(value) { 
            _selectedChapterFlow.value = value 
            val sharedPrefs = getApplication<Application>().getSharedPreferences("baptist_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putInt("last_selected_chapter", value).apply()
            lastBibleScrollIndex = 0
            lastBibleScrollOffset = 0
            loadRealVersesIfNeeded(selectedBook, value)
        }

    fun fetchKjvVersesOnline(book: String, chapter: Int) {
        isFetchingKjv = true
        kjvFetchError = null
        viewModelScope.launch {
            try {
                repository.loadKjvChapterVerses(book, chapter)
            } catch (e: Exception) {
                e.printStackTrace()
                kjvFetchError = "Offline: Could not connect to Bible API"
            } finally {
                isFetchingKjv = false
            }
        }
    }

    fun loadRealVersesIfNeeded(book: String, chapter: Int) {
        viewModelScope.launch {
            try {
                if (selectedTranslation == "KJV") {
                    repository.loadKjvChapterVerses(book, chapter)
                } else {
                    repository.loadChapterVerses(book, chapter)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var selectedTestamentFilter by mutableStateOf("NT") // "OT" or "NT"
    var bibleFontSize by mutableStateOf(16f)
    var bibleSearchQuery by mutableStateOf("")
    var bibleSearchResults by mutableStateOf<List<BibleVerse>>(emptyList())

    val bibleBooks: StateFlow<List<BookWithTestament>> = repository.bibleBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentVerses: StateFlow<List<BibleVerse>> = combine(
        _selectedBookFlow.combine(_selectedChapterFlow) { book, chap -> Pair(book, chap) },
        snapshotFlow { currentUser?.email ?: "" }
    ) { (book, chap), email ->
        Pair(book, chap) to email
    }.flatMapLatest { (bc, email) ->
        repository.getVersesByChapterForUser(bc.first, bc.second, email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val kjvVerses: StateFlow<List<BibleVerse>> = combine(
        _selectedBookFlow.combine(_selectedChapterFlow) { book, chap -> Pair(book, chap) },
        snapshotFlow { currentUser?.email ?: "" }
    ) { (book, chap), email ->
        Pair(book, chap) to email
    }.flatMapLatest { (bc, email) ->
        repository.getKjvVersesByChapterForUser(bc.first, bc.second, email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bookmarkedVerses: StateFlow<List<BibleVerse>> = snapshotFlow { currentUser?.email ?: "" }
        .flatMapLatest { email ->
            repository.getBookmarkedVersesForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val highlightedVerses: StateFlow<List<BibleVerse>> = snapshotFlow { currentUser?.email ?: "" }
        .flatMapLatest { email ->
            repository.getHighlightedVersesForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun searchBible(query: String) {
        bibleSearchQuery = query
        if (query.isBlank()) {
            bibleSearchResults = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            bibleSearchResults = repository.searchBible(query)
        }
    }

    fun toggleBookmark(verse: BibleVerse) {
        viewModelScope.launch(Dispatchers.IO) {
            val email = currentUser?.email ?: ""
            val status = repository.getSingleUserVerseStatus(verse.book, verse.chapter, verse.verseNum, email)
            val updatedStatus = if (status != null) {
                status.copy(
                    isBookmarked = !status.isBookmarked,
                    bookmarkDate = if (!status.isBookmarked) System.currentTimeMillis() else 0L
                )
            } else {
                UserVerseStatus(
                    book = verse.book,
                    chapter = verse.chapter,
                    verseNum = verse.verseNum,
                    ownerEmail = email,
                    isBookmarked = true,
                    bookmarkDate = System.currentTimeMillis()
                )
            }
            repository.insertUserVerseStatus(updatedStatus)
        }
    }

    fun applyHighlight(verse: BibleVerse, colorHex: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val email = currentUser?.email ?: ""
            val status = repository.getSingleUserVerseStatus(verse.book, verse.chapter, verse.verseNum, email)
            val updatedStatus = if (status != null) {
                status.copy(highlightColor = colorHex)
            } else {
                UserVerseStatus(
                    book = verse.book,
                    chapter = verse.chapter,
                    verseNum = verse.verseNum,
                    ownerEmail = email,
                    highlightColor = colorHex
                )
            }
            repository.insertUserVerseStatus(updatedStatus)
        }
    }

    // ==========================================
    // 3. NOTES STATE
    // ==========================================
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allNotes: StateFlow<List<Note>> = snapshotFlow { currentUser?.email ?: "" }
        .flatMapLatest { email ->
            repository.getAllNotesForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var noteSearchQuery by mutableStateOf("")
    var selectedNoteCategory by mutableStateOf("All") // "All", "Corporate Worship", "Sunday School", "Other Classes"
    var isCloudSyncing by mutableStateOf(false)

    fun addNote(
        category: String,
        title: String,
        speaker: String,
        churchName: String,
        bibleVerses: String,
        richTextNotes: String,
        tags: String,
        isFavorite: Boolean,
        customDate: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateToUse = customDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val newNote = Note(
                category = category,
                title = title,
                date = dateToUse,
                speaker = speaker,
                churchName = churchName,
                bibleVerses = bibleVerses,
                richTextNotes = richTextNotes,
                tags = tags,
                isFavorite = isFavorite,
                syncStatus = "synced",
                ownerEmail = currentUser?.email ?: ""
            )
            repository.insertNote(newNote)
            com.example.data.ScribeAnalytics.logNoteCreated(
                context = getApplication(),
                title = title,
                category = category,
                verses = bibleVerses
            )
        }
    }

    fun updateNoteDetails(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(note.copy(ownerEmail = note.ownerEmail.ifEmpty { currentUser?.email ?: "" }))
        }
    }

    fun deleteNoteDetails(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(note)
        }
    }

    fun triggerCloudSync() {
        val userEmail = currentUser?.email
        if (userEmail.isNullOrBlank()) {
            return
        }
        viewModelScope.launch {
            isCloudSyncing = true
            
            val context = getApplication<Application>()
            val localNotes = allNotes.value
            val localChurches = allChurches.value
            
            var notesSynced = false
            var churchesSynced = false
            
            com.example.data.FirebaseProfileHelper.syncNotesWithFirebase(
                context = context,
                email = userEmail,
                localNotes = localNotes,
                insertOrUpdateLocal = { downloadedNote ->
                    repository.insertNote(downloadedNote)
                },
                onComplete = { success ->
                    notesSynced = true
                    if (notesSynced && churchesSynced) {
                        isCloudSyncing = false
                    }
                }
            )
            
            com.example.data.FirebaseProfileHelper.syncChurchesWithFirebase(
                context = context,
                localChurches = localChurches,
                insertOrUpdateLocal = { downloadedChurch ->
                    repository.insertChurch(downloadedChurch)
                },
                onComplete = { success ->
                    churchesSynced = true
                    if (notesSynced && churchesSynced) {
                        isCloudSyncing = false
                    }
                }
            )
            
            // To ensure the UI loading state completes even if Firestore experiences lag or is offline
            kotlinx.coroutines.delay(4000)
            isCloudSyncing = false
        }
    }

    // ==========================================
    // 4. STATS & ANALYTICS computation
    // ==========================================
    val statsMostReferencedVerses = allNotes.map { notes ->
        val verseCountMap = mutableMapOf<String, Int>()
        notes.forEach { note ->
            if (note.bibleVerses.isNotBlank()) {
                val split = note.bibleVerses.split(Regex("[,;\\n]"))
                split.forEach { v ->
                    val cleanVal = v.trim()
                    if (cleanVal.length > 3) {
                        verseCountMap[cleanVal] = verseCountMap.getOrDefault(cleanVal, 0) + 1
                    }
                }
            }
        }
        verseCountMap.entries.sortedByDescending { it.value }.take(5).map { it.key }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsMonthlySummary = allNotes.map { notes ->
        val monthMap = mutableMapOf<String, Int>()
        notes.forEach { note ->
            try {
                val dateVal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(note.date)
                dateVal?.let {
                    val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(it)
                    monthMap[label] = monthMap.getOrDefault(label, 0) + 1
                }
            } catch (e: Exception) {
                monthMap["May 2026"] = monthMap.getOrDefault("May 2026", 0) + 1
            }
        }
        monthMap.entries.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // 5. CHURCH ACTIVITIES
    // ==========================================
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allActivities: StateFlow<List<ChurchActivity>> = snapshotFlow { currentUser?.email ?: "" }
        .flatMapLatest { email ->
            repository.getAllActivitiesForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createActivity(
        title: String,
        date: String,
        time: String,
        location: String,
        notes: String,
        notification: Boolean,
        recur: Boolean,
        recurDay: String,
        onComplete: (ChurchActivity) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val act = ChurchActivity(
                title = title,
                date = date,
                time = time,
                location = location,
                notes = notes,
                notificationEnabled = notification,
                isRecurring = recur,
                recurrenceDay = recurDay,
                ownerEmail = currentUser?.email ?: ""
            )
            val generatedId = repository.insertActivity(act)
            val finalAct = act.copy(id = generatedId.toInt())
            withContext(Dispatchers.Main) {
                onComplete(finalAct)
            }
        }
    }

    fun removeActivity(activity: ChurchActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteActivity(activity)
        }
    }

    fun updateActivity(activity: ChurchActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateActivity(activity)
        }
    }

    // ==========================================
    // 6. CHURCHES DIRECTORY (PH)
    // ==========================================
    var churchSearchQuery by mutableStateOf("")
    var selectedProvinceFilter by mutableStateOf("All")

    val approvedChurches: StateFlow<List<BaptistChurch>> = repository.approvedChurches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChurches: StateFlow<List<BaptistChurch>> = repository.allChurches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun submitNewChurch(
        name: String,
        province: String,
        cityMunicipality: String,
        address: String,
        pastor: String,
        contact: String,
        schedule: String,
        description: String,
        latitude: Double,
        longitude: Double,
        facebookUrl: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val isAdmin = currentUser?.isAdmin == true
            val newChurch = BaptistChurch(
                name = name,
                province = province,
                cityMunicipality = cityMunicipality,
                address = address,
                pastorName = pastor,
                contactNumber = contact,
                worshipSchedule = schedule,
                description = description,
                latitude = latitude,
                longitude = longitude,
                isApproved = isAdmin, // If Admin submits, it's live instantly!
                submittedBy = currentUser?.let { if (it.name.isNotBlank()) "${it.name} (${it.email})" else it.email } ?: "Guest User",
                timestamp = System.currentTimeMillis(),
                facebookUrl = facebookUrl
            )
            repository.insertChurch(newChurch)
            com.example.data.ScribeAnalytics.logChurchSubmitted(
                context = getApplication(),
                churchName = name,
                province = province,
                submittedBy = currentUser?.email ?: "Guest User"
            )
        }
    }

    fun approveChurch(church: BaptistChurch) {
        viewModelScope.launch(Dispatchers.IO) {
            if (church.replacesChurchId != null) {
                // This is a proposed edit! Copy fields to original and delete this request
                val original = repository.getChurchById(church.replacesChurchId)
                if (original != null) {
                    val updated = original.copy(
                        name = church.name,
                        province = church.province,
                        cityMunicipality = church.cityMunicipality,
                        address = church.address,
                        pastorName = church.pastorName,
                        contactNumber = church.contactNumber,
                        worshipSchedule = church.worshipSchedule,
                        description = church.description,
                        latitude = church.latitude,
                        longitude = church.longitude,
                        isApproved = true
                    )
                    repository.updateChurch(updated)
                }
                repository.deleteChurch(church)
            } else {
                // Standard new church approval
                val updated = church.copy(isApproved = true)
                repository.updateChurch(updated)
            }
            com.example.data.ScribeAnalytics.logChurchApproved(
                context = getApplication(),
                churchName = church.name,
                province = church.province
            )
        }
    }

    fun editChurch(
        churchId: Int,
        name: String,
        province: String,
        cityMunicipality: String,
        address: String,
        pastor: String,
        contact: String,
        schedule: String,
        description: String,
        latitude: Double,
        longitude: Double,
        facebookUrl: String,
        onComplete: (isDirect: Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val original = repository.getChurchById(churchId)
            val isAdmin = currentUser?.isAdmin == true
            if (isAdmin) {
                if (original != null) {
                    val updated = original.copy(
                        name = name,
                        province = province,
                        cityMunicipality = cityMunicipality,
                        address = address,
                        pastorName = pastor,
                        contactNumber = contact,
                        worshipSchedule = schedule,
                        description = description,
                        latitude = latitude,
                        longitude = longitude,
                        facebookUrl = facebookUrl
                    )
                    repository.updateChurch(updated)
                    withContext(Dispatchers.Main) {
                        onComplete(true)
                    }
                }
            } else {
                // Determine edited fields to log
                val changedFields = mutableListOf<String>()
                if (original != null) {
                    if (original.name != name) changedFields.add("Name")
                    if (original.province != province) changedFields.add("Province")
                    if (original.cityMunicipality != cityMunicipality) changedFields.add("City/Municipality")
                    if (original.address != address) changedFields.add("Address")
                    if (original.pastorName != pastor) changedFields.add("Pastor(s)")
                    if (original.contactNumber != contact) changedFields.add("Contact Number")
                    if (original.worshipSchedule != schedule) changedFields.add("Worship Schedule")
                    if (original.description != description) changedFields.add("Description")
                    if (original.facebookUrl != facebookUrl) changedFields.add("Facebook Link")
                    if (original.latitude != latitude || original.longitude != longitude) changedFields.add("GPS coordinates")
                }
                val editedFieldsString = if (changedFields.isEmpty()) "None" else changedFields.joinToString(", ")

                // Normal user: Submit edit as a pending request copying fields
                val editRequest = BaptistChurch(
                    name = name,
                    province = province,
                    cityMunicipality = cityMunicipality,
                    address = address,
                    pastorName = pastor,
                    contactNumber = contact,
                    worshipSchedule = schedule,
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    facebookUrl = facebookUrl,
                    isApproved = false,
                    submittedBy = currentUser?.let { if (it.name.isNotBlank()) "${it.name} (${it.email})" else it.email } ?: "Guest User",
                    timestamp = System.currentTimeMillis(),
                    replacesChurchId = churchId
                )
                repository.insertChurch(editRequest)
                com.example.data.ScribeAnalytics.logChurchEditProposed(
                    context = getApplication(),
                    churchName = original?.name ?: name,
                    editedFields = editedFieldsString,
                    submittedBy = currentUser?.email ?: "Guest User"
                )
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun requestDeleteChurch(church: BaptistChurch, onComplete: (isDirect: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val isAdmin = currentUser?.isAdmin == true
            if (isAdmin) {
                repository.deleteChurch(church)
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } else {
                val updated = church.copy(
                    isDeletePending = true,
                    submittedBy = currentUser?.let { if (it.name.isNotBlank()) "${it.name} (${it.email})" else it.email } ?: "Guest User"
                )
                repository.updateChurch(updated)
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun approveDeletionRequest(church: BaptistChurch) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChurch(church)
        }
    }

    fun rejectDeletionRequest(church: BaptistChurch) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = church.copy(isDeletePending = false)
            repository.updateChurch(updated)
        }
    }

    fun updateChurchLocation(church: BaptistChurch, latitude: Double, longitude: Double, address: String = church.address, description: String = church.description, city: String = church.cityMunicipality) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = church.copy(latitude = latitude, longitude = longitude, address = address, description = description, cityMunicipality = city)
            repository.updateChurch(updated)
        }
    }

    fun rejectChurch(church: BaptistChurch) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChurch(church)
            com.example.data.ScribeAnalytics.logChurchRejected(
                context = getApplication(),
                churchName = church.name,
                province = church.province
            )
        }
    }

    // ==========================================
    // VIEW MODEL PROVIDER FACTORY
    // ==========================================
    class Factory(
        private val application: Application,
        private val repository: AppRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
