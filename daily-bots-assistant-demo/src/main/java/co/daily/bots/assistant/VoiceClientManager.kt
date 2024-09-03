package co.daily.bots.assistant

import ai.rtvi.client.VoiceClient
import ai.rtvi.client.VoiceClientOptions
import ai.rtvi.client.VoiceEventCallbacks
import ai.rtvi.client.daily.DailyVoiceClient
import ai.rtvi.client.helper.LLMFunctionCall
import ai.rtvi.client.helper.LLMHelper
import ai.rtvi.client.result.Future
import ai.rtvi.client.result.Result
import ai.rtvi.client.result.VoiceError
import ai.rtvi.client.types.ActionDescription
import ai.rtvi.client.types.Option
import ai.rtvi.client.types.Participant
import ai.rtvi.client.types.PipecatMetrics
import ai.rtvi.client.types.ServiceConfig
import ai.rtvi.client.types.ServiceRegistration
import ai.rtvi.client.types.Tracks
import ai.rtvi.client.types.Transcript
import ai.rtvi.client.types.TransportState
import ai.rtvi.client.types.Value
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import co.daily.bots.assistant.tools.Tools
import co.daily.bots.assistant.utils.Timestamp

@Immutable
data class Error(val message: String)

@Stable
class VoiceClientManager(private val context: Context, private var endCallback: (() -> Unit)?) {

    companion object {
        private const val TAG = "VoiceClientManager"
    }

    @Immutable
    data class InitOptions(
        val botProfile: BotProfile,
        val ttsProvider: TTSProvider,
        val llmProvider: LLMProvider,
        val sttProvider: STTProvider,
    )

    @Immutable
    data class RuntimeOptions(
        val ttsVoice: TTSOptionVoice,
        val llmModel: LLMOptionModel,
        val sttModel: STTOptionModel,
        val sttLanguage: STTOptionLanguage,
    )

    private val client = mutableStateOf<VoiceClient?>(null)

    val errors = mutableStateListOf<Error>()

    val state = mutableStateOf<TransportState?>(null)

    val actionDescriptions =
        mutableStateOf<Result<List<ActionDescription>, VoiceError>?>(null)

    val expiryTime = mutableStateOf<Timestamp?>(null)

    val botReady = mutableStateOf(false)
    val botIsTalking = mutableStateOf(false)
    val userIsTalking = mutableStateOf(false)
    val botAudioLevel = mutableFloatStateOf(0f)
    val userAudioLevel = mutableFloatStateOf(0f)

    val mic = mutableStateOf(false)
    val camera = mutableStateOf(false)
    val tracks = mutableStateOf<Tracks?>(null)

    val chatLog = SnapshotStateList<ChatLogEntry>()

    private var connectStartTime: Timestamp? = null

    private fun <E> Future<E, VoiceError>.displayErrors() = withErrorCallback {
        Log.e(TAG, "Future resolved with error: ${it.description}", it.exception)
        errors.add(Error(it.description))
    }

