package com.unpad.padukampus.ui.tasks

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
import com.unpad.padukampus.data.repository.TugasFilter
import com.unpad.padukampus.databinding.FragmentTaskListBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch

class TaskListFragment : Fragment(R.layout.fragment_task_list) {
    private var _binding: FragmentTaskListBinding? = null
    private val model: TaskViewModel by viewModels { appContainer.taskFactory() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentTaskListBinding.bind(view)
        _binding = ui
        val adapter =
            TaskAdapter({ id -> openForm(id) }, model::setSelesai) { id ->
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.task_delete_confirm)
                    .setNegativeButton(R.string.task_cancel, null)
                    .setPositiveButton(R.string.task_delete) { _, _ -> model.delete(id) }
                    .show()
            }
        ui.tasks.layoutManager = LinearLayoutManager(requireContext())
        ui.tasks.adapter = adapter
        ui.addTask.setOnClickListener { openForm("") }
        ui.filterAll.setOnClickListener { model.setFilter(TugasFilter.SEMUA) }
        ui.filterPending.setOnClickListener { model.setFilter(TugasFilter.BELUM_SELESAI) }
        ui.filterDone.setOnClickListener { model.setFilter(TugasFilter.SELESAI) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { state ->
                    adapter.submitList(state.tugas)
                    ui.empty.isVisible = !state.loading && state.tugas.isEmpty()
                    ui.status.text =
                        state.error ?: if (state.loading) getString(R.string.task_loading) else ""
                    ui.filterAll.isEnabled = state.filter != TugasFilter.SEMUA
                    ui.filterPending.isEnabled = state.filter != TugasFilter.BELUM_SELESAI
                    ui.filterDone.isEnabled = state.filter != TugasFilter.SELESAI
                }
            }
        }
    }

    private fun openForm(id: String) {
        findNavController()
            .navigate(TaskListFragmentDirections.actionTaskListToTaskForm(taskId = id))
    }

    override fun onDestroyView() {
        _binding?.tasks?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
