package com.lumodroid.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.lumodroid.agent.Tool

class DeviceInfoTool : Tool("get_device_info") {
    override val description = "Get device information: model, OS version, battery, storage, memory."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>(),
        "required" to emptyList<String>(),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        return try {
            val sb = StringBuilder()
            sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            sb.appendLine("Brand: ${Build.BRAND}")
            sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            sb.appendLine("Build: ${Build.DISPLAY}")

            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val level = intent.getIntExtra("level", -1)
                val scale = intent.getIntExtra("scale", -1)
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
                val plugged = intent.getIntExtra("plugged", 0)
                val charging = plugged != 0
                sb.appendLine("Battery: ${if (pct >= 0) "$pct%" else "Unknown"}${if (charging) " (charging)" else ""}")
            }

            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            val usedPct = if (total > 0) (total - free) * 100 / total else 0
            sb.appendLine("Storage: ${formatBytes(total)} total, ${formatBytes(free)} free ($usedPct% used)")

            val memInfo = android.app.ActivityManager.MemoryInfo()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.getMemoryInfo(memInfo)
            sb.appendLine("RAM: ${formatBytes(memInfo.totalMem)} total, ${formatBytes(memInfo.availMem)} available")

            sb.toString().trimEnd()
        } catch (e: Exception) {
            "Device info error: ${e.message}"
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }
}
