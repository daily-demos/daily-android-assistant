package co.daily.bots.assistant.ui

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import co.daily.bots.assistant.ConfigConstants
import co.daily.bots.assistant.Preferences
import co.daily.bots.assistant.R
import co.daily.bots.assistant.VoiceClientManager
import co.daily.bots.assistant.tools.Tools
import co.daily.bots.assistant.ui.theme.RTVIClientTheme
import co.daily.bots.assistant.ui.theme.TextStyles
import java.util.concurrent.atomic.AtomicBoolean

class AssistActivity : ComponentActivity() {

    private val mainThread = Handler(Looper.getMainLooper())

    private var voiceClientManager: VoiceClientManager? = null

    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        mainThread.postDelayed({
            playAudio(this, R.raw.ready) {
            }
        }, 100)

        mainThread.postDelayed({
            playAudio(this, R.raw.australian_woman_intro) {
                voiceClientManager?.enableMic(true)
            }
        }, 1500)

        var sessionEnded by mutableStateOf(false)

        fun onEndSession() {
            mainThread.postDelayed({ sessionEnded = true }, 1000)
        }

        voiceClientManager = VoiceClientManager(this, ::onEndSession).apply {

            val backendUrl = Preferences.backendUrl.value
            val apiKey = Preferences.apiKey.value

            Tools.toolProviderFactMemory.onLoaded {
                // TODO move all this into VCM
                start(
                    baseUrl = backendUrl ?: ConfigConstants.DefaultBackend,
                    apiKey = apiKey,
                    initOptions = VoiceClientManager.InitOptions(
                        botProfile = ConfigConstants.botProfiles.default,
                        ttsProvider = ConfigConstants.Cartesia,
                        llmProvider = ConfigConstants.OpenAI,
                        sttProvider = ConfigConstants.Deepgram
                    ),
                    runtimeOptions = VoiceClientManager.RuntimeOptions(
                        ttsVoice = ConfigConstants.Cartesia.voices.default,
                        llmModel = ConfigConstants.OpenAI.models.default,
                        sttModel = ConfigConstants.Deepgram.models.default,
                        sttLanguage = ConfigConstants.Deepgram.English
                    )
                )
            }
        }

        setContent {

            LaunchedEffect(voiceClientManager?.errors?.size, sessionEnded) {
                if (sessionEnded && voiceClientManager?.errors?.isEmpty() != false) {
                    finish()
                }
            }

            RTVIClientTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentColor = Color.Transparent
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        PermissionScreen()

                        voiceClientManager?.let { vcm ->
                            InCallLayout(vcm)

                            vcm.errors.firstOrNull()?.let { errorText ->

                                val dismiss: () -> Unit =
                                    { vcm.errors.removeFirst() }

                                AlertDialog(
                                    onDismissRequest = dismiss,
                                    confirmButton = {
                                        Button(onClick = dismiss) {
                                            Text(
                                                text = "OK",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.W700,
                                                color = Color.White,
                                                style = TextStyles.base
                                            )
                                        }
                                    },
                                    containerColor = Color.White,
                                    title = {
                                        Text(
                                            text = "Error",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.W600,
                                            color = Color.Black,
                                            style = TextStyles.base
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = errorText.message,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.W400,
                                            color = Color.Black,
                                            style = TextStyles.base
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceClientManager?.stop()
        voiceClientManager = null

        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
    }
}

private fun playAudio(context: Context, @RawRes data: Int, onComplete: () -> Unit) {

    val player = MediaPlayer()

    player.setVolume(1f, 1f)
    player.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
    player.setDataSource(context.resources.openRawResourceFd(data)!!)

    val completed = AtomicBoolean(false)

    player.setOnCompletionListener {
        if (!completed.getAndSet(true)) {
            onComplete()
            player.release()
        }
    }

    player.setOnPreparedListener {
        player.start()
    }

    player.prepareAsync()
}