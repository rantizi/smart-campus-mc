package com.unpad.padukampus.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.unpad.padukampus.data.local.entity.*

data class RuanganDetail(
    @Embedded val ruangan: RuanganEntity,
    @Relation(parentColumn = "gedung_id", entityColumn = "id") val gedung: GedungEntity,
)

data class PerubahanJadwalDetail(
    @Embedded val perubahan: PerubahanJadwalEntity,
    @Relation(parentColumn = "ruangan_baru_id", entityColumn = "id", entity = RuanganEntity::class)
    val lokasiBaru: RuanganDetail?,
)

/** Nested relations are loaded together by a @Transaction DAO query. */
data class JadwalDetail(
    @Embedded val jadwal: JadwalEntity,
    @Relation(parentColumn = "mata_kuliah_id", entityColumn = "id")
    val mataKuliah: MataKuliahEntity,
    @Relation(parentColumn = "ruangan_id", entityColumn = "id", entity = RuanganEntity::class)
    val lokasi: RuanganDetail?,
    @Relation(
        parentColumn = "id",
        entityColumn = "jadwal_id",
        entity = PerubahanJadwalEntity::class,
    )
    val perubahan: List<PerubahanJadwalDetail>,
)
