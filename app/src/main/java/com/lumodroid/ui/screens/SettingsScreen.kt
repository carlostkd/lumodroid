package com.lumodroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumodroid.ui.theme.*
import com.lumodroid.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onAbout: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", color = Accent, fontFamily = FontFamily.Monospace)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = TextPrimary,
                ),
            )
        },
        containerColor = DarkBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text("API Key") },
                placeholder = { Text("Enter your Lumo API key", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text(
                    if (state.saved) "✓ Saved" else "Save",
                    fontFamily = FontFamily.Monospace,
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAbout,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent),
            ) {
                Text("About", fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Available Tools",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(8.dp))
            val tools = listOf(
                "web_search", "web_fetch", "generate_image", "edit_image",
                "read_sms", "send_sms", "contacts", "list_apps", "launch_app",
                "set_alarm", "set_timer", "create_calendar_event", "make_call",
                "open_url", "open_device_settings", "share_text",
                "read_file", "search_files", "list_files", "extract_pdf_text",
                "get_network_info", "get_device_info",
                "clipboard_read", "clipboard_write", "run_shell", "get_location",
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    tools.forEach { tool ->
                        Text(
                            text = "  • $tool",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = DarkSurfaceVariant,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextTertiary,
    cursorColor = Accent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkSurface,
    unfocusedContainerColor = DarkSurface,
)
