package com.lumodroid.agent

import android.content.Context
import com.lumodroid.data.ToolDefinition

abstract class Tool(val name: String) {
    abstract val description: String
    abstract val parameters: Map<String, Any>

    fun definition(): ToolDefinition = ToolDefinition(
        function = com.lumodroid.data.FunctionDefinition(
            name = name,
            description = description,
            parameters = parameters,
        )
    )

    abstract suspend fun execute(args: Map<String, Any?>, context: Context): String
}
