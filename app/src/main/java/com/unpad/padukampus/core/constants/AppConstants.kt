package com.unpad.padukampus.core.constants

object AppConstants {
    const val DATABASE_NAME = "padukampus.db"
    const val WEATHER_BASE_URL = "https://api.open-meteo.com/"
    const val WEATHER_CACHE_MILLIS = 30 * 60_000L
    const val CAMPUS_TIME_ZONE = "Asia/Jakarta"
    const val REMINDER_CHANNEL_ID = "task_reminders"
    const val REMINDER_TAG = "padukampus_task_reminder"
    const val REMINDER_WORK_PREFIX = "task-"
    const val KEY_TASK_ID = "task_id"
    const val KEY_TASK_REVISION = "task_revision"
    const val SYNC_PENDING = "PENDING"
    const val MAX_REMINDER_MINUTES = 525_600
}
