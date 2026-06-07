package com.example

import android.app.Application
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class BaptistNotesApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { AppRepository(database) }

    override fun onCreate() {
        super.onCreate()
        initializeFirebaseIfConfigured()
    }

    private fun initializeFirebaseIfConfigured() {
        try {
            var apiKey: String? = null
            var appId: String? = null
            var projectId: String? = null
            var storageBucket: String? = null

            // 1. Try to read from assets/google-services.json first!
            try {
                assets.open("google-services.json").use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val root = org.json.JSONObject(jsonString)
                    val projectInfo = root.optJSONObject("project_info")
                    projectId = projectInfo?.optString("project_id")
                    storageBucket = projectInfo?.optString("storage_bucket")

                    val clients = root.optJSONArray("client")
                    if (clients != null && clients.length() > 0) {
                        val client = clients.getJSONObject(0)
                        val clientInfo = client.optJSONObject("client_info")
                        appId = clientInfo?.optString("mobilesdk_app_id")

                        val apiKeyList = client.optJSONArray("api_key")
                        if (apiKeyList != null && apiKeyList.length() > 0) {
                            apiKey = apiKeyList.getJSONObject(0).optString("current_key")
                        }
                    }
                    Log.d("BaptistNotesApp", "Successfully loaded Firebase configuration from assets/google-services.json")
                }
            } catch (e: Exception) {
                Log.d("BaptistNotesApp", "google-services.json not found in assets, checking build config or secrets.")
            }

            // 2. If not found in assets, fall back to BuildConfig secrets!
            if (apiKey.isNullOrBlank() || appId.isNullOrBlank() || projectId.isNullOrBlank()) {
                apiKey = BuildConfig.FIREBASE_API_KEY
                appId = BuildConfig.FIREBASE_APP_ID
                projectId = BuildConfig.FIREBASE_PROJECT_ID
                storageBucket = BuildConfig.FIREBASE_STORAGE_BUCKET
            }

            val isConfigured = !apiKey.isNullOrBlank() && !apiKey.contains("PLACEHOLDER") &&
                    !appId.isNullOrBlank() && !appId.contains("PLACEHOLDER") &&
                    !projectId.isNullOrBlank() && !projectId.contains("PLACEHOLDER")

            if (isConfigured) {
                Log.d("BaptistNotesApp", "Initializing Firebase programmatically with Project ID: $projectId...")
                val builder = FirebaseOptions.Builder()
                    .setApiKey(apiKey!!)
                    .setApplicationId(appId!!)
                    .setProjectId(projectId!!)

                if (!storageBucket.isNullOrBlank() && !storageBucket.contains("PLACEHOLDER")) {
                    builder.setStorageBucket(storageBucket)
                }

                FirebaseApp.initializeApp(this, builder.build())
                Log.d("BaptistNotesApp", "Firebase programmatically initialized successfully with Project ID: $projectId!")
            } else {
                Log.d("BaptistNotesApp", "Firebase parameters not fully configured in assets/google-services.json or secrets panel; running in mock offline mode.")
            }
        } catch (e: Exception) {
            Log.e("BaptistNotesApp", "Error programmatically initializing Firebase: ${e.message}", e)
        }
    }
}
