package com.example.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Church Activity Reminder"
        val content = intent.getStringExtra("content") ?: "Your church event is starting soon."
        NotificationHelper.showNotification(context, title, content)
    }
}
