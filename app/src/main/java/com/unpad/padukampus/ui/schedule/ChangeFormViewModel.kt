package com.unpad.padukampus.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.local.entity.PerubahanJadwalEntity
import com.unpad.padukampus.data.model.JenisPerubahan
import com.unpad.padukampus.data.repository.JadwalRepository
import com.unpad.padukampus.data.repository.KampusRepository
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Form Perubahan (FR-04): mencatat pindah ruangan, ganti jam, atau kelas batal pada satu tanggal.
 * Perubahan menggantikan jadwal reguler hanya pada tanggal tersebut (JadwalResolver).
 */
class ChangeFormViewModel(
    private val repository: JadwalRepository,
    kampusRepository: KampusRepository,
    private val savedState: SavedStateHandle,
    jadwalId: String,
    tanggal: String,
    clock: Clock,
) : ViewModel() {
    private fun restoredDraft(defaultJadwal: String?, defaultTanggal: String) =
        ChangeFormDraft(
            savedState["schedule"] ?: defaultJadwal,
            savedState["date"] ?: defaultTanggal,
            savedState["type"] ?: JenisPerubahan.PINDAH_RUANG,
            savedState["room"],
            savedState["start"] ?: "",
            savedState["end"] ?: "",
            savedState["note"] ?: "",
        )

    private val _state =
        MutableStateFlow(
            ChangeFormUiState(
                draft =
                    restoredDraft(
                        jadwalId.ifEmpty { null },
                        tanggal.ifEmpty { DateTimeUtils.today(clock).toString() },
                    )
            )
        )
    val state: StateFlow<ChangeFormUiState> = _state.asStateFlow()
    private var lookupJob: Job? = null
    private var lookedUp: Pair<String, String>? = savedState["looked_up"]

    init {
        viewModelScope.launch {
            repository
                .observeSemuaJadwal()
                .catch {
                    _state.value =
                        _state.value.copy(error = UiText.Res(R.string.error_schedule_read))
                }
                .collect { rows ->
                    val draft = _state.value.draft
                    _state.value = _state.value.copy(jadwal = rows)
                    // Tanpa argumen, pilih jadwal pertama agar form langsung dapat dipakai.
                    if (draft.jadwalId == null && rows.isNotEmpty())
                        updateDraft { it.copy(jadwalId = rows.first().jadwal.id) }
                }
        }
        viewModelScope.launch {
            kampusRepository
                .observeRuangan("")
                .catch {
                    _state.value = _state.value.copy(error = UiText.Res(R.string.error_rooms_read))
                }
                .collect { rooms -> _state.value = _state.value.copy(ruangan = rooms) }
        }
        lookupExisting()
    }

    fun updateDraft(transform: (ChangeFormDraft) -> ChangeFormDraft) {
        val before = _state.value.draft
        val draft = transform(before)
        savedState["schedule"] = draft.jadwalId
        savedState["date"] = draft.tanggal
        savedState["type"] = draft.jenis
        savedState["room"] = draft.ruanganBaruId
        savedState["start"] = draft.jamMulaiBaru
        savedState["end"] = draft.jamSelesaiBaru
        savedState["note"] = draft.catatan
        _state.value = _state.value.copy(draft = draft)
        if (draft.jadwalId != before.jadwalId || draft.tanggal != before.tanggal) lookupExisting()
    }

    /** Muat perubahan yang sudah tercatat untuk jadwal + tanggal agar dapat diubah atau dihapus. */
    private fun lookupExisting() {
        val draft = _state.value.draft
        val id = draft.jadwalId ?: return
        val date = runCatching { LocalDate.parse(draft.tanggal.trim()) }.getOrNull()
        if (date == null) {
            _state.value = _state.value.copy(existing = false)
            return
        }
        val key = id to date.toString()
        lookupJob?.cancel()
        lookupJob =
            viewModelScope.launch {
                val old = runCatching { repository.getPerubahan(id, date) }.getOrNull()
                _state.value = _state.value.copy(existing = old != null)
                // Isi form dari data tersimpan sekali per kombinasi agar ketikan tidak ditimpa.
                if (old != null && lookedUp != key) {
                    updateDraft {
                        it.copy(
                            jenis = old.jenis,
                            ruanganBaruId = old.ruanganBaruId,
                            jamMulaiBaru = old.jamMulaiBaru.orEmpty(),
                            jamSelesaiBaru = old.jamSelesaiBaru.orEmpty(),
                            catatan = old.catatan,
                        )
                    }
                }
                lookedUp = key
                savedState["looked_up"] = key
            }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val draft = current.draft
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val id = draft.jadwalId
                if (id == null) {
                    _state.value =
                        _state.value.copy(
                            saving = false,
                            error = UiText.Res(R.string.error_change_pick_schedule),
                        )
                    return@launch
                }
                val date = LocalDate.parse(draft.tanggal.trim())
                val pindahRuang = draft.jenis == JenisPerubahan.PINDAH_RUANG
                val gantiJam = draft.jenis == JenisPerubahan.GANTI_JAM
                repository.simpanPerubahan(
                    PerubahanJadwalEntity(
                        jadwalId = id,
                        ruanganBaruId = draft.ruanganBaruId.takeIf { pindahRuang },
                        tanggal = date.toString(),
                        jenis = draft.jenis,
                        jamMulaiBaru = draft.jamMulaiBaru.trim().takeIf { gantiJam },
                        jamSelesaiBaru = draft.jamSelesaiBaru.trim().takeIf { gantiJam },
                        catatan = draft.catatan.trim(),
                    )
                )
                _state.value = _state.value.copy(saving = false, saved = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: DateTimeParseException) {
                _state.value =
                    _state.value.copy(
                        saving = false,
                        error = UiText.Res(R.string.error_change_date),
                    )
            } catch (error: IllegalArgumentException) {
                _state.value =
                    _state.value.copy(
                        saving = false,
                        error = UiText.from(error, R.string.error_change_save),
                    )
            } catch (_: Exception) {
                _state.value =
                    _state.value.copy(
                        saving = false,
                        error = UiText.Res(R.string.error_change_save),
                    )
            }
        }
    }

    fun remove() {
        val draft = _state.value.draft
        val id = draft.jadwalId ?: return
        val date = runCatching { LocalDate.parse(draft.tanggal.trim()) }.getOrNull() ?: return
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                repository.hapusPerubahan(id, date)
                _state.value = _state.value.copy(saving = false, saved = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value =
                    _state.value.copy(
                        saving = false,
                        error = UiText.Res(R.string.error_change_delete),
                    )
            }
        }
    }

    fun consumeSaved() {
        _state.value = _state.value.copy(saved = false)
    }
}
