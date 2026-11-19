package com.unpad.padukampus.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.unpad.padukampus.data.local.dao.*
import com.unpad.padukampus.data.local.entity.*

// Splitting Kotlin classes does not change the seven existing SQLite tables/columns.
@Database(
    entities =
        [
            GedungEntity::class,
            RuanganEntity::class,
            MataKuliahEntity::class,
            JadwalEntity::class,
            PerubahanJadwalEntity::class,
            TugasEntity::class,
            CacheCuacaEntity::class,
        ],
    version = 1,
    exportSchema = true,
)
abstract class PaduKampusDatabase : RoomDatabase() {
    abstract fun kampusDao(): KampusDao

    abstract fun jadwalDao(): JadwalDao

    abstract fun tugasDao(): TugasDao

    abstract fun cuacaDao(): CuacaDao
}
