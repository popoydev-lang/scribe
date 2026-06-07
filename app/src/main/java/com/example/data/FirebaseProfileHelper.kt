package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FirebaseProfileHelper {
    private const val TAG = "FirebaseProfileHelper"

    fun isFirebaseAvailable(context: Context): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            try {
                FirebaseApp.initializeApp(context)
                true
            } catch (ex: Exception) {
                Log.w(TAG, "Firebase not initialized or available: ${ex.message}")
                false
            }
        }
    }

    fun saveProfileToFirestore(
        email: String,
        fullName: String,
        photoUrl: String?,
        initials: String,
        province: String = "",
        city: String = "",
        church: String = "",
        context: Context,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!isFirebaseAvailable(context)) {
            Log.d(TAG, "Firebase unavailable; profiles will persist locally.")
            onComplete(false)
            return
        }
        try {
            val db = FirebaseFirestore.getInstance()
            val userMap = hashMapOf(
                "email" to email,
                "fullName" to fullName,
                "photoUrl" to (photoUrl ?: ""),
                "initials" to initials,
                "province" to province,
                "city" to city,
                "church" to church,
                "timestamp" to System.currentTimeMillis()
            )
            
            val docId = email.lowercase().trim()
            db.collection("profiles").document(docId)
                .set(userMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Profile saved to Firestore successfully for $docId")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving profile to Firestore", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore error", e)
            onComplete(false)
        }
    }

    fun uploadProfilePhoto(
        uri: Uri,
        context: Context,
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isFirebaseAvailable(context)) {
            try {
                val localUri = copyUriToInternalStorage(context, uri)
                onSuccess(localUri.toString())
            } catch (e: Exception) {
                onFailure(e)
            }
            return
        }

        try {
            val compressedBytes = compressImageUri(context, uri) ?: throw Exception("Failed to compress image")
            
            val storageRef = FirebaseStorage.getInstance().reference
            val photoRef = storageRef.child("profiles/${email.lowercase().trim()}/profile_photo.jpg")

            val uploadTask = photoRef.putBytes(compressedBytes)
            uploadTask.addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }.addOnFailureListener { e ->
                    onFailure(e)
                }
            }.addOnFailureListener { e ->
                onFailure(e)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    fun uploadProfileBitmap(
        bitmap: Bitmap,
        context: Context,
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isFirebaseAvailable(context)) {
            try {
                val localUri = saveBitmapToInternalStorage(context, bitmap)
                onSuccess(localUri.toString())
            } catch (e: Exception) {
                onFailure(e)
            }
            return
        }

        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()

            val storageRef = FirebaseStorage.getInstance().reference
            val photoRef = storageRef.child("profiles/${email.lowercase().trim()}/profile_photo.jpg")

            val uploadTask = photoRef.putBytes(byteArray)
            uploadTask.addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }.addOnFailureListener { e ->
                    onFailure(e)
                }
            }.addOnFailureListener { e ->
                onFailure(e)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private fun compressImageUri(context: Context, uri: Uri): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return null
        
        val outputStream = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return outputStream.toByteArray()
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri): Uri {
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri) ?: throw Exception("Unable to open input stream")
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: throw Exception("Failed to decode stream")
        
        return saveBitmapToInternalStorage(context, originalBitmap)
    }

    private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri {
        val directory = File(context.filesDir, "profiles")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "profile_${UUID.randomUUID()}.jpg")
        val outStream = FileOutputStream(file)
        
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
        outStream.flush()
        outStream.close()
        
        return Uri.fromFile(file)
    }

    fun syncNotesWithFirebase(
        context: Context,
        email: String,
        localNotes: List<Note>,
        insertOrUpdateLocal: suspend (Note) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isFirebaseAvailable(context) || email.isBlank()) {
            onComplete(false)
            return
        }
        try {
            val db = FirebaseFirestore.getInstance()
            val userNotesRef = db.collection("users").document(email.lowercase().trim()).collection("notes")
            
            // 1. Upload local notes that are either newly created or pending sync
            for (note in localNotes) {
                val docId = if (note.id > 0) note.id.toString() else UUID.randomUUID().toString()
                val noteMap = hashMapOf(
                    "id" to note.id,
                    "category" to note.category,
                    "title" to note.title,
                    "date" to note.date,
                    "speaker" to note.speaker,
                    "churchName" to note.churchName,
                    "bibleVerses" to note.bibleVerses,
                    "richTextNotes" to note.richTextNotes,
                    "photoUri" to (note.photoUri ?: ""),
                    "tags" to note.tags,
                    "isFavorite" to note.isFavorite,
                    "submittedBy" to email
                )
                userNotesRef.document(docId).set(noteMap)
            }

            // 2. Download any notes belonging to the user from Firestore
            userNotesRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val coroutineScope = CoroutineScope(Dispatchers.IO)
                    coroutineScope.launch {
                        for (doc in querySnapshot.documents) {
                            try {
                                val title = doc.getString("title") ?: continue
                                val category = doc.getString("category") ?: ""
                                val date = doc.getString("date") ?: ""
                                val speaker = doc.getString("speaker") ?: ""
                                val churchName = doc.getString("churchName") ?: ""
                                val bibleVerses = doc.getString("bibleVerses") ?: ""
                                val richTextNotes = doc.getString("richTextNotes") ?: ""
                                val photoUri = doc.getString("photoUri").takeIf { !it.isNullOrBlank() }
                                val tags = doc.getString("tags") ?: ""
                                val isFavorite = doc.getBoolean("isFavorite") ?: false
                                val localId = doc.getLong("id")?.toInt() ?: 0

                                val existingNote = localNotes.find { 
                                    (localId > 0 && it.id == localId) || (it.title == title && it.date == date) 
                                }
                                if (existingNote == null) {
                                    val downloadedNote = Note(
                                        category = category,
                                        title = title,
                                        date = date,
                                        speaker = speaker,
                                        churchName = churchName,
                                        bibleVerses = bibleVerses,
                                        richTextNotes = richTextNotes,
                                        photoUri = photoUri,
                                        tags = tags,
                                        isFavorite = isFavorite,
                                        syncStatus = "synced",
                                        ownerEmail = email
                                    )
                                    insertOrUpdateLocal(downloadedNote)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing downloaded note", e)
                            }
                        }
                        onComplete(true)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to download notes from Firestore", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncNotesWithFirebase", e)
            onComplete(false)
        }
    }

    fun syncChurchesWithFirebase(
        context: Context,
        localChurches: List<BaptistChurch>,
        insertOrUpdateLocal: suspend (BaptistChurch) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isFirebaseAvailable(context)) {
            onComplete(false)
            return
        }
        try {
            val db = FirebaseFirestore.getInstance()
            val churchesRef = db.collection("churches")

            // 1. Upload local custom submitted approved/pending churches
            for (church in localChurches) {
                if (church.submittedBy != "Local Database") {
                    val docId = if (church.id > 0) church.id.toString() else UUID.randomUUID().toString()
                    val churchMap = hashMapOf(
                        "id" to church.id,
                        "name" to church.name,
                        "province" to church.province,
                        "cityMunicipality" to church.cityMunicipality,
                        "address" to church.address,
                        "pastorName" to church.pastorName,
                        "contactNumber" to church.contactNumber,
                        "worshipSchedule" to church.worshipSchedule,
                        "description" to church.description,
                        "latitude" to church.latitude,
                        "longitude" to church.longitude,
                        "isApproved" to church.isApproved,
                        "submittedBy" to church.submittedBy,
                        "timestamp" to church.timestamp,
                        "facebookUrl" to church.facebookUrl
                    )
                    churchesRef.document(docId).set(churchMap)
                }
            }

            // 2. Download other submitted churches
            churchesRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val coroutineScope = CoroutineScope(Dispatchers.IO)
                    coroutineScope.launch {
                        for (doc in querySnapshot.documents) {
                            try {
                                val name = doc.getString("name") ?: continue
                                val province = doc.getString("province") ?: ""
                                val cityMunicipality = doc.getString("cityMunicipality") ?: ""
                                val address = doc.getString("address") ?: ""
                                val pastorName = doc.getString("pastorName") ?: ""
                                val contactNumber = doc.getString("contactNumber") ?: ""
                                val worshipSchedule = doc.getString("worshipSchedule") ?: ""
                                val description = doc.getString("description") ?: ""
                                val latitude = doc.getDouble("latitude") ?: 14.5995
                                val longitude = doc.getDouble("longitude") ?: 120.9842
                                val isApproved = doc.getBoolean("isApproved") ?: false
                                val submittedBy = doc.getString("submittedBy") ?: "Cloud"
                                val timestamp = doc.getLong("timestamp") ?: 0L
                                val localId = doc.getLong("id")?.toInt() ?: 0
                                val facebookUrl = doc.getString("facebookUrl") ?: ""

                                val existingChurch = localChurches.find {
                                    (localId > 0 && it.id == localId) || (it.name == name && it.province == province)
                                }
                                if (existingChurch == null) {
                                    val downloadedChurch = BaptistChurch(
                                        name = name,
                                        province = province,
                                        cityMunicipality = cityMunicipality,
                                        address = address,
                                        pastorName = pastorName,
                                        contactNumber = contactNumber,
                                        worshipSchedule = worshipSchedule,
                                        description = description,
                                        latitude = latitude,
                                        longitude = longitude,
                                        isApproved = isApproved,
                                        submittedBy = submittedBy,
                                        timestamp = timestamp,
                                        facebookUrl = facebookUrl
                                    )
                                    insertOrUpdateLocal(downloadedChurch)
                                } else {
                                    if (isApproved && !existingChurch.isApproved) {
                                        insertOrUpdateLocal(existingChurch.copy(isApproved = true))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing downloaded church", e)
                            }
                        }
                        onComplete(true)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to download churches", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncChurchesWithFirebase", e)
            onComplete(false)
        }
    }
}
