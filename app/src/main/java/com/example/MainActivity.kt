package com.example

import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.MainAppContainer
import com.example.ui.state.AppViewModel

class MainActivity : ComponentActivity() {
    private var crashError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request local push notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
        
        // Capture any asynchronous/uncaught thread crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ScribeCrash", "Uncaught exception in thread ${thread.name}", throwable)
            crashError = throwable.stackTraceToString()
            runOnUiThread {
                showCrashRecoveryScreen(crashError ?: "Unknown uncaught exception")
            }
        }

        try {
            enableEdgeToEdge()
            
            // Instantiate central database Repository from custom Application
            val app = application as BaptistNotesApplication
            val factory = AppViewModel.Factory(app, app.repository)
            val viewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]

            setContent {
                MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        MainAppContainer(viewModel = viewModel)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("ScribeCrash", "Crash during onCreate", t)
            showCrashRecoveryScreen(t.stackTraceToString())
        }
    }

    private fun showCrashRecoveryScreen(errorDetails: String) {
        try {
            setContent {
                MyApplicationTheme(darkTheme = true) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF1E1E1E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "App Recovery Console",
                                color = Color.Red,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "A diagnostic error was captured during launch. Please read or copy this error report:",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            SelectionContainer {
                                Text(
                                    text = errorDetails,
                                    color = Color.Yellow,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }
}
