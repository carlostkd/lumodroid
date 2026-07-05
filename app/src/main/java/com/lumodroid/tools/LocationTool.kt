package com.lumodroid.tools

import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import com.lumodroid.agent.Tool
import java.util.Locale

class LocationTool : Tool("get_location") {
    override val description = "Get the device's current geographic location (requires location permission)."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>(),
        "required" to emptyList<String>(),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var bestLocation: android.location.Location? = null
            for (provider in providers) {
                try {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                        bestLocation = loc
                    }
                } catch (_: SecurityException) {}
            }
            if (bestLocation == null) return "Location unavailable (permissions not granted or GPS disabled)"
            val lat = bestLocation.latitude
            val lng = bestLocation.longitude
            val acc = bestLocation.accuracy
            val sb = StringBuilder()
            sb.appendLine("Latitude: $lat")
            sb.appendLine("Longitude: $lng")
            sb.appendLine("Accuracy: ${acc ?: "?"} meters")
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val addr = addresses[0]
                    sb.appendLine("Address: ${addr.getAddressLine(0)}")
                }
            } catch (_: Exception) {}
            sb.toString().trimEnd()
        } catch (e: SecurityException) {
            "Permission denied: Location permission not granted"
        } catch (e: Exception) {
            "Location error: ${e.message}"
        }
    }
}