    fun start(
        baseUrl: String,
        apiKey: String?,
        initOptions: InitOptions,
        runtimeOptions: RuntimeOptions,
    ) {
        connectStartTime = Timestamp.now()

        if (client.value != null) {
            endCallback?.let { it() }
            return
        }

        if (Preferences.openaiApiKey.value.isNullOrBlank() || Preferences.apiKey.value.isNullOrBlank()) {
            errors.add(Error("Please ensure both API keys (Daily Bots and OpenAI) are set."))
            endCallback?.let { it() }
            return
        }

        val prompt = context.resources.openRawResource(R.raw.prompt).readBytes().decodeToString()
            .replace("\$FACT_KEYWORDS", Tools.toolProviderFactMemory.allKeywordsAsJson())
            .replace("\$START_TIME", Timestamp.now().toDescriptiveLocalString())

        val options = VoiceClientOptions(
            services = listOf(
                ServiceRegistration("tts", initOptions.ttsProvider.id),
                ServiceRegistration("llm", initOptions.llmProvider.id),
            ),
            config = listOf(
                ServiceConfig(
                    "tts", listOf(
                        Option("voice", runtimeOptions.ttsVoice.id)
                    )
                ),
                ServiceConfig(
                    "llm", listOf(
                        Option("model", runtimeOptions.llmModel.id),
                        Option(
                            "initial_messages", Value.Array(
                                Value.Object(
                                    "role" toStr "system",
                                    "content" toStr prompt
                                ),
                            )
                        ),
                        Option("tools", Tools.toolDefinitionsOpenAI())
                    )
                ),
            ),
            enableMic = false,
            enableCam = false,
            // Note: For security reasons, don't include your API key in a production
            // client app. See: https://docs.dailybots.ai/architecture
            customHeaders = apiKey
                ?.takeUnless { it.isEmpty() }
                ?.let { listOf("Authorization" to "Bearer $it") }
                ?: emptyList(),
            customBodyParams = listOf(
                "bot_profile" to Value.Str(initOptions.botProfile.id),
                "max_duration" to Value.Number(600.0),
                "api_keys" to Value.Object(
                    "openai" to (Preferences.openaiApiKey.value?.let(Value::Str) ?: Value.Null)
                )
            )
        )

        state.value = TransportState.Idle

        val llmHelper = LLMHelper(object : LLMHelper.Callbacks() {
            override fun onLLMFunctionCall(func: LLMFunctionCall, onResult: (Value) -> Unit) {
                Log.i(TAG, "[Function call] Received request: $func")

                val extraText = mutableStateOf<String?>(null)

                chatLog.add(
                    ChatLogEntry.FunctionCall(
                        name = func.functionName,
                        extraText = extraText
                    )
                )

                val args = (func.args as? Value.Object)?.value ?: run {
                    onResult(Value.Object("error" to Value.Str("`args` must be an object")))
                    return
                }

                Tools.invoke(func.functionName, args, extraText, this@VoiceClientManager) {
                    Log.i(TAG, "[Function call] Sending result: $it")
                    onResult(it)
                }
            }
        })

        val callbacks = object : VoiceEventCallbacks() {

            override fun onTransportStateChanged(state: TransportState) {
                this@VoiceClientManager.state.value = state
            }

            override fun onBackendError(message: String) {
                "Error from backend: $message".let {
                    Log.e(TAG, it)
                    errors.add(Error(it))
                }
            }

            override fun onBotReady(version: String, config: List<ServiceConfig>) {

                Log.i(TAG, "Bot ready. Version $version, config: $config")

                connectStartTime?.let {
                    val duration = Timestamp.now() - it

                    Log.i(TAG, "Took $duration to connect")
                }

                botReady.value = true

                client.value?.describeActions()?.withCallback {
                    actionDescriptions.value = it
                }
            }

            override fun onPipecatMetrics(data: PipecatMetrics) {
                Log.i(TAG, "Pipecat metrics: $data")
            }

            override fun onUserTranscript(data: Transcript) {
                Log.i(TAG, "User transcript: $data")

                val mostRecentEntry = chatLog.lastOrNull()

                if (mostRecentEntry is ChatLogEntry.User && !mostRecentEntry.final) {
                    chatLog.removeLast()
                }

                chatLog.add(ChatLogEntry.User(data.text.trim(), data.final))
            }

            override fun onBotTranscript(text: String) {
                Log.i(TAG, "Bot transcript: $text")

                val mostRecentEntry = chatLog.lastOrNull()

                if (mostRecentEntry is ChatLogEntry.Bot) {
                    chatLog.removeLast()
                    chatLog.add(
                        ChatLogEntry.Bot(
                            (mostRecentEntry.text + " " + text).replace(
                                "  ",
                                " "
                            ).trim()
                        )
                    )
                } else {
                    chatLog.add(ChatLogEntry.Bot(text.trim()))
                }
            }

            override fun onBotStartedSpeaking() {
                Log.i(TAG, "Bot started speaking")
                botIsTalking.value = true
            }

            override fun onBotStoppedSpeaking() {
                Log.i(TAG, "Bot stopped speaking")
                botIsTalking.value = false
            }

            override fun onUserStartedSpeaking() {
                Log.i(TAG, "User started speaking")
                userIsTalking.value = true
            }

            override fun onUserStoppedSpeaking() {
                Log.i(TAG, "User stopped speaking")
                userIsTalking.value = false
            }

            override fun onTracksUpdated(tracks: Tracks) {
                this@VoiceClientManager.tracks.value = tracks
            }

            override fun onInputsUpdated(camera: Boolean, mic: Boolean) {
                this@VoiceClientManager.camera.value = camera
                this@VoiceClientManager.mic.value = mic
            }

            override fun onConnected() {
                expiryTime.value = client.value?.expiry?.let(Timestamp::ofEpochSecs)
            }

            override fun onDisconnected() {
                expiryTime.value = null
                actionDescriptions.value = null
                botIsTalking.value = false
                userIsTalking.value = false
                state.value = null
                actionDescriptions.value = null
                botReady.value = false
                tracks.value = null

                client.value?.release()
                client.value = null

                endCallback?.let { it() }
                endCallback = null
            }

            override fun onUserAudioLevel(level: Float) {
                userAudioLevel.floatValue = level
            }

            override fun onRemoteAudioLevel(level: Float, participant: Participant) {
                botAudioLevel.floatValue = level
            }
        }

        val client = DailyVoiceClient(context, baseUrl, callbacks, options)

        client.registerHelper("llm", llmHelper)

        client.start().displayErrors().withErrorCallback {
            callbacks.onDisconnected()
        }

        this.client.value = client
    }

    fun enableCamera(enabled: Boolean) {
        client.value?.enableCam(enabled)?.displayErrors()
    }

    fun enableMic(enabled: Boolean) {
        client.value?.enableMic(enabled)?.displayErrors()
    }

    fun toggleCamera() = enableCamera(!camera.value)
    fun toggleMic() = enableMic(!mic.value)

    fun stop() {
        val voiceClient = client.value
        client.value = null

        voiceClient?.disconnect()?.displayErrors()?.withCallback {
            voiceClient.release()
        }
    }

    fun action(service: String, action: String, args: Map<String, Value>) =
        client.value?.action(
            service = service,
            action = action,
            arguments = args.map { Option(it.key, it.value) })?.displayErrors()
}

private infix fun String.toStr(value: String) = this to Value.Str(value)