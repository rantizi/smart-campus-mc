package com.unpad.padukampus

import android.app.Application
import android.util.Log
import com.unpad.padukampus.data.local.seed.JadwalAwal
import com.unpad.padukampus.data.local.seed.KurikulumTI
import com.unpad.padukampus.di.AppContainer
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class PaduKampusApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    private val applicationScope
        get() = container.applicationScope

    override fun onCreate() {
        super.onCreate()
        container.notifications.createChannel()
        configureMap()
        container.pantauPerubahanLokal()
        applicationScope.launch {
            try {
                container.kampusRepository.seedDataAwal()
                // Build debug: jadwal kuliah anggota untuk uji di HP; build release: daftar kosong.
                // Dengan Firebase, jadwal awal diisi setelah login bila akun masih kosong.
                if (!container.authRepository.tersedia) {
                    container.jadwalRepository.seedKurikulum(KurikulumTI.mataKuliah)
                    container.jadwalRepository.seedJadwalAwal(JadwalAwal.daftar)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("PaduKampus", "Data awal kampus belum dapat disimpan", error)
            }
        }
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

    /**
     * osmdroid: user agent wajib menurut kebijakan tile OpenStreetMap; cache tile di folder cache
     * aplikasi sehingga tidak perlu izin penyimpanan.
     */
    private fun configureMap() {
        Configuration.getInstance().apply {
            load(this@PaduKampusApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
    }
}
