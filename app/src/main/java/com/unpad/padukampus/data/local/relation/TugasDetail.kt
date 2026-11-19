package com.unpad.padukampus.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.unpad.padukampus.data.local.entity.MataKuliahEntity
import com.unpad.padukampus.data.local.entity.TugasEntity

data class TugasDetail(
    @Embedded val tugas: TugasEntity,
    @Relation(parentColumn = "mata_kuliah_id", entityColumn = "id") val mataKuliah: MataKuliahEntity,
)
