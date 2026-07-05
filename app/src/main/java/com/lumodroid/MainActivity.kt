package com.lumodroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumodroid.ui.screens.AboutScreen
import com.lumodroid.ui.screens.AnimatedSplashScreen
import com.lumodroid.ui.screens.ChatScreen
import com.lumodroid.ui.screens.SettingsScreen
import com.lumodroid.ui.theme.LumoDroidTheme
import com.lumodroid.ui.viewmodel.ChatViewModel
import com.lumodroid.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissions()
        requestAllFilesAccess()

        setContent {
            LumoDroidTheme {
                var showSplash by remember { mutableStateOf(true) }
                var showSettings by remember { mutableStateOf(false) }
                var showAbout by remember { mutableStateOf(false) }
                val chatViewModel: ChatViewModel = viewModel()
                val settingsViewModel: SettingsViewModel = viewModel()

                // Check if launched from widget or shortcut
                val widgetMessage = intent.getStringExtra("widget_message")
                    ?: intent.getStringExtra("prefill_message")
                val shortcutAction = intent.getStringExtra("shortcut_action")
                val quickAsk = intent.action == "WIDGET_QUICK_ASK" || shortcutAction == "quick_ask"

                if (widgetMessage != null && !showSplash) {
                    LaunchedEffect(widgetMessage) {
                        chatViewModel.sendMessage(widgetMessage)
                    }
                }
                if (quickAsk && !showSplash) {
                    LaunchedEffect(quickAsk) {
                        chatViewModel.updateInput("")
                    }
                }
                if (shortcutAction == "scan_network" && !showSplash) {
                    LaunchedEffect(shortcutAction) {
                        chatViewModel.sendMessage("Run a network scan: check connectivity, ping a few hosts, and summarize the results.")
                    }
                }
                if (shortcutAction == "check_sms" && !showSplash) {
                    LaunchedEffect(shortcutAction) {
                        chatViewModel.sendMessage("Read my recent SMS messages and summarize them.")
                    }
                }

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "splashTransition",
                ) { isSplash ->
                    if (isSplash) {
                        AnimatedSplashScreen(onFinished = { showSplash = false })
                    } else {
                        if (showAbout) {
                            AboutScreen(onBack = { showAbout = false })
                        } else if (showSettings) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { showSettings = false },
                                onAbout = { showAbout = true; showSettings = false },
                            )
                        } else {
                            ChatScreen(
                                viewModel = chatViewModel,
                                onOpenSettings = { showSettings = true },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.SEND_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.READ_CONTACTS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_WIFI_STATE") != PackageManager.PERMISSION_GRANTED)
            needed.add("android.permission.ACCESS_WIFI_STATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.CALL_PHONE)

        try {
            if (ContextCompat.checkSelfPermission(this, "com.termux.permission.RUN_COMMAND") != PackageManager.PERMISSION_GRANTED)
                needed.add("com.termux.permission.RUN_COMMAND")
        } catch (e: Exception) { }

        if (needed.isNotEmpty()) {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
                .launch(needed.toTypedArray())
        }
    }
}
