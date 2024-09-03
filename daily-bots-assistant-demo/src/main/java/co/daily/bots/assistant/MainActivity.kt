package co.daily.bots.assistant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.daily.bots.assistant.ui.AssistActivity
import co.daily.bots.assistant.ui.Logo
import co.daily.bots.assistant.ui.PermissionScreen
import co.daily.bots.assistant.ui.theme.Colors
import co.daily.bots.assistant.ui.theme.RTVIClientTheme
import co.daily.bots.assistant.ui.theme.TextStyles
import co.daily.bots.assistant.ui.theme.textFieldColors


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RTVIClientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        PermissionScreen()
                        MainSettings()
                    }
                }
            }
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
fun ColumnScope.SetupStep(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.padding(horizontal = 28.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.W700,
            style = TextStyles.base
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 18.sp,
            fontWeight = FontWeight.W700,
            style = TextStyles.base
        )
    }

    Column(
        Modifier.padding(vertical = 18.dp, horizontal = 28.dp)
    ) {
        content()
    }
}

@Composable
@Preview
private fun PreviewMainSettings() {
    MainSettings()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettings() {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Colors.mainSurfaceBackground)
        ) {
            Column {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 24.dp,
                            horizontal = 28.dp
                        )
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Logo(Modifier)
                    }

                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = "Daily Bots Assistant",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W700,
                        style = TextStyles.base
                    )
                }

                SetupStep(title = "Step 1", description = "Enter Daily Bots API key") {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Colors.textFieldBorder, RoundedCornerShape(12.dp)),
                        value = Preferences.apiKey.value ?: "",
                        onValueChange = { Preferences.apiKey.value = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                        ),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                SetupStep(title = "Step 2", description = "Enter OpenAI API key") {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Colors.textFieldBorder, RoundedCornerShape(12.dp)),
                        value = Preferences.openaiApiKey.value ?: "",
                        onValueChange = { Preferences.openaiApiKey.value = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                        ),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                SetupStep(title = "Step 3", description = "Set your system assistant app") {
                    Text(
                        text = "Select the Daily Bots assistant app in the Android settings, under \"Default digital assistant app\".",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400,
                        style = TextStyles.base
                    )

                    Spacer(Modifier.height(24.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        val activity = LocalContext.current

                        ConnectDialogButton(
                            onClick = {
                                (activity as? Activity)?.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                            },
                            text = "Open settings",
                            foreground = Color.White,
                            background = Colors.buttonNormal,
                            border = Colors.buttonNormal
                        )
                    }
                }

                SetupStep(title = "Step 4", description = "Start talking") {
                    Text(
                        text = "On most devices, long-pressing the Home button will start the assistant.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400,
                        style = TextStyles.base
                    )

                    Spacer(Modifier.height(36.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(
                            modifier = Modifier.weight(1f),
                        )

                        val activity = LocalContext.current

                        ConnectDialogButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                (activity as? Activity)?.startActivity(
                                    Intent(
                                        activity,
                                        AssistActivity::class.java
                                    )
                                )
                            },
                            text = "Talk now",
                            foreground = Color.White,
                            background = Colors.buttonNormal,
                            border = Colors.buttonNormal
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ConnectDialogButton(
    onClick: () -> Unit,
    text: String,
    foreground: Color,
    background: Color,
    border: Color,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier
            .border(1.dp, border, shape)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(icon),
                tint = foreground,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            color = foreground
        )
    }
}
