package com.unpad.padukampus.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.core.util.AuthValidator
import com.unpad.padukampus.data.repository.AuthGagal
import com.unpad.padukampus.data.repository.AuthHasil
import com.unpad.padukampus.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * FR-01 Login/Daftar. Navigasi setelah berhasil diurus MainActivity yang mengamati status akun,
 * sehingga layar ini tidak perlu tahu tujuan berikutnya. Kata sandi tidak disimpan di
 * SavedStateHandle.
 */
class LoginViewModel(
    private val auth: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            LoginUiState(
                modeDaftar = savedState[KEY_MODE] ?: false,
                email = savedState[KEY_EMAIL] ?: "",
            )
        )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun ubahEmail(value: String) {
        savedState[KEY_EMAIL] = value
        _uiState.update { it.copy(email = value, emailError = null, pesan = null) }
    }

    fun ubahSandi(value: String) {
        _uiState.update { it.copy(sandi = value, sandiError = null, pesan = null) }
    }

    fun gantiMode() {
        val daftar = !_uiState.value.modeDaftar
        savedState[KEY_MODE] = daftar
        _uiState.update {
            it.copy(modeDaftar = daftar, emailError = null, sandiError = null, pesan = null)
        }
    }

    fun kirim() {
        val state = _uiState.value
        if (state.loading) return
        val emailError =
            if (AuthValidator.emailValid(state.email)) null else UiText.Res(R.string.auth_email_invalid)
        val sandiError =
            if (AuthValidator.sandiValid(state.sandi)) null
            else UiText.Res(R.string.auth_password_short)
        if (emailError != null || sandiError != null) {
            _uiState.update { it.copy(emailError = emailError, sandiError = sandiError) }
            return
        }
        _uiState.update { it.copy(loading = true, pesan = null) }
        viewModelScope.launch {
            val hasil =
                if (state.modeDaftar) auth.daftar(state.email, state.sandi)
                else auth.masuk(state.email, state.sandi)
            _uiState.update {
                when (hasil) {
                    is AuthHasil.Berhasil -> it.copy(loading = false, sandi = "")
                    is AuthHasil.Gagal -> it.copy(loading = false, pesan = pesan(hasil.alasan))
                }
            }
        }
    }

    private fun pesan(alasan: AuthGagal): UiText =
        UiText.Res(
            when (alasan) {
                AuthGagal.EMAIL_TIDAK_VALID -> R.string.auth_email_invalid
                AuthGagal.SANDI_TERLALU_PENDEK -> R.string.auth_password_short
                AuthGagal.KREDENSIAL_SALAH -> R.string.auth_error_credentials
                AuthGagal.EMAIL_SUDAH_TERDAFTAR -> R.string.auth_error_email_used
                AuthGagal.SANDI_LEMAH -> R.string.auth_error_weak_password
                AuthGagal.JARINGAN -> R.string.auth_error_network
                AuthGagal.TERLALU_BANYAK_PERCOBAAN -> R.string.auth_error_too_many
                AuthGagal.BELUM_DIKONFIGURASI -> R.string.auth_error_not_configured
                AuthGagal.LAINNYA -> R.string.auth_error_unknown
            }
        )

    private companion object {
        const val KEY_EMAIL = "login_email"
        const val KEY_MODE = "login_mode_daftar"
    }
}
