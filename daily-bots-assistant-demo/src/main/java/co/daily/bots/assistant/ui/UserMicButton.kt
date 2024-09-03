package co.daily.bots.assistant.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
fun UserMicButton(
    onClick: () -> Unit,
    micEnabled: Boolean,
    isTalking: State<Boolean>,
    audioLevel: FloatState,
) {
    val borderThickness by animateDpAsState(
        if (isTalking.value) {
            (12.dp * Math.pow(audioLevel.floatValue.toDouble(), 0.3).toFloat())
        } else {
            0.dp
        }
    )

    val color by animateColorAsState(
        if (!micEnabled) {
            Colors.mutedMicBackground
        } else if (isTalking.value) {
            Color(0xFF11C020)
        } else {
            Colors.unmutedMicBackground
        }
    )

    Box(
        Modifier
            .size(64.dp)
            .border(borderThickness, Color.White, CircleShape)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(30.dp),
            painter = painterResource(
                if (micEnabled) {
                    R.drawable.microphone
                } else {
                    R.drawable.microphone_off
                }
            ),
            tint = Color.White,
            contentDescription = if (micEnabled) {
                "Mute microphone"
            } else {
                "Unmute microphone"
            },
        )
    }
}

@Composable
@Preview
fun PreviewUserMicButton() {
    UserMicButton(
        onClick = {},
        micEnabled = true,
        isTalking = remember { mutableStateOf(false) },
        audioLevel = remember { mutableFloatStateOf(1.0f) }
    )
}

@Composable
@Preview
fun PreviewUserMicButtonMuted() {
    UserMicButton(
        onClick = {},
        micEnabled = false,
        isTalking = remember { mutableStateOf(false) },
        audioLevel = remember { mutableFloatStateOf(1.0f) }
    )
}

@Composable
@Preview
fun PreviewUserMicButtonActive() {
    UserMicButton(
        onClick = {},
        micEnabled = true,
        isTalking = remember { mutableStateOf(true) },
        audioLevel = remember { mutableFloatStateOf(0.5f) }
    )
}