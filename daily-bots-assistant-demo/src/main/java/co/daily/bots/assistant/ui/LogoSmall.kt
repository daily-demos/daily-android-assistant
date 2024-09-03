package co.daily.bots.assistant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.bots.assistant.R
import co.daily.bots.assistant.ui.theme.Colors

@Composable
fun LogoSmall(modifier: Modifier = Modifier) {

    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .size(32.dp)
            .shadow(2.dp, shape)
            .border(1.dp, Colors.logoBorder, shape)
            .clip(shape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(20.dp, 22.dp),
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "RTVI"
        )
    }
}

@Composable
@Preview
private fun PreviewLogoSmall() {
    LogoSmall(Modifier)
}