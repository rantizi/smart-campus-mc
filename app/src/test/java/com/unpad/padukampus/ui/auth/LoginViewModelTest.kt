package com.unpad.padukampus.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.data.repository.*
import com.unpad.padukampus.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()

    @After
    fun cleanup() {
        store.clear()
    }

    private class FakeAuth(var hasil: AuthHasil = AuthHasil.Berhasil(AkunPengguna("u", "a@b.id"))) :
        AuthRepository {
        val panggilan = mutableListOf<String>()
        override val tersedia = true
        override val akun = MutableStateFlow<AkunPengguna?>(null)

        override suspend fun masuk(email: String, sandi: String): AuthHasil {
            panggilan += "masuk:$email"
            return hasil
        }

        override suspend fun daftar(email: String, sandi: String): AuthHasil {
            panggilan += "daftar:$email"
            return hasil
        }

        override suspend fun keluar(paksa: Boolean) = KeluarHasil.Berhasil
    }

    private fun model(auth: FakeAuth, saved: SavedStateHandle = SavedStateHandle()) =
        LoginViewModel(auth, saved).also { store.put("login", it) }

    @Test
    fun invalidInputShowsFieldErrorsWithoutCallingFirebase() = runTest {
        val auth = FakeAuth()
        val vm = model(auth)
        vm.ubahEmail("bukan-email")
        vm.ubahSandi("123")
        vm.kirim()
        runCurrent()
        assertEquals(UiText.Res(R.string.auth_email_invalid), vm.uiState.value.emailError)
        assertEquals(UiText.Res(R.string.auth_password_short), vm.uiState.value.sandiError)
        assertTrue(auth.panggilan.isEmpty())
    }

    @Test
    fun registerModeCallsDaftarAndClearsPasswordOnSuccess() = runTest {
        val auth = FakeAuth()
        val vm = model(auth)
        vm.gantiMode()
        vm.ubahEmail("mhs@unpad.ac.id")
        vm.ubahSandi("rahasia1")
        vm.kirim()
        runCurrent()
        assertEquals(listOf("daftar:mhs@unpad.ac.id"), auth.panggilan)
        assertEquals("", vm.uiState.value.sandi)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun firebaseErrorIsMappedToMessage() = runTest {
        val auth = FakeAuth(AuthHasil.Gagal(AuthGagal.KREDENSIAL_SALAH))
        val vm = model(auth)
        vm.ubahEmail("mhs@unpad.ac.id")
        vm.ubahSandi("salah123")
        vm.kirim()
        runCurrent()
        assertEquals(UiText.Res(R.string.auth_error_credentials), vm.uiState.value.pesan)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun emailAndModeSurviveProcessDeathButPasswordDoesNot() = runTest {
        val saved = SavedStateHandle()
        val first = model(FakeAuth(), saved)
        first.gantiMode()
        first.ubahEmail("mhs@unpad.ac.id")
        first.ubahSandi("rahasia1")
        val restored = LoginViewModel(FakeAuth(), saved)
        assertEquals("mhs@unpad.ac.id", restored.uiState.value.email)
        assertTrue(restored.uiState.value.modeDaftar)
        assertEquals("", restored.uiState.value.sandi)
    }
}
