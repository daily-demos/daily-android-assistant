package co.daily.bots.assistant.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Date
import java.util.Locale

// Wrapper for Compose stability
@Immutable
@JvmInline
value class Timestamp(
    val value: Instant
) : Comparable<Timestamp> {
    val isInPast: Boolean
        get() = value < Instant.now()

    val isInFuture: Boolean
        get() = value > Instant.now()

    fun toEpochMilli() = value.toEpochMilli()

    operator fun plus(duration: Duration) = Timestamp(value + duration)

    operator fun minus(duration: Duration) = Timestamp(value - duration)

    operator fun minus(rhs: Timestamp) = Duration.between(rhs.value, value)

    override operator fun compareTo(other: Timestamp) = value.compareTo(other.value)

    fun toISOString(): String = DateTimeFormatter.ISO_INSTANT.format(value)

    override fun toString() = toISOString()

    fun toDescriptiveLocalString(): String {

        fun toOrdinal(n: Int) = when {
            n in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }

        val zoneId = ZoneId.systemDefault()

        val zonedDateTime = value.atZone(zoneId)

        val formatter = DateTimeFormatterBuilder()
            .appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL_STANDALONE)
            .appendLiteral(' ')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral(toOrdinal(zonedDateTime.get(ChronoField.DAY_OF_MONTH)))
            .appendLiteral(' ')
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL_STANDALONE)
            .appendLiteral(' ')
            .appendValue(ChronoField.YEAR)
            .appendLiteral(", ")
            .appendPattern("h:mm")
            .appendText(ChronoField.AMPM_OF_DAY, TextStyle.SHORT)
            .appendLiteral(", ")
            .appendZoneText(TextStyle.SHORT)
            .toFormatter(Locale.US) // Adjust locale as needed

        return zonedDateTime.format(formatter)
    }

    companion object {
        fun now() = Timestamp(Instant.now())

        fun ofEpochMilli(value: Long) = Timestamp(Instant.ofEpochMilli(value))

        fun ofEpochSecs(value: Long) = ofEpochMilli(value * 1000)

        fun parse(value: CharSequence) = Timestamp(Instant.parse(value))

        fun from(date: Date) = Timestamp(date.toInstant())
    }
}

@Composable
fun formatTimer(duration: Duration): String {

    if (duration.seconds < 0) {
        return "0s"
    }

    val mins = duration.seconds / 60
    val secs = duration.seconds % 60

    return if (mins == 0L) {
        "${secs}s"
    } else {
        "${mins}m ${secs}s"
    }
}
