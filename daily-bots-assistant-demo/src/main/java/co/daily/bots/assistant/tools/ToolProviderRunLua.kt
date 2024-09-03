package co.daily.bots.assistant.tools

import ai.rtvi.client.types.Value
import androidx.compose.runtime.MutableState
import co.daily.bots.assistant.VoiceClientManager
import kotlin.concurrent.thread

class ToolProviderRunLua : ToolProvider {

    companion object {
        private const val TAG = "ToolProviderRunLua"
    }

    private val tool = object : Tool {

        override val definition = ToolDefinition(
            name = "run_lua",
            description = "Run arbitrary Lua code. Any output printed using `print` will be returned as the result.",
            inputSchema = ToolInputSchema(
                type = "object",
                properties = mapOf(
                    "code" to JsonSchema(
                        type = "string",
                        description = "The Lua code to execute"
                    ),
                ),
                required = listOf("code")
            )
        )

        override fun invoke(
            args: Map<String, Value>,
            extraText: MutableState<String?>,
            voiceClientManager: VoiceClientManager,
            onResult: (Value) -> Unit
        ) {
            val code = (args["code"] as? Value.Str)?.value ?: run {
                onResult(Value.Object("error" to Value.Str("parameter `code` must be present and of type `string`")))
                return
            }

            thread {
                extraText.value = "Executing code:\n$code".trim()
                val output = nativeRunCode(code).trim().takeIf(String::isNotEmpty) ?: "<no printed output>"
                onResult(Value.Str(output))
                extraText.value = (extraText.value + "\n\nExecution complete:\n$output").trim()
            }
        }
    }

    override val tools: List<Tool> = listOf(tool)

    external fun nativeRunCode(code: String): String
}