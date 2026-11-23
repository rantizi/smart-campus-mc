package com.unpad.padukampus.data.remote.sync

import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.data.local.entity.*
import org.junit.Assert.*
import org.junit.Test

class SyncMapperTest {
    @Test
    fun mataKuliahRoundTripKeepsFieldsAndMarksSynced() {
        val local = MataKuliahEntity(id = "mk-1", nama = "Mobile Computing", kode = "D10G", dosen = "Dosen", sks = 3, updatedAt = 10, isDeleted = true)
        val back = SyncMapper.mataKuliah("mk-1", SyncMapper.toMap(local))
        assertEquals(local.copy(syncStatus = AppConstants.SYNC_SYNCED), back)
    }

    @Test
    fun jadwalRoundTripKeepsNullableRoom() {
        val local = JadwalEntity(id = "j-1", mataKuliahId = "mk-1", ruanganId = null, hari = 2, jamMulai = "07:30", jamSelesai = "09:10", pengingatMenit = 10, updatedAt = 20)
        assertEquals(local.copy(syncStatus = AppConstants.SYNC_SYNCED), SyncMapper.jadwal("j-1", SyncMapper.toMap(local)))
    }

    @Test
    fun perubahanAndTugasRoundTrip() {
        val change = PerubahanJadwalEntity(id = "p-1", jadwalId = "j-1", ruanganBaruId = "r-1", tanggal = "2026-11-24", jenis = "GANTI_JAM", jamMulaiBaru = "13:00", jamSelesaiBaru = "14:40", catatan = "dosen rapat", updatedAt = 30)
        assertEquals(change.copy(syncStatus = AppConstants.SYNC_SYNCED), SyncMapper.perubahan("p-1", SyncMapper.toMap(change)))
        val task = TugasEntity(id = "t-1", mataKuliahId = "mk-1", judul = "Laporan 3", deskripsi = "PDF", deadline = 1_000, sumber = "WA", selesai = true, pengingatMenit = 60, updatedAt = 40)
        assertEquals(task.copy(syncStatus = AppConstants.SYNC_SYNCED), SyncMapper.tugas("t-1", SyncMapper.toMap(task)))
    }

    @Test
    fun syncStatusIsNeverSentToCloud() {
        val map = SyncMapper.toMap(TugasEntity(mataKuliahId = "mk", judul = "x", deadline = 1))
        assertFalse(map.containsKey("sync_status"))
        assertTrue(map["updated_at"] is Long)
    }

    @Test
    fun missingOrWrongTypedFieldsUseDefaults() {
        val task = SyncMapper.tugas("t-2", mapOf("judul" to 5, "updated_at" to 7))
        assertEquals("", task.judul)
        assertEquals("LAINNYA", task.sumber)
        assertEquals(1440, task.pengingatMenit)
        assertEquals(7, task.updatedAt)
    }

    @Test
    fun lastWriteWinsOnlyWhenRemoteIsNewer() {
        assertTrue(SyncMapper.remoteMenang(null, 1))
        assertTrue(SyncMapper.remoteMenang(5, 6))
        assertFalse(SyncMapper.remoteMenang(6, 6))
        assertFalse(SyncMapper.remoteMenang(7, 6))
    }
}
