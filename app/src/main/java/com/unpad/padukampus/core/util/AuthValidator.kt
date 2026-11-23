package com.unpad.padukampus.core.util

import com.unpad.padukampus.core.constants.AppConstants

/** NFR-04: format email dan panjang kata sandi divalidasi sebelum dikirim ke Firebase. */
object AuthValidator {
    // Sengaja tidak memakai android.util.Patterns agar bisa diuji di JVM.
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailValid(email: String): Boolean = EMAIL.matches(email.trim())

    fun sandiValid(sandi: String): Boolean = sandi.length >= AppConstants.MIN_PASSWORD_LENGTH
}
