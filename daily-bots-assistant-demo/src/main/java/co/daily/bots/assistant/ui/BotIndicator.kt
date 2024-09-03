package co.daily.bots.assistant.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.bots.assistant.ui.theme.Colors

// TODO isError
@Composable
fun BotIndicator(
    modifier: Modifier,
    isReady: Boolean,
    isTalking: State<Boolean>,
    audioLevel: FloatState,
) {
    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        val color by animateColorAsState(if (isTalking.value || !isReady) {
            Color.Black
        } else {
            Colors.botIndicatorBackground
        })

        Box(
            Modifier
                .aspectRatio(1f)
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = isReady
            ) { isReadyVal ->
                if (isReadyVal) {
                    ListeningAnimation(
                        modifier = Modifier.fillMaxSize(),
                        active = isTalking.value,
                        level = audioLevel.floatValue,
                        color = Color.White
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White,
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = color
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun PreviewBotIndicator() {
    BotIndicator(
        modifier = Modifier,
        isReady = false,
        isTalking = remember { mutableStateOf(true) },
        audioLevel = remember { mutableFloatStateOf(1.0f) }
    )
}