package co.daily.bots.assistant.tools

import ai.rtvi.client.types.Value
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import androidx.compose.runtime.MutableState
import co.daily.bots.assistant.VoiceClientManager
import co.daily.bots.assistant.utils.Timestamp
import kotlinx.serialization.Serializable
import java.time.Duration

class ToolProviderCalendar(context: Context) : ToolProvider {

    companion object {
        private const val TAG = "ToolProviderCalendar"
    }

    @Serializable
    data class CalendarEvent(
        val id: String,
        val title: String,
        val timeStart: String,
        val timeEnd: String,
        val calendarId: String,
    )

    @Serializable
    data class CalendarEventList(
        val events: List<CalendarEvent>
    )

    private val listCalendar = object : Tool {

        override val definition = ToolDefinition(
            name = "list_calendar",
            description = "Lists the users next 10 appointments on their calendar.",
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
            val eventsList = mutableListOf<CalendarEvent>()

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            )

            val now = Timestamp.now()

            // Workaround for calendar API excluding events which are happening today
            val thisTimeYesterday = now - Duration.ofDays(1)

            val selection = "(${CalendarContract.Events.DTSTART} >= ?)"
            val selectionArgs = arrayOf(thisTimeYesterday.toEpochMilli().toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT 50"

            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(CalendarContract.Events._ID)
                    val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                    val dtStartIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
                    val dtEndIndex = it.getColumnIndex(CalendarContract.Events.DTEND)
                    val calendarIdIndex =
                        it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

                    while (it.moveToNext() && eventsList.size < 10) {
                        val id = it.getLong(idIndex)
                        val title = it.getString(titleIndex) ?: "<null>"
                        val dtStart = it.getLong(dtStartIndex)
                        val dtEnd = it.getLong(dtEndIndex)
                        val calendarId = it.getString(calendarIdIndex)

                        if (dtStart >= now.toEpochMilli()) {
                            eventsList.add(
                                CalendarEvent(
                                    id = id.toString(),
                                    title = title,
                                    timeStart = Timestamp.ofEpochMilli(dtStart)
                                        .toDescriptiveLocalString() + " ($dtStart ms since UTC epoch)",
                                    timeEnd = Timestamp.ofEpochMilli(dtEnd)
                                        .toDescriptiveLocalString(),
                                    calendarId = calendarId
                                )
                            )
                        }
                    }
                }
            }

            onResult.sendSerializable(CalendarEventList(eventsList))
        }
    }

    override val tools: List<Tool> = listOf(listCalendar)
}