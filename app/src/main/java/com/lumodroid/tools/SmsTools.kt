package com.lumodroid.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.lumodroid.agent.Tool

class ReadSmsTool : Tool("read_sms") {
    override val description = "Read SMS messages from the device inbox (requires READ_SMS permission)."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "limit" to mapOf("type" to "integer", "description" to "Max messages to return (default: 20)"),
            "query" to mapOf("type" to "string", "description" to "Optional text filter to search messages")
        ),
        "required" to emptyList<String>(),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return "Permission denied: READ_SMS not granted. Please grant SMS permission in Android Settings."
        }
        val limit = (args["limit"] as? Double)?.toInt() ?: 20
        val queryFilter = args["query"] as? String
        return try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("address", "body", "date", "thread_id")
            val selection = if (queryFilter != null) "body LIKE ?" else null
            val selectionArgs = if (queryFilter != null) arrayOf("%$queryFilter%") else null
            val cursor: Cursor = context.contentResolver.query(
                uri, projection, selection, selectionArgs, "date DESC LIMIT $limit"
            ) ?: return "No SMS access (permission denied or empty)"
            val results = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val addr = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                results.add("From: $addr | ${java.util.Date(date)}\n$body\n")
            }
            cursor.close()
            if (results.isEmpty()) "No SMS messages found"
            else results.joinToString("\n---\n")
        } catch (e: SecurityException) {
            "Permission denied: READ_SMS not granted"
        } catch (e: Exception) {
            "SMS error: ${e.message}"
        }
    }
}

class SendSmsTool : Tool("send_sms") {
    override val description = "Send an SMS message to a recipient (requires SEND_SMS permission)."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "phone_number" to mapOf("type" to "string", "description" to "Recipient phone number"),
            "message" to mapOf("type" to "string", "description" to "Message text")
        ),
        "required" to listOf("phone_number", "message"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return "Permission denied: SEND_SMS not granted. Please grant SMS permission in Android Settings."
        }
        val phone = args["phone_number"] as? String ?: return "Error: missing 'phone_number'"
        val msg = args["message"] as? String ?: return "Error: missing 'message'"
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(msg)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phone, null, ArrayList(parts), null, null)
            } else {
                smsManager.sendTextMessage(phone, null, msg, null, null)
            }
            "SMS sent to $phone: $msg"
        } catch (e: SecurityException) {
            "Permission denied: SEND_SMS not granted"
        } catch (e: Exception) {
            "SMS error: ${e.message}"
        }
    }
}
