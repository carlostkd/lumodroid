package com.lumodroid.tools

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.lumodroid.agent.Tool
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

private const val TAG = "ImageEditorTool"

class ImageEditorTool(
    private val baseUrl: String,
    private val apiKey: String,
) : Tool("edit_image") {
    override val description = "Edit an image using AI. Pass image_base64 (the reference like 'attached_image_1' or raw base64) and instructions describing what to change (e.g. 'add a red hat on the cat', 'make the sky blue', 'remove the person'). Returns IMAGE_DATA:<base64> for display."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "image_base64" to mapOf("type" to "string", "description" to "Reference like 'attached_image_1' or raw base64 image data"),
            "instructions" to mapOf("type" to "string", "description" to "Natural language description of what to edit (e.g. 'add a red hat on the cat', 'change background to beach', 'remove the person')"),
        ),
        "required" to listOf("image_base64", "instructions"),
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val instructions = args["instructions"] as? String ?: return "Error: missing 'instructions'"
        val imageBase64 = args["image_base64"] as? String ?: return "Error: missing 'image_base64'"

        Log.d(TAG, "Editing image: instructions='$instructions', b64_len=${imageBase64.length}")

        return try {
            val payload = mapOf(
                "model" to "auto",
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to listOf(
                            mapOf("type" to "text", "text" to instructions),
                            mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$imageBase64")),
                        )
                    )
                ),
                "stream" to true,
                "tools" to listOf(listOf("edit_image")),
            )

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("x-pm-appversion", "web-lumo@2.0.0.1")
                .addHeader("Content-Type", "application/json")
                .build()

            val imageDeferred = CompletableDeferred<String?>()
            val contentBuilder = StringBuilder()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Response: HTTP ${response.code}")
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "HTTP error body: $errBody")
                    return "Image edit failed: HTTP ${response.code} - $errBody"
                }

                val body = response.body ?: return "Empty response from server"
                val reader = BufferedReader(InputStreamReader(body.byteStream()))

                while (true) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                    if (!trimmed.startsWith("data:")) continue
                    val jsonStr = trimmed.substring(5).trim()
                    if (jsonStr == "[DONE]") break

                    try {
                        val chunk = gson.fromJson(jsonStr, Map::class.java) as? Map<String, Any?> ?: continue

                        val obj = chunk["object"] as? String
                        if (obj == "lumo.image_data") {
                            Log.d(TAG, "Got lumo.image_data event!")
                            val image = chunk["image"] as? Map<String, Any?>
                            val data = image?.get("data") as? String
                            if (data != null) {
                                Log.d(TAG, "Image data length: ${data.length}")
                                imageDeferred.complete(data)
                                continue
                            }
                        }

                        val choices = chunk["choices"] as? List<Map<String, Any?>>
                        choices?.forEach { choice ->
                            val delta = choice["delta"] as? Map<String, Any?>
                            val content = delta?.get("content") as? String
                            if (content != null) {
                                contentBuilder.append(content)
                                if (content.contains("base64,") && !imageDeferred.isCompleted) {
                                    val startIdx = content.indexOf("base64,") + 7
                                    if (startIdx < content.length) {
                                        val b64 = content.substring(startIdx)
                                            .replace(Regex("[^a-zA-Z0-9+/=].*$"), "")
                                        if (b64.length > 100) {
                                            imageDeferred.complete(b64)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Parse error: ${e.message}")
                    }
                }
            }

            val imageB64 = if (imageDeferred.isCompleted) imageDeferred.await() else null
            Log.d(TAG, "Final: imageB64=${if (imageB64 != null) "len=${imageB64.length}" else "null"}")
            if (imageB64 != null && imageB64.length > 100) {
                "IMAGE_DATA:$imageB64"
            } else if (contentBuilder.isNotEmpty()) {
                contentBuilder.toString().take(500)
            } else {
                "Image edit returned no image data."
            }
        } catch (e: Exception) {
            "Image edit error: ${e.message}"
        }
    }
}
