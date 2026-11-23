package com.unpad.padukampus.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.data.repository.JadwalInput
import com.unpad.padukampus.data.repository.JadwalRepository
import com.unpad.padukampus.data.repository.KampusRepository
import kotlinx.coroutines.CancellationException
import com.unpad.padukampus.data.local.entity.MataKuliahEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Form Jadwal (FR-02): tambah atau ubah mata kuliah beserta jadwal mingguannya. */
class ScheduleFormViewModel(
    private val repository: JadwalRepository,
    kampusRepository: KampusRepository,
    private val savedState: SavedStateHandle,
    private val jadwalId: String = "",
) : ViewModel() {
    private fun restoredDraft() =
        ScheduleFormDraft(
            savedState["course"] ?: "",
            savedState["code"] ?: "",
            savedState["lecturer"] ?: "",
            savedState["credits"] ?: "",
            savedState["day"] ?: 1,
            savedState["start"] ?: "",
            savedState["end"] ?: "",
            savedState["room"],
            savedState["reminder"] ?: "15",
        )

    private val _state =
        MutableStateFlow(
            ScheduleFormUiState(draft = restoredDraft(), editing = jadwalId.isNotEmpty())
        )
    val state: StateFlow<ScheduleFormUiState> = _state.asStateFlow()

    /** Mata kuliah kurikulum dan buatan pengguna untuk dropdown Form Jadwal. */
    val daftarMataKuliah: StateFlow<List<MataKuliahEntity>> =
        repository
            .observeMataKuliah()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            kampusRepository
                .observeRuangan("")
                .catch {
                    _state.value = _state.value.copy(error = UiText.Res(R.string.error_rooms_read))
                }
                .collect { rooms -> _state.value = _state.value.copy(ruangan = rooms) }
        }
        if (jadwalId.isNotEmpty() && savedState.get<Boolean>("schedule_loaded") != true) {
            _state.value = _state.value.copy(loading = true)
            viewModelScope.launch {
                try {
                    val detail =
                        repository.getJadwalDetail(jadwalId) ?: error("Jadwal tidak tersedia")
                    updateDraft {
                        ScheduleFormDraft(
                            detail.mataKuliah.nama,
                            detail.mataKuliah.kode,
                            detail.mataKuliah.dosen,
                            detail.mataKuliah.sks.takeIf { it > 0 }?.toString().orEmpty(),
                            detail.jadwal.hari,
                            detail.jadwal.jamMulai,
                            detail.jadwal.jamSelesai,
                            detail.jadwal.ruanganId,
                            detail.jadwal.pengingatMenit.toString(),
                        )
                    }
                    savedState["schedule_loaded"] = true
                    _state.value = _state.value.copy(loading = false)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.value =
                        _state.value.copy(
                            loading = false,
                            unavailable = true,
                            error = UiText.Res(R.string.error_schedule_unavailable),
                        )
                }
            }
        }
    }

    /** Memilih mata kuliah dari dropdown mengisi sandi, SKS dan dosen yang masih kosong. */
    fun pilihMataKuliah(nama: String) {
        val mk = daftarMataKuliah.value.firstOrNull { it.nama == nama } ?: return
        updateDraft { draft ->
            draft.copy(
                mataKuliah = mk.nama,
                kode = draft.kode.ifBlank { mk.kode },
                dosen = draft.dosen.ifBlank { mk.dosen },
                sks = draft.sks.ifBlank { mk.sks.takeIf { it > 0 }?.toString().orEmpty() },
            )
        }
    }

    fun updateDraft(transform: (ScheduleFormDraft) -> ScheduleFormDraft) {
        val draft = transform(_state.value.draft)
        savedState["course"] = draft.mataKuliah
        savedState["code"] = draft.kode
        savedState["lecturer"] = draft.dosen
        savedState["credits"] = draft.sks
        savedState["day"] = draft.hari
        savedState["start"] = draft.jamMulai
        savedState["end"] = draft.jamSelesai
        savedState["room"] = draft.ruanganId
        savedState["reminder"] = draft.pengingatMenit
        _state.value = _state.value.copy(draft = draft)
    }

    fun save() {
        val current = _state.value
        if (current.loading || current.saving || current.unavailable) return
        val draft = current.draft
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val sks = if (draft.sks.isBlank()) 0 else draft.sks.trim().toIntOrNull()
                val minutes = draft.pengingatMenit.trim().toIntOrNull()
                if (sks == null || minutes == null) {
                    val message =
                        if (sks == null) R.string.error_credits_number
                        else R.string.error_reminder_number
                    _state.value = _state.value.copy(saving = false, error = UiText.Res(message))
                    return@launch
                }
                repository.simpanJadwal(
                    JadwalInput(
                        id = jadwalId.takeIf { it.isNotEmpty() },
                        mataKuliahNama = draft.mataKuliah,
                        kode = draft.kode,
                        dosen = draft.dosen,
                        sks = sks,
                        hari = draft.hari,
                        jamMulai = draft.jamMulai,
                        jamSelesai = draft.jamSelesai,
                        ruanganId = draft.ruanganId,
                        pengingatMenit = minutes,
                    )
                )
                _state.value = _state.value.copy(saving = false, saved = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalArgumentException) {
                _state.value =
                    _state.value.copy(
                        saving = false,
                        error = UiText.from(error, R.string.error_schedule_save),
                    )
            } catch (_: Exception) {
                _state.value =
                    _state.value.copy(
                        saving = false,
                        error = UiText.Res(R.string.error_schedule_save),
                    )
            }
        }
    }

    fun consumeSaved() {
        _state.value = _state.value.copy(saved = false)
    }
}
