package com.unpad.padukampus.ui.tasks

import com.unpad.padukampus.data.local.relation.TugasDetail
import com.unpad.padukampus.data.repository.TugasFilter

data class TaskUiState(
    val loading: Boolean = true,
    val tugas: List<TugasDetail> = emptyList(),
    val filter: TugasFilter = TugasFilter.SEMUA,
    val error: String? = null,
)

data class TaskFormDraft(
    val mataKuliah: String = "",
    val judul: String = "",
    val deskripsi: String = "",
    val deadline: String = "",
    val sumber: String = "LAINNYA",
    val pengingatMenit: String = "1440",
)

data class TaskFormUiState(
    val draft: TaskFormDraft = TaskFormDraft(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val unavailable: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val error: String? = null,
)
