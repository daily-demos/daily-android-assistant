package co.daily.bots.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.bots.assistant.R
import co.daily.bots.assistant.ui.theme.Colors

@Composable
fun EndCallButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Colors.mutedMicBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(30.dp),
            painter = painterResource(R.drawable.phone_hangup),
            tint = Color.White,
            contentDescription = null
        )
    }
}

@Composable
fun InCallFooter(
    onEndCall: () -> Unit,
    onToggleMic: () -> Unit,
    isReady: Boolean,
    isBotTalking: State<Boolean>,
    botAudioLevel: FloatState,
    isUserTalking: State<Boolean>,
    userAudioLevel: FloatState,
    userMicEnabled: Boolean
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BotIndicator(
            modifier = Modifier,
            isReady = isReady,
            isTalking = isBotTalking,
            audioLevel = botAudioLevel
        )

        Spacer(Modifier.weight(1f))

        UserMicButton(
            onClick = onToggleMic,
            micEnabled = userMicEnabled,
            isTalking = isUserTalking,
            audioLevel = userAudioLevel
        )

        EndCallButton(onClick = onEndCall)
    }
}

@Composable
@Preview
fun PreviewInCallFooter() {
    InCallFooter(
        onEndCall = {},
        onToggleMic = {},
        isReady = true,
        isBotTalking = remember { mutableStateOf(true) },
        botAudioLevel = remember { mutableFloatStateOf(1f) },
        isUserTalking = remember { mutableStateOf(true) },
        userAudioLevel = remember { mutableFloatStateOf(1f) },
        userMicEnabled = true
    )
}