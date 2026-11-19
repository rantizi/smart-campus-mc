package com.unpad.padukampus.data.local.entity

import androidx.room.*

@Entity(tableName = "cache_cuaca")
data class CacheCuacaEntity(
    @PrimaryKey val id: String,
    val suhu: Double,
    @ColumnInfo(name = "kode_cuaca") val kodeCuaca: Int,
    @ColumnInfo(name = "hujan_per_jam") val hujanPerJam: String,
    @ColumnInfo(name = "diambil_pada") val diambilPada: Long,
)
