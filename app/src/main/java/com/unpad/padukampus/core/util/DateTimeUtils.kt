package com.unpad.padukampus.core.util

import com.unpad.padukampus.core.constants.AppConstants
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

object DateTimeUtils {
    val campusZone: ZoneId = ZoneId.of(AppConstants.CAMPUS_TIME_ZONE)
    private val inputFormat =
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)
    private val displayFormat = DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm 'WIB'")

    fun parseDeadline(text: String): Long =
        LocalDateTime.parse(text.trim(), inputFormat).atZone(campusZone).toInstant().toEpochMilli()

    fun formatDeadline(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(campusZone).format(displayFormat)

    fun deadlineInput(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(campusZone).format(inputFormat)

    fun today(clock: Clock = Clock.systemUTC()): LocalDate =
        clock.instant().atZone(campusZone).toLocalDate()

    fun localTime(clock: Clock = Clock.systemUTC()): LocalTime =
        clock.instant().atZone(campusZone).toLocalTime()

    fun parseClassTime(text: String): LocalTime {
        require(Regex("\\d{2}:\\d{2}").matches(text)) { "Jam harus berformat HH:mm" }
        return LocalTime.parse(text)
    }

    fun reminderDelay(deadline: Long, minutesBefore: Int, now: Long): Long? {
        require(minutesBefore in 0..AppConstants.MAX_REMINDER_MINUTES) {
            "Menit pengingat tidak valid"
        }
        if (deadline <= now) return null
        return (deadline - minutesBefore * 60_000L - now).coerceAtLeast(0L)
    }

    fun isCacheFresh(fetchedAt: Long, now: Long): Boolean =
        now - fetchedAt in 0 until AppConstants.WEATHER_CACHE_MILLIS

    /** Recompute the date and next class after midnight or a time change. */
    fun ticks(clock: Clock) = flow {
        while (true) {
            emit(clock.instant())
            delay(30_000)
        }
    }
}
