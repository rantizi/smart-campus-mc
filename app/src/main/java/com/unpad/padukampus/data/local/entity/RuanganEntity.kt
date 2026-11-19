package com.unpad.padukampus.data.local.entity

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "ruangan",
    indices = [Index("gedung_id")],
    foreignKeys =
        [
            ForeignKey(
                entity = GedungEntity::class,
                parentColumns = ["id"],
                childColumns = ["gedung_id"],
            )
        ],
)
data class RuanganEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "gedung_id") val gedungId: String,
    val kode: String,
    val nama: String,
    val lantai: Int,
)
