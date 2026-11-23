package com.unpad.padukampus.ui.profile

import com.unpad.padukampus.core.common.UiText

data class ProfileUiState(
    val loading: Boolean = true,
    val latitude: String = "",
    val longitude: String = "",
    val saving: Boolean = false,
    val message: UiText? = null,
    val cloudTersedia: Boolean = false,
    val email: String = "",
    val jumlahMenunggu: Int = 0,
    val terakhirSinkron: Long? = null,
    val sinkronBerjalan: Boolean = false,
    val keluarBerjalan: Boolean = false,
    /** Diisi jumlah perubahan belum terkirim saat logout perlu dikonfirmasi pengguna. */
    val konfirmasiKeluar: Int? = null,
)
