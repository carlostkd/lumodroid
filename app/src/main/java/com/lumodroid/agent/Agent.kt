package com.lumodroid.agent

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lumodroid.api.LumoApi
import com.lumodroid.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

class Agent(
    private val context: Context,
    private val api: LumoApi,
    private val tools: List<Tool>,
) {
    private val gson = Gson()
    private val messages = mutableListOf<ChatMessage>()
    val attachedImages = mutableMapOf<String, String>() // ref -> base64
    @Volatile
    var isCancelled = false
        private set

    fun addSystemMessage(text: String) {
        messages.add(ChatMessage(role = "system", content = text))
    }

    fun cancel() {
        isCancelled = true
    }

    private fun checkCancelled() {
        if (isCancelled) throw kotlinx.coroutines.CancellationException("Generation cancelled by user")
    }

    suspend fun processUserMessage(
        text: String,
        onDelta: (String) -> Unit,
        onReasoning: (String) -> Unit,
        onToolCallStart: (String, String) -> Unit,
        onImageGenerated: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit,
        onToolResult: (String, String) -> Unit = { _, _ -> },
        imageData: String? = null,
        attachments: List<String> = emptyList(),
    ) {
        isCancelled = false
        val effectiveText = if (imageData != null) {
            val ref = "attached_image_${attachedImages.size + 1}"
            attachedImages[ref] = imageData
            val base = text.ifBlank { "(attached image)" }
            "$base\n[Image reference: $ref — pass this as image_base64 to edit_image to edit this image]"
        } else {
            text.ifBlank { "(attached file/image)" }
        }
        messages.add(ChatMessage(role = "user", content = effectiveText, imageData = imageData))
        agentLoop(onDelta, onReasoning, onToolCallStart, onImageGenerated, onError, onComplete, onToolResult)
    }

    private suspend fun agentLoop(
        onDelta: (String) -> Unit,
        onReasoning: (String) -> Unit,
        onToolCallStart: (String, String) -> Unit,
        onImageGenerated: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit,
        onToolResult: (String, String) -> Unit = { _, _ -> },
    ) {
        val toolDefs = tools.map { it.definition() }
        val reqMessages = messages.map { msg ->
            when (msg.role) {
                "tool" -> RequestMessage(
                    role = "tool",
                    content = msg.content,
                    tool_call_id = msg.toolCallId,
                    name = msg.name,
                )
                "assistant" -> RequestMessage(
                    role = "assistant",
                    content = msg.content,
                    tool_calls = msg.toolCalls?.map { tc ->
                        RequestToolCall(
                            id = tc.id,
                            type = tc.type,
                            function = RequestFunctionCall(tc.function.name, tc.function.arguments),
                        )
                    },
                )
                "user" -> if (msg.imageData != null) {
                    RequestMessage(
                        role = "user",
                        content = listOf(
                            mapOf("type" to "text", "text" to (msg.content ?: "(image)")),
                            mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,${msg.imageData}")),
                        )
                    )
                } else {
                    RequestMessage(role = msg.role, content = msg.content)
                }
                else -> RequestMessage(role = msg.role, content = msg.content)
            }
        }

        val currentToolCalls = mutableListOf<DeltaToolCall>()
        val contentBuilder = StringBuilder()
        val reasoningBuilder = StringBuilder()
        var finishReason: String? = null

        api.streamChat(
            request = ChatCompletionRequest(
                model = "auto",
                messages = reqMessages,
                stream = true,
                tools = toolDefs,
            ),
            onDelta = { event ->
                checkCancelled()
                when (event) {
                    is LumoApi.DeltaEvent.Content -> {
                        contentBuilder.append(event.text)
                        onDelta(event.text)
                    }
                    is LumoApi.DeltaEvent.Reasoning -> {
                        reasoningBuilder.append(event.text)
                        onReasoning(event.text)
                    }
                    is LumoApi.DeltaEvent.ToolCallDelta -> {
                        val existing = currentToolCalls.find { it.index == event.delta.index }
                        if (existing != null) {
                            if (event.delta.id != null) existing.id = event.delta.id
                            if (event.delta.type != null) existing.type = event.delta.type
                            event.delta.function?.let { dFunc ->
                                if (dFunc.name != null) {
                                    existing.function = existing.function ?: DeltaFunction()
                                    existing.function!!.name = (existing.function!!.name ?: "") + dFunc.name
                                }
                                if (dFunc.arguments != null) {
                                    existing.function = existing.function ?: DeltaFunction()
                                    existing.function!!.arguments = (existing.function!!.arguments ?: "") + dFunc.arguments
                                }
                            }
                        } else {
                            currentToolCalls.add(event.delta.copy(
                                id = event.delta.id ?: "",
                                function = event.delta.function?.let { f ->
                                    DeltaFunction(
                                        name = f.name ?: "",
                                        arguments = f.arguments ?: "",
                                    )
                                }
                            ))
                        }
                    }
                    is LumoApi.DeltaEvent.FinishReason -> {
                        finishReason = event.reason
                    }
                    else -> {}
                }
            },
            onImage = { b64 ->
                messages.add(ChatMessage(
                    role = "assistant",
                    imageData = b64,
                ))
                onImageGenerated(b64)
            },
            onDone = {
                checkCancelled()
                if (finishReason == "tool_calls" && currentToolCalls.isNotEmpty()) {
                    val tcList = currentToolCalls.map { tc ->
                        val functionName = tc.function?.name ?: ""
                        val functionArgs = tc.function?.arguments ?: "{}"
                        ToolCallData(
                            id = tc.id ?: "",
                            function = FunctionCallData(functionName, functionArgs),
                        )
                    }

                    messages.add(ChatMessage(
                        role = "assistant",
                        content = contentBuilder.toString().ifEmpty { null },
                        toolCalls = tcList,
                        reasoning = reasoningBuilder.toString().ifEmpty { null },
                    ))

                    runBlocking {
                        executeToolCalls(tcList, onToolCallStart, onToolResult, onError) {
                            agentLoop(onDelta, onReasoning, onToolCallStart, onImageGenerated, onError, onComplete, onToolResult)
                        }
                    }
                } else {
                    messages.add(ChatMessage(
                        role = "assistant",
                        content = contentBuilder.toString().ifEmpty { null },
                        reasoning = reasoningBuilder.toString().ifEmpty { null },
                    ))
                    onComplete()
                }
            },
            onError = { err ->
                onError(err)
                onComplete()
            },
        )
    }

    private suspend fun executeToolCalls(
        toolCalls: List<ToolCallData>,
        onToolCallStart: (String, String) -> Unit,
        onToolResult: (String, String) -> Unit,
        onError: (String) -> Unit,
        onComplete: suspend () -> Unit,
    ) {
        var remaining = toolCalls.size

        for (tc in toolCalls) {
            checkCancelled()
            val tool = tools.find { it.name == tc.function.name }
            if (tool == null) {
                val errMsg = "Unknown tool: ${tc.function.name}"
                messages.add(ChatMessage(
                    role = "tool",
                    content = errMsg,
                    toolCallId = tc.id,
                    name = tc.function.name,
                ))
                remaining--
                if (remaining == 0) onComplete()
                continue
            }

            onToolCallStart(tc.function.name, tc.function.arguments)
            var args: Map<String, Any?> = try {
                val type = object : TypeToken<Map<String, Any?>>() {}.type
                gson.fromJson(tc.function.arguments, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            // Resolve image_base64 reference (e.g. "attached_image_1") to actual base64
            val imgRef = args["image_base64"] as? String
            Log.d(TAG, "edit_image image_base64 arg: '${imgRef?.take(80)}' (len=${imgRef?.length}), refs=${attachedImages.keys}")
            if (imgRef != null) {
                when {
                    attachedImages.containsKey(imgRef) -> {
                        args = args.toMutableMap().apply { this["image_base64"] = attachedImages[imgRef] }
                    }
                    attachedImages.values.isNotEmpty() -> {
                        // LLM passed something that's not a ref but also not valid base64 image data;
                        // fall back to the most recently attached image
                        Log.w(TAG, "image_base64 '$imgRef' not a known ref, using last attached image")
                        args = args.toMutableMap().apply { this["image_base64"] = attachedImages.values.last() }
                    }
                }
            }

            val result: String
            try {
                result = tool.execute(args, context)
            } catch (e: Exception) {
                Log.e(TAG, "Tool ${tc.function.name} failed", e)
                val err = if (e.message != null) "Error: ${e.message}" else "Unknown error"
                messages.add(ChatMessage(
                    role = "tool",
                    content = err,
                    toolCallId = tc.id,
                    name = tc.function.name,
                ))
                onToolResult(tc.function.name, err)
                remaining--
                if (remaining == 0) onComplete()
                continue
            }

            onToolResult(tc.function.name, result)

            val llmResult = if (result.startsWith("IMAGE_DATA:")) {
                "Image generated successfully and displayed to the user."
            } else {
                result
            }

            messages.add(ChatMessage(
                role = "tool",
                content = llmResult,
                toolCallId = tc.id,
                name = tc.function.name,
            ))
            remaining--
            if (remaining == 0) onComplete()
        }
    }

    companion object {
        private const val TAG = "Agent"
    }
}
