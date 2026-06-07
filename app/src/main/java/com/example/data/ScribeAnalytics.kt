package com.example.data

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AnalyticsEvent(
    val name: String,
    val description: String,
    val timestamp: String,
    val details: Map<String, String> = emptyMap()
)

object ScribeAnalytics {
    private const val TAG = "ScribeAnalytics"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    
    // Live in-memory events list for admin console stream
    private val _liveEvents = MutableStateFlow<List<AnalyticsEvent>>(
        listOf(
            AnalyticsEvent("AppLaunch", "New Session handshake registered", "Just now"),
            AnalyticsEvent("NoteCreated", "Popoy Dev created an outline on Galatians 2", "2m ago"),
            AnalyticsEvent("ShareVerse", "Shared Ephesians 2:8 to clipboard", "12m ago"),
            AnalyticsEvent("OfflineBibleSync", "Local SQLite indexing 31,086 Berean verses", "25m ago"),
            AnalyticsEvent("ChurchSubmission", "Submitted Sovereign Grace Church in Cebu", "1h ago")
        )
    )
    val liveEvents: StateFlow<List<AnalyticsEvent>> = _liveEvents.asStateFlow()

    fun initialize(context: Context) {
        try {
            if (FirebaseProfileHelper.isFirebaseAvailable(context)) {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context)
                Log.d(TAG, "Firebase Analytics initialized successfully.")
            } else {
                Log.w(TAG, "Firebase unavailable. Falling back to local offline logging.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Analytics: ${e.message}", e)
        }
    }

    /**
     * Log a general analytic event, propagating to Firebase if available, and appending to our real-time list.
     */
    fun logEvent(context: Context, eventName: String, params: Bundle = Bundle(), description: String = "") {
        val detailsMap = mutableMapOf<String, String>()
        params.keySet().forEach { key ->
            detailsMap[key] = params.get(key)?.toString() ?: ""
        }
        
        // Append locally for the Live Admin log display
        val newEvent = AnalyticsEvent(
            name = eventName,
            description = if (description.isNotEmpty()) description else "Event action executed with ${detailsMap.size} parameters.",
            timestamp = "Just now",
            details = detailsMap
        )
        
        val currentList = _liveEvents.value.toMutableList()
        currentList.add(0, newEvent)
        if (currentList.size > 20) {
            currentList.removeLast()
        }
        _liveEvents.value = currentList

        // Send to Firebase Analytics
        try {
            if (firebaseAnalytics == null && FirebaseProfileHelper.isFirebaseAvailable(context)) {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            }
            firebaseAnalytics?.let {
                it.logEvent(eventName, params)
                Log.d(TAG, "Logged event '$eventName' to Firebase: $params")
            } ?: Log.d(TAG, "Logged local-only event '$eventName' (Firebase analytics offline): $params")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Analytics logging error", e)
        }
    }

    // Specific event helpers matching user request

    fun logUserRegistration(context: Context, email: String, role: String) {
        val bundle = Bundle().apply {
            putString("user_email", email)
            putString("user_role", role)
        }
        logEvent(
            context = context,
            eventName = "auth_registration",
            params = bundle,
            description = "Account registration: $email as role $role"
        )
    }

    fun logUserLogin(context: Context, email: String, role: String) {
        val bundle = Bundle().apply {
            putString("user_email", email)
            putString("user_role", role)
        }
        logEvent(
            context = context,
            eventName = "auth_login",
            params = bundle,
            description = "Secure login handshake: $email logged in as $role"
        )
    }

    fun logForgotPassword(context: Context, email: String) {
        val bundle = Bundle().apply {
            putString("user_email", email)
        }
        logEvent(
            context = context,
            eventName = "auth_forgot_password",
            params = bundle,
            description = "Instruction link requested for $email"
        )
    }

    fun logInstallDownload(context: Context) {
        logEvent(
            context = context,
            eventName = "scribe_installation",
            params = Bundle().apply { putString("platform", "Android Emulator Stream") },
            description = "Handshake download telemetry synced."
        )
    }

    fun logNoteCreated(context: Context, title: String, category: String, verses: String) {
        val bundle = Bundle().apply {
            putString("note_title", title)
            putString("note_category", category)
            putString("referenced_verses", verses)
        }
        logEvent(
            context = context,
            eventName = "note_created",
            params = bundle,
            description = "Completed sermon outline '$title' in category $category"
        )
    }

    fun logChurchSubmitted(context: Context, churchName: String, province: String, submittedBy: String) {
        val bundle = Bundle().apply {
            putString("church_name", churchName)
            putString("church_province", province)
            putString("submitted_by", submittedBy)
        }
        logEvent(
            context = context,
            eventName = "church_registry_submitted",
            params = bundle,
            description = "New registry proposed: $churchName ($province) by $submittedBy"
        )
    }

    fun logChurchApproved(context: Context, churchName: String, province: String) {
        val bundle = Bundle().apply {
            putString("church_name", churchName)
            putString("church_province", province)
        }
        logEvent(
            context = context,
            eventName = "church_registry_approved",
            params = bundle,
            description = "Admin Approved: $churchName is now active and live!"
        )
    }

    fun logChurchRejected(context: Context, churchName: String, province: String) {
        val bundle = Bundle().apply {
            putString("church_name", churchName)
            putString("church_province", province)
        }
        logEvent(
            context = context,
            eventName = "church_registry_rejected",
            params = bundle,
            description = "Admin Rejected: $churchName directory entry cleared"
        )
    }

    fun logChurchEditProposed(context: Context, churchName: String, editedFields: String, submittedBy: String) {
        val bundle = Bundle().apply {
            putString("church_name", churchName)
            putString("edited_fields", editedFields)
            putString("submitted_by", submittedBy)
        }
        logEvent(
            context = context,
            eventName = "church_edit_proposed",
            params = bundle,
            description = "Modifications proposed for '$churchName' by $submittedBy. Edited: $editedFields"
        )
    }
}
