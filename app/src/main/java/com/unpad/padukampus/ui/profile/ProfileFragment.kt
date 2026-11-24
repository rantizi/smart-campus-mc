package com.unpad.padukampus.ui.profile

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.unpad.padukampus.R
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.databinding.FragmentProfileBinding
import com.unpad.padukampus.di.appContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var _binding: FragmentProfileBinding? = null
    private var rendering = false
    private var dialogKeluar: AlertDialog? = null
    private val formatWaktu =
        DateTimeFormatter.ofPattern("d MMM, HH.mm", Locale.forLanguageTag("id-ID"))
            .withZone(ZoneId.of(AppConstants.CAMPUS_TIME_ZONE))
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
        ui.logout.setOnClickListener { model.keluar() }
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
                    ui.status.text = state.message?.asString(requireContext()).orEmpty()
                    renderAkun(state)
                }
            }
        }
    }

    private fun renderAkun(state: ProfileUiState) {
        val ui = _binding ?: return
        ui.account.text = state.email.ifBlank { getString(R.string.profile_account_local) }
        ui.accountHint.isVisible = state.cloudTersedia
        ui.accountHint.setText(R.string.profile_account_hint)
        ui.sync.isVisible = state.cloudTersedia
        ui.sync.isEnabled = !state.sinkronBerjalan && !state.keluarBerjalan
        ui.logout.isVisible = state.cloudTersedia
        ui.logout.isEnabled = !state.keluarBerjalan
        ui.syncStatus.text =
            when {
                !state.cloudTersedia -> getString(R.string.profile_sync_local_mode)
                state.sinkronBerjalan -> getString(R.string.profile_sync_running)
                state.keluarBerjalan -> getString(R.string.profile_logout_running)
                else -> {
                    val waktu =
                        state.terakhirSinkron?.let {
                            getString(
                                R.string.profile_sync_last,
                                formatWaktu.format(Instant.ofEpochMilli(it)),
                            )
                        } ?: getString(R.string.profile_sync_never)
                    resources.getQuantityString(
                        R.plurals.profile_sync_status,
                        state.jumlahMenunggu,
                        waktu,
                        state.jumlahMenunggu,
                    )
                }
            }
        val jumlah = state.konfirmasiKeluar
        if (jumlah != null && dialogKeluar == null) {
            dialogKeluar =
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.profile_logout_confirm_title)
                    .setMessage(
                        resources.getQuantityString(
                            R.plurals.profile_logout_confirm_message,
                            jumlah,
                            jumlah,
                        )
                    )
                    .setPositiveButton(R.string.profile_logout_confirm_positive) { _, _ ->
                        model.keluar(paksa = true)
                    }
                    .setNegativeButton(R.string.profile_logout_confirm_negative, null)
                    .setOnDismissListener {
                        dialogKeluar = null
                        model.batalKeluar()
                    }
                    .show()
        }
    }

    override fun onDestroyView() {
        dialogKeluar?.setOnDismissListener(null)
        dialogKeluar?.dismiss()
        dialogKeluar = null
        _binding = null
        super.onDestroyView()
    }
}
