package com.unpad.padukampus.data.local.entity

import androidx.room.*
import com.unpad.padukampus.core.constants.AppConstants
import java.util.UUID

@Entity(tableName = "mata_kuliah")
data class MataKuliahEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nama: String,
    val kode: String = "",
    val dosen: String = "",
    val sks: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_status") val syncStatus: String = AppConstants.SYNC_PENDING,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
)
