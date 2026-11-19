package com.unpad.padukampus.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unpad.padukampus.R
import com.unpad.padukampus.core.common.AppResult
import com.unpad.padukampus.core.common.dataOrNull
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.databinding.FragmentHomeBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val model: HomeViewModel by viewModels { appContainer.homeFactory() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentHomeBinding.bind(view)
        _binding = ui
        ui.refresh.setOnClickListener { model.refreshWeather() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HomeUiState) {
        val ui = _binding ?: return
        ui.connection.text =
            getString(if (state.offline) R.string.home_offline else R.string.home_local_first)
        ui.summary.text =
            resources.getQuantityString(
                R.plurals.home_task_count,
                state.jumlahTugas,
                state.jumlahTugas,
            )
        ui.schedule.text =
            if (state.loading) getString(R.string.home_loading)
            else if (state.jadwal.isEmpty()) getString(R.string.home_empty_schedule)
            else
                state.jadwal.joinToString("\n") {
                    "${it.jamMulai} ${it.mataKuliah}" +
                        if (it.dibatalkan) " (${getString(R.string.home_cancelled)})" else ""
                }
        ui.nextClass.text =
            state.kelasBerikutnya?.let { "${it.mataKuliah} · ${it.jamMulai}\n${it.lokasiLabel}" }
                ?: getString(R.string.home_no_next_class)
        val weather = state.cuaca.dataOrNull()
        ui.weather.text =
            if (weather != null) {
                val rain =
                    state.hujanBerikutnya.joinToString("\n") {
                        "${it.waktu.toLocalTime()} WIB: ${it.persen?.let { p -> "$p%" } ?: "—"}"
                    }
                getString(R.string.home_weather_value, weather.suhu.toString(), weather.kodeCuaca) +
                    "\n$rain\n" +
                    DateTimeUtils.formatDeadline(weather.diambilPada) +
                    if (state.cacheStale) "\n${getString(R.string.home_old_cache)}" else ""
            } else (state.cuaca as? AppResult.Error)?.message ?: getString(R.string.home_loading)
        ui.status.text = state.message.orEmpty()
        ui.refresh.isEnabled = !state.refreshing
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
