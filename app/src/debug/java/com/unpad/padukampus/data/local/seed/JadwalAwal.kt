package com.unpad.padukampus.data.local.seed

import com.unpad.padukampus.data.repository.JadwalInput

/**
 * Jadwal kuliah nyata Fadhli (Teknik Informatika Unpad, semester ganjil 2026/2027), disalin dari
 * jadwal akademik pada 19 November 2026. Hanya ikut build debug untuk uji/demo di HP; build
 * release memakai daftar kosong (src/release). Dimasukkan sekali saat tabel jadwal masih kosong.
 *
 * Ruangan dari kolom "Ruangan" portal (format 0201-GGGG-RRRR, lihat KampusSeed.ruangan).
 * Dosen hanya diisi bila tercantum di KONTEKS_KELOMPOK.md.
 * Hari: 1 = Senin … 7 = Minggu.
 */
object JadwalAwal {
    val daftar =
        listOf(
            // kelas B, ruang 0201-0726-0201
            kuliah("Proyek Perangkat Lunak II", "D10G.7202", 2, 2, "07:30", "09:10", B, "0201"),
            // kelas A, ruang 0201-0724-0205
            kuliah(
                "Mobile Computing",
                "D10G.62D2",
                3,
                2,
                "10:30",
                "13:00",
                A,
                "0205",
                dosen = "Deni Setiana",
            ),
            // kelas A, ruang 0201-0724-0204
            kuliah("Sistem Informasi Geografi", "D10G.72B2", 3, 2, "13:30", "16:00", A, "0204"),
            // kelas A, ruang 0201-0720-0309
            kuliah("Kapita Selekta JKKD", "D10K-7C01", 3, 3, "07:30", "09:30", C, "0309"),
            // kelas A, ruang 0201-0724-0207
            kuliah("Seminar", "UNX10.040801", 1, 3, "16:00", "17:00", A, "0207"),
            // kelas A, ruang 0201-0724-0208
            kuliah("Skripsi", "UNX10.040802", 6, 4, "16:00", "17:00", A, "0208"),
            // lapangan, kelas D, ruang 0201-0724-0207
            kuliah("Magang", "UNX10.040701", 2, 6, "11:00", "12:00", A, "0207"),
        )

    private const val A = "0724"
    private const val B = "0726"
    private const val C = "0720"

    private fun kuliah(
        nama: String,
        kode: String,
        sks: Int,
        hari: Int,
        mulai: String,
        selesai: String,
        gedung: String,
        ruang: String,
        dosen: String = "",
    ) =
        JadwalInput(
            mataKuliahNama = nama,
            kode = kode,
            dosen = dosen,
            sks = sks,
            hari = hari,
            jamMulai = mulai,
            jamSelesai = selesai,
            ruanganId = KampusSeed.idRuang(gedung, ruang),
            pengingatMenit = 15,
        )
}
