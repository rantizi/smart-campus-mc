package com.unpad.padukampus.ui.schedule

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.unpad.padukampus.R
import com.unpad.padukampus.data.local.relation.JadwalDetail
import com.unpad.padukampus.data.model.JenisPerubahan
import com.unpad.padukampus.databinding.FragmentChangeFormBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch

class ChangeFormFragment : Fragment(R.layout.fragment_change_form) {
    private var _binding: FragmentChangeFormBinding? = null
    private var rendering = false
    private var scheduleIds: List<String> = emptyList()
    private var roomIds: List<String> = emptyList()
    private val args: ChangeFormFragmentArgs by navArgs()
    private val model: ChangeFormViewModel by viewModels {
        appContainer.changeFormFactory(args.jadwalId, args.tanggal)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentChangeFormBinding.bind(view)
        _binding = ui
        ui.schedule.onSelected { position ->
            val id = scheduleIds.getOrNull(position)
            if (id != null && id != model.state.value.draft.jadwalId)
                model.updateDraft { it.copy(jadwalId = id) }
        }
        ui.room.onSelected { position ->
            val id = roomIds.getOrNull(position)
            if (id != null && id != model.state.value.draft.ruanganBaruId)
                model.updateDraft { it.copy(ruanganBaruId = id) }
        }
        ui.type.setOnCheckedChangeListener { _, checkedId ->
            if (rendering) return@setOnCheckedChangeListener
            val jenis =
                when (checkedId) {
                    R.id.type_time -> JenisPerubahan.GANTI_JAM
                    R.id.type_cancel -> JenisPerubahan.BATAL
                    else -> JenisPerubahan.PINDAH_RUANG
                }
            model.updateDraft { it.copy(jenis = jenis) }
        }
        ui.date.bind { value -> model.updateDraft { it.copy(tanggal = value) } }
        ui.start.bind { value -> model.updateDraft { it.copy(jamMulaiBaru = value) } }
        ui.end.bind { value -> model.updateDraft { it.copy(jamSelesaiBaru = value) } }
        ui.note.bind { value -> model.updateDraft { it.copy(catatan = value) } }
        ui.save.setOnClickListener { model.save() }
        ui.cancel.setOnClickListener { findNavController().popBackStack() }
        ui.removeChange.setOnClickListener { model.remove() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state -> render(ui, state) }
            }
        }
    }

    private fun render(ui: FragmentChangeFormBinding, state: ChangeFormUiState) {
        rendering = true
        val draft = state.draft
        val ids = state.jadwal.map { it.jadwal.id }
        if (ids != scheduleIds || ui.schedule.adapter == null) {
            scheduleIds = ids
            ui.schedule.adapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    state.jadwal.map(::scheduleLabel),
                )
        }
        val scheduleIndex = scheduleIds.indexOf(draft.jadwalId)
        if (scheduleIndex >= 0 && scheduleIndex != ui.schedule.selectedItemPosition)
            ui.schedule.setSelection(scheduleIndex)
        val rooms = state.ruangan.map { it.ruangan.id }
        if (rooms != roomIds || ui.room.adapter == null) {
            roomIds = rooms
            ui.room.adapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    state.ruangan.map {
                        getString(
                            R.string.map_room_item,
                            it.ruangan.nama,
                            it.ruangan.kode,
                            it.ruangan.lantai,
                        )
                    },
                )
        }
        val roomIndex = roomIds.indexOf(draft.ruanganBaruId)
        if (roomIndex >= 0 && roomIndex != ui.room.selectedItemPosition)
            ui.room.setSelection(roomIndex)
        ui.roomHint.text =
            if (state.ruangan.isEmpty()) getString(R.string.schedule_rooms_empty) else ""
        setText(ui.date, draft.tanggal)
        setText(ui.start, draft.jamMulaiBaru)
        setText(ui.end, draft.jamSelesaiBaru)
        setText(ui.note, draft.catatan)
        val checked =
            when (draft.jenis) {
                JenisPerubahan.GANTI_JAM -> R.id.type_time
                JenisPerubahan.BATAL -> R.id.type_cancel
                else -> R.id.type_move
            }
        if (ui.type.checkedRadioButtonId != checked) ui.type.check(checked)
        ui.roomGroup.isVisible = draft.jenis == JenisPerubahan.PINDAH_RUANG
        ui.timeGroup.isVisible = draft.jenis == JenisPerubahan.GANTI_JAM
        rendering = false
        ui.removeChange.isVisible = state.existing
        val enabled = !state.saving && state.jadwal.isNotEmpty()
        listOf(
                ui.schedule,
                ui.date,
                ui.type,
                ui.room,
                ui.start,
                ui.end,
                ui.note,
                ui.save,
                ui.removeChange,
            )
            .forEach { it.isEnabled = enabled }
        ui.status.text =
            state.error?.asString(requireContext())
                ?: if (state.jadwal.isEmpty()) getString(R.string.change_no_schedule) else ""
        if (state.saved) {
            model.consumeSaved()
            findNavController().popBackStack()
        }
    }

    private fun scheduleLabel(detail: JadwalDetail): String {
        val days = resources.getStringArray(R.array.schedule_days)
        val day = days.getOrElse(detail.jadwal.hari - 1) { "?" }
        return getString(
            R.string.change_schedule_item,
            detail.mataKuliah.nama,
            day,
            detail.jadwal.jamMulai,
        )
    }

    private fun EditText.bind(onChange: (String) -> Unit) {
        doAfterTextChanged { if (!rendering) onChange(it.toString()) }
    }

    private fun Spinner.onSelected(onChange: (Int) -> Unit) {
        onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (!rendering) onChange(position)
                }
            }
    }

    private fun setText(field: EditText, value: String) {
        if (field.text.toString() != value) field.setText(value)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
