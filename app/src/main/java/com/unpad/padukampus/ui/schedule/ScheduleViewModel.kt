package com.unpad.padukampus.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.repository.JadwalRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    repository: JadwalRepository,
    private val savedState: SavedStateHandle,
    clock: Clock,
) : ViewModel() {
    private val dates =
        savedState.getStateFlow("schedule_date", DateTimeUtils.today(clock).toString())
    val uiState =
        dates
            .flatMapLatest { value ->
                val date = LocalDate.parse(value)
                repository
                    .observeJadwal(date)
                    .map { ScheduleUiState(date, loading = false, jadwal = it) }
                    .onStart { emit(ScheduleUiState(date)) }
                    .catch {
                        emit(
                            ScheduleUiState(
                                date,
                                loading = false,
                                error = "Jadwal belum dapat dibaca",
                            )
                        )
                    }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ScheduleUiState(LocalDate.parse(dates.value)),
            )

    fun shiftDay(days: Long) {
        savedState["schedule_date"] = LocalDate.parse(dates.value).plusDays(days).toString()
    }
}
