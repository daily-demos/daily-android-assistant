package co.daily.bots.assistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import co.daily.bots.assistant.ChatLogEntry
import co.daily.bots.assistant.VoiceClientManager
import co.daily.bots.assistant.ui.theme.Colors
import co.daily.bots.assistant.ui.theme.TextStyles

@Composable
fun InCallLayout(voiceClientManager: VoiceClientManager) {

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (log, footer) = createRefs()

        val scrollState = rememberScrollState()

        LaunchedEffect(scrollState.maxValue) {
            scrollState.scrollTo(scrollState.maxValue)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(log) {
                    top.linkTo(parent.top)
                    bottom.linkTo(footer.top)
                    height = Dimension.fillToConstraints
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = voiceClientManager.chatLog.isNotEmpty(),
                enter = slideInVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .shadow(10.dp)
                        .background(Colors.lightGrey)
                        .padding(24.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    voiceClientManager.chatLog.forEach {
                        when (it) {
                            is ChatLogEntry.Bot -> ChatLogEntryText(
                                name = "Bot",
                                text = it.text,
                                isBot = true
                            )

                            is ChatLogEntry.FunctionCall -> ChatLogEntryFnCall(name = it.name, extraText = it.extraText.value)

                            is ChatLogEntry.User -> ChatLogEntryText(
                                name = "User",
                                text = it.text,
                                isBot = false
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(footer) {
                    bottom.linkTo(parent.bottom)
                }
                .background(Colors.activityBackground),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogoSmall()

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Daily Bots Assistant",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W700,
                    style = TextStyles.base,
                    color = Color.Black
                )
            }

            InCallFooter(
                onEndCall = voiceClientManager::stop,
                onToggleMic = voiceClientManager::toggleMic,
                isReady = voiceClientManager.botReady.value,
                isBotTalking = voiceClientManager.botIsTalking,
                botAudioLevel = voiceClientManager.botAudioLevel,
                isUserTalking = voiceClientManager.userIsTalking,
                userAudioLevel = voiceClientManager.userAudioLevel,
                userMicEnabled = voiceClientManager.mic.value
            )
        }
    }
}

@Composable
fun ChatLogEntryText(name: String, text: String, isBot: Boolean) {

    val color = if (isBot) {
        Color.Black
    } else {
        Colors.unmutedMicBackground
    }

    Column() {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.W700,
            style = TextStyles.base,
            color = color
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.W400,
            style = TextStyles.base,
            color = color
        )
    }
}

@Composable
fun ChatLogEntryFnCall(name: String, extraText: String?) {

    val color = Color(0xFFDA8E5E)

    Column() {
        Text(
            text = "Bot invoked function: $name",
            fontSize = 14.sp,
            fontWeight = FontWeight.W700,
            style = TextStyles.base,
            color = color
        )

        if (extraText != null) {

            Spacer(Modifier.height(4.dp))

            Text(
                text = extraText,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400,
                style = TextStyles.base,
                color = color
            )
        }
    }
}

@Composable
@Preview
private fun PreviewChatLogEntryText() {
    ChatLogEntryText(name = "Bot", text = "Hello world", isBot = true)
}