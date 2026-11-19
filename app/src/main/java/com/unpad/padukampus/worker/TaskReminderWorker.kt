package com.unpad.padukampus.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unpad.padukampus.PaduKampusApp
import com.unpad.padukampus.core.constants.AppConstants
import kotlinx.coroutines.CancellationException

class TaskReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(AppConstants.KEY_TASK_ID) ?: return Result.failure()
        val revision = inputData.getLong(AppConstants.KEY_TASK_REVISION, -1)
        val container = (applicationContext as PaduKampusApp).container
        return try {
            val task = container.tugasRepository.getTugas(id)?.tugas ?: return Result.success()
            // An edited/deleted/completed task must never be notified by an obsolete request.
            if (
                task.isDeleted ||
                    task.selesai ||
                    task.updatedAt != revision ||
                    task.deadline <= container.clock.millis()
            ) {
                return Result.success()
            }
            container.notifications.showTaskReminder(task)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
