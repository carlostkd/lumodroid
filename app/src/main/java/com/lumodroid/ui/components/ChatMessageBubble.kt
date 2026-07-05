package com.lumodroid.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumodroid.ui.theme.*
import com.lumodroid.ui.viewmodel.ChatUiMessage

@Composable
fun ChatMessageBubble(msg: ChatUiMessage, onImageClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    val isUser = msg.role == "user"
    val isTool = msg.role == "tool"
    val isAssistant = msg.role == "assistant"

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("LumoDroid", text))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        if (isUser) {
            Surface(
                shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
                color = Accent.copy(alpha = 0.85f),
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp, 10.dp)) {
                    if (msg.imageData != null) {
                        val bitmap = remember(msg.imageData) {
                            try {
                                val bytes = Base64.decode(msg.imageData, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Attached image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (msg.content != null) 8.dp else 0.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImageClick(msg.imageData) },
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    if (msg.content != null) {
                        SelectionContainer {
                            Text(
                                text = msg.content,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                    msg.attachments?.forEach { path ->
                        SelectionContainer {
                            Text(
                                text = "📎 $path",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        if (isAssistant) {
            Column(modifier = Modifier.widthIn(max = 320.dp)) {
                if (msg.reasoning != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Accent.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "REASONING",
                                color = AccentLight,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            SelectionContainer {
                                Text(
                                    text = msg.reasoning,
                                    color = AccentLight.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }
                }

                    if (msg.content != null && msg.imageData == null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            SelectionContainer {
                                Text(
                                    text = msg.content + if (msg.isStreaming) " ▊" else "",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(12.dp, 10.dp),
                                )
                            }
                        }
                    }

                if (msg.imageData != null) {
                    val bitmap = remember(msg.imageData) {
                        try {
                            val bytes = Base64.decode(msg.imageData, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Generated image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(msg.imageData) },
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = "[Failed to decode image]",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        if (isTool) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface,
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    msg.toolCalls?.forEach { (name, args) ->
                        Text(
                            text = "🛠 $name",
                            color = Amber,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        SelectionContainer {
                            Text(
                                text = args.take(300),
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    if (msg.content != null) {
                        SelectionContainer {
                            Text(
                                text = msg.content,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
