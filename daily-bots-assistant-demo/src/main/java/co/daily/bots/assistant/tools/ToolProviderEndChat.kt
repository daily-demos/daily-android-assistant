package co.daily.bots.assistant.tools

import ai.rtvi.client.types.Value
import androidx.compose.runtime.MutableState
import co.daily.bots.assistant.VoiceClientManager

class ToolProviderEndChat : ToolProvider {

    companion object {
        private const val TAG = "ToolProviderEndChat"
    }

    private val tool = object : Tool {

        override val definition = ToolDefinition(
            name = "end_chat",
            description = "Invoke this when the user thanks you, or otherwise indicates that the chat is over. Don't say anything before or after invoking this.",
            inputSchema = ToolInputSchema(
                type = "object",
                properties = mapOf(),
                required = listOf()
            )
        )

        override fun invoke(
            args: Map<String, Value>,
            extraText: MutableState<String?>,
            voiceClientManager: VoiceClientManager,
            onResult: (Value) -> Unit
        ) {
            voiceClientManager.stop()
        }
    }

    override val tools: List<Tool> = listOf(tool)
}