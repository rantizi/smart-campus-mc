package com.unpad.padukampus.ui.tasks

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.unpad.padukampus.R
import com.unpad.padukampus.databinding.FragmentTaskFormBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch

class TaskFormFragment : Fragment(R.layout.fragment_task_form) {
    private var _binding: FragmentTaskFormBinding? = null
    private var rendering = false
    private val args: TaskFormFragmentArgs by navArgs()
    private val model: TaskViewModel by viewModels { appContainer.taskFactory(args.taskId) }
    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            model.notificationPermissionChanged(appContainer.notifications.canPost())
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentTaskFormBinding.bind(view)
        _binding = ui
        val sources = resources.getStringArray(R.array.task_sources).toList()
        ui.source.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sources)
        ui.source.setSelection(sources.indexOf(model.formState.value.draft.sumber).coerceAtLeast(0))
        ui.source.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (!rendering && !model.formState.value.loading)
                        model.updateDraft { it.copy(sumber = sources[position]) }
                }
            }
        ui.course.doAfterTextChanged {
            if (!rendering) model.updateDraft { draft -> draft.copy(mataKuliah = it.toString()) }
        }
        ui.title.doAfterTextChanged {
            if (!rendering) model.updateDraft { draft -> draft.copy(judul = it.toString()) }
        }
        ui.description.doAfterTextChanged {
            if (!rendering) model.updateDraft { draft -> draft.copy(deskripsi = it.toString()) }
        }
        ui.deadline.doAfterTextChanged {
            if (!rendering) model.updateDraft { draft -> draft.copy(deadline = it.toString()) }
        }
        ui.reminder.doAfterTextChanged {
            if (!rendering)
                model.updateDraft { draft -> draft.copy(pengingatMenit = it.toString()) }
        }
        ui.save.setOnClickListener { model.save() }
        ui.cancel.setOnClickListener { findNavController().popBackStack() }
        ui.enableNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.formState.collect { state ->
                    rendering = true
                    setText(ui.course, state.draft.mataKuliah)
                    setText(ui.title, state.draft.judul)
                    setText(ui.description, state.draft.deskripsi)
                    setText(ui.deadline, state.draft.deadline)
                    setText(ui.reminder, state.draft.pengingatMenit)
                    ui.source.setSelection(sources.indexOf(state.draft.sumber).coerceAtLeast(0))
                    rendering = false
                    val enabled = !state.loading && !state.saving && !state.unavailable
                    listOf(
                            ui.course,
                            ui.title,
                            ui.description,
                            ui.deadline,
                            ui.reminder,
                            ui.source,
                            ui.save,
                        )
                        .forEach { it.isEnabled = enabled }
                    ui.status.text =
                        state.error?.asString(requireContext())
                            ?: if (state.loading) getString(R.string.task_loading) else ""
                    ui.notificationStatus.text =
                        getString(
                            if (state.notificationsEnabled) R.string.task_notifications_on
                            else R.string.task_notifications_off
                        )
                    if (state.saved) {
                        model.consumeSaved()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun setText(field: EditText, value: String) {
        if (field.text.toString() != value) field.setText(value)
    }

    override fun onResume() {
        super.onResume()
        model.notificationPermissionChanged(appContainer.notifications.canPost())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
