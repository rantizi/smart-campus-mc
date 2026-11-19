package com.unpad.padukampus.data.repository

import com.google.gson.Gson
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.util.NetworkMonitor
import com.unpad.padukampus.data.local.dao.CuacaDao
import com.unpad.padukampus.data.local.entity.CacheCuacaEntity
import com.unpad.padukampus.data.remote.api.OpenMeteoApi
import com.unpad.padukampus.data.remote.dto.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CuacaRepositoryTest {
    private val now = 1_800_000_000_000L
    private val clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)
    private val location = KoordinatKampus(0.0, 0.0) // Synthetic test point, not campus seed data.
    private val hourly = HourlyWeatherDto(listOf("2026-11-19T13:00"), listOf(50))

    private fun cache(time: Long = now, key: String = location.cacheKey) =
        CacheCuacaEntity(key, 25.0, 3, Gson().toJson(hourly), time)

    private class FakeDao(initial: CacheCuacaEntity?) : CuacaDao {
        val stored = MutableStateFlow(initial)

        override fun observeCache(key: String) = stored.map { it?.takeIf { row -> row.id == key } }

        override suspend fun getCache(key: String) = stored.value?.takeIf { it.id == key }

        override suspend fun simpanCache(value: CacheCuacaEntity) {
            stored.value = value
        }
    }

    private class FakeNetwork(var online: Boolean = true) : NetworkMonitor {
        override val isOnline = flowOf(online)

        override fun isOnlineNow() = online
    }

    private inner class FakeApi : OpenMeteoApi {
        var calls = 0
        var failure: Exception? = null
        var result = WeatherDto(CurrentWeatherDto(26.0, 3), hourly)

        override suspend fun forecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            timezone: String,
            days: Int,
        ): WeatherDto {
            calls++
            failure?.let { throw it }
            return result
        }
    }

    @Test
    fun freshCacheSkipsHttp() = runTest {
        val api = FakeApi()
        val repository = OfflineCuacaRepository(api, FakeDao(cache()), FakeNetwork(), clock)
        assertTrue(repository.refresh(location) is AppResult.Success)
        assertEquals(0, api.calls)
    }

    @Test
    fun expiredCacheRefreshIsPersistedBeforeUiReadsIt() = runTest {
        val api = FakeApi()
        val dao = FakeDao(cache(now - 1_800_000))
        val repository = OfflineCuacaRepository(api, dao, FakeNetwork(), clock)
        assertTrue(repository.refresh(location) is AppResult.Success)
        assertEquals(1, api.calls)
        assertEquals(
            26.0,
            (repository.observeCuaca(location).first() as AppResult.Success).data.suhu,
            0.0,
        )
    }

    @Test
    fun offlineRetainsExpiredDataWithoutHttp() = runTest {
        val api = FakeApi()
        val old = cache(now - 2_000_000)
        val dao = FakeDao(old)
        val repository = OfflineCuacaRepository(api, dao, FakeNetwork(false), clock)
        assertTrue(repository.refresh(location) is AppResult.Error)
        assertEquals(old, dao.stored.value)
        assertEquals(0, api.calls)
    }

    @Test
    fun networkFailureDoesNotOverwriteCache() = runTest {
        val api = FakeApi().apply { failure = java.io.IOException("fixture") }
        val old = cache(now - 2_000_000)
        val dao = FakeDao(old)
        assertTrue(
            OfflineCuacaRepository(api, dao, FakeNetwork(), clock).refresh(location)
                is AppResult.Error
        )
        assertEquals(old, dao.stored.value)
    }

    @Test
    fun aDifferentLocationCannotReuseFreshCache() = runTest {
        val api = FakeApi()
        val dao = FakeDao(cache())
        OfflineCuacaRepository(api, dao, FakeNetwork(), clock).refresh(KoordinatKampus(1.0, 1.0))
        assertEquals(1, api.calls)
        assertEquals("1.0,1.0", dao.stored.value!!.id)
    }

    @Test
    fun malformedHourlyResponseDoesNotOverwriteCache() = runTest {
        val api =
            FakeApi().apply {
                result =
                    WeatherDto(
                        CurrentWeatherDto(26.0, 3),
                        HourlyWeatherDto(listOf("bad"), emptyList()),
                    )
            }
        val old = cache(now - 2_000_000)
        val dao = FakeDao(old)
        assertTrue(
            OfflineCuacaRepository(api, dao, FakeNetwork(), clock).refresh(location)
                is AppResult.Error
        )
        assertEquals(old, dao.stored.value)
    }

    @Test
    fun cancellationPropagatesRatherThanBecomingNetworkError() = runTest {
        val api = FakeApi().apply { failure = CancellationException("fixture") }
        try {
            OfflineCuacaRepository(api, FakeDao(null), FakeNetwork(), clock).refresh(location)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {}
    }
}
