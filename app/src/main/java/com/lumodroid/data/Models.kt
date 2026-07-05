package com.lumodroid.data

data class ChatMessage(
    val role: String,
    val content: String? = null,
    val toolCalls: List<ToolCallData>? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    val imageData: String? = null,
    val reasoning: String? = null,
)

data class ToolCallData(
    val id: String,
    val type: String = "function",
    val function: FunctionCallData,
)

data class FunctionCallData(
    val name: String,
    val arguments: String,
)

data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition,
)

data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>,
)

data class ChatCompletionRequest(
    val model: String = "auto",
    val messages: List<RequestMessage>,
    val stream: Boolean = true,
    val tools: List<ToolDefinition>? = null,
)

data class RequestMessage(
    val role: String,
    val content: Any? = null,
    val tool_call_id: String? = null,
    val name: String? = null,
    val tool_calls: List<RequestToolCall>? = null,
)

data class RequestToolCall(
    val id: String,
    val type: String = "function",
    val function: RequestFunctionCall,
)

data class RequestFunctionCall(
    val name: String,
    val arguments: String,
)

data class StreamChunk(
    val model: String? = null,
    val `object`: String? = null,
    val image: ImageData? = null,
    val choices: List<Choice>? = null,
    val usage: UsageData? = null,
)

data class ImageData(
    val data: String? = null,
)

data class Choice(
    val delta: Delta? = null,
    val index: Int? = null,
    val finish_reason: String? = null,
)

data class Delta(
    val content: String? = null,
    val reasoning: String? = null,
    val tool_calls: List<DeltaToolCall>? = null,
)

data class DeltaToolCall(
    val index: Int,
    var id: String? = null,
    var type: String? = null,
    var function: DeltaFunction? = null,
)

data class DeltaFunction(
    var name: String? = null,
    var arguments: String? = null,
)

data class UsageData(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null,
)
