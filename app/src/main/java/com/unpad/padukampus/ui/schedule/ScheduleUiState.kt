package com.unpad.padukampus.ui.schedule

import com.unpad.padukampus.data.repository.KelasHarian
import java.time.LocalDate

data class ScheduleUiState(
    val tanggal: LocalDate,
    val loading: Boolean = true,
    val jadwal: List<KelasHarian> = emptyList(),
    val error: String? = null,
)
