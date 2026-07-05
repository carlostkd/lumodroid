package com.lumodroid.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.lumodroid.agent.Tool

class NetworkInfoTool : Tool("get_network_info") {
    override val description = "Get detailed information about the device's current network connections including WiFi, mobile data, signal strength, IP address, carrier, and connection status."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>(),
        "required" to emptyList<String>(),
    )

    @SuppressLint("MissingPermission")
    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val sb = StringBuilder()

            if (caps == null) {
                sb.appendLine("=== Network Status ===")
                sb.appendLine("Status: No active network connection")
                sb.appendLine("The device appears to be offline.")
                return sb.toString().trimEnd()
            }

            val hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val hasMobile = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val hasEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            sb.appendLine("=== Network Status ===")
            sb.appendLine("Connected: YES")
            sb.appendLine("WiFi: ${if (hasWifi) "YES" else "NO"}")
            sb.appendLine("Mobile Data: ${if (hasMobile) "YES" else "NO"}")
            sb.appendLine("Ethernet: ${if (hasEthernet) "YES" else "NO"}")
            sb.appendLine("VPN: ${if (isVpn) "YES" else "NO"}")
            sb.appendLine("Upstream: ${caps.linkUpstreamBandwidthKbps ?: 0} Kbps")
            sb.appendLine("Downstream: ${caps.linkDownstreamBandwidthKbps ?: 0} Kbps")

            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                sb.appendLine("Internet: YES")
            }
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                sb.appendLine("Validated Internet: YES")
            }

            if (hasWifi) {
                sb.appendLine()
                sb.appendLine("=== WiFi Details ===")
                try {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val info: WifiInfo = wm.connectionInfo
                    sb.appendLine("SSID: ${info.ssid}")
                    sb.appendLine("BSSID: ${info.bssid}")
                    sb.appendLine("RSSI: ${info.rssi} dBm")
                    val signalLevel = WifiManager.calculateSignalLevel(info.rssi, 5)
                    sb.appendLine("Signal Level: $signalLevel/5")
                    sb.appendLine("Link Speed: ${info.linkSpeed} Mbps")
                    sb.appendLine("Frequency: ${if (info.frequency >= 5000) "5 GHz" else "2.4 GHz"} (${info.frequency} MHz)")
                    sb.appendLine("IP Address: ${formatIpAddress(info.ipAddress)}")
                    sb.appendLine("Network ID: ${info.networkId}")
                } catch (e: Exception) {
                    sb.appendLine("WiFi details unavailable: ${e.message}")
                }
            }

            if (hasMobile) {
                sb.appendLine()
                sb.appendLine("=== Mobile Data Details ===")
                try {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    sb.appendLine("Carrier: ${tm.networkOperatorName}")
                    sb.appendLine("SIM Operator: ${tm.simOperatorName}")
                    sb.appendLine("Network Type: ${getNetworkTypeName(tm)}")
                    sb.appendLine("Data State: ${getDataStateName(tm)}")
                    sb.appendLine("Data Roaming: ${isDataRoamingEnabled(context)}")
                    sb.appendLine("Country: ${tm.networkCountryIso}")
                    sb.appendLine("SIM Country: ${tm.simCountryIso}")
                } catch (e: Exception) {
                    sb.appendLine("Mobile data details unavailable: ${e.message}")
                }
            }

            sb.toString().trimEnd()
        } catch (e: Exception) {
            "Network info error: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun getNetworkTypeName(tm: TelephonyManager): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (tm.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                    TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                    TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                    TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                    TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                    TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                    TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                    TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
                    TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
                    TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
                    TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                    TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                    TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
                    TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                    else -> "Unknown (${tm.dataNetworkType})"
                }
            } else {
                @Suppress("DEPRECATION")
                when (tm.networkType) {
                    TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                    TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                    TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                    TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                    TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                    TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                    TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                    TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
                    TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
                    TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
                    TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                    TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                    TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
                    TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                    else -> "Unknown"
                }
            }
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    private fun getDataStateName(tm: TelephonyManager): String {
        return when (tm.dataState) {
            TelephonyManager.DATA_CONNECTED -> "Connected"
            TelephonyManager.DATA_CONNECTING -> "Connecting"
            TelephonyManager.DATA_DISCONNECTED -> "Disconnected"
            TelephonyManager.DATA_SUSPENDED -> "Suspended"
            else -> "Unknown"
        }
    }

    private fun isDataRoamingEnabled(context: Context): Boolean {
        return try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.DATA_ROAMING,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun formatIpAddress(ipAddress: Int): String {
        return String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
    }
}
