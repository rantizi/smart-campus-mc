package com.unpad.padukampus.data.local.dao

import androidx.room.*
import com.unpad.padukampus.data.local.entity.*
import com.unpad.padukampus.data.local.relation.JadwalDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface JadwalDao {
    // Fetch candidates before applying overrides and selecting the next class in the repository.
    @Transaction
    @Query(
        """
        SELECT j.* FROM jadwal j JOIN mata_kuliah m ON m.id = j.mata_kuliah_id
        WHERE j.is_deleted = 0 AND m.is_deleted = 0 AND
          (j.hari = :hari OR EXISTS (SELECT 1 FROM perubahan_jadwal p
           WHERE p.jadwal_id = j.id AND p.tanggal = :tanggal AND p.is_deleted = 0))
        ORDER BY j.jam_mulai
    """
    )
    fun observeJadwalHarian(hari: Int, tanggal: String): Flow<List<JadwalDetail>>

    // Pilihan jadwal pada Form Perubahan (FR-04), urut Senin–Minggu lalu jam mulai.
    @Transaction
    @Query(
        """
        SELECT j.* FROM jadwal j JOIN mata_kuliah m ON m.id = j.mata_kuliah_id
        WHERE j.is_deleted = 0 AND m.is_deleted = 0
        ORDER BY j.hari, j.jam_mulai
    """
    )
    fun observeSemuaJadwal(): Flow<List<JadwalDetail>>

    @Transaction
    @Query("SELECT * FROM jadwal WHERE id = :id AND is_deleted = 0")
    suspend fun getJadwalDetail(id: String): JadwalDetail?

    @Query("SELECT * FROM mata_kuliah WHERE nama = :nama COLLATE NOCASE AND is_deleted = 0 LIMIT 1")
    suspend fun getMataKuliahByNama(nama: String): MataKuliahEntity?

    @Query(
        "UPDATE perubahan_jadwal SET is_deleted = 1, updated_at = :now, sync_status = 'PENDING' " +
            "WHERE id = :id"
    )
    suspend fun hapusPerubahan(id: String, now: Long)

    // Termasuk baris terhapus: seed hanya untuk database yang benar-benar baru.
    @Query("SELECT COUNT(*) FROM jadwal") suspend fun hitungJadwal(): Int

    @Query("SELECT * FROM mata_kuliah WHERE is_deleted = 0 ORDER BY nama")
    fun observeMataKuliah(): Flow<List<MataKuliahEntity>>

    @Query("SELECT * FROM mata_kuliah WHERE id = :id AND is_deleted = 0")
    suspend fun getMataKuliah(id: String): MataKuliahEntity?

    @Query("SELECT * FROM jadwal WHERE id = :id AND is_deleted = 0")
    suspend fun getJadwal(id: String): JadwalEntity?

    // Include tombstones so restoring an override reuses its unique (schedule, date) row.
    @Query(
        "SELECT * FROM perubahan_jadwal WHERE jadwal_id = :jadwalId AND tanggal = :tanggal LIMIT 1"
    )
    suspend fun getPerubahan(jadwalId: String, tanggal: String): PerubahanJadwalEntity?

    // Termasuk baris terhapus: mata kuliah kurikulum yang dihapus pengguna tidak disemai ulang.
    @Query("SELECT EXISTS(SELECT 1 FROM mata_kuliah WHERE id = :id)")
    suspend fun adaMataKuliahId(id: String): Boolean

    @Query(
        "SELECT * FROM jadwal WHERE mata_kuliah_id = :mataKuliahId AND hari = :hari " +
            "AND jam_mulai = :jamMulai AND ruangan_id IS NULL AND is_deleted = 0"
    )
    suspend fun jadwalTanpaRuangan(
        mataKuliahId: String,
        hari: Int,
        jamMulai: String,
    ): List<JadwalEntity>

    @Upsert suspend fun simpanMataKuliah(value: MataKuliahEntity)

    @Upsert suspend fun simpanJadwal(value: JadwalEntity)

    @Upsert suspend fun simpanPerubahan(value: PerubahanJadwalEntity)

    @Query(
        "UPDATE jadwal SET is_deleted = 1, updated_at = :now, sync_status = 'PENDING' " +
            "WHERE id = :id"
    )
    suspend fun hapusJadwal(id: String, now: Long)
}
