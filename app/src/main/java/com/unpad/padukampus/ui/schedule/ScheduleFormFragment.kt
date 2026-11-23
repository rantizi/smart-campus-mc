package com.unpad.padukampus.ui.schedule

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Spinner
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.unpad.padukampus.R
import com.unpad.padukampus.data.local.relation.RuanganDetail
import com.unpad.padukampus.databinding.FragmentScheduleFormBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch

class ScheduleFormFragment : Fragment(R.layout.fragment_schedule_form) {
    private var _binding: FragmentScheduleFormBinding? = null
    private var rendering = false
    private var roomIds: List<String?> = listOf(null)
    private val args: ScheduleFormFragmentArgs by navArgs()
    private val model: ScheduleFormViewModel by viewModels {
        appContainer.scheduleFormFactory(args.jadwalId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentScheduleFormBinding.bind(view)
        _binding = ui
        val days = resources.getStringArray(R.array.schedule_days).toList()
        ui.day.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, days)
        ui.day.onSelected { position ->
            model.updateDraft { it.copy(hari = position + 1) }
        }
        ui.room.onSelected { position ->
            // Spinner melaporkan pilihan secara tertunda; jangan hapus ruangan draf yang daftarnya
            // belum termuat (indeks tidak ditemukan dianggap posisi 0).
            val current = roomIds.indexOf(model.state.value.draft.ruanganId).coerceAtLeast(0)
            if (position != current)
                model.updateDraft { it.copy(ruanganId = roomIds.getOrNull(position)) }
        }
        ui.course.bind { value -> model.updateDraft { it.copy(mataKuliah = value) } }
        ui.code.bind { value -> model.updateDraft { it.copy(kode = value) } }
        ui.lecturer.bind { value -> model.updateDraft { it.copy(dosen = value) } }
        ui.credits.bind { value -> model.updateDraft { it.copy(sks = value) } }
        ui.start.bind { value -> model.updateDraft { it.copy(jamMulai = value) } }
        ui.end.bind { value -> model.updateDraft { it.copy(jamSelesai = value) } }
        ui.reminder.bind { value -> model.updateDraft { it.copy(pengingatMenit = value) } }
        ui.save.setOnClickListener { model.save() }
        ui.cancel.setOnClickListener { findNavController().popBackStack() }
        ui.course.setOnItemClickListener { parent, _, position, _ ->
            model.pilihMataKuliah(parent.getItemAtPosition(position).toString())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.state.collect { state -> render(ui, state) } }
                launch {
                    model.daftarMataKuliah.collect { rows ->
                        ui.course.setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                rows.map { it.nama },
                            )
                        )
                    }
                }
            }
        }
    }

    private fun render(ui: FragmentScheduleFormBinding, state: ScheduleFormUiState) {
        rendering = true
        ui.formTitle.setText(
            if (state.editing) R.string.schedule_form_edit_title else R.string.schedule_form_title
        )
        setText(ui.course, state.draft.mataKuliah)
        setText(ui.code, state.draft.kode)
        setText(ui.lecturer, state.draft.dosen)
        setText(ui.credits, state.draft.sks)
        setText(ui.start, state.draft.jamMulai)
        setText(ui.end, state.draft.jamSelesai)
        setText(ui.reminder, state.draft.pengingatMenit)
        if (ui.day.selectedItemPosition != state.draft.hari - 1)
            ui.day.setSelection((state.draft.hari - 1).coerceIn(0, 6))
        val ids = listOf<String?>(null) + state.ruangan.map { it.ruangan.id }
        if (ids != roomIds || ui.room.adapter == null) {
            roomIds = ids
            ui.room.adapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(getString(R.string.schedule_no_room)) + state.ruangan.map(::roomLabel),
                )
        }
        val roomIndex = roomIds.indexOf(state.draft.ruanganId).coerceAtLeast(0)
        if (ui.room.selectedItemPosition != roomIndex) ui.room.setSelection(roomIndex)
        ui.roomHint.text =
            if (state.ruangan.isEmpty()) getString(R.string.schedule_rooms_empty) else ""
        rendering = false
        val enabled = !state.loading && !state.saving && !state.unavailable
        listOf(
                ui.course,
                ui.code,
                ui.lecturer,
                ui.credits,
                ui.day,
                ui.start,
                ui.end,
                ui.room,
                ui.reminder,
                ui.save,
            )
            .forEach { it.isEnabled = enabled }
        ui.status.text =
            state.error?.asString(requireContext())
                ?: if (state.loading) getString(R.string.form_loading) else ""
        if (state.saved) {
            model.consumeSaved()
            findNavController().popBackStack()
        }
    }

    private fun roomLabel(room: RuanganDetail) =
        getString(R.string.map_room_item, room.ruangan.nama, room.ruangan.kode, room.ruangan.lantai)

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
                    if (!rendering && !model.state.value.loading) onChange(position)
                }
            }
    }

    private fun setText(field: EditText, value: String) {
        if (field.text.toString() == value) return
        // Dropdown mata kuliah: jangan munculkan saran saat teks dipulihkan dari state.
        if (field is AutoCompleteTextView) field.setText(value, false) else field.setText(value)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
