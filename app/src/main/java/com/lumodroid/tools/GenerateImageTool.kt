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

private const val TAG = "GenerateImageTool"

class GenerateImageTool(
    private val baseUrl: String,
    private val apiKey: String,
) : Tool("generate_image") {
    override val description = "Generate an image from a text prompt using the Lumo AI image generation API. The image is returned as base64-encoded data that gets displayed to the user."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "prompt" to mapOf("type" to "string", "description" to "Detailed text description of the image to generate"),
            "style" to mapOf("type" to "string", "description" to "Optional style modifier (e.g. photorealistic, anime, oil painting, watercolor)"),
        ),
        "required" to listOf("prompt"),
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val prompt = args["prompt"] as? String ?: return "Error: missing 'prompt' parameter"
        val style = args["style"] as? String ?: ""
        val fullPrompt = if (style.isNotBlank()) "$prompt, $style" else prompt
        Log.d(TAG, "Generating image: prompt='$fullPrompt'")
        var chunkCount = 0
        var imageEventReceived = false

        return try {
            val payload = mapOf(
                "model" to "auto",
                "messages" to listOf(
                    mapOf("role" to "user", "content" to fullPrompt)
                ),
                "stream" to true,
                "tools" to listOf(listOf("generate_image"), listOf("edit_image")),
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
                    return "Image generation failed: HTTP ${response.code} - $errBody"
                }

                val body = response.body ?: return "Empty response from server"
                val reader = BufferedReader(InputStreamReader(body.byteStream()))

                while (true) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                    if (!trimmed.startsWith("data:")) continue
                    val jsonStr = trimmed.substring(5).trim()
                    if (jsonStr == "[DONE]") {
                        Log.d(TAG, "Stream DONE. Total chunks: $chunkCount, imageEvent: $imageEventReceived, contentLen: ${contentBuilder.length}")
                        break
                    }

                    chunkCount++

                    try {
                        val chunk = gson.fromJson(jsonStr, Map::class.java) as? Map<String, Any?> ?: continue

                        if (chunkCount <= 3) {
                            Log.d(TAG, "Chunk $chunkCount keys: ${chunk.keys}")
                        }

                        val obj = chunk["object"] as? String
                        if (obj == "lumo.image_data") {
                            imageEventReceived = true
                            Log.d(TAG, "Got lumo.image_data event!")
                            val image = chunk["image"] as? Map<String, Any?>
                            val data = image?.get("data") as? String
                            if (data != null) {
                                Log.d(TAG, "Image data length: ${data.length}")
                                imageDeferred.complete(data)
                                continue
                            } else {
                                Log.w(TAG, "image_data event but no data field. image keys: ${image?.keys}")
                            }
                        }

                        val choices = chunk["choices"] as? List<Map<String, Any?>>
                        choices?.forEach { choice ->
                            val delta = choice["delta"] as? Map<String, Any?>
                            val content = delta?.get("content") as? String
                            if (content != null) {
                                contentBuilder.append(content)
                                if (chunkCount <= 5 || chunkCount % 50 == 0) {
                                    Log.d(TAG, "Chunk $chunkCount content len so far: ${contentBuilder.length}")
                                }
                                if (content.contains("base64,") && !imageDeferred.isCompleted) {
                                    Log.d(TAG, "Found base64 in content at chunk $chunkCount")
                                    val startIdx = content.indexOf("base64,") + 7
                                    if (startIdx < content.length) {
                                        val b64 = content.substring(startIdx)
                                            .replace(Regex("[^a-zA-Z0-9+/=].*$"), "")
                                        Log.d(TAG, "Extracted b64 length: ${b64.length}")
                                        if (b64.length > 100) {
                                            imageDeferred.complete(b64)
                                        }
                                    }
                                }
                            }
                            val finishReason = choice["finish_reason"] as? String
                            if (finishReason != null) {
                                Log.d(TAG, "Finish reason: $finishReason")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Parse error at chunk $chunkCount: ${e.message}")
                    }
                }
                Log.d(TAG, "Stream ended. Chunks: $chunkCount, imageEvent: $imageEventReceived, contentLen: ${contentBuilder.length}")
            }

            val imageB64 = if (imageDeferred.isCompleted) imageDeferred.await() else null
            Log.d(TAG, "Final: imageB64=${if (imageB64 != null) "len=${imageB64.length}" else "null"}, contentLen=${contentBuilder.length}")
            if (imageB64 != null && imageB64.length > 100) {
                "IMAGE_DATA:$imageB64"
            } else if (contentBuilder.isNotEmpty()) {
                Log.d(TAG, "Returning content instead of image. First 200 chars: ${contentBuilder.take(200)}")
                contentBuilder.toString().take(500)
            } else {
                "Image generation returned no image data."
            }
        } catch (e: Exception) {
            "Image generation error: ${e.message}"
        }
    }
}
