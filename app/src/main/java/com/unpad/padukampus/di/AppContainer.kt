package com.unpad.padukampus.di

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.unpad.padukampus.PaduKampusApp
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.core.util.AndroidNetworkMonitor
import com.unpad.padukampus.core.util.NetworkMonitor
import com.unpad.padukampus.data.local.db.PaduKampusDatabase
import com.unpad.padukampus.data.local.seed.JadwalAwal
import com.unpad.padukampus.data.remote.api.OpenMeteoApi
import com.unpad.padukampus.data.remote.sync.*
import com.unpad.padukampus.data.repository.*
import com.unpad.padukampus.ui.auth.LoginViewModel
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val clock: Clock = Clock.systemUTC()
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

    /** FirebaseInitProvider hanya aktif bila build memuat google-services.json. */
    private val firebaseTersedia: Boolean by lazy { FirebaseApp.getApps(appContext).isNotEmpty() }
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val syncState by lazy {
        PrefsSyncStateStore(appContext.getSharedPreferences("sync", Context.MODE_PRIVATE))
    }
    val syncScheduler: SyncScheduler by lazy {
        WorkManagerSyncScheduler(WorkManager.getInstance(appContext))
    }
    val cloudSyncManager: CloudSyncManager by lazy {
        if (!firebaseTersedia) return@lazy NotConfiguredCloudSyncManager()
        val firestore =
            FirebaseFirestore.getInstance().apply {
                // Room sudah menjadi cache luring; cache Firestore cukup di memori.
                firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(memoryCacheSettings {})
                }
            }
        FirestoreCloudSyncManager(
            dao = database.syncDao(),
            remote = FirestoreSyncDataSource(firestore),
            uid = { firebaseAuth.currentUser?.uid },
            network = networkMonitor,
            state = syncState,
            clock = clock,
            runInTransaction = { block -> database.withTransaction { block() } },
            onTugasDiperbarui = reminderScheduler::schedule,
        )
    }
    private val sesi =
        object : SesiPengguna {
            override suspend fun setelahMasuk(akun: AkunPengguna) {
                Log.i(TAG, "Sinkron awal setelah login dimulai")
                syncState.muatAkun(akun.uid)
                // Token login baru kadang belum sampai ke Firestore pada detik pertama; coba ulang.
                var hasil = cloudSyncManager.sync()
                for (percobaan in 1..3) {
                    if (hasil !is AppResult.Error) break
                    Log.w(TAG, "Sinkron awal gagal (percobaan $percobaan): ${hasil.message}")
                    delay(2_000L * percobaan)
                    hasil = cloudSyncManager.sync()
                }
                // Data akun diambil dulu; jadwal awal (build debug) hanya untuk akun kosong.
                if (hasil is AppResult.Success) {
                    jadwalRepository.seedJadwalAwal(JadwalAwal.daftar)
                }
                tugasRepository.reconcileReminders()
                syncScheduler.mulai()
                Log.i(TAG, "Sinkron awal selesai: ${hasil.javaClass.simpleName}")
            }

            override suspend fun kirimSebelumKeluar(): Int {
                withTimeoutOrNull(20_000) { cloudSyncManager.sync() }
                return cloudSyncManager.jumlahMenunggu.first()
            }

            override suspend fun bersihkanSaatKeluar() {
                syncScheduler.hentikan()
                reminderScheduler.cancelAll()
                cloudSyncManager.hapusDataLokal()
            }
        }
    val authRepository: AuthRepository by lazy {
        if (firebaseTersedia) FirebaseAuthRepository(firebaseAuth, sesi, applicationScope)
        else LocalOnlyAuthRepository()
    }

    /** Setiap perubahan tabel yang disinkronkan memicu pengiriman ke cloud saat online. */
    fun pantauPerubahanLokal() {
        if (!firebaseTersedia) return
        syncState.muatAkun(firebaseAuth.currentUser?.uid)
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(SyncMapper.KOLEKSI.toTypedArray()) {
                override fun onInvalidated(tables: Set<String>) {
                    if (authRepository.akun.value != null) syncScheduler.mintaSegera()
                }
            }
        )
        if (authRepository.akun.value != null) syncScheduler.mulai()
    }

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
            ProfileViewModel(
                kampusRepository,
                cloudSyncManager,
                authRepository,
                createSavedStateHandle(),
            )
        }
    }

    fun loginFactory() = viewModelFactory {
        initializer { LoginViewModel(authRepository, createSavedStateHandle()) }
    }
}

private const val TAG = "PaduKampusSync"

/** UI may resolve its factory here; it does not access Room, Retrofit or preferences directly. */
val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as PaduKampusApp).container
