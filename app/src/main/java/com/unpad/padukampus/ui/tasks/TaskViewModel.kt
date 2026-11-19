package com.unpad.padukampus.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.repository.*
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TugasRepository,
    private val savedState: SavedStateHandle,
    private val taskId: String = "",
) : ViewModel() {
    private val filter = savedState.getStateFlow("task_filter", TugasFilter.SEMUA.name)
    private val mutationError = MutableStateFlow<String?>(null)
    private val taskList =
        filter.flatMapLatest { value ->
            val selected = TugasFilter.valueOf(value)
            repository
                .observeTugas(selected)
                .map { TaskUiState(loading = false, tugas = it, filter = selected) }
                .onStart { emit(TaskUiState(filter = selected)) }
                .catch {
                    emit(
                        TaskUiState(
                            loading = false,
                            filter = selected,
                            error = "Daftar tugas belum dapat dibaca",
                        )
                    )
                }
        }
    val uiState =
        combine(taskList, mutationError) { list, error -> list.copy(error = error ?: list.error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskUiState())

    private fun restoredDraft() =
        TaskFormDraft(
            savedState["course"] ?: "",
            savedState["title"] ?: "",
            savedState["description"] ?: "",
            savedState["deadline"] ?: "",
            savedState["source"] ?: "LAINNYA",
            savedState["reminder"] ?: "1440",
        )

    private val _formState = MutableStateFlow(TaskFormUiState(draft = restoredDraft()))
    val formState: StateFlow<TaskFormUiState> = _formState.asStateFlow()

    init {
        if (taskId.isNotEmpty() && savedState.get<Boolean>("task_loaded") != true) {
            _formState.value = _formState.value.copy(loading = true)
            viewModelScope.launch {
                try {
                    val detail = repository.getTugas(taskId) ?: error("Tugas tidak tersedia")
                    updateDraft {
                        TaskFormDraft(
                            detail.mataKuliah.nama,
                            detail.tugas.judul,
                            detail.tugas.deskripsi,
                            DateTimeUtils.deadlineInput(detail.tugas.deadline),
                            detail.tugas.sumber,
                            detail.tugas.pengingatMenit.toString(),
                        )
                    }
                    savedState["task_loaded"] = true
                    _formState.value = _formState.value.copy(loading = false)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _formState.value =
                        _formState.value.copy(
                            loading = false,
                            unavailable = true,
                            error =
                                "Tugas telah dihapus atau belum dapat dibaca. Kembali ke daftar tugas.",
                        )
                }
            }
        }
    }

    fun setFilter(value: TugasFilter) {
        savedState["task_filter"] = value.name
    }

    fun updateDraft(transform: (TaskFormDraft) -> TaskFormDraft) {
        val draft = transform(_formState.value.draft)
        savedState["course"] = draft.mataKuliah
        savedState["title"] = draft.judul
        savedState["description"] = draft.deskripsi
        savedState["deadline"] = draft.deadline
        savedState["source"] = draft.sumber
        savedState["reminder"] = draft.pengingatMenit
        _formState.value = _formState.value.copy(draft = draft)
    }

    fun save() {
        if (_formState.value.loading || _formState.value.saving || _formState.value.unavailable)
            return
        val draft = _formState.value.draft
        _formState.value = _formState.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val minutes =
                    draft.pengingatMenit.toIntOrNull()
                        ?: throw IllegalArgumentException("Menit pengingat harus berupa angka")
                val input =
                    TugasInput(
                        taskId.takeIf { it.isNotEmpty() },
                        draft.mataKuliah,
                        draft.judul,
                        draft.deskripsi,
                        DateTimeUtils.parseDeadline(draft.deadline),
                        draft.sumber,
                        minutes,
                    )
                repository.simpan(input)
                _formState.value = _formState.value.copy(saving = false, saved = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: DateTimeParseException) {
                _formState.value =
                    _formState.value.copy(
                        saving = false,
                        error = "Tanggal tidak valid. Gunakan yyyy-MM-dd HH:mm dalam WIB.",
                    )
            } catch (error: IllegalArgumentException) {
                _formState.value = _formState.value.copy(saving = false, error = error.message)
            } catch (_: Exception) {
                _formState.value =
                    _formState.value.copy(saving = false, error = "Tugas belum dapat disimpan")
            }
        }
    }

    fun consumeSaved() {
        _formState.value = _formState.value.copy(saved = false)
    }

    fun notificationPermissionChanged(enabled: Boolean) {
        val previouslyEnabled = _formState.value.notificationsEnabled
        _formState.value = _formState.value.copy(notificationsEnabled = enabled)
        if (enabled && !previouslyEnabled) mutate { repository.reconcileReminders() }
    }

    fun setSelesai(id: String, selesai: Boolean) = mutate { repository.setSelesai(id, selesai) }

    fun delete(id: String) = mutate { repository.hapus(id) }

    private fun mutate(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                mutationError.value = null
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutationError.value = "Perubahan tugas belum dapat disimpan"
            }
        }
    }
}
