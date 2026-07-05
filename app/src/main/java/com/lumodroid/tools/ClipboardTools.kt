package com.lumodroid.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.lumodroid.agent.Tool

class ClipboardReadTool : Tool("clipboard_read") {
    override val description = "Read the current content of the system clipboard."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>(),
        "required" to emptyList<String>(),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            if (clip == null || clip.itemCount == 0) return "Clipboard is empty"
            val item = clip.getItemAt(0)
            val text = item.text?.toString() ?: item.uri?.toString() ?: "Non-text clipboard data"
            text.take(2000)
        } catch (e: Exception) {
            "Clipboard error: ${e.message}"
        }
    }
}

class ClipboardWriteTool : Tool("clipboard_write") {
    override val description = "Write text to the system clipboard."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "text" to mapOf("type" to "string", "description" to "Text to copy to clipboard")
        ),
        "required" to listOf("text"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val text = args["text"] as? String ?: return "Error: missing 'text'"
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("lumodroid", text))
            "Copied ${text.length} characters to clipboard"
        } catch (e: Exception) {
            "Clipboard error: ${e.message}"
        }
    }
}
