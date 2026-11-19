package com.unpad.padukampus.core.util

import java.time.Instant
import java.time.format.DateTimeParseException
import org.junit.Assert.*
import org.junit.Test

class DateTimeUtilsTest {
    @Test
    fun deadlineUsesCampusTimezoneRegardlessOfDeviceZone() {
        assertEquals(
            Instant.parse("2026-11-19T06:00:00Z").toEpochMilli(),
            DateTimeUtils.parseDeadline("2026-11-19 13:00"),
        )
    }

    @Test(expected = DateTimeParseException::class)
    fun invalidCalendarDateIsRejected() {
        DateTimeUtils.parseDeadline("2026-02-31 13:00")
    }

    @Test
    fun reminderUsesMinutesBeforeDeadline() {
        assertEquals(60_000L, DateTimeUtils.reminderDelay(180_000, 2, 0))
    }

    @Test
    fun alreadyDueReminderRunsImmediately() {
        assertEquals(0L, DateTimeUtils.reminderDelay(30_000, 1, 0))
    }

    @Test
    fun expiredTaskDoesNotScheduleReminder() {
        assertNull(DateTimeUtils.reminderDelay(100, 0, 100))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeReminderRejected() {
        DateTimeUtils.reminderDelay(100, -1, 0)
    }

    @Test
    fun cacheExpiresExactlyAtThirtyMinutes() {
        assertTrue(DateTimeUtils.isCacheFresh(100, 100 + 1_799_999))
        assertFalse(DateTimeUtils.isCacheFresh(100, 100 + 1_800_000))
        assertFalse(DateTimeUtils.isCacheFresh(100, 99))
    }
}
