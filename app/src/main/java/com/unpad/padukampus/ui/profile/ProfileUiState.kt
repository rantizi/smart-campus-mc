package com.unpad.padukampus.ui.profile

import com.unpad.padukampus.core.common.UiText

data class ProfileUiState(
    val loading: Boolean = true,
    val latitude: String = "",
    val longitude: String = "",
    val saving: Boolean = false,
    val message: UiText? = null,
)
