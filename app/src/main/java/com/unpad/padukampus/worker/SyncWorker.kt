package com.unpad.padukampus.worker

import android.content.Context
import androidx.work.*
import com.unpad.padukampus.PaduKampusApp
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.constants.AppConstants
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** FR-11: sinkronisasi latar belakang, hanya berjalan saat ada koneksi (NFR-06). */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PaduKampusApp).container
        if (container.authRepository.akun.value == null) return Result.success()
        return try {
            // Perubahan yang dibuat selama sinkronisasi ikut dikirim pada putaran berikutnya.
            repeat(MAX_PUTARAN) {
                val hasil = container.cloudSyncManager.sync()
                if (hasil is AppResult.Error) {
                    return if (runAttemptCount < 5) Result.retry() else Result.failure()
                }
                if (container.cloudSyncManager.jumlahMenunggu.first() == 0) return Result.success()
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_PUTARAN = 3
    }
}

interface SyncScheduler {
    /** Setelah login: sinkron berkala dan sinkron awal. */
    fun mulai()

    /** Ada perubahan lokal: kirim segera saat online. */
    fun mintaSegera()

    /** Logout: hentikan semua pekerjaan sinkronisasi. */
    fun hentikan()
}

class WorkManagerSyncScheduler(private val workManager: WorkManager) : SyncScheduler {
    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override fun mulai() {
        val periodik =
            PeriodicWorkRequestBuilder<SyncWorker>(AppConstants.SYNC_PERIODIC_HOURS, TimeUnit.HOURS)
                .setConstraints(online)
                .build()
        workManager.enqueueUniquePeriodicWork(
            AppConstants.SYNC_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodik,
        )
        mintaSegera()
    }

    override fun mintaSegera() {
        val request =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(online)
                // Kumpulkan beberapa perubahan berurutan menjadi satu pengiriman.
                .setInitialDelay(3, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        workManager.enqueueUniqueWork(
            AppConstants.SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override fun hentikan() {
        workManager.cancelUniqueWork(AppConstants.SYNC_WORK_NAME)
        workManager.cancelUniqueWork(AppConstants.SYNC_PERIODIC_WORK_NAME)
    }
}
