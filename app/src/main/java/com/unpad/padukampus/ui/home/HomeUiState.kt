package com.unpad.padukampus.ui.home

import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.data.local.relation.TugasDetail
import com.unpad.padukampus.data.repository.Cuaca
import com.unpad.padukampus.data.repository.KelasHarian
import com.unpad.padukampus.data.repository.PeluangHujan
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val jadwal: List<KelasHarian> = emptyList(),
    val kelasBerikutnya: KelasHarian? = null,
    val jumlahTugas: Int = 0,
    val cuaca: AppResult<Cuaca> = AppResult.Loading(),
    val offline: Boolean = false,
    val refreshing: Boolean = false,
    val cacheStale: Boolean = false,
    val message: UiText? = null,
    val hujanBerikutnya: List<PeluangHujan> = emptyList(),
    val tanggal: LocalDate? = null,
    /** Tugas belum selesai dengan deadline paling dekat (kartu "Tugas terdekat"). */
    val tugasTerdekat: TugasDetail? = null,
    /** Koordinat cuaca belum diatur di Profil. */
    val butuhKoordinat: Boolean = false,
)
