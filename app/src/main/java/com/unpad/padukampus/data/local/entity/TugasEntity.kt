package com.unpad.padukampus.data.local.entity

import androidx.room.*
import com.unpad.padukampus.core.constants.AppConstants
import java.util.UUID

@Entity(
    tableName = "tugas",
    indices = [Index("mata_kuliah_id")],
    foreignKeys =
        [
            ForeignKey(
                entity = MataKuliahEntity::class,
                parentColumns = ["id"],
                childColumns = ["mata_kuliah_id"],
            )
        ],
)
data class TugasEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "mata_kuliah_id") val mataKuliahId: String,
    val judul: String,
    val deskripsi: String = "",
    val deadline: Long,
    val sumber: String = "LAINNYA",
    val selesai: Boolean = false,
    @ColumnInfo(name = "pengingat_menit") val pengingatMenit: Int = 1440,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_status") val syncStatus: String = AppConstants.SYNC_PENDING,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)
