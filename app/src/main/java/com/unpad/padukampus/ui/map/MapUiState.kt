package com.unpad.padukampus.ui.map

import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.data.local.entity.GedungEntity
import com.unpad.padukampus.data.local.relation.RuanganDetail
import com.unpad.padukampus.data.repository.KoordinatKampus

data class MapUiState(
    val loading: Boolean = true,
    val gedung: List<GedungEntity> = emptyList(),
    /** Hasil pencarian ruangan (FR-09); kosong saat kolom cari kosong. */
    val ruangan: List<RuanganDetail> = emptyList(),
    val search: String = "",
    val terpilih: RuanganDetail? = null,
    val koordinatKampus: KoordinatKampus? = null,
    val error: UiText? = null,
)
