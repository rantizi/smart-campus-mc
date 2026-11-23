package com.unpad.padukampus.data.remote.sync

import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.core.util.NetworkMonitor
import com.unpad.padukampus.data.local.dao.SyncDao
import com.unpad.padukampus.data.local.entity.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CloudSyncManagerTest {
    private class FakeDao : SyncDao {
        val mk = linkedMapOf<String, MataKuliahEntity>()
        val jadwal = linkedMapOf<String, JadwalEntity>()
        val perubahan = linkedMapOf<String, PerubahanJadwalEntity>()
        val tugas = linkedMapOf<String, TugasEntity>()
        val ruangan = mutableSetOf<String>()

        override suspend fun pendingMataKuliah() = mk.values.filter { it.syncStatus == AppConstants.SYNC_PENDING }
        override suspend fun pendingJadwal() = jadwal.values.filter { it.syncStatus == AppConstants.SYNC_PENDING }
        override suspend fun pendingPerubahan() = perubahan.values.filter { it.syncStatus == AppConstants.SYNC_PENDING }
        override suspend fun pendingTugas() = tugas.values.filter { it.syncStatus == AppConstants.SYNC_PENDING }
        override fun observePendingCount() = flowOf(0)

        override suspend fun markMataKuliahSynced(id: String, updatedAt: Long) {
            mk[id]?.takeIf { it.updatedAt == updatedAt }?.let { mk[id] = it.copy(syncStatus = AppConstants.SYNC_SYNCED) }
        }
        override suspend fun markJadwalSynced(id: String, updatedAt: Long) {
            jadwal[id]?.takeIf { it.updatedAt == updatedAt }?.let { jadwal[id] = it.copy(syncStatus = AppConstants.SYNC_SYNCED) }
        }
        override suspend fun markPerubahanSynced(id: String, updatedAt: Long) {
            perubahan[id]?.takeIf { it.updatedAt == updatedAt }?.let { perubahan[id] = it.copy(syncStatus = AppConstants.SYNC_SYNCED) }
        }
        override suspend fun markTugasSynced(id: String, updatedAt: Long) {
            tugas[id]?.takeIf { it.updatedAt == updatedAt }?.let { tugas[id] = it.copy(syncStatus = AppConstants.SYNC_SYNCED) }
        }

        override suspend fun mataKuliahUpdatedAt(id: String) = mk[id]?.updatedAt
        override suspend fun jadwalUpdatedAt(id: String) = jadwal[id]?.updatedAt
        override suspend fun perubahanUpdatedAt(id: String) = perubahan[id]?.updatedAt
        override suspend fun tugasUpdatedAt(id: String) = tugas[id]?.updatedAt
        override suspend fun ruanganAda(id: String) = id in ruangan
        override suspend fun mataKuliahAda(id: String) = id in mk
        override suspend fun jadwalAda(id: String) = id in jadwal

        override suspend fun upsertMataKuliah(value: MataKuliahEntity) { mk[value.id] = value }
        override suspend fun upsertJadwal(value: JadwalEntity) { jadwal[value.id] = value }
        override suspend fun upsertPerubahan(value: PerubahanJadwalEntity) { perubahan[value.id] = value }
        override suspend fun upsertTugas(value: TugasEntity) { tugas[value.id] = value }

        override suspend fun hapusSemuaTugas() = tugas.clear()
        override suspend fun hapusSemuaPerubahan() = perubahan.clear()
        override suspend fun hapusSemuaJadwal() = jadwal.clear()
        override suspend fun hapusSemuaMataKuliah() = mk.clear()
        override suspend fun hitungJadwalAktif() = jadwal.values.count { !it.isDeleted }
    }

    private class FakeRemote : SyncRemoteDataSource {
        val store = mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()
        val ambilSejak = mutableMapOf<String, Long>()
        var saatKirim: (String) -> Unit = {}

        fun isi(uid: String, koleksi: String, id: String, data: Map<String, Any?>) {
            store.getOrPut("$uid/$koleksi") { mutableMapOf() }[id] = data
        }

        override suspend fun ambil(uid: String, koleksi: String, sejak: Long): List<DokumenCloud> {
            ambilSejak[koleksi] = sejak
            return store["$uid/$koleksi"].orEmpty()
                .filter { ((it.value["updated_at"] as Number).toLong()) > sejak }
                .map { DokumenCloud(it.key, it.value) }
        }

        override suspend fun kirim(uid: String, koleksi: String, dokumen: List<DokumenCloud>) {
            saatKirim(koleksi)
            dokumen.forEach { isi(uid, koleksi, it.id, it.data) }
        }
    }

    private class FakeState : SyncStateStore {
        private val _t = MutableStateFlow<Long?>(null)
        override val terakhirSinkron = _t.asStateFlow()
        val batas = mutableMapOf<String, Long>()
        override fun batasAmbil(uid: String, koleksi: String) = batas["$uid/$koleksi"] ?: 0
        override fun simpanBatasAmbil(uid: String, koleksi: String, nilai: Long) { batas["$uid/$koleksi"] = nilai }
        override fun simpanTerakhirSinkron(uid: String, waktu: Long) { _t.value = waktu }
        override fun muatAkun(uid: String?) = Unit
        override fun bersihkan() { batas.clear(); _t.value = null }
    }

    private class FakeNetwork(var online: Boolean = true) : NetworkMonitor {
        override val isOnline = flowOf(online)
        override fun isOnlineNow() = online
    }

    private val dao = FakeDao()
    private val remote = FakeRemote()
    private val state = FakeState()
    private val network = FakeNetwork()
    private val dijadwalkan = mutableListOf<TugasEntity>()
    private var uid: String? = "uid-a"
    private val clock = Clock.fixed(Instant.ofEpochMilli(5_000_000), ZoneOffset.UTC)

    private fun manager() =
        FirestoreCloudSyncManager(
            dao = dao,
            remote = remote,
            uid = { uid },
            network = network,
            state = state,
            clock = clock,
            runInTransaction = { it() },
            onTugasDiperbarui = { dijadwalkan += it },
        )

    private fun mk(id: String, updatedAt: Long, nama: String = "MK $id", status: String = AppConstants.SYNC_PENDING) =
        MataKuliahEntity(id = id, nama = nama, updatedAt = updatedAt, syncStatus = status)

    @Test
    fun offlineReturnsErrorAndSendsNothing() = runTest {
        network.online = false
        dao.upsertMataKuliah(mk("mk-1", 1))
        assertTrue(manager().sync() is AppResult.Error)
        assertTrue(remote.store.isEmpty())
        assertEquals(AppConstants.SYNC_PENDING, dao.mk["mk-1"]!!.syncStatus)
    }

    @Test
    fun withoutLoginNothingIsSent() = runTest {
        uid = null
        dao.upsertMataKuliah(mk("mk-1", 1))
        assertTrue(manager().sync() is AppResult.Error)
        assertTrue(remote.store.isEmpty())
    }

    @Test
    fun pendingRowsArePushedUnderOwnUidAndMarkedSynced() = runTest {
        dao.upsertMataKuliah(mk("mk-1", 1))
        dao.upsertTugas(TugasEntity(id = "t-1", mataKuliahId = "mk-1", judul = "Laporan", deadline = 9, updatedAt = 2))
        assertTrue(manager().sync() is AppResult.Success)
        assertEquals("MK mk-1", remote.store["uid-a/mata_kuliah"]!!["mk-1"]!!["nama"])
        assertEquals("Laporan", remote.store["uid-a/tugas"]!!["t-1"]!!["judul"])
        assertEquals(AppConstants.SYNC_SYNCED, dao.mk["mk-1"]!!.syncStatus)
        assertEquals(AppConstants.SYNC_SYNCED, dao.tugas["t-1"]!!.syncStatus)
        assertEquals(5_000_000L, state.terakhirSinkron.value)
    }

    @Test
    fun rowEditedDuringPushStaysPending() = runTest {
        dao.upsertMataKuliah(mk("mk-1", 1))
        remote.saatKirim = { if (it == SyncMapper.MATA_KULIAH) dao.mk["mk-1"] = mk("mk-1", 2, nama = "baru") }
        manager().sync()
        assertEquals(AppConstants.SYNC_PENDING, dao.mk["mk-1"]!!.syncStatus)
        assertEquals("baru", dao.mk["mk-1"]!!.nama)
    }

    @Test
    fun newerCloudVersionReplacesLocalButOlderDoesNot() = runTest {
        dao.upsertMataKuliah(mk("lama", 10, nama = "lokal lama", status = AppConstants.SYNC_SYNCED))
        dao.upsertMataKuliah(mk("baru", 50, nama = "lokal baru"))
        remote.isi("uid-a", "mata_kuliah", "lama", SyncMapper.toMap(mk("lama", 20, nama = "cloud")))
        remote.isi("uid-a", "mata_kuliah", "baru", SyncMapper.toMap(mk("baru", 40, nama = "cloud usang")))
        manager().sync()
        assertEquals("cloud", dao.mk["lama"]!!.nama)
        assertEquals(AppConstants.SYNC_SYNCED, dao.mk["lama"]!!.syncStatus)
        // Versi lokal lebih baru tetap dipakai lalu dikirim menimpa cloud.
        assertEquals("lokal baru", dao.mk["baru"]!!.nama)
        assertEquals("lokal baru", remote.store["uid-a/mata_kuliah"]!!["baru"]!!["nama"])
    }

    @Test
    fun pulledScheduleDropsUnknownRoomAndSkipsMissingCourse() = runTest {
        remote.isi("uid-a", "mata_kuliah", "mk-1", SyncMapper.toMap(mk("mk-1", 1)))
        remote.isi("uid-a", "jadwal", "j-1", SyncMapper.toMap(JadwalEntity(id = "j-1", mataKuliahId = "mk-1", ruanganId = "ruang-asing", hari = 2, jamMulai = "07:30", jamSelesai = "09:10", updatedAt = 2)))
        remote.isi("uid-a", "jadwal", "j-2", SyncMapper.toMap(JadwalEntity(id = "j-2", mataKuliahId = "mk-hilang", ruanganId = null, hari = 3, jamMulai = "08:00", jamSelesai = "09:00", updatedAt = 3)))
        manager().sync()
        assertNull(dao.jadwal["j-1"]!!.ruanganId)
        assertFalse("j-2" in dao.jadwal)
    }

    @Test
    fun pulledTasksAreRescheduledAndWatermarkAdvances() = runTest {
        remote.isi("uid-a", "mata_kuliah", "mk-1", SyncMapper.toMap(mk("mk-1", 1)))
        remote.isi("uid-a", "tugas", "t-1", SyncMapper.toMap(TugasEntity(id = "t-1", mataKuliahId = "mk-1", judul = "Kuis", deadline = 9_000_000, updatedAt = 900_000)))
        manager().sync()
        assertEquals(listOf("t-1"), dijadwalkan.map { it.id })
        assertEquals(900_000L, state.batasAmbil("uid-a", "tugas"))
        manager().sync()
        // Batas dikurangi toleransi jam 10 menit; dokumen yang sama tidak dijadwalkan ulang.
        assertEquals(300_000L, remote.ambilSejak["tugas"])
        assertEquals(1, dijadwalkan.size)
    }

    @Test
    fun otherUsersDataIsNeverRead() = runTest {
        remote.isi("uid-b", "mata_kuliah", "rahasia", SyncMapper.toMap(mk("rahasia", 1)))
        manager().sync()
        assertFalse("rahasia" in dao.mk)
    }

    @Test
    fun logoutCleanupWipesAccountDataAndState() = runTest {
        dao.upsertMataKuliah(mk("mk-1", 1))
        dao.upsertTugas(TugasEntity(id = "t-1", mataKuliahId = "mk-1", judul = "x", deadline = 1))
        state.simpanBatasAmbil("uid-a", "tugas", 9)
        manager().hapusDataLokal()
        assertTrue(dao.mk.isEmpty() && dao.tugas.isEmpty())
        assertEquals(0L, state.batasAmbil("uid-a", "tugas"))
    }
}
