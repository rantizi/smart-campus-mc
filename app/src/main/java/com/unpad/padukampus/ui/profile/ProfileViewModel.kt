package com.unpad.padukampus.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.data.remote.sync.CloudSyncManager
import com.unpad.padukampus.data.repository.AuthRepository
import com.unpad.padukampus.data.repository.KampusRepository
import com.unpad.padukampus.data.repository.KeluarHasil
import com.unpad.padukampus.data.repository.KoordinatKampus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: KampusRepository,
    private val cloud: CloudSyncManager,
    private val auth: AuthRepository,
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
            combine(auth.akun, cloud.jumlahMenunggu, cloud.terakhirSinkron) { akun, menunggu, waktu ->
                    Triple(akun?.email.orEmpty(), menunggu, waktu)
                }
                .catch { /* Status sinkron hanya informasi; kegagalan baca tidak menghentikan layar. */ }
                .collect { (email, menunggu, waktu) ->
                    _uiState.update {
                        it.copy(
                            cloudTersedia = cloud.tersedia && auth.tersedia,
                            email = email,
                            jumlahMenunggu = menunggu,
                            terakhirSinkron = waktu,
                        )
                    }
                }
        }
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
                    _uiState.value.copy(
                        loading = false,
                        message = UiText.Res(R.string.error_settings_read),
                    )
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
                if (lat == null || lon == null) {
                    _uiState.value =
                        _uiState.value.copy(
                            saving = false,
                            message = UiText.Res(R.string.error_coordinates_number),
                        )
                    return@launch
                }
                repository.simpanKoordinat(KoordinatKampus(lat, lon))
                _uiState.value =
                    _uiState.value.copy(
                        saving = false,
                        message = UiText.Res(R.string.profile_coordinates_saved),
                    )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalArgumentException) {
                _uiState.value =
                    _uiState.value.copy(
                        saving = false,
                        message = UiText.from(error, R.string.error_coordinates_save),
                    )
            } catch (_: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        saving = false,
                        message = UiText.Res(R.string.error_coordinates_save),
                    )
            }
        }
    }

    fun requestSync() {
        if (_uiState.value.sinkronBerjalan) return
        _uiState.update { it.copy(sinkronBerjalan = true, message = null) }
        viewModelScope.launch {
            val result = cloud.sync()
            _uiState.update {
                it.copy(
                    sinkronBerjalan = false,
                    message =
                        when (result) {
                            is AppResult.Error -> UiText.Raw(result.message)
                            else -> UiText.Res(R.string.profile_sync_done)
                        },
                )
            }
        }
    }

    /** FR-01 logout; [paksa] dipakai setelah pengguna setuju perubahan lokal ikut terhapus. */
    fun keluar(paksa: Boolean = false) {
        if (_uiState.value.keluarBerjalan) return
        _uiState.update { it.copy(keluarBerjalan = true, konfirmasiKeluar = null, message = null) }
        viewModelScope.launch {
            when (val hasil = auth.keluar(paksa)) {
                is KeluarHasil.AdaPerubahanBelumTerkirim ->
                    _uiState.update {
                        it.copy(keluarBerjalan = false, konfirmasiKeluar = hasil.jumlah)
                    }
                // MainActivity mengamati akun dan membuka Login dengan back stack bersih.
                KeluarHasil.Berhasil -> _uiState.update { it.copy(keluarBerjalan = false) }
            }
        }
    }

    fun batalKeluar() {
        _uiState.update { it.copy(konfirmasiKeluar = null) }
    }
}
