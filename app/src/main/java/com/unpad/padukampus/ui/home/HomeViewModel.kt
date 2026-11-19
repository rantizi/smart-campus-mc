package com.unpad.padukampus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.core.common.*
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.core.util.NetworkMonitor
import com.unpad.padukampus.data.repository.*
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val jadwalRepository: JadwalRepository,
    private val tugasRepository: TugasRepository,
    private val cuacaRepository: CuacaRepository,
    private val kampusRepository: KampusRepository,
    network: NetworkMonitor,
    private val clock: Clock,
) : ViewModel() {
    private val tick =
        DateTimeUtils.ticks(clock).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)
    private val coordinates =
        kampusRepository.koordinat.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val weather =
        coordinates.flatMapLatest { location ->
            if (location == null)
                flowOf<AppResult<Cuaca>>(
                    AppResult.Error("Isi koordinat kampus terverifikasi pada Profil.")
                )
            else cuacaRepository.observeCuaca(location)
        }
    private val classes =
        tick
            .map { it.atZone(DateTimeUtils.campusZone).toLocalDate() }
            .distinctUntilChanged()
            .flatMapLatest { date ->
                jadwalRepository
                    .observeJadwal(date)
                    .map<List<KelasHarian>, AppResult<List<KelasHarian>>> { AppResult.Success(it) }
                    .onStart { emit(AppResult.Loading()) }
                    .catch { emit(AppResult.Error("Jadwal lokal belum dapat dibaca")) }
            }
    private val count = tugasRepository.observeTugas(TugasFilter.BELUM_SELESAI).map { it.size }

    private data class RefreshState(val loading: Boolean = false, val message: String? = null)

    private val refreshState = MutableStateFlow(RefreshState())
    private var refreshJob: Job? = null
    private var refreshLocation: KoordinatKampus? = null
    private var refreshGeneration = 0L
    private val dashboard =
        combine(classes, count, weather, network.isOnline, tick) {
                schedule,
                tasks,
                cuaca,
                online,
                instant ->
                val rows = schedule.dataOrNull().orEmpty()
                val cached = cuaca.dataOrNull()
                HomeUiState(
                    loading = schedule is AppResult.Loading,
                    jadwal = rows,
                    jumlahTugas = tasks,
                    kelasBerikutnya =
                        JadwalResolver.nextClass(
                            rows,
                            instant.atZone(DateTimeUtils.campusZone).toLocalTime(),
                        ),
                    cuaca = cuaca,
                    offline = !online,
                    cacheStale =
                        cached != null &&
                            !DateTimeUtils.isCacheFresh(cached.diambilPada, instant.toEpochMilli()),
                    message = (schedule as? AppResult.Error)?.message,
                    hujanBerikutnya =
                        cached
                            ?.hujan
                            .orEmpty()
                            .filter {
                                it.waktu >=
                                    instant
                                        .atZone(DateTimeUtils.campusZone)
                                        .toLocalDateTime()
                                        .withMinute(0)
                                        .withSecond(0)
                                        .withNano(0)
                            }
                            .take(3),
                )
            }
            .catch { emit(HomeUiState(loading = false, message = "Data lokal belum dapat dibaca")) }
    val uiState: StateFlow<HomeUiState> =
        combine(dashboard, refreshState) { state, refresh ->
                state.copy(refreshing = refresh.loading, message = refresh.message ?: state.message)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            combine(coordinates, network.isOnline) { location, online -> location to online }
                .distinctUntilChanged()
                .collectLatest { (location, online) ->
                    if (location != null && online) refreshWeather()
                }
        }
    }

    fun refreshWeather() {
        val location =
            coordinates.value
                ?: run {
                    refreshState.value =
                        RefreshState(message = "Isi koordinat kampus pada Profil terlebih dahulu.")
                    return
                }
        if (refreshState.value.loading && location == refreshLocation) return
        refreshJob?.cancel()
        refreshLocation = location
        val generation = ++refreshGeneration
        refreshJob =
            viewModelScope.launch {
                refreshState.value = RefreshState(loading = true)
                try {
                    val result = cuacaRepository.refresh(location)
                    if (generation == refreshGeneration)
                        refreshState.value =
                            RefreshState(message = (result as? AppResult.Error)?.message)
                } catch (cancelled: CancellationException) {
                    if (generation == refreshGeneration) refreshState.value = RefreshState()
                    throw cancelled
                } catch (_: Exception) {
                    if (generation == refreshGeneration)
                        refreshState.value = RefreshState(message = "Cuaca belum dapat diperbarui")
                }
            }
    }
}
