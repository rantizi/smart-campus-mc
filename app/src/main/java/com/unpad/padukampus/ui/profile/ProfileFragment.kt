package com.unpad.padukampus.ui.profile

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unpad.padukampus.R
import com.unpad.padukampus.databinding.FragmentProfileBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var _binding: FragmentProfileBinding? = null
    private var rendering = false
    private val model: ProfileViewModel by viewModels { appContainer.profileFactory() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentProfileBinding.bind(view)
        _binding = ui
        ui.latitude.doAfterTextChanged {
            if (!rendering) model.updateCoordinates(it.toString(), ui.longitude.text.toString())
        }
        ui.longitude.doAfterTextChanged {
            if (!rendering) model.updateCoordinates(ui.latitude.text.toString(), it.toString())
        }
        ui.save.setOnClickListener { model.saveCoordinates() }
        ui.sync.setOnClickListener { model.requestSync() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { state ->
                    rendering = true
                    if (ui.latitude.text.toString() != state.latitude)
                        ui.latitude.setText(state.latitude)
                    if (ui.longitude.text.toString() != state.longitude)
                        ui.longitude.setText(state.longitude)
                    rendering = false
                    ui.save.isEnabled = !state.loading && !state.saving
                    ui.latitude.isEnabled = !state.loading && !state.saving
                    ui.longitude.isEnabled = !state.loading && !state.saving
                    ui.status.text = state.message.orEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
