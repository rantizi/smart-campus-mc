package com.unpad.padukampus.data.remote.sync

import android.content.SharedPreferences
import androidx.core.content.edit
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.util.NetworkMonitor
import com.unpad.padukampus.data.local.dao.SyncDao
import com.unpad.padukampus.data.local.entity.TugasEntity
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** Sinkronisasi Room <-> Firestore (FR-11, NFR-06). */
interface CloudSyncManager {
    /** False bila google-services.json tidak ada saat build: aplikasi berjalan mode lokal. */
    val tersedia: Boolean
    val jumlahMenunggu: Flow<Int>
    val terakhirSinkron: StateFlow<Long?>

    suspend fun sync(): AppResult<Unit>

    /** Menghapus data akun dari perangkat (NFR-03). Dipanggil saat logout. */
    suspend fun hapusDataLokal()
}

class NotConfiguredCloudSyncManager : CloudSyncManager {
    override val tersedia = false
    override val jumlahMenunggu: Flow<Int> = flowOf(0)
    override val terakhirSinkron: StateFlow<Long?> = MutableStateFlow(null)

    override suspend fun sync(): AppResult<Unit> =
        AppResult.Error("Firebase belum dikonfigurasi pada build ini. Data tersimpan lokal.")

    override suspend fun hapusDataLokal() = Unit
}

/** Penanda sinkronisasi per akun: batas waktu pengambilan per koleksi dan waktu sinkron terakhir. */
interface SyncStateStore {
    val terakhirSinkron: StateFlow<Long?>

    fun batasAmbil(uid: String, koleksi: String): Long

    fun simpanBatasAmbil(uid: String, koleksi: String, nilai: Long)

    fun simpanTerakhirSinkron(uid: String, waktu: Long)

    fun muatAkun(uid: String?)

    fun bersihkan()
}

class PrefsSyncStateStore(private val prefs: SharedPreferences) : SyncStateStore {
    private val _terakhir = MutableStateFlow<Long?>(null)
    override val terakhirSinkron = _terakhir.asStateFlow()

    override fun batasAmbil(uid: String, koleksi: String) = prefs.getLong("ambil_${uid}_$koleksi", 0)

    override fun simpanBatasAmbil(uid: String, koleksi: String, nilai: Long) {
        prefs.edit { putLong("ambil_${uid}_$koleksi", nilai) }
    }

    override fun simpanTerakhirSinkron(uid: String, waktu: Long) {
        prefs.edit { putLong("terakhir_$uid", waktu) }
        _terakhir.value = waktu
    }

    override fun muatAkun(uid: String?) {
        _terakhir.value = uid?.let { prefs.getLong("terakhir_$it", 0).takeIf { t -> t > 0 } }
    }

    override fun bersihkan() {
        prefs.edit { clear() }
        _terakhir.value = null
    }
}

/**
 * Alur Laporan 1 Tabel 6: ambil dokumen cloud yang lebih baru (last-write-wins), lalu kirim baris
 * PENDING. Ambil lebih dulu supaya induk (mata kuliah) ada sebelum anak (jadwal, tugas).
 */
