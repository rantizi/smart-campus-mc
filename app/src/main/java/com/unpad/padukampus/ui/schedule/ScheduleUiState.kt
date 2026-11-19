package com.unpad.padukampus.ui.schedule

import com.unpad.padukampus.core.common.UiText
import com.unpad.padukampus.data.local.relation.JadwalDetail
import com.unpad.padukampus.data.local.relation.RuanganDetail
import com.unpad.padukampus.data.model.JenisPerubahan
import com.unpad.padukampus.data.repository.KelasHarian
import java.time.LocalDate

data class ScheduleUiState(
    val tanggal: LocalDate,
    val loading: Boolean = true,
    val jadwal: List<KelasHarian> = emptyList(),
    val error: UiText? = null,
)

/** Isian Form Jadwal (FR-02); disimpan di SavedStateHandle agar pulih setelah rotasi. */
data class ScheduleFormDraft(
    val mataKuliah: String = "",
    val kode: String = "",
    val dosen: String = "",
    val sks: String = "",
    /** 1 = Senin … 7 = Minggu (format Laporan 1 Bagian 6.1). */
    val hari: Int = 1,
    val jamMulai: String = "",
    val jamSelesai: String = "",
    val ruanganId: String? = null,
    val pengingatMenit: String = "15",
)

data class ScheduleFormUiState(
    val draft: ScheduleFormDraft = ScheduleFormDraft(),
    val ruangan: List<RuanganDetail> = emptyList(),
    val editing: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val unavailable: Boolean = false,
    val error: UiText? = null,
)

/** Isian Form Perubahan (FR-04). */
data class ChangeFormDraft(
    val jadwalId: String? = null,
    val tanggal: String = "",
    /** PINDAH_RUANG, GANTI_JAM, atau BATAL. */
    val jenis: String = JenisPerubahan.PINDAH_RUANG,
    val ruanganBaruId: String? = null,
    val jamMulaiBaru: String = "",
    val jamSelesaiBaru: String = "",
    val catatan: String = "",
)

data class ChangeFormUiState(
    val draft: ChangeFormDraft = ChangeFormDraft(),
    val jadwal: List<JadwalDetail> = emptyList(),
    val ruangan: List<RuanganDetail> = emptyList(),
    /** True bila sudah ada perubahan aktif untuk jadwal + tanggal ini (tombol hapus tampil). */
    val existing: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: UiText? = null,
)
