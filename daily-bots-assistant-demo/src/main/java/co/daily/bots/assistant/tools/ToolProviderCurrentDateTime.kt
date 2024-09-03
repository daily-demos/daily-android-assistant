package co.daily.bots.assistant.tools

import ai.rtvi.client.types.Value
import androidx.compose.runtime.MutableState
import co.daily.bots.assistant.VoiceClientManager
import co.daily.bots.assistant.utils.Timestamp

class ToolProviderCurrentDateTime : ToolProvider {

    companion object {
        private const val TAG = "ToolProviderCurrentDateTime"
    }

    private val tool = object : Tool {

        override val definition = ToolDefinition(
            name = "current_date_time",
            description = "Returns the current date and time as a human-readable string, in the user's current time zone.",
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
            onResult(
                Value.Object(
                    "current_date_time" to Value.Str(
                        Timestamp.now().toDescriptiveLocalString().apply {
                            extraText.value = this
                        }
                    )
                )
            )
        }
    }

    override val tools: List<Tool> = listOf(tool)
}