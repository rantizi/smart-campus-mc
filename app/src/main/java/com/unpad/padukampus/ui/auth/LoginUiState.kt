package com.unpad.padukampus.ui.auth

import com.unpad.padukampus.core.common.UiText

data class LoginUiState(
    val modeDaftar: Boolean = false,
    val email: String = "",
    val sandi: String = "",
    val emailError: UiText? = null,
    val sandiError: UiText? = null,
    val loading: Boolean = false,
    val pesan: UiText? = null,
)
