package com.unpad.padukampus.data.local.entity

import androidx.room.*
import com.unpad.padukampus.core.constants.AppConstants
import java.util.UUID

@Entity(
    tableName = "perubahan_jadwal",
    indices =
        [
            Index("jadwal_id"),
            Index("ruangan_baru_id"),
            Index(value = ["jadwal_id", "tanggal"], unique = true),
        ],
    foreignKeys =
        [
            ForeignKey(
                entity = JadwalEntity::class,
                parentColumns = ["id"],
                childColumns = ["jadwal_id"],
            ),
            ForeignKey(
                entity = RuanganEntity::class,
                parentColumns = ["id"],
                childColumns = ["ruangan_baru_id"],
            ),
        ],
)
data class PerubahanJadwalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "jadwal_id") val jadwalId: String,
    @ColumnInfo(name = "ruangan_baru_id") val ruanganBaruId: String?,
    val tanggal: String,
    val jenis: String,
    @ColumnInfo(name = "jam_mulai_baru") val jamMulaiBaru: String?,
    @ColumnInfo(name = "jam_selesai_baru") val jamSelesaiBaru: String?,
    val catatan: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_status") val syncStatus: String = AppConstants.SYNC_PENDING,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)
