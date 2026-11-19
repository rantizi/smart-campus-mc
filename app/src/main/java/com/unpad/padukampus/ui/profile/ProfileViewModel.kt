package com.unpad.padukampus.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.data.remote.sync.CloudSyncManager
import com.unpad.padukampus.data.repository.KampusRepository
import com.unpad.padukampus.data.repository.KoordinatKampus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: KampusRepository,
    private val cloud: CloudSyncManager,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            ProfileUiState(
                latitude = savedState["lat_draft"] ?: "",
                longitude = savedState["lon_draft"] ?: "",
            )
        )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val location = repository.koordinat.first()
                if (savedState.get<Boolean>("profile_initialized") != true) {
                    updateCoordinates(
                        location?.latitude?.toString().orEmpty(),
                        location?.longitude?.toString().orEmpty(),
                    )
                    savedState["profile_initialized"] = true
                }
                _uiState.value = _uiState.value.copy(loading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value =
                    _uiState.value.copy(loading = false, message = "Pengaturan belum dapat dibaca")
            }
        }
    }

    fun updateCoordinates(lat: String, lon: String) {
        savedState["lat_draft"] = lat
        savedState["lon_draft"] = lon
        _uiState.value = _uiState.value.copy(latitude = lat, longitude = lon)
    }

    fun saveCoordinates() {
        if (_uiState.value.saving || _uiState.value.loading) return
        val draft = _uiState.value
        _uiState.value = draft.copy(saving = true, message = null)
        viewModelScope.launch {
            try {
                val lat = draft.latitude.toDoubleOrNull()
                val lon = draft.longitude.toDoubleOrNull()
                require(lat != null && lon != null) { "Latitude dan longitude harus berupa angka" }
                repository.simpanKoordinat(KoordinatKampus(lat, lon))
                _uiState.value =
                    _uiState.value.copy(
                        saving = false,
                        message = "Koordinat tersimpan. Cuaca dapat diperbarui dari Beranda.",
                    )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(saving = false, message = error.message)
            } catch (_: Exception) {
                _uiState.value =
                    _uiState.value.copy(saving = false, message = "Koordinat belum dapat disimpan")
            }
        }
    }

    fun requestSync() {
        viewModelScope.launch {
            val result = cloud.sync()
            _uiState.value = _uiState.value.copy(message = (result as? AppResult.Error)?.message)
        }
    }
}
