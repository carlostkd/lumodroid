package com.lumodroid.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.lumodroid.agent.Tool

class LaunchAppTool : Tool("launch_app") {
    override val description = "Launch an app on the device by package name (e.g. com.whatsapp) or app label (e.g. 'YouTube'). Use list_apps to find available packages."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "package_name" to mapOf("type" to "string", "description" to "App package name (e.g. com.whatsapp)"),
            "app_label" to mapOf("type" to "string", "description" to "App display name (e.g. 'YouTube') — used if package_name is not provided"),
        ),
        "required" to emptyList<String>(),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val pkg = args["package_name"] as? String
        val label = args["app_label"] as? String
        if (pkg == null && label == null) return "Error: provide package_name or app_label"

        return try {
            val targetPkg = if (pkg != null) {
                pkg
            } else {
                val pm = context.packageManager
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val match = apps.find {
                    pm.getApplicationLabel(it).toString().equals(label, ignoreCase = true) ||
                    it.loadLabel(pm).toString().equals(label, ignoreCase = true)
                }
                match?.packageName ?: return "App not found: $label"
            }

            val intent = context.packageManager.getLaunchIntentForPackage(targetPkg)
                ?: return "Cannot launch $targetPkg (no launch intent)"
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Launched: $targetPkg"
        } catch (e: Exception) {
            "Launch error: ${e.message}"
        }
    }
}

class SetAlarmTool : Tool("set_alarm") {
    override val description = "Set an alarm on the device. Specify hour (0-23), minute (0-59), and optional label."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "hour" to mapOf("type" to "integer", "description" to "Hour in 24-hour format (0-23)"),
            "minute" to mapOf("type" to "integer", "description" to "Minute (0-59)"),
            "label" to mapOf("type" to "string", "description" to "Optional alarm label"),
        ),
        "required" to listOf("hour", "minute"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val hour = (args["hour"] as? Number)?.toInt() ?: return "Error: missing 'hour'"
        val minute = (args["minute"] as? Number)?.toInt() ?: return "Error: missing 'minute'"
        val label = args["label"] as? String ?: "Alarm"
        if (hour !in 0..23 || minute !in 0..59) return "Error: invalid time"

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Alarm set for %02d:%02d — %s".format(hour, minute, label)
        } catch (e: Exception) {
            "Alarm error: ${e.message}"
        }
    }
}

class SetTimerTool : Tool("set_timer") {
    override val description = "Set a countdown timer on the device. Specify seconds (and optional label)."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "seconds" to mapOf("type" to "integer", "description" to "Duration in seconds"),
            "label" to mapOf("type" to "string", "description" to "Optional timer label"),
        ),
        "required" to listOf("seconds"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val seconds = (args["seconds"] as? Number)?.toInt() ?: return "Error: missing 'seconds'"
        val label = args["label"] as? String ?: "Timer"

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Timer set for $seconds seconds — $label"
        } catch (e: Exception) {
            "Timer error: ${e.message}"
        }
    }
}

class CalendarEventTool : Tool("create_calendar_event") {
    override val description = "Create a calendar event. Specify title, start time (epoch millis), and optional end time, description, location."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "title" to mapOf("type" to "string", "description" to "Event title"),
            "begin_time" to mapOf("type" to "integer", "description" to "Start time in epoch milliseconds"),
            "end_time" to mapOf("type" to "integer", "description" to "End time in epoch milliseconds (optional)"),
            "description" to mapOf("type" to "string", "description" to "Event description (optional)"),
            "location" to mapOf("type" to "string", "description" to "Event location (optional)"),
        ),
        "required" to listOf("title", "begin_time"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val title = args["title"] as? String ?: return "Error: missing 'title'"
        val beginTime = (args["begin_time"] as? Number)?.toLong() ?: return "Error: missing 'begin_time'"
        val endTime = (args["end_time"] as? Number)?.toLong() ?: beginTime + 3600000
        val desc = args["description"] as? String ?: ""
        val loc = args["location"] as? String ?: ""

        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                putExtra(CalendarContract.Events.DESCRIPTION, desc)
                putExtra(CalendarContract.Events.EVENT_LOCATION, loc)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Calendar event created: $title"
        } catch (e: Exception) {
            "Calendar error: ${e.message}"
        }
    }
}

class OpenUrlTool : Tool("open_url") {
    override val description = "Open a URL in the device's default browser."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "url" to mapOf("type" to "string", "description" to "URL to open (e.g. https://example.com)"),
        ),
        "required" to listOf("url"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val url = args["url"] as? String ?: return "Error: missing 'url'"
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened URL: $url"
        } catch (e: Exception) {
            "Open URL error: ${e.message}"
        }
    }
}

class MakeCallTool : Tool("make_call") {
    override val description = "Make a phone call to a number. Requires CALL_PHONE permission."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "phone_number" to mapOf("type" to "string", "description" to "Phone number to call"),
        ),
        "required" to listOf("phone_number"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val phone = args["phone_number"] as? String ?: return "Error: missing 'phone_number'"
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // Fall back to dial pad without placing call
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                "Opened dialer for $phone (CALL_PHONE permission not granted — call not placed automatically)"
            } else {
                context.startActivity(intent)
                "Calling $phone..."
            }
        } catch (e: Exception) {
            "Call error: ${e.message}"
        }
    }
}

class OpenSettingsTool : Tool("open_device_settings") {
    override val description = "Open a specific Android settings page (wifi, bluetooth, location, notifications, etc.)"
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "page" to mapOf("type" to "string", "description" to "Settings page: wifi, bluetooth, location, notifications, display, sound, battery, apps, about"),
        ),
        "required" to listOf("page"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val page = args["page"] as? String ?: return "Error: missing 'page'"
        return try {
            val action = when (page.lowercase()) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                "notifications" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS else Settings.ACTION_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound" -> Settings.ACTION_SOUND_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "apps" -> Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
                "about" -> Settings.ACTION_DEVICE_INFO_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            "Opened settings: $page"
        } catch (e: Exception) {
            "Settings error: ${e.message}"
        }
    }
}

class ShareTextTool : Tool("share_text") {
    override val description = "Share text content via the Android share sheet (e.g. to messaging apps, email, etc.)"
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "text" to mapOf("type" to "string", "description" to "Text to share"),
            "title" to mapOf("type" to "string", "description" to "Optional title for the share dialog"),
        ),
        "required" to listOf("text"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val text = args["text"] as? String ?: return "Error: missing 'text'"
        val title = args["title"] as? String ?: "Share"
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            "Shared text: ${text.take(80)}"
        } catch (e: Exception) {
            "Share error: ${e.message}"
        }
    }
}

class TakeScreenshotTool : Tool("take_screenshot") {
    override val description = "Take a screenshot of the current screen. Note: requires the app to have accessibility or media projection permissions."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf<String, Any>(),
        "required" to emptyList<String>(),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        return "Screenshot requires Media Projection permission. This feature is not yet implemented."
    }
}
