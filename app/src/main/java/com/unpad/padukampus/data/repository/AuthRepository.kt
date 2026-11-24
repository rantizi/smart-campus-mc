package com.unpad.padukampus.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.unpad.padukampus.core.util.AuthValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AkunPengguna(val uid: String, val email: String)

enum class AuthGagal {
    EMAIL_TIDAK_VALID,
    SANDI_TERLALU_PENDEK,
    KREDENSIAL_SALAH,
    EMAIL_SUDAH_TERDAFTAR,
    SANDI_LEMAH,
    JARINGAN,
    TERLALU_BANYAK_PERCOBAAN,
    BELUM_DIKONFIGURASI,
    LAINNYA,
}

sealed interface AuthHasil {
    data class Berhasil(val akun: AkunPengguna) : AuthHasil

    data class Gagal(val alasan: AuthGagal) : AuthHasil
}

sealed interface KeluarHasil {
    data object Berhasil : KeluarHasil

    /** Ada perubahan yang belum terkirim; logout akan menghapusnya dari perangkat. */
    data class AdaPerubahanBelumTerkirim(val jumlah: Int) : KeluarHasil
}

/** Langkah sesi yang dijalankan di luar umur layar (sinkron awal, pembersihan data). */
interface SesiPengguna {
    suspend fun setelahMasuk(akun: AkunPengguna)

    /** Mencoba mengirim perubahan terakhir; mengembalikan jumlah yang masih menunggu. */
    suspend fun kirimSebelumKeluar(): Int

    suspend fun bersihkanSaatKeluar()
}

/** FR-01: daftar, login, logout dengan email dan kata sandi. */
interface AuthRepository {
    /** False bila Firebase belum dikonfigurasi: aplikasi berjalan dalam mode lokal tanpa login. */
    val tersedia: Boolean
    val akun: StateFlow<AkunPengguna?>

    suspend fun masuk(email: String, sandi: String): AuthHasil

    suspend fun daftar(email: String, sandi: String): AuthHasil

    suspend fun keluar(paksa: Boolean = false): KeluarHasil
}

class LocalOnlyAuthRepository : AuthRepository {
    override val tersedia = false
    override val akun: StateFlow<AkunPengguna?> = MutableStateFlow(null)

    override suspend fun masuk(email: String, sandi: String) =
        AuthHasil.Gagal(AuthGagal.BELUM_DIKONFIGURASI)

    override suspend fun daftar(email: String, sandi: String) =
        AuthHasil.Gagal(AuthGagal.BELUM_DIKONFIGURASI)

    override suspend fun keluar(paksa: Boolean) = KeluarHasil.Berhasil
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val sesi: SesiPengguna,
    private val applicationScope: CoroutineScope,
) : AuthRepository {
    override val tersedia = true
    private val _akun = MutableStateFlow(auth.currentUser?.toAkun())
    override val akun: StateFlow<AkunPengguna?> = _akun.asStateFlow()

    init {
        auth.addAuthStateListener { _akun.value = it.currentUser?.toAkun() }
    }

    override suspend fun masuk(email: String, sandi: String): AuthHasil =
        jalankan(email, sandi) { auth.signInWithEmailAndPassword(email.trim(), sandi).await().user }

    override suspend fun daftar(email: String, sandi: String): AuthHasil =
        jalankan(email, sandi) {
            auth.createUserWithEmailAndPassword(email.trim(), sandi).await().user
        }

    override suspend fun keluar(paksa: Boolean): KeluarHasil =
        // Dijalankan di scope aplikasi: layar Profil boleh hilang di tengah proses.
        applicationScope
            .async {
                if (!paksa) {
                    val menunggu = sesi.kirimSebelumKeluar()
                    if (menunggu > 0) return@async KeluarHasil.AdaPerubahanBelumTerkirim(menunggu)
                }
                // Data dihapus sebelum signOut: bila proses terhenti di tengah, akun berikutnya
                // tidak pernah melihat data akun sebelumnya (NFR-03).
                sesi.bersihkanSaatKeluar()
                auth.signOut()
                _akun.value = null
                KeluarHasil.Berhasil
            }
            .await()

    private suspend fun jalankan(
        email: String,
        sandi: String,
        aksi: suspend () -> FirebaseUser?,
    ): AuthHasil {
        if (!AuthValidator.emailValid(email)) return AuthHasil.Gagal(AuthGagal.EMAIL_TIDAK_VALID)
        if (!AuthValidator.sandiValid(sandi)) return AuthHasil.Gagal(AuthGagal.SANDI_TERLALU_PENDEK)
        // Begitu Firebase memberi tahu login berhasil, MainActivity menutup layar Login dan
        // ViewModel-nya dibatalkan. Proses ini dijalankan di scope aplikasi agar sinkron awal
        // tetap dimulai walaupun pemanggilnya sudah tidak ada (bug B-02, docs/LOG_BUG_MINGGU_03.md).
        return applicationScope
            .async {
                try {
                    val user = aksi() ?: return@async AuthHasil.Gagal(AuthGagal.LAINNYA)
                    val akun = user.toAkun()
                    _akun.value = akun
                    // Saudara, bukan anak, dari async ini: hasil login tidak menunggu sinkron selesai.
                    applicationScope.launch { sesi.setelahMasuk(akun) }
                    AuthHasil.Berhasil(akun)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    AuthHasil.Gagal(alasan(error))
                }
            }
            .await()
    }

    private fun alasan(error: Exception): AuthGagal =
        when (error) {
            is FirebaseAuthWeakPasswordException -> AuthGagal.SANDI_LEMAH
            is FirebaseAuthUserCollisionException -> AuthGagal.EMAIL_SUDAH_TERDAFTAR
            is FirebaseAuthInvalidUserException -> AuthGagal.KREDENSIAL_SALAH
            is FirebaseAuthInvalidCredentialsException ->
                if (error.errorCode == "ERROR_INVALID_EMAIL") AuthGagal.EMAIL_TIDAK_VALID
                else AuthGagal.KREDENSIAL_SALAH
            is FirebaseNetworkException -> AuthGagal.JARINGAN
            is FirebaseTooManyRequestsException -> AuthGagal.TERLALU_BANYAK_PERCOBAAN
            else -> AuthGagal.LAINNYA
        }

    private fun FirebaseUser.toAkun() = AkunPengguna(uid, email.orEmpty())
}
