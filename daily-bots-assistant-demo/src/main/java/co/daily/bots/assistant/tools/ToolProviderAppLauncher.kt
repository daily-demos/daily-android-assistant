package co.daily.bots.assistant.tools

import ai.rtvi.client.types.Value
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.MutableState
import co.daily.bots.assistant.VoiceClientManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

private val JSON_INSTANCE = Json {
    ignoreUnknownKeys = true
}

class ToolProviderAppLauncher(context: Context) : ToolProvider {

    companion object {
        private const val TAG = "ToolProviderAppLauncher"
    }

    private val apps = ArrayList(run {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0).mapIndexed { index, resolveInfo ->
            AppInfo(
                index = index,
                appName = resolveInfo.loadLabel(pm).toString(),
                packageName = resolveInfo.activityInfo.packageName,
            )
        }
    })

    @Serializable
    data class AppInfo(val index: Int, val appName: String, val packageName: String)

    @Serializable
    data class AppListResult(val installedApps: List<AppInfo>)

    private val listApps = object : Tool {

        override val definition = ToolDefinition(
            name = "list_apps",
            description = "Returns a list of the apps installed on the user's device",
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
            extraText.value = "${apps.size} apps currently installed"

            onResult(
                JSON_INSTANCE.decodeFromJsonElement(
                    JSON_INSTANCE.encodeToJsonElement(
                        AppListResult(apps)
                    )
                )
            )
        }
    }

    private val launchApp = object : Tool {

        override val definition = ToolDefinition(
            name = "launch_app",
            description = "Launches an app on the device using its numeric index",
            inputSchema = ToolInputSchema(
                type = "object",
                properties = mapOf(
                    "index" to JsonSchema(
                        type = "number",
                        description = "The index of the app to launch"
                    ),
                ),
                required = listOf("index")
            )
        )

        override fun invoke(
            args: Map<String, Value>,
            extraText: MutableState<String?>,
            voiceClientManager: VoiceClientManager,
            onResult: (Value) -> Unit
        ) {
            val index = (args["index"] as? Value.Number)?.value ?: run {
                onResult.sendError("`index` must be present and of type `number`")
                return
            }

            val app = apps[index.toInt()] ?: run {
                onResult.sendError("app with index $index not found")
                return
            }

            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                context.startActivity(intent)
                onResult.sendSuccess()
            } else {
                onResult.sendError("failed to get app launch intent")
            }
        }
    }

    override val tools: List<Tool> = listOf(listApps, launchApp)
}