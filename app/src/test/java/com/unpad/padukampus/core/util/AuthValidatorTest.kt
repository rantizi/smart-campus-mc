package com.unpad.padukampus.core.util

import org.junit.Assert.*
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun acceptsCommonEmailFormats() {
        assertTrue(AuthValidator.emailValid("mahasiswa@mail.unpad.ac.id"))
        assertTrue(AuthValidator.emailValid("  nama.belakang+tag@gmail.com "))
    }

    @Test
    fun rejectsMalformedEmail() {
        listOf("", "nama", "nama@", "@unpad.ac.id", "nama@unpad", "nama unpad@x.id").forEach {
            assertFalse(it, AuthValidator.emailValid(it))
        }
    }

    @Test
    fun passwordNeedsSixCharacters() {
        assertFalse(AuthValidator.sandiValid("12345"))
        assertTrue(AuthValidator.sandiValid("123456"))
    }
}
