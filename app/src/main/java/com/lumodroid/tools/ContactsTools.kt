package com.lumodroid.tools

import android.content.Context
import android.provider.ContactsContract
import com.lumodroid.agent.Tool

class ContactsTool : Tool("list_contacts") {
    override val description = "List contacts from the device address book (requires READ_CONTACTS permission)."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "query" to mapOf("type" to "string", "description" to "Optional search filter")
        ),
        "required" to emptyList<String>(),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val query = args["query"] as? String
        return try {
            val uri = ContactsContract.Contacts.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
            )
            val selection = if (query != null)
                "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?"
            else null
            val selectionArgs = if (query != null) arrayOf("%$query%") else null
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, "display_name ASC LIMIT 100")
                ?: return "No contacts access"
            val results = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                val hasPhone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                var phones = ""
                if (hasPhone == "1") {
                    val pCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id), null
                    )
                    pCursor?.use { pc ->
                        val nums = mutableListOf<String>()
                        while (pc.moveToNext()) {
                            nums.add(pc.getString(0))
                        }
                        if (nums.isNotEmpty()) phones = " [${nums.joinToString(", ")}]"
                    }
                }
                results.add("$name$phones")
            }
            cursor.close()
            if (results.isEmpty()) "No contacts found${if (query != null) " matching '$query'" else ""}"
            else results.joinToString("\n")
        } catch (e: SecurityException) {
            "Permission denied: READ_CONTACTS not granted"
        } catch (e: Exception) {
            "Contacts error: ${e.message}"
        }
    }
}
