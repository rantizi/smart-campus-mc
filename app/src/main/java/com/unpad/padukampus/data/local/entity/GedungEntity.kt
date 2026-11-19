package com.unpad.padukampus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "gedung")
data class GedungEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nama: String,
    val kode: String,
    val latitude: Double,
    val longitude: Double,
)
