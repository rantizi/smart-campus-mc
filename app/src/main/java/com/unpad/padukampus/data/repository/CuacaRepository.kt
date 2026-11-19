package com.unpad.padukampus.data.repository

import com.google.gson.Gson
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.core.util.NetworkMonitor
import com.unpad.padukampus.data.local.dao.CuacaDao
import com.unpad.padukampus.data.local.entity.CacheCuacaEntity
import com.unpad.padukampus.data.remote.api.OpenMeteoApi
import com.unpad.padukampus.data.remote.dto.HourlyWeatherDto
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class PeluangHujan(val waktu: LocalDateTime, val persen: Int?)

data class Cuaca(
    val suhu: Double,
    val kodeCuaca: Int,
    val hujan: List<PeluangHujan>,
    val diambilPada: Long,
)

interface CuacaRepository {
    fun observeCuaca(koordinat: KoordinatKampus): Flow<AppResult<Cuaca>>

    suspend fun refresh(koordinat: KoordinatKampus, force: Boolean = false): AppResult<Unit>
}

class OfflineCuacaRepository(
    private val api: OpenMeteoApi,
    private val dao: CuacaDao,
    private val network: NetworkMonitor,
    private val clock: Clock,
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CuacaRepository {
    private val refreshLock = Mutex()

    override fun observeCuaca(koordinat: KoordinatKampus): Flow<AppResult<Cuaca>> =
        dao.observeCache(koordinat.cacheKey)
            .map { cache ->
                if (cache == null) AppResult.Error("Cuaca belum tersimpan untuk lokasi ini")
                else
                    try {
                        AppResult.Success(decode(cache))
                    } catch (_: Exception) {
                        AppResult.Error(
                            "Cache cuaca tidak valid. Perbarui saat terhubung internet."
                        )
                    }
            }
            .flowOn(ioDispatcher)

    override suspend fun refresh(koordinat: KoordinatKampus, force: Boolean): AppResult<Unit> =
        withContext(ioDispatcher) {
            refreshLock.withLock {
                try {
                    val cached = dao.getCache(koordinat.cacheKey)
                    val validCache = cached?.let { runCatching { decode(it) }.isSuccess } == true
                    if (
                        !force &&
                            validCache &&
                            DateTimeUtils.isCacheFresh(cached!!.diambilPada, clock.millis())
                    ) {
                        return@withLock AppResult.Success(Unit)
                    }
                    if (!network.isOnlineNow())
                        return@withLock AppResult.Error("Sedang luring. Data lokal tetap tersedia.")
                    val response = api.forecast(koordinat.latitude, koordinat.longitude)
                    val current = requireNotNull(response.current) { "Suhu tidak tersedia" }
                    val temperature = requireNotNull(current.temperature)
                    val code = requireNotNull(current.weatherCode)
                    require(temperature.isFinite())
                    val hourly = requireNotNull(response.hourly)
                    validateHourly(hourly)
                    // UI only receives the subsequent Room emission, never this network DTO.
                    dao.simpanCache(
                        CacheCuacaEntity(
                            koordinat.cacheKey,
                            temperature,
                            code,
                            gson.toJson(hourly),
                            clock.millis(),
                        )
                    )
                    AppResult.Success(Unit)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    AppResult.Error(
                        "Cuaca gagal diperbarui. Data terakhir yang tersimpan tetap digunakan."
                    )
                }
            }
        }

    private fun validateHourly(dto: HourlyWeatherDto): List<PeluangHujan> {
        val time = requireNotNull(dto.time)
        val probability = requireNotNull(dto.probability)
        require(time.isNotEmpty() && time.size == probability.size)
        return time.indices.map { i ->
            require(probability[i] == null || probability[i]!! in 0..100)
            PeluangHujan(LocalDateTime.parse(time[i]), probability[i])
        }
    }

    private fun decode(cache: CacheCuacaEntity): Cuaca {
        require(cache.suhu.isFinite())
        val hourly = requireNotNull(gson.fromJson(cache.hujanPerJam, HourlyWeatherDto::class.java))
        return Cuaca(cache.suhu, cache.kodeCuaca, validateHourly(hourly), cache.diambilPada)
    }
}
