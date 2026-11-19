package com.unpad.padukampus.ui.profile

data class ProfileUiState(
    val loading: Boolean = true,
    val latitude: String = "",
    val longitude: String = "",
    val saving: Boolean = false,
    val message: String? = null,
)
