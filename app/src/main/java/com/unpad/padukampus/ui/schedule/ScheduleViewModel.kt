package com.unpad.padukampus.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.repository.JadwalRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val repository: JadwalRepository,
    private val savedState: SavedStateHandle,
    clock: Clock,
) : ViewModel() {
    private val dates =
        savedState.getStateFlow("schedule_date", DateTimeUtils.today(clock).toString())
    private val mutationError = MutableStateFlow<UiText?>(null)
    private val schedule =
        dates.flatMapLatest { value ->
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
                            error = UiText.Res(R.string.error_schedule_read),
                        )
                    )
                }
        }
    val uiState =
        combine(schedule, mutationError) { state, error ->
                state.copy(error = error ?: state.error)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ScheduleUiState(LocalDate.parse(dates.value)),
            )

    val tanggal: LocalDate
        get() = LocalDate.parse(dates.value)

    fun shiftDay(days: Long) {
        savedState["schedule_date"] = LocalDate.parse(dates.value).plusDays(days).toString()
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                mutationError.value = null
                repository.hapusJadwal(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutationError.value = UiText.Res(R.string.error_schedule_delete)
            }
        }
    }
}
