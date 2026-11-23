package com.unpad.padukampus.ui.map

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.unpad.padukampus.R
import com.unpad.padukampus.data.local.entity.GedungEntity
import com.unpad.padukampus.data.local.relation.RuanganDetail
import com.unpad.padukampus.databinding.FragmentMapBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Peta kampus (FR-08) dengan penanda dari tabel gedung dan pencarian ruangan (FR-09).
 * Tile OpenStreetMap butuh internet; daftar gedung/ruangan tetap dari Room saat luring.
 */
class MapFragment : Fragment(R.layout.fragment_map) {
    private var _binding: FragmentMapBinding? = null
    private val args: MapFragmentArgs by navArgs()
    private val model: MapViewModel by viewModels { appContainer.mapFactory(args.kodeRuangan) }
    private val markers = mutableMapOf<String, Marker>()
    private var shownBuildings: List<GedungEntity>? = null
    private var centered = false
    private var shownSelection: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentMapBinding.bind(view)
        _binding = ui
        setupMap(ui.map)
        val adapter = RoomAdapter { room ->
            model.select(room.ruangan.id)
            ui.search.clearFocus()
        }
        ui.rooms.layoutManager = LinearLayoutManager(requireContext())
        ui.rooms.adapter = adapter
        if (ui.search.text.isNullOrEmpty() && model.uiState.value.search.isNotEmpty())
            ui.search.setText(model.uiState.value.search)
        if (ui.search.text.isNullOrEmpty() && args.kodeRuangan.isNotEmpty())
            ui.search.setText(args.kodeRuangan)
        ui.search.doAfterTextChanged { model.search(it.toString()) }
        ui.closeSelected.setOnClickListener { model.select(null) }
        ui.centerSelected.setOnClickListener {
            model.uiState.value.terpilih?.let { focus(ui.map, it, animate = true) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { state -> render(ui, adapter, state) }
            }
        }
    }

    private fun setupMap(map: MapView) {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        map.overlays.add(CopyrightOverlay(requireContext()))
        map.minZoomLevel = 3.0
        map.maxZoomLevel = 20.0
    }

    private fun render(ui: FragmentMapBinding, adapter: RoomAdapter, state: MapUiState) {
        if (state.gedung != shownBuildings) {
            shownBuildings = state.gedung
            showMarkers(ui.map, state.gedung)
        }
        if (!centered && !state.loading) {
            centered = true
            val first = state.gedung.firstOrNull()
            when {
                state.terpilih != null -> Unit // ditangani oleh focus() di bawah
                first != null -> center(ui.map, GeoPoint(first.latitude, first.longitude), 17.0)
                state.koordinatKampus != null ->
                    center(
                        ui.map,
                        GeoPoint(state.koordinatKampus.latitude, state.koordinatKampus.longitude),
                        16.0,
                    )
                // Belum ada data terverifikasi: tampilkan Indonesia, bukan koordinat karangan.
                else -> center(ui.map, GeoPoint(-2.5, 118.0), 4.0)
            }
        }
        val selected = state.terpilih
        ui.selectedCard.isVisible = selected != null
        if (selected != null) {
            ui.selectedTitle.text = getString(R.string.map_room_title, selected.ruangan.kode)
            ui.selectedDetail.text =
                getString(
                    R.string.map_room_detail,
                    selected.gedung.nama,
                    selected.gedung.kode,
                    selected.ruangan.lantai,
                )
            if (shownSelection != selected.ruangan.id) {
                shownSelection = selected.ruangan.id
                focus(ui.map, selected, animate = centered)
            }
        } else {
            shownSelection = null
            markers.values.forEach { it.closeInfoWindow() }
        }
        val showResults = state.search.isNotBlank() && selected == null
        ui.rooms.isVisible = showResults
        adapter.submitList(if (showResults) state.ruangan else emptyList())
        ui.status.text =
            state.error?.asString(requireContext())
                ?: when {
                    state.loading -> getString(R.string.map_loading)
                    state.gedung.isEmpty() -> getString(R.string.map_no_buildings)
                    showResults && state.ruangan.isEmpty() -> getString(R.string.map_no_rooms)
                    else -> getString(R.string.map_tiles_offline)
                }
    }

    private fun showMarkers(map: MapView, buildings: List<GedungEntity>) {
        markers.values.forEach { it.closeInfoWindow(); map.overlays.remove(it) }
        markers.clear()
        buildings.forEach { gedung ->
            val marker =
                Marker(map).apply {
                    position = GeoPoint(gedung.latitude, gedung.longitude)
                    title = gedung.nama
                    snippet = gedung.kode
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(map.context, R.drawable.ic_marker_gedung)
                }
            markers[gedung.id] = marker
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    private fun focus(map: MapView, room: RuanganDetail, animate: Boolean) {
        centered = true
        val point = GeoPoint(room.gedung.latitude, room.gedung.longitude)
        if (animate) map.controller.animateTo(point, 18.0, 600L) else center(map, point, 18.0)
        markers[room.gedung.id]?.showInfoWindow()
    }

    private fun center(map: MapView, point: GeoPoint, zoom: Double) {
        map.controller.setZoom(zoom)
        map.controller.setCenter(point)
    }

    override fun onResume() {
        super.onResume()
        _binding?.map?.onResume()
    }

    override fun onPause() {
        _binding?.map?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding?.rooms?.adapter = null
        _binding?.map?.onDetach()
        markers.clear()
        shownBuildings = null
        shownSelection = null
        centered = false
        _binding = null
        super.onDestroyView()
    }
}
