package com.unpad.padukampus

import android.app.Application
import android.util.Log
import com.unpad.padukampus.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaduKampusApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container.notifications.createChannel()
        // Repair the small window between committing a task and enqueuing its work.
        applicationScope.launch {
            try {
                container.tugasRepository.reconcileReminders()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("PaduKampus", "Pengingat belum dapat dijadwalkan ulang", error)
            }
        }
    }
}
