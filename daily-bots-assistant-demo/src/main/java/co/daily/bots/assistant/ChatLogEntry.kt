package co.daily.bots.assistant

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State

@Immutable
sealed interface ChatLogEntry {

    @Immutable
    data class User(val text: String, val final: Boolean) : ChatLogEntry

    @Immutable
    data class Bot(val text: String) : ChatLogEntry

    @Immutable
    data class FunctionCall(val name: String, val extraText: State<String?>) : ChatLogEntry
}