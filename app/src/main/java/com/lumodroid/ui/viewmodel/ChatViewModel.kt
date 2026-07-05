package com.lumodroid.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumodroid.agent.Agent
import com.lumodroid.agent.Tool
import com.lumodroid.api.LumoApi
import com.lumodroid.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "ChatViewModel"

data class ChatUiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String? = null,
    val toolCalls: List<Pair<String, String>>? = null,
    val toolResult: String? = null,
    val imageData: String? = null,
    val reasoning: String? = null,
    val isStreaming: Boolean = false,
    val attachments: List<String>? = null,
)

data class ChatState(
    val messages: List<ChatUiMessage> = emptyList(),
    val isProcessing: Boolean = false,
    val inputText: String = "",
    val error: String? = null,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("lumodroid", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var agent: Agent? = null
    private var currentAssistantMsgId: String? = null
    private var currentProcessingJob: Job? = null

    private fun getApi(): LumoApi {
        val apiKey = prefs.getString("api_key", "")?.takeIf { it.isNotBlank() }
            ?: com.lumodroid.BuildConfig.DEFAULT_API_KEY
        val baseUrl = prefs.getString("base_url", "https://lumo.proton.me/api/ai/v1")
            ?: "https://lumo.proton.me/api/ai/v1"
        return LumoApi(baseUrl.trimEnd('/'), apiKey)
    }

    private fun getTools(context: Context, api: LumoApi): List<Tool> = listOf(
        GenerateImageTool(api.baseUrl, api.apiKey),
        WebSearchTool(),
        WebFetchTool(),
        ListFilesTool(),
        SearchFilesTool(),
        ReadFileTool(),
        PdfExtractorTool(),
        ImageEditorTool(api.baseUrl, api.apiKey),
        ReadSmsTool(),
        SendSmsTool(),
        ContactsTool(),
        NetworkInfoTool(),
        ListAppsTool(),
        DeviceInfoTool(),
        ClipboardReadTool(),
        ClipboardWriteTool(),
        ShellTool(),
        LocationTool(),
        LaunchAppTool(),
        SetAlarmTool(),
        SetTimerTool(),
        CalendarEventTool(),
        OpenUrlTool(),
        MakeCallTool(),
        OpenSettingsTool(),
        ShareTextTool(),
    )

    private fun getOrCreateAgent(): Agent {
        agent?.let { return it }
        val ctx = getApplication<Application>()
        val api = getApi()
        val tools = getTools(ctx, api)
        val newAgent = Agent(ctx, api, tools)
        newAgent.addSystemMessage("You are LumoDroid, an AI assistant with full tool access on an Android device. You can act on behalf of the user like a real phone assistant.\n\nAvailable tools:\n- web_search: Search the web\n- web_fetch: Extract content from web pages\n- generate_image: Create a NEW image from a text prompt\n- edit_image: Edit an image the user sent (pass image_base64 reference like 'attached_image_1' and natural language instructions)\n- read_sms / send_sms: Read and send SMS messages\n- contacts: Search contacts\n- list_apps / launch_app: List installed apps and launch them by package name or label\n- set_alarm / set_timer: Set alarms and timers\n- create_calendar_event: Create calendar events\n- make_call: Make phone calls\n- open_url: Open URLs in browser\n- open_device_settings: Open Android settings pages (wifi, bluetooth, location, etc.)\n- share_text: Share text via the Android share sheet\n- read_file / search_files / list_files / extract_pdf_text: Access files on device\n- get_network_info / get_device_info: Get device and network information\n- clipboard_read / clipboard_write: Read and write clipboard\n- run_shell: Run shell commands. Advanced networking tools (dig, nslookup, whois, nmap, traceroute, curl, tcpdump, ssh, etc.) are automatically routed to Termux if installed. The command runs in a Termux session.\n- get_location: Get current GPS location\n\nWhen the user sends an image, you can see it directly. To edit it, call edit_image with image_base64='attached_image_1' and instructions in natural language.\n\nAlways use the appropriate tool to fulfill the user's request. Be proactive and helpful.")
        agent = newAgent
        return newAgent
    }

    fun sendMessage(text: String, attachments: List<String> = emptyList(), imageBase64: String? = null) {
        if (text.isBlank() && attachments.isEmpty() && imageBase64 == null) return

        val apiKey = prefs.getString("api_key", "")?.takeIf { it.isNotBlank() }
        if (apiKey.isNullOrBlank()) {
            _state.value = _state.value.copy(
                error = "No API key set. Open Settings (gear icon) to enter your Lumo API key.",
            )
            return
        }

        _state.value = _state.value.copy(inputText = "", isProcessing = true, error = null)

        val userMsg = ChatUiMessage(
            role = "user",
            content = text.ifBlank { "(attached file/image)" },
            imageData = imageBase64,
            attachments = attachments.takeIf { it.isNotEmpty() },
        )
        _state.value = _state.value.copy(messages = _state.value.messages + userMsg)

        val agent = getOrCreateAgent()
        currentAssistantMsgId = UUID.randomUUID().toString()

        val placeholder = ChatUiMessage(
            id = currentAssistantMsgId!!,
            role = "assistant",
            isStreaming = true,
        )
        _state.value = _state.value.copy(messages = _state.value.messages + placeholder)

        viewModelScope.launch(Dispatchers.IO) {
            currentProcessingJob = this.coroutineContext[Job]
            val contentBuf = StringBuilder()
            val reasoningBuf = StringBuilder()

            agent.processUserMessage(
                text = text,
                imageData = imageBase64,
                attachments = attachments,
                onDelta = { chunk ->
                    contentBuf.append(chunk)
                    updateStreamingMessage(
                        id = currentAssistantMsgId!!,
                        content = contentBuf.toString(),
                        reasoning = reasoningBuf.toString().ifEmpty { null },
                    )
                },
                onReasoning = { chunk ->
                    reasoningBuf.append(chunk)
                    updateStreamingMessage(
                        id = currentAssistantMsgId!!,
                        content = contentBuf.toString().ifEmpty { null },
                        reasoning = reasoningBuf.toString(),
                    )
                },
                onToolCallStart = { name, args ->
                    val toolMsg = ChatUiMessage(
                        role = "tool",
                        content = "Calling $name...",
                        toolCalls = listOf(name to args),
                    )
                    runBlocking(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            messages = _state.value.messages + toolMsg
                        )
                    }
                },
                onToolResult = { name, result ->
                    Log.d(TAG, "onToolResult: $name, result length=${result.length}")
                    if (result.startsWith("IMAGE_DATA:")) {
                        val b64 = result.removePrefix("IMAGE_DATA:")
                        Log.d(TAG, "Detected IMAGE_DATA, b64 length=${b64.length} first10=${b64.take(10)}")
                        val imgMsg = ChatUiMessage(
                            role = "assistant",
                            imageData = b64,
                            content = "Generated image",
                        )
                        runBlocking(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                messages = _state.value.messages + imgMsg
                            )
                        }
                    } else {
                        Log.d(TAG, "Regular tool result for $name")
                        val resultMsg = ChatUiMessage(
                            role = "tool",
                            content = "$name result:\n$result",
                            toolResult = result,
                        )
                        runBlocking(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                messages = _state.value.messages + resultMsg
                            )
                        }
                    }
                },
                onImageGenerated = { b64 ->
                    val imgMsg = ChatUiMessage(
                        role = "assistant",
                        imageData = b64,
                        content = "Generated image",
                    )
                    runBlocking(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            messages = _state.value.messages + imgMsg
                        )
                    }
                },
                onError = { err ->
                    runBlocking(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            error = err,
                            isProcessing = false,
                        )
                        finalizeMessage(id = currentAssistantMsgId!!, content = contentBuf.toString())
                    }
                },
                onComplete = {
                    runBlocking(Dispatchers.Main) {
                        _state.value = _state.value.copy(isProcessing = false)
                        finalizeMessage(
                            id = currentAssistantMsgId!!,
                            content = contentBuf.toString().ifEmpty { null },
                            reasoning = reasoningBuf.toString().ifEmpty { null },
                        )
                    }
                },
            )
        }
    }

    private fun updateStreamingMessage(id: String, content: String?, reasoning: String?) {
        val updated = _state.value.messages.map { msg ->
            if (msg.id == id) msg.copy(content = content, reasoning = reasoning, isStreaming = true)
            else msg
        }
        _state.value = _state.value.copy(messages = updated)
    }

    private fun finalizeMessage(id: String, content: String?, reasoning: String? = null) {
        val updated = _state.value.messages.map { msg ->
            if (msg.id == id) msg.copy(
                content = if (msg.imageData != null) null else content,
                reasoning = reasoning,
                isStreaming = false,
            )
            else msg
        }
        _state.value = _state.value.copy(messages = updated)
    }

    fun updateInput(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun cancelGeneration() {
        agent?.cancel()
        currentProcessingJob?.cancel()
        currentAssistantMsgId?.let { id ->
            val content = _state.value.messages.find { it.id == id }?.content
            finalizeMessage(
                id = id,
                content = content?.ifBlank { "*(cancelled)*" } ?: "*(cancelled)*",
            )
        }
        _state.value = _state.value.copy(isProcessing = false)
    }

    fun clearMessages() {
        agent = null
        _state.value = _state.value.copy(messages = emptyList(), error = null)
    }
}
