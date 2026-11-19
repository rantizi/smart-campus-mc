package com.unpad.padukampus.ui.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.data.repository.KampusRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    repository: KampusRepository,
    private val savedState: SavedStateHandle,
    kodeRuangan: String = "",
) : ViewModel() {
    private val query = savedState.getStateFlow("map_search", kodeRuangan)
    private val selectedId = savedState.getStateFlow<String?>("map_selected", null)
    // Dari Beranda: pilih otomatis ruangan yang kodenya sama persis sekali saja.
    private var autoSelectCode: String? =
        kodeRuangan.takeIf { it.isNotBlank() && savedState.get<Boolean>("map_auto") != true }

    private val rooms =
        query.flatMapLatest { search ->
            if (search.isBlank()) flowOf(emptyList())
            else repository.observeRuangan(search).map { it.take(MAX_RESULTS) }
        }
    private val allRooms = repository.observeRuangan("")

    val uiState =
        combine(
                repository.observeGedung(),
                rooms,
                allRooms,
                combine(query, selectedId) { search, selected -> search to selected },
                repository.koordinat,
            ) { buildings, results, all, (search, selected), koordinat ->
                autoSelectCode?.let { code ->
                    all.firstOrNull { it.ruangan.kode.equals(code, ignoreCase = true) }?.let {
                        autoSelectCode = null
                        savedState["map_auto"] = true
                        savedState["map_selected"] = it.ruangan.id
                    }
                }
                MapUiState(
                    loading = false,
                    gedung = buildings,
                    ruangan = results,
                    search = search,
                    terpilih = all.firstOrNull { it.ruangan.id == selected },
                    koordinatKampus = koordinat,
                )
            }
            .catch {
                emit(MapUiState(loading = false, error = UiText.Res(R.string.error_campus_read)))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun search(value: String) {
        if (value != query.value) savedState["map_search"] = value
    }

    fun select(roomId: String?) {
        savedState["map_selected"] = roomId
    }

    private companion object {
        const val MAX_RESULTS = 5
    }
}
