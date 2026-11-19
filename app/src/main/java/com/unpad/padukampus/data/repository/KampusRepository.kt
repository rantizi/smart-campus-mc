package com.unpad.padukampus.data.repository

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.unpad.padukampus.data.local.dao.KampusDao
import com.unpad.padukampus.data.local.entity.GedungEntity
import com.unpad.padukampus.data.local.entity.RuanganEntity
import com.unpad.padukampus.data.local.relation.RuanganDetail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

data class KoordinatKampus(val latitude: Double, val longitude: Double) {
    init {
        require(
            latitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude.isFinite() &&
                longitude in -180.0..180.0
        ) {
            "Koordinat kampus tidak valid"
        }
    }

    val cacheKey: String
        get() = "$latitude,$longitude"
}

interface KampusRepository {
    val koordinat: Flow<KoordinatKampus?>

    fun observeGedung(): Flow<List<GedungEntity>>

    fun observeRuangan(search: String): Flow<List<RuanganDetail>>

    suspend fun simpanKoordinat(value: KoordinatKampus)

    suspend fun simpanGedung(value: GedungEntity)

    suspend fun simpanRuangan(value: RuanganEntity)
}

class OfflineKampusRepository(
    private val dao: KampusDao,
    private val preferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : KampusRepository {
    private fun readCoordinates(): KoordinatKampus? =
        runCatching {
                val lat = preferences.getString("lat", null)?.toDoubleOrNull() ?: return null
                val lon = preferences.getString("lon", null)?.toDoubleOrNull() ?: return null
                KoordinatKampus(lat, lon)
            }
            .getOrNull()

    override val koordinat: Flow<KoordinatKampus?> =
        callbackFlow {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                        trySend(readCoordinates())
                    }
                preferences.registerOnSharedPreferenceChangeListener(listener)
                trySend(readCoordinates())
                awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            .distinctUntilChanged()

    override fun observeGedung() = dao.observeGedung()

    override fun observeRuangan(search: String) = dao.observeRuangan(search.trim())

    @SuppressLint("UseKtx") // Check commit's Boolean result; the KTX edit helper returns Unit.
    override suspend fun simpanKoordinat(value: KoordinatKampus) =
        withContext(ioDispatcher) {
            check(
                preferences
                    .edit()
                    .putString("lat", value.latitude.toString())
                    .putString("lon", value.longitude.toString())
                    .commit()
            ) {
                "Koordinat gagal disimpan"
            }
        }

    override suspend fun simpanGedung(value: GedungEntity) {
        require(value.nama.isNotBlank() && value.kode.isNotBlank()) {
            "Nama dan kode gedung wajib diisi"
        }
        KoordinatKampus(value.latitude, value.longitude)
        dao.simpanGedung(value)
    }

    override suspend fun simpanRuangan(value: RuanganEntity) {
        require(value.nama.isNotBlank() && value.kode.isNotBlank()) {
            "Nama dan kode ruangan wajib diisi"
        }
        dao.simpanRuangan(value)
    }
}
