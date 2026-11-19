package com.unpad.padukampus.di

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.work.WorkManager
import com.unpad.padukampus.PaduKampusApp
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.core.util.AndroidNetworkMonitor
import com.unpad.padukampus.core.util.NetworkMonitor
import com.unpad.padukampus.data.local.db.PaduKampusDatabase
import com.unpad.padukampus.data.remote.api.OpenMeteoApi
import com.unpad.padukampus.data.remote.sync.*
import com.unpad.padukampus.data.repository.*
import com.unpad.padukampus.ui.home.HomeViewModel
import com.unpad.padukampus.ui.map.MapViewModel
import com.unpad.padukampus.ui.profile.ProfileViewModel
import com.unpad.padukampus.ui.schedule.ChangeFormViewModel
import com.unpad.padukampus.ui.schedule.ScheduleFormViewModel
import com.unpad.padukampus.ui.schedule.ScheduleViewModel
import com.unpad.padukampus.ui.tasks.TaskViewModel
import com.unpad.padukampus.worker.*
import java.time.Clock
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val clock: Clock = Clock.systemUTC()
    private val database by lazy {
        Room.databaseBuilder(appContext, PaduKampusDatabase::class.java, AppConstants.DATABASE_NAME)
            .build()
    }
    val networkMonitor: NetworkMonitor by lazy { AndroidNetworkMonitor(appContext) }
    val notifications by lazy { NotificationHelper(appContext) }
    val reminderScheduler: TaskReminderScheduler by lazy {
        WorkManagerTaskReminderScheduler(WorkManager.getInstance(appContext), notifications, clock)
    }
    private val api by lazy {
        val client =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        Retrofit.Builder()
            .baseUrl(AppConstants.WEATHER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }
    val kampusRepository: KampusRepository by lazy {
        OfflineKampusRepository(
            database.kampusDao(),
            appContext.getSharedPreferences("campus", Context.MODE_PRIVATE),
        )
    }
    val jadwalRepository: JadwalRepository by lazy { OfflineJadwalRepository(database, clock) }
    val tugasRepository: TugasRepository by lazy {
        OfflineTugasRepository(database, reminderScheduler, clock)
    }
    val cuacaRepository: CuacaRepository by lazy {
        OfflineCuacaRepository(api, database.cuacaDao(), networkMonitor, clock)
    }
    val cloudSyncManager: CloudSyncManager = NotConfiguredCloudSyncManager()

    fun homeFactory() = viewModelFactory {
        initializer {
            HomeViewModel(
                jadwalRepository,
                tugasRepository,
                cuacaRepository,
                kampusRepository,
                networkMonitor,
                clock,
            )
        }
    }

    fun scheduleFactory() = viewModelFactory {
        initializer { ScheduleViewModel(jadwalRepository, createSavedStateHandle(), clock) }
    }

    fun scheduleFormFactory(jadwalId: String = "") = viewModelFactory {
        initializer {
            ScheduleFormViewModel(
                jadwalRepository,
                kampusRepository,
                createSavedStateHandle(),
                jadwalId,
            )
        }
    }

    fun changeFormFactory(jadwalId: String = "", tanggal: String = "") = viewModelFactory {
        initializer {
            ChangeFormViewModel(
                jadwalRepository,
                kampusRepository,
                createSavedStateHandle(),
                jadwalId,
                tanggal,
                clock,
            )
        }
    }

    fun taskFactory(taskId: String = "") = viewModelFactory {
        initializer { TaskViewModel(tugasRepository, createSavedStateHandle(), taskId) }
    }

    fun mapFactory(kodeRuangan: String = "") = viewModelFactory {
        initializer { MapViewModel(kampusRepository, createSavedStateHandle(), kodeRuangan) }
    }

    fun profileFactory() = viewModelFactory {
        initializer {
            ProfileViewModel(kampusRepository, cloudSyncManager, createSavedStateHandle())
        }
    }
}

/** UI may resolve its factory here; it does not access Room, Retrofit or preferences directly. */
val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as PaduKampusApp).container
