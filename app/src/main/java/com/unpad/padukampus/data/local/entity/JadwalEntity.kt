package com.unpad.padukampus.data.local.entity

import androidx.room.*
import com.unpad.padukampus.core.constants.AppConstants
import java.util.UUID

@Entity(
    tableName = "jadwal",
    indices = [Index("mata_kuliah_id"), Index("ruangan_id")],
    foreignKeys =
        [
            ForeignKey(
                entity = MataKuliahEntity::class,
                parentColumns = ["id"],
                childColumns = ["mata_kuliah_id"],
            ),
            ForeignKey(
                entity = RuanganEntity::class,
                parentColumns = ["id"],
                childColumns = ["ruangan_id"],
            ),
        ],
)
data class JadwalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "mata_kuliah_id") val mataKuliahId: String,
    @ColumnInfo(name = "ruangan_id") val ruanganId: String?,
    val hari: Int,
    @ColumnInfo(name = "jam_mulai") val jamMulai: String,
    @ColumnInfo(name = "jam_selesai") val jamSelesai: String,
    @ColumnInfo(name = "pengingat_menit") val pengingatMenit: Int = 15,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_status") val syncStatus: String = AppConstants.SYNC_PENDING,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)
