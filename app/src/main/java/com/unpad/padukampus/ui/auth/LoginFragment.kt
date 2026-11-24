package com.unpad.padukampus.ui.auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unpad.padukampus.R
import com.unpad.padukampus.databinding.FragmentLoginBinding
import com.unpad.padukampus.di.appContainer
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val model: LoginViewModel by viewModels { appContainer.loginFactory() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentLoginBinding.bind(view)
        _binding = ui
        ui.email.setText(model.uiState.value.email)
        ui.email.doAfterTextChanged { model.ubahEmail(it.toString()) }
        ui.password.doAfterTextChanged { model.ubahSandi(it.toString()) }
        ui.password.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) model.kirim() }
        }
        ui.submit.setOnClickListener { model.kirim() }
        ui.switchMode.setOnClickListener { model.gantiMode() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { state ->
                    val context = requireContext()
                    ui.emailLayout.error = state.emailError?.asString(context)
                    ui.passwordLayout.error = state.sandiError?.asString(context)
                    ui.submit.setText(
                        if (state.modeDaftar) R.string.auth_register else R.string.auth_login
                    )
                    ui.switchMode.setText(
                        if (state.modeDaftar) R.string.auth_switch_to_login
                        else R.string.auth_switch_to_register
                    )
                    ui.submit.isEnabled = !state.loading
                    ui.switchMode.isEnabled = !state.loading
                    ui.progress.isVisible = state.loading
                    ui.message.text = state.pesan?.asString(context).orEmpty()
                    ui.message.isVisible = state.pesan != null
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
