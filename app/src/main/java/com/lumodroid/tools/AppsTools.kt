package com.lumodroid.tools

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.lumodroid.agent.Tool

class ListAppsTool : Tool("list_apps") {
    override val description = "List installed applications on the device."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "system_apps" to mapOf("type" to "boolean", "description" to "Include system apps (default: false)")
        ),
        "required" to emptyList<String>(),
    )

    @Suppress("DEPRECATION")
    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val includeSystem = args["system_apps"] as? Boolean ?: false
        return try {
            val pm = context.packageManager
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                pm.getInstalledApplications(0)
            }
            val results = mutableListOf<String>()
            for (app in apps) {
                if (!includeSystem && (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
                val name = pm.getApplicationLabel(app)
                val pkg = app.packageName
                results.add("$name ($pkg)")
            }
            results.sorted().joinToString("\n").take(8000)
        } catch (e: Exception) {
            "Apps error: ${e.message}"
        }
    }
}
