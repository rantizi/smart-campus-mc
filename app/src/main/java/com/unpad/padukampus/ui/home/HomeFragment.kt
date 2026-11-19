package com.unpad.padukampus.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.common.dataOrNull
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.model.JenisPerubahan
import com.unpad.padukampus.databinding.FragmentHomeBinding
import com.unpad.padukampus.di.appContainer
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val model: HomeViewModel by viewModels { appContainer.homeFactory() }
    private val dateFormat =
        DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu", Locale.forLanguageTag("id-ID"))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentHomeBinding.bind(view)
        _binding = ui
        ui.refresh.setOnClickListener { model.refreshWeather() }
        ui.openMap.setOnClickListener {
            val code = model.uiState.value.kelasBerikutnya?.lokasi?.ruangan?.kode.orEmpty()
            findNavController().navigate(HomeFragmentDirections.actionHomeToMap(kodeRuangan = code))
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HomeUiState) {
        val ui = _binding ?: return
        ui.date.text = state.tanggal?.format(dateFormat).orEmpty()
        ui.connection.text =
            getString(if (state.offline) R.string.home_offline else R.string.home_local_first)

        // Cuaca (FR-10)
        val weather = state.cuaca.dataOrNull()
        if (weather != null) {
            ui.weather.text =
                getString(
                    R.string.home_weather_value,
                    weather.suhu.toString(),
                    WeatherCodes.describe(weather.kodeCuaca),
                )
            ui.rain.isVisible = state.hujanBerikutnya.isNotEmpty()
            ui.rain.text =
                getString(R.string.home_rain_title) + "\n" +
                    state.hujanBerikutnya.joinToString("   ") {
                        "${it.waktu.toLocalTime()}  ${it.persen?.let { p -> "$p%" } ?: "—"}"
                    }
            ui.weatherMeta.text =
                getString(
                    R.string.home_fetched_at,
                    DateTimeUtils.formatDeadline(weather.diambilPada),
                ) +
                    if (state.cacheStale) "\n${getString(R.string.home_old_cache)}" else ""
        } else {
            ui.weather.text =
                when {
                    state.butuhKoordinat -> getString(R.string.error_need_coordinates)
                    state.cuaca is AppResult.Error -> state.cuaca.message
                    else -> getString(R.string.home_loading)
                }
            ui.rain.isVisible = false
            ui.weatherMeta.text = ""
        }
        ui.refresh.isEnabled = !state.refreshing

        // Kelas berikutnya (FR-03)
        val next = state.kelasBerikutnya
        ui.nextTitle.text =
            next?.let { getString(R.string.home_next_at, it.jamMulai.toString()) }
                ?: getString(R.string.home_next)
        ui.nextClass.text = next?.mataKuliah ?: getString(R.string.home_no_next_class)
        ui.nextLocation.isVisible = next != null
        ui.nextLocation.text = next?.lokasiLabel.orEmpty()
        ui.openMap.isVisible = next?.lokasi != null

        // Jadwal hari ini (FR-03, FR-04)
        ui.schedule.text =
            if (state.loading) getString(R.string.home_loading)
            else if (state.jadwal.isEmpty()) getString(R.string.home_empty_schedule)
            else
                state.jadwal.joinToString("\n") {
                    val note =
                        when {
                            it.dibatalkan -> getString(R.string.home_cancelled)
                            it.jenisPerubahan == JenisPerubahan.PINDAH_RUANG ->
                                getString(R.string.home_moved)
                            it.jenisPerubahan == JenisPerubahan.GANTI_JAM ->
                                getString(R.string.home_time_changed)
                            else -> null
                        }
                    "${it.jamMulai}  ${it.mataKuliah}" + (note?.let { n -> "  ($n)" } ?: "")
                }

        // Tugas terdekat (FR-05)
        ui.nearestTask.text =
            state.tugasTerdekat?.let {
                getString(
                    R.string.home_task_line,
                    it.tugas.judul,
                    DateTimeUtils.formatDeadline(it.tugas.deadline),
                )
            } ?: getString(R.string.home_no_task)
        ui.summary.text =
            resources.getQuantityString(
                R.plurals.home_task_count,
                state.jumlahTugas,
                state.jumlahTugas,
            )
        ui.status.text = state.message?.asString(requireContext()).orEmpty()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
