package com.unpad.padukampus.worker

import androidx.work.*
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.local.entity.TugasEntity
import java.time.Clock
import java.util.concurrent.TimeUnit

interface TaskReminderScheduler {
    fun schedule(task: TugasEntity)

    fun cancel(id: String)
}

class WorkManagerTaskReminderScheduler(
    private val workManager: WorkManager,
    private val notifications: NotificationHelper,
    private val clock: Clock,
) : TaskReminderScheduler {
    override fun schedule(task: TugasEntity) {
        val delay = DateTimeUtils.reminderDelay(task.deadline, task.pengingatMenit, clock.millis())
        if (task.isDeleted || task.selesai || delay == null) {
            cancel(task.id)
            return
        }
        val request =
            OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInputData(
                    workDataOf(
                        AppConstants.KEY_TASK_ID to task.id,
                        AppConstants.KEY_TASK_REVISION to task.updatedAt,
                    )
                )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(AppConstants.REMINDER_TAG)
                .build()
        workManager.enqueueUniqueWork(
            AppConstants.REMINDER_WORK_PREFIX + task.id,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun cancel(id: String) {
        workManager.cancelUniqueWork(AppConstants.REMINDER_WORK_PREFIX + id)
        notifications.cancelTask(id)
    }
}
