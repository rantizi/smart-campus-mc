package com.unpad.padukampus.ui.schedule

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.unpad.padukampus.R
import com.unpad.padukampus.databinding.FragmentScheduleBinding
import com.unpad.padukampus.di.appContainer
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {
    private var _binding: FragmentScheduleBinding? = null
    private val model: ScheduleViewModel by viewModels { appContainer.scheduleFactory() }
    private val dateFormat =
        DateTimeFormatter.ofPattern("EEEE, d MMM uuuu", Locale.forLanguageTag("id-ID"))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentScheduleBinding.bind(view)
        _binding = ui
        val adapter =
            ScheduleAdapter(
                edit = { item ->
                    findNavController()
                        .navigate(
                            ScheduleFragmentDirections.actionScheduleToScheduleForm(
                                jadwalId = item.id
                            )
                        )
                },
                recordChange = { item ->
                    findNavController()
                        .navigate(
                            ScheduleFragmentDirections.actionScheduleToChangeForm(
                                jadwalId = item.id,
                                tanggal = item.tanggal.toString(),
                            )
                        )
                },
                delete = { item ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.schedule_delete_confirm)
                        .setNegativeButton(R.string.form_cancel, null)
                        .setPositiveButton(R.string.schedule_delete) { _, _ ->
                            model.delete(item.id)
                        }
                        .show()
                },
            )
        ui.schedules.layoutManager = LinearLayoutManager(requireContext())
        ui.schedules.adapter = adapter
        ui.previous.setOnClickListener { model.shiftDay(-1) }
        ui.next.setOnClickListener { model.shiftDay(1) }
        ui.addSchedule.setOnClickListener {
            findNavController()
                .navigate(ScheduleFragmentDirections.actionScheduleToScheduleForm(jadwalId = ""))
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { state ->
                    ui.date.text = state.tanggal.format(dateFormat)
                    ui.status.text =
                        state.error?.asString(requireContext())
                            ?: if (state.loading) getString(R.string.schedule_loading) else ""
                    ui.empty.isVisible = !state.loading && state.jadwal.isEmpty()
                    adapter.submitList(state.jadwal)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding?.schedules?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
