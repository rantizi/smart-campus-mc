package com.unpad.padukampus.data.local.dao

import androidx.room.*
import com.unpad.padukampus.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Query khusus sinkronisasi Firestore (FR-11). Baris dengan sync_status PENDING dikirim, lalu
 * ditandai SYNCED hanya bila updated_at belum berubah selama pengiriman. Termasuk tombstone
 * (is_deleted = 1) agar penghapusan ikut tersinkron.
 */
@Dao
interface SyncDao {
    @Query("SELECT * FROM mata_kuliah WHERE sync_status = 'PENDING'")
    suspend fun pendingMataKuliah(): List<MataKuliahEntity>

    @Query("SELECT * FROM jadwal WHERE sync_status = 'PENDING'")
    suspend fun pendingJadwal(): List<JadwalEntity>

    @Query("SELECT * FROM perubahan_jadwal WHERE sync_status = 'PENDING'")
    suspend fun pendingPerubahan(): List<PerubahanJadwalEntity>

    @Query("SELECT * FROM tugas WHERE sync_status = 'PENDING'")
    suspend fun pendingTugas(): List<TugasEntity>

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM mata_kuliah WHERE sync_status = 'PENDING')
             + (SELECT COUNT(*) FROM jadwal WHERE sync_status = 'PENDING')
             + (SELECT COUNT(*) FROM perubahan_jadwal WHERE sync_status = 'PENDING')
             + (SELECT COUNT(*) FROM tugas WHERE sync_status = 'PENDING')
    """
    )
    fun observePendingCount(): Flow<Int>

    @Query(
        "UPDATE mata_kuliah SET sync_status = 'SYNCED' WHERE id = :id AND updated_at = :updatedAt"
    )
    suspend fun markMataKuliahSynced(id: String, updatedAt: Long)

    @Query("UPDATE jadwal SET sync_status = 'SYNCED' WHERE id = :id AND updated_at = :updatedAt")
    suspend fun markJadwalSynced(id: String, updatedAt: Long)

    @Query(
        "UPDATE perubahan_jadwal SET sync_status = 'SYNCED' " +
            "WHERE id = :id AND updated_at = :updatedAt"
    )
    suspend fun markPerubahanSynced(id: String, updatedAt: Long)

    @Query("UPDATE tugas SET sync_status = 'SYNCED' WHERE id = :id AND updated_at = :updatedAt")
    suspend fun markTugasSynced(id: String, updatedAt: Long)

    @Query("SELECT updated_at FROM mata_kuliah WHERE id = :id")
    suspend fun mataKuliahUpdatedAt(id: String): Long?

    @Query("SELECT updated_at FROM jadwal WHERE id = :id")
    suspend fun jadwalUpdatedAt(id: String): Long?

    @Query("SELECT updated_at FROM perubahan_jadwal WHERE id = :id")
    suspend fun perubahanUpdatedAt(id: String): Long?

    @Query("SELECT updated_at FROM tugas WHERE id = :id")
    suspend fun tugasUpdatedAt(id: String): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM ruangan WHERE id = :id)")
    suspend fun ruanganAda(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM mata_kuliah WHERE id = :id)")
    suspend fun mataKuliahAda(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM jadwal WHERE id = :id)")
    suspend fun jadwalAda(id: String): Boolean

    @Upsert suspend fun upsertMataKuliah(value: MataKuliahEntity)

    @Upsert suspend fun upsertJadwal(value: JadwalEntity)

    @Upsert suspend fun upsertPerubahan(value: PerubahanJadwalEntity)

    @Upsert suspend fun upsertTugas(value: TugasEntity)

    @Query("DELETE FROM tugas") suspend fun hapusSemuaTugas()

    @Query("DELETE FROM perubahan_jadwal") suspend fun hapusSemuaPerubahan()

    @Query("DELETE FROM jadwal") suspend fun hapusSemuaJadwal()

    @Query("DELETE FROM mata_kuliah") suspend fun hapusSemuaMataKuliah()

    @Query("SELECT COUNT(*) FROM jadwal WHERE is_deleted = 0") suspend fun hitungJadwalAktif(): Int
}
