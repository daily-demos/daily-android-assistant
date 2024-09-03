package co.daily.bots.assistant.tools

import ai.rtvi.client.types.Value
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.MutableState
import co.daily.bots.assistant.VoiceClientManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@OptIn(ExperimentalSerializationApi::class)
private val JSON_INSTANCE = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
}

object Tools {

    @SuppressLint("StaticFieldLeak")
    lateinit var toolProviderFactMemory: ToolProviderFactMemory
    private lateinit var toolProviderAppLauncher: ToolProviderAppLauncher
    private lateinit var toolProviderCurrentDateTime: ToolProviderCurrentDateTime
    private lateinit var toolProviderRunLua: ToolProviderRunLua
    private lateinit var toolProviderCalendar: ToolProviderCalendar
    private lateinit var toolProviderEndChat: ToolProviderEndChat

    private lateinit var tools: List<Tool>

    fun init(context: Context) {

        toolProviderFactMemory = ToolProviderFactMemory(context)
        toolProviderAppLauncher = ToolProviderAppLauncher(context)
        toolProviderCurrentDateTime = ToolProviderCurrentDateTime()
        toolProviderRunLua = ToolProviderRunLua()
        toolProviderCalendar = ToolProviderCalendar(context)
        toolProviderEndChat = ToolProviderEndChat()

        tools = listOf(
            toolProviderFactMemory,
            toolProviderAppLauncher,
            toolProviderCurrentDateTime,
            toolProviderRunLua,
            toolProviderCalendar,
            toolProviderEndChat
        ).map { it.tools }.flatten()
    }

    fun invoke(
        name: String,
        args: Map<String, Value>,
        extraText: MutableState<String?>,
        voiceClientManager: VoiceClientManager,
        onResult: (Value) -> Unit
    ) {

        tools.firstOrNull { it.definition.name == name }?.let { tool ->
            tool.invoke(args, extraText, voiceClientManager, onResult)
            return
        }

        onResult(Value.Object("error" to Value.Str("no tool found matching '$name'")))
    }

    fun toolDefinitions(): List<Value> = tools.map {
        val toolJson = JSON_INSTANCE.encodeToJsonElement(it.definition)
        JSON_INSTANCE.decodeFromJsonElement(toolJson)
    }

    fun toolDefinitionsOpenAI(): List<Value> = tools.map {
        val toolJson = JSON_INSTANCE.encodeToJsonElement(it.definition.convertToOpenAI())
        JSON_INSTANCE.decodeFromJsonElement(toolJson)
    }
}

interface ToolProvider {
    val tools: List<Tool>
}

interface Tool {
    val definition: ToolDefinition

    fun invoke(
        args: Map<String, Value>,
        extraText: MutableState<String?>,
        voiceClientManager: VoiceClientManager,
        onResult: (Value) -> Unit
    )
}

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    @SerialName("input_schema")
    val inputSchema: ToolInputSchema
) {
    fun convertToOpenAI() = OpenAIToolDefinition(
        type = "function",
        function = OpenAIToolFunctionDefinition(
            name = name,
            description = description,
            parameters = inputSchema
        )
    )
}

@Serializable
data class ToolInputSchema(
    val type: String, // Should be set to "object"
    val properties: Map<String, JsonSchema>? = null,
    val required: List<String>? = null,
)

@Serializable
data class JsonSchema(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null,
    val items: JsonSchema? = null
)

@Serializable
data class OpenAIToolFunctionDefinition(
    val name: String,
    val description: String,
    @SerialName("parameters")
    val parameters: ToolInputSchema
)

@Serializable
data class OpenAIToolDefinition(
    val type: String,
    val function: OpenAIToolFunctionDefinition
)

fun ((Value) -> Unit).sendError(msg: String) {
    this(Value.Object("error" to Value.Str(msg)))
}

fun ((Value) -> Unit).sendSuccess() {
    this(Value.Object("success" to Value.Bool(true)))
}

inline fun <reified T> ((Value) -> Unit).sendSerializable(value: T) {
    this(Json.decodeFromJsonElement(Json.encodeToJsonElement(value)))
}