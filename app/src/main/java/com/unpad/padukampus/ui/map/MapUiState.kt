package com.unpad.padukampus.ui.map

import com.unpad.padukampus.data.local.entity.GedungEntity
import com.unpad.padukampus.data.local.relation.RuanganDetail

data class MapUiState(
    val loading: Boolean = true,
    val gedung: List<GedungEntity> = emptyList(),
    val ruangan: List<RuanganDetail> = emptyList(),
    val search: String = "",
    val error: String? = null,
)
