package com.lumodroid.ui.screens

import com.lumodroid.R

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumodroid.ui.components.ChatMessageBubble
import com.lumodroid.ui.components.ZoomableImageDialog
import com.lumodroid.ui.theme.*
import com.lumodroid.ui.viewmodel.ChatViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var zoomedImage by remember { mutableStateOf<String?>(null) }
    var attachedFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var attachedImageB64 by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null && bytes.isNotEmpty()) {
                    attachedImageB64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (_: Exception) {}
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val displayName = cursor?.use {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && it.moveToFirst()) it.getString(nameIdx) else null
                } ?: "unknown_file"
                val tempFile = File(context.cacheDir, displayName)
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                attachedFiles = attachedFiles + tempFile.absolutePath
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    zoomedImage?.let { b64 ->
        ZoomableImageDialog(base64 = b64, onDismiss = { zoomedImage = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.app_logo),
                            contentDescription = "App icon",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("LumoDroid", fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearMessages() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = TextTertiary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextTertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Accent,
                ),
            )
        },
        containerColor = DarkBg,
        snackbarHost = {
            state.error?.let { err ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    containerColor = Red,
                    contentColor = TextPrimary,
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = TextPrimary)
                        }
                    },
                ) {
                    Text(err, fontSize = 12.sp)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.messages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LumoDroid",
                            color = Accent,
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Your Android Private Assistant",
                            color = Accent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Powered by Lumo",
                            color = TextTertiary,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Ask me to search the web, read files,\nextract PDFs, edit images, check network,\nsend SMS, or run shell commands.",
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 40.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        ChatMessageBubble(msg) { b64 -> zoomedImage = b64 }
                    }
                }
            }

            if (attachedFiles.isNotEmpty() || attachedImageB64 != null) {
                Surface(
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (attachedImageB64 != null) {
                            Text(
                                text = "🖼️ Image attached",
                                color = AccentLight,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            TextButton(onClick = { attachedImageB64 = null }) {
                                Text("Remove", color = TextTertiary, fontSize = 11.sp)
                            }
                        }
                        attachedFiles.forEach { path ->
                            val name = File(path).name
                            Text(
                                text = "📎 $name",
                                color = AccentLight,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        if (attachedFiles.isNotEmpty()) {
                            TextButton(onClick = { attachedFiles = emptyList() }) {
                                Text("Clear files", color = TextTertiary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 0.dp,
                color = DarkBg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { imagePicker.launch("image/*") },
                        enabled = !state.isProcessing,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Attach image", tint = Accent)
                    }
                    IconButton(
                        onClick = { filePicker.launch("*/*") },
                        enabled = !state.isProcessing,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach file", tint = Accent)
                    }
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.updateInput(it) },
                        placeholder = {
                            Text(
                                "Ask me anything…",
                                color = TextTertiary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isProcessing,
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            cursorColor = Accent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedPlaceholderColor = TextTertiary,
                            unfocusedPlaceholderColor = TextTertiary,
                        ),
                        textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (state.isProcessing) {
                                viewModel.cancelGeneration()
                            } else {
                                viewModel.sendMessage(state.inputText, attachedFiles, attachedImageB64)
                                attachedFiles = emptyList()
                                attachedImageB64 = null
                            }
                        },
                        enabled = state.isProcessing || (state.inputText.isNotBlank() || attachedFiles.isNotEmpty() || attachedImageB64 != null),
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (state.isProcessing) DarkSurfaceVariant else Accent,
                            disabledContainerColor = DarkSurfaceVariant,
                        ),
                    ) {
                        if (state.isProcessing) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = TextPrimary)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
