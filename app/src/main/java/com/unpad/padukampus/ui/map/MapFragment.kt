package com.unpad.padukampus.ui.map

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unpad.padukampus.R
import com.unpad.padukampus.databinding.FragmentMapBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch

class MapFragment : Fragment(R.layout.fragment_map) {
    private var _binding: FragmentMapBinding? = null
    private val model: MapViewModel by viewModels { appContainer.mapFactory() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentMapBinding.bind(view)
        _binding = ui
        ui.search.doAfterTextChanged { model.search(it.toString()) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { state ->
                    ui.status.text =
                        state.error ?: if (state.loading) getString(R.string.map_loading) else ""
                    ui.buildings.text =
                        if (state.gedung.isEmpty()) getString(R.string.map_no_buildings)
                        else state.gedung.joinToString("\n") { "${it.kode} · ${it.nama}" }
                    ui.rooms.text =
                        if (state.ruangan.isEmpty()) getString(R.string.map_no_rooms)
                        else
                            state.ruangan.joinToString("\n") {
                                "${it.ruangan.kode} · ${it.gedung.nama} · ${it.ruangan.lantai}"
                            }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
