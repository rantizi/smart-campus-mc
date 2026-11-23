package com.unpad.padukampus.data.local.dao

import androidx.room.*
import com.unpad.padukampus.data.local.entity.MataKuliahEntity
import com.unpad.padukampus.data.local.entity.TugasEntity
import com.unpad.padukampus.data.local.relation.TugasDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface TugasDao {
    @Transaction
    @Query(
        """
        SELECT t.* FROM tugas t JOIN mata_kuliah m ON m.id = t.mata_kuliah_id
        WHERE t.is_deleted = 0 AND m.is_deleted = 0
          AND (:selesai IS NULL OR t.selesai = :selesai)
        ORDER BY t.selesai, t.deadline, t.id
    """
    )
    fun observeTugas(selesai: Boolean?): Flow<List<TugasDetail>>

    @Transaction
    @Query("SELECT * FROM tugas WHERE id = :id AND is_deleted = 0")
    suspend fun getDetail(id: String): TugasDetail?

    @Query("SELECT * FROM tugas WHERE id = :id") suspend fun getEntity(id: String): TugasEntity?

    @Query("SELECT * FROM tugas WHERE is_deleted = 0")
    suspend fun getReminderTasks(): List<TugasEntity>

    @Query("SELECT * FROM mata_kuliah WHERE nama = :nama COLLATE NOCASE AND is_deleted = 0 LIMIT 1")
    suspend fun getMataKuliahByNama(nama: String): MataKuliahEntity?

    @Query("SELECT * FROM mata_kuliah WHERE is_deleted = 0 ORDER BY nama")
    fun observeMataKuliah(): Flow<List<MataKuliahEntity>>

    @Upsert suspend fun simpanMataKuliah(value: MataKuliahEntity)

    @Upsert suspend fun simpanTugas(value: TugasEntity)

    @Query(
        "UPDATE tugas SET selesai = :selesai, updated_at = :now, sync_status = 'PENDING' WHERE id = :id AND is_deleted = 0"
    )
    suspend fun setSelesai(id: String, selesai: Boolean, now: Long)

    @Query(
        "UPDATE tugas SET is_deleted = 1, updated_at = :now, sync_status = 'PENDING' WHERE id = :id"
    )
    suspend fun hapus(id: String, now: Long)
}
