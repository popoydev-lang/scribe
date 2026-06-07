package com.example.data

import android.util.Log
import com.example.ui.screens.OutlineNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

object GeminiService {
    private const val TAG = "GeminiService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateSermonSummary(
        title: String,
        speaker: String,
        church: String,
        nodes: List<OutlineNode>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is not configured. Please add your key in the Secrets panel."
        }

        val prompt = if (nodes.isEmpty()) {
            """
                Summarize the following sermon or lesson for a Christian believer.
                ${if (title.isNotBlank()) "Title: $title" else ""}
                ${if (speaker.isNotBlank()) "Speaker: $speaker" else ""}
                ${if (church.isNotBlank()) "Church Name: $church" else ""}
                
                Please provide an encouraging, clear, and spiritually-rich summary of the lesson (about 2-4 sentences). Focus strictly on the core message and the biblical truth described. Do not use markdown headers or lists, keep it as a neat cohesive paragraph.
            """.trimIndent()
        } else {
            """
                Summarize the following sermon or lesson for a Christian believer.
                ${if (title.isNotBlank()) "Title: $title" else ""}
                ${if (speaker.isNotBlank()) "Speaker: $speaker" else ""}
                ${if (church.isNotBlank()) "Church Name: $church" else ""}
                
                Outlines, Scripture, and Takeaways:
                ${nodes.joinToString("\n\n") { "Point: ${it.outline}\nScripture: ${it.verses}\nTakeaway: ${it.takeaways}" }}
                
                Please provide an encouraging, clear, and spiritually-rich summary of the lesson (about 2-4 sentences). Focus strictly on the core message and the biblical truth described. Do not use markdown headers or lists, keep it as a neat cohesive paragraph.
            """.trimIndent()
        }

        try {
            val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            
            val requestJson = JSONObject()
            val contentsArray = org.json.JSONArray()
            val contentObj = JSONObject()
            val partsArray = org.json.JSONArray()
            val partObj = JSONObject()
            
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API error: $errBody")
                    return@withContext "Error: API call failed with code ${response.code}."
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    return@withContext "Error: Received empty response from Gemini API."
                }

                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext "Error: No summary candidates found in Gemini response."
                }
                
                val firstCandidate = candidates.getJSONObject(0)
                val responseContent = firstCandidate.optJSONObject("content")
                if (responseContent == null) {
                    return@withContext "Error: Candidate content is missing."
                }
                
                val parts = responseContent.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    return@withContext "Error: Candidate parts are empty."
                }
                
                val text = parts.getJSONObject(0).optString("text", "")
                if (text.isBlank()) {
                    "Error: No summary text generated."
                } else {
                    text.trim()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            "Error: ${e.message}"
        }
    }
}
