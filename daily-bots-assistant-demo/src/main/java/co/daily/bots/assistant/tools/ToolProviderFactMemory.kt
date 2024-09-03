package co.daily.bots.assistant.tools

import ai.rtvi.client.types.Value
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import co.daily.bots.assistant.VoiceClientManager
import co.daily.bots.assistant.utils.DataFile
import co.daily.bots.assistant.utils.Timestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File

private val JSON_INSTANCE = Json {
    ignoreUnknownKeys = true
}

class ToolProviderFactMemory(private val context: Context) : ToolProvider {

    companion object {
        private const val TAG = "ToolProviderFactMemory"
    }

    private val file = DataFile(
        serializer = FactDb.serializer(),
        defaultValue = FactDb(nextId = 1, facts = emptyMap()),
        file = File(context.filesDir, "fact_memory")
    )

    // TODO deletion support

    private val toolStore = object : Tool {

        override val definition = ToolDefinition(
            name = "store_fact",
            description = "Remembers a fact, which can later be looked up using `lookup_fact`. Include several descriptive keywords so you can search for the fact in future. For example, if the user tells you they're putting the mustard in the cupboard above the oven, suitable keywords might be `[\"mustard\", \"location\", \"cupboard\", \"kitchen\"]`, and the `fact` field should include the full description: `\"User put the mustard in the cupboard above the oven\"`.",
            inputSchema = ToolInputSchema(
                type = "object",
                properties = mapOf(
                    "keywords" to JsonSchema(
                        type = "array",
                        items = JsonSchema(type = "string"),
                        description = "List of keywords with which this fact will be associated, for example `[\"mustard\", \"location\", \"cupboard\", \"kitchen\"]`."
                    ),
                    "fact" to JsonSchema(
                        type = "string",
                        description = "The full description of the fact. This will be returned from `lookup_fact` when searching with the relevant keywords."
                    ),
                    "overwrite_id" to JsonSchema(
                        type = "number",
                        description = "Optional. If this is set, overwrite the fact with the specified numeric ID with these new details. This can be used to update the existing keywords and description of a fact."
                    )
                ),
                required = listOf("keywords", "fact")
            )
        )

        override fun invoke(
            args: Map<String, Value>,
            extraText: MutableState<String?>,
            voiceClientManager: VoiceClientManager,
            onResult: (Value) -> Unit
        ) {
            try {
                val keywords = (args["keywords"] as? Value.Array)?.value?.map {
                    (it as? Value.Str)?.value?.lowercase()
                        ?: throw Exception("`keywords` array element type must be `string`")
                }
                    ?: throw Exception("`keywords` must be present, with type `array`")

                val fact = (args["fact"] as? Value.Str)?.value
                    ?: throw Exception("`fact` must be present and of type `string`")

                val overwriteId = args["overwrite_id"]?.let {
                    (it as? Value.Number)?.value?.toInt()
                        ?: throw Exception("`overwrite_id` must be an integer")
                }

                val existingFacts = file.contents ?: throw Exception("File not loaded")

                val newNextId = if (overwriteId == null) {
                    existingFacts.nextId + 1
                } else {
                    existingFacts.nextId
                }

                val id = overwriteId ?: existingFacts.nextId

                val entry = Fact(
                    id = id,
                    keywords = keywords.toSet(),
                    fact = fact,
                    timestamp = Timestamp.now().toDescriptiveLocalString()
                )

                extraText.value =
                    (overwriteId?.let { "Overwriting fact $it" } ?: "Adding new fact ${id}") +
                            ": \"$fact\"\nKeywords: $keywords"

                file.write(
                    FactDb(
                        nextId = newNextId,
                        facts = existingFacts.facts + (id to entry)
                    )
                )

                onResult(
                    Value.Object(
                        "result" to Value.Str("success"),
                        "id" to Value.Number(entry.id.toDouble())
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Exception when invoking store_fact", e)
                onResult(Value.Object("error" to Value.Str(e.toString())))
            }
        }
    }

    private val toolLookup = object : Tool {

        override val definition = ToolDefinition(
            name = "lookup_fact",
            description = "Returns a list of facts which were previous stored using `store_fact`, along with their IDs and timestamps. This will return facts matching any of the specified keywords. For example, to lookup where the user parked their car, you could specify keywords `[\"car\", \"vehicle\", \"parking\"]`.",
            inputSchema = ToolInputSchema(
                type = "object",
                properties = mapOf(
                    "keywords" to JsonSchema(
                        type = "array",
                        description = "List of keywords to search for, for example `[\"mustard\", \"location\", \"cupboard\", \"kitchen\"]`. If this is empty (`[]`), all facts will be returned with no filter.",
                        items = JsonSchema(type = "string")
                    ),
                ),
                required = listOf("keywords")
            )
        )

        override fun invoke(
            args: Map<String, Value>,
            extraText: MutableState<String?>,
            voiceClientManager: VoiceClientManager,
            onResult: (Value) -> Unit
        ) {
            try {
                val keywords = (args["keywords"] as? Value.Array)?.value?.map {
                    (it as? Value.Str)?.value?.lowercase()
                        ?: throw Exception("`keywords` array element type must be `string`")
                }
                    ?: throw Exception("`keywords` must be present, with type `array`")

                val existingFacts = file.contents ?: throw Exception("File not loaded")

                val entries = existingFacts.facts.values.filter { fact ->
                    keywords.isEmpty() || keywords.any { keyword ->
                        fact.keywords.contains(keyword)
                    }
                }

                extraText.value = "Retrieved ${entries.size} fact(s) matching keywords $keywords"

                val entriesJson = JSON_INSTANCE.encodeToJsonElement(entries)

                val entriesValue: Value = JSON_INSTANCE.decodeFromJsonElement(entriesJson)

                onResult(
                    Value.Object(
                        "count" to Value.Number(entries.size.toDouble()),
                        "facts" to entriesValue
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Exception when invoking lookup_fact", e)
                onResult(Value.Object("error" to Value.Str(e.toString())))
            }
        }
    }

    override val tools: List<Tool> = listOf(toolStore, toolLookup)

    fun allKeywords(): Set<String> =
        (file.contents ?: throw Exception("File not loaded")).facts.values.map { it.keywords }
            .flatten().toSet()

    fun allKeywordsAsJson(): String = JSON_INSTANCE.encodeToString(
        JsonArray.serializer(),
        JsonArray(allKeywords().map { JsonPrimitive(it) })
    )

    fun onLoaded(callback: () -> Unit) {
        file.onLoaded(callback)
    }
}

@Serializable
private class FactDb(
    val nextId: Int,
    val facts: Map<Int, Fact>
)

@Serializable
private data class Fact(
    val id: Int,
    val keywords: Set<String>,
    val fact: String,
    val timestamp: String
)