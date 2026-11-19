package com.unpad.padukampus.ui.home

import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.data.repository.Cuaca
import com.unpad.padukampus.data.repository.KelasHarian
import com.unpad.padukampus.data.repository.PeluangHujan

data class HomeUiState(
    val loading: Boolean = true,
    val jadwal: List<KelasHarian> = emptyList(),
    val kelasBerikutnya: KelasHarian? = null,
    val jumlahTugas: Int = 0,
    val cuaca: AppResult<Cuaca> = AppResult.Loading(),
    val offline: Boolean = false,
    val refreshing: Boolean = false,
    val cacheStale: Boolean = false,
    val message: String? = null,
    val hujanBerikutnya: List<PeluangHujan> = emptyList(),
)
