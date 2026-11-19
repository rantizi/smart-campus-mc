package com.unpad.padukampus.data.model

/**
 * Nilai kolom perubahan_jadwal.jenis (Laporan 1 Bagian 6.1). Disimpan sebagai TEXT agar skema
 * Room versi 1 dan dokumen Firestore tetap sama; semua kode memakai konstanta ini, bukan literal.
 */
object JenisPerubahan {
    const val PINDAH_RUANG = "PINDAH_RUANG"
    const val GANTI_JAM = "GANTI_JAM"
    const val BATAL = "BATAL"
    val semua = setOf(PINDAH_RUANG, GANTI_JAM, BATAL)
}
