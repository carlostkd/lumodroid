package com.lumodroid.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lumodroid.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

class LumoApi(val baseUrl: String, val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    fun streamChat(
        request: ChatCompletionRequest,
        onDelta: (DeltaEvent) -> Unit,
        onImage: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val url = "$baseUrl/chat/completions"
        val bodyJson = gson.toJson(request)
        Log.d(TAG, "Request body: $bodyJson")
        val requestBody = bodyJson.toRequestBody(jsonMediaType)
        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("x-pm-appversion", "web-lumo@2.0.0.1")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e(TAG, "Request failed", e)
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body ?: run {
                    onError("Empty response body")
                    return
                }

                if (!response.isSuccessful) {
                    try {
                        val errStr = body.string()
                        onError("HTTP ${response.code}: $errStr")
                    } catch (e: Exception) {
                        onError("HTTP ${response.code}")
                    }
                    return
                }

                try {
                    val reader = BufferedReader(InputStreamReader(body.byteStream()))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line?.trim() ?: continue
                        if (l.isEmpty() || l.startsWith(":")) continue
                        if (!l.startsWith("data:")) continue
                        val jsonStr = l.substring(5).trim()
                        if (jsonStr == "[DONE]") {
                            onDone()
                            return
                        }
                        try {
                            val chunk = gson.fromJson(jsonStr, StreamChunk::class.java)
                            if (chunk == null) continue
                            if (chunk.model != null) {
                                onDelta(DeltaEvent.ModelInfo(chunk.model))
                            }
                            if (chunk.`object` == "lumo.image_data" && chunk.image?.data != null) {
                                onImage(chunk.image.data)
                                continue
                            }
                            chunk.choices?.forEach { choice ->
                                val delta = choice.delta
                                if (delta != null) {
                                    if (delta.content != null) {
                                        onDelta(DeltaEvent.Content(delta.content))
                                    }
                                    if (delta.reasoning != null) {
                                        onDelta(DeltaEvent.Reasoning(delta.reasoning))
                                    }
                                    if (delta.tool_calls != null) {
                                        delta.tool_calls.forEach { tc ->
                                            onDelta(DeltaEvent.ToolCallDelta(tc))
                                        }
                                    }
                                }
                                if (choice.finish_reason != null) {
                                    onDelta(DeltaEvent.FinishReason(choice.finish_reason))
                                }
                            }
                            if (chunk.usage != null) {
                                onDelta(DeltaEvent.Usage(chunk.usage))
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse chunk: $l", e)
                        }
                    }
                    onDone()
                } catch (e: Exception) {
                    Log.e(TAG, "Stream reading failed", e)
                    onError("Stream error: ${e.message}")
                }
            }
        })
    }

    sealed class DeltaEvent {
        data class Content(val text: String) : DeltaEvent()
        data class Reasoning(val text: String) : DeltaEvent()
        data class ToolCallDelta(val delta: DeltaToolCall) : DeltaEvent()
        data class FinishReason(val reason: String) : DeltaEvent()
        data class ModelInfo(val model: String) : DeltaEvent()
        data class Usage(val data: UsageData) : DeltaEvent()
    }

    companion object {
        private const val TAG = "LumoApi"
    }
}
