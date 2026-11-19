package com.unpad.padukampus.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unpad.padukampus.data.repository.KampusRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(repository: KampusRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val uiState =
        query
            .flatMapLatest { search ->
                combine(repository.observeGedung(), repository.observeRuangan(search)) {
                        buildings,
                        rooms ->
                        MapUiState(
                            loading = false,
                            gedung = buildings,
                            ruangan = rooms,
                            search = search,
                        )
                    }
                    .onStart { emit(MapUiState(search = search)) }
                    .catch {
                        emit(
                            MapUiState(
                                loading = false,
                                search = search,
                                error = "Data kampus belum dapat dibaca",
                            )
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun search(value: String) {
        query.value = value
    }
}