class FirestoreCloudSyncManager(
    private val dao: SyncDao,
    private val remote: SyncRemoteDataSource,
    private val uid: () -> String?,
    private val network: NetworkMonitor,
    private val state: SyncStateStore,
    private val clock: Clock,
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit,
    private val onTugasDiperbarui: (TugasEntity) -> Unit,
) : CloudSyncManager {
    override val tersedia = true
    override val jumlahMenunggu: Flow<Int> = dao.observePendingCount()
    override val terakhirSinkron: StateFlow<Long?> = state.terakhirSinkron
    private val lock = Mutex()

    override suspend fun sync(): AppResult<Unit> =
        lock.withLock {
            val akun = uid() ?: return@withLock AppResult.Error("Masuk dulu untuk sinkronisasi.")
            if (!network.isOnlineNow()) {
                return@withLock AppResult.Error(
                    "Tidak ada koneksi. Perubahan tersimpan lokal dan dikirim saat online."
                )
            }
            try {
                withTimeout(TIMEOUT_MILLIS) {
                    SyncMapper.KOLEKSI.forEach { ambil(akun, it) }
                    kirim(akun)
                }
                state.simpanTerakhirSinkron(akun, clock.millis())
                AppResult.Success(Unit)
            } catch (timeout: TimeoutCancellationException) {
                AppResult.Error("Sinkronisasi terlalu lama. Coba lagi saat koneksi stabil.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppResult.Error("Sinkronisasi gagal: ${error.message ?: error.javaClass.simpleName}")
            }
        }

    override suspend fun hapusDataLokal() =
        lock.withLock {
            runInTransaction {
                dao.hapusSemuaTugas()
                dao.hapusSemuaPerubahan()
                dao.hapusSemuaJadwal()
                dao.hapusSemuaMataKuliah()
            }
            state.bersihkan()
        }

    private suspend fun ambil(akun: String, koleksi: String) {
        val batas = state.batasAmbil(akun, koleksi)
        // Mundur sedikit untuk mengantisipasi jam perangkat lain yang tidak sama persis;
        // dokumen yang sudah dimiliki dilewati oleh aturan last-write-wins.
        val dokumen = remote.ambil(akun, koleksi, maxOf(0, batas - TOLERANSI_JAM_MILLIS))
        if (dokumen.isEmpty()) return
        val tugasBaru = mutableListOf<TugasEntity>()
        runInTransaction {
            dokumen.forEach { doc ->
                when (koleksi) {
                    SyncMapper.MATA_KULIAH -> {
                        val value = SyncMapper.mataKuliah(doc.id, doc.data)
                        if (SyncMapper.remoteMenang(dao.mataKuliahUpdatedAt(doc.id), value.updatedAt))
                            dao.upsertMataKuliah(value)
                    }
                    SyncMapper.JADWAL -> {
                        val value = SyncMapper.jadwal(doc.id, doc.data)
                        if (
                            SyncMapper.remoteMenang(dao.jadwalUpdatedAt(doc.id), value.updatedAt) &&
                                dao.mataKuliahAda(value.mataKuliahId)
                        ) {
                            // Ruangan adalah data referensi lokal; ID yang tidak dikenal dikosongkan.
                            val ruangan = value.ruanganId?.takeIf { dao.ruanganAda(it) }
                            dao.upsertJadwal(value.copy(ruanganId = ruangan))
                        }
                    }
                    SyncMapper.PERUBAHAN_JADWAL -> {
                        val value = SyncMapper.perubahan(doc.id, doc.data)
                        if (
                            SyncMapper.remoteMenang(dao.perubahanUpdatedAt(doc.id), value.updatedAt) &&
                                dao.jadwalAda(value.jadwalId)
                        ) {
                            val ruangan = value.ruanganBaruId?.takeIf { dao.ruanganAda(it) }
                            dao.upsertPerubahan(value.copy(ruanganBaruId = ruangan))
                        }
                    }
                    SyncMapper.TUGAS -> {
                        val value = SyncMapper.tugas(doc.id, doc.data)
                        if (
                            SyncMapper.remoteMenang(dao.tugasUpdatedAt(doc.id), value.updatedAt) &&
                                dao.mataKuliahAda(value.mataKuliahId)
                        ) {
                            dao.upsertTugas(value)
                            tugasBaru += value
                        }
                    }
                }
            }
        }
        // Pengingat dijadwalkan ulang setelah transaksi selesai (FR-06 di perangkat lain).
        tugasBaru.forEach(onTugasDiperbarui)
        val terbaru = dokumen.maxOf { (it.data["updated_at"] as? Number)?.toLong() ?: 0 }
        state.simpanBatasAmbil(akun, koleksi, maxOf(batas, terbaru))
    }

    private suspend fun kirim(akun: String) {
        dao.pendingMataKuliah().takeIf { it.isNotEmpty() }?.let { rows ->
            remote.kirim(akun, SyncMapper.MATA_KULIAH, rows.map { DokumenCloud(it.id, SyncMapper.toMap(it)) })
            rows.forEach { dao.markMataKuliahSynced(it.id, it.updatedAt) }
        }
        dao.pendingJadwal().takeIf { it.isNotEmpty() }?.let { rows ->
            remote.kirim(akun, SyncMapper.JADWAL, rows.map { DokumenCloud(it.id, SyncMapper.toMap(it)) })
            rows.forEach { dao.markJadwalSynced(it.id, it.updatedAt) }
        }
        dao.pendingPerubahan().takeIf { it.isNotEmpty() }?.let { rows ->
            remote.kirim(
                akun,
                SyncMapper.PERUBAHAN_JADWAL,
                rows.map { DokumenCloud(it.id, SyncMapper.toMap(it)) },
            )
            rows.forEach { dao.markPerubahanSynced(it.id, it.updatedAt) }
        }
        dao.pendingTugas().takeIf { it.isNotEmpty() }?.let { rows ->
            remote.kirim(akun, SyncMapper.TUGAS, rows.map { DokumenCloud(it.id, SyncMapper.toMap(it)) })
            rows.forEach { dao.markTugasSynced(it.id, it.updatedAt) }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 60_000L
        const val TOLERANSI_JAM_MILLIS = 10 * 60_000L
    }
}
