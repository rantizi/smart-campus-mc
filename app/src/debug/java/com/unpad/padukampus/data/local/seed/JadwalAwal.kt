package com.unpad.padukampus.data.local.seed

import com.unpad.padukampus.data.repository.JadwalInput

/**
 * Jadwal kuliah nyata Fadhli (Teknik Informatika Unpad, semester ganjil 2026/2027), disalin dari
 * jadwal akademik pada 19 November 2026. Hanya ikut build debug untuk uji/demo di HP; build
 * release memakai daftar kosong (src/release). Dimasukkan sekali saat tabel jadwal masih kosong.
 *
 * Ruangan belum diisi: jadwal sumber tidak memuat kolom ruang. Setelah kode/lantai ruangan
 * diketahui, tambahkan ke KampusSeed.ruangan lalu isi ruanganId di sini.
 * Dosen hanya diisi bila tercantum di KONTEKS_KELOMPOK.md.
 * Hari: 1 = Senin … 7 = Minggu.
 */
object JadwalAwal {
    val daftar =
        listOf(
            // kelas B
            kuliah("Proyek Perangkat Lunak II", "D10G.7202", 2, hari = 2, "07:30", "09:10"),
            // kelas A
            kuliah(
                "Mobile Computing",
                "D10G.62D2",
                3,
                hari = 2,
                "10:30",
                "13:00",
                dosen = "Deni Setiana",
            ),
            // kelas A
            kuliah("Sistem Informasi Geografi", "D10G.72B2", 3, hari = 2, "13:30", "16:00"),
            kuliah("Kapita Selekta JKKD", "D10K-7C01", 3, hari = 3, "07:30", "09:30"), // kelas A
            kuliah("Seminar", "UNX10.040801", 1, hari = 3, "16:00", "17:00"), // kelas A
            kuliah("Skripsi", "UNX10.040802", 6, hari = 4, "16:00", "17:00"), // kelas A
            kuliah("Magang", "UNX10.040701", 2, hari = 6, "11:00", "12:00"), // lapangan, kelas D
        )

    private fun kuliah(
        nama: String,
        kode: String,
        sks: Int,
        hari: Int,
        mulai: String,
        selesai: String,
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
            ruanganId = null,
            pengingatMenit = 15,
        )
}
