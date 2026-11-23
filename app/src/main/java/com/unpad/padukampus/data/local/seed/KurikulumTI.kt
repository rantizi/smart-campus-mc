package com.unpad.padukampus.data.local.seed

import com.unpad.padukampus.data.local.entity.MataKuliahEntity

/**
 * Seluruh mata kuliah kurikulum S-1 Teknik Informatika Unpad, disalin dari Buku Pedoman Prodi S-1
 * Teknik Informatika 2021-2022 (Revisi 2022 OBE), Bab IV.A Struktur Kurikulum, halaman 28-30:
 * 45 mata kuliah wajib semester I-VIII dan 24 mata kuliah pilihan wajib dari 4 peminatan.
 * Baris "Pilihan Wajib Minat" dan "Pilihan Bebas" hanya slot SKS sehingga tidak dimasukkan.
 *
 * Empat mata kuliah memakai sandi dan nama dari portal akademik semester ganjil 2026/2027 karena
 * berbeda dengan buku pedoman (data portal lebih baru):
 * - Kapita Selekta JKKD: D10K-7C01 (buku: D10G.72D1)
 * - Magang: UNX10.040701 (buku: KKN - Magang, UNX10.7001/02)
 * - Seminar: UNX10.040801 (buku: UNX10.8001)
 * - Skripsi 6 SKS: UNX10.040802 (buku: Tugas Akhir 7 SKS, UNX10.8002)
 *
 * Data kurikulum bersifat publik (bukan data pribadi) sehingga ikut build debug dan release.
 * ID dibuat dari sandi sehingga sama di semua perangkat: seed ulang atau dua perangkat yang
 * menyemai bersamaan tidak membuat duplikat di Firestore.
 */
object KurikulumTI {
    data class Mk(
        val kode: String,
        val nama: String,
        val sks: Int,
        val semester: Int,
        /** WAJIB atau singkatan peminatan (IKMN, SISM, SCSG, JKKD). */
        val kelompok: String = WAJIB,
    )

    const val WAJIB = "WAJIB"

    val daftar: List<Mk> =
        listOf(
            // Semester I (21 SKS)
            Mk("UNX10.1001", "Agama", 2, 1),
            Mk("UNX10.1002", "Pancasila", 2, 1),
            Mk("UNX10.1003", "Bahasa Indonesia", 2, 1),
            Mk("UNX10.1004", "Olah Raga, Kebugaran dan Kreatifitas (OKK)", 3, 1),
            Mk("D10A.1201", "Kalkulus I", 3, 1),
            Mk("D10G.1201", "Logika Informatika", 3, 1),
            Mk("D10G.1202", "Algoritma & Pemrograman", 3, 1),
            Mk("D10G.1203", "Pengantar Teknologi Komputer & Informatika", 3, 1),
            // Semester II (21 SKS)
            Mk("UNX10.2001", "Bahasa Inggris I", 2, 2),
            Mk("UNX10.2002", "Kewarganegaraan", 2, 2),
            Mk("D10A.2201", "Kalkulus II", 3, 2),
            Mk("D10H.2201", "Fisika Informatika", 3, 2),
            Mk("D10E.2201", "Statistika", 2, 2),
            Mk("D10G.2201", "Struktur Data", 3, 2),
            Mk("D10G.2202", "Arsitektur & Organisasi Komputer", 3, 2),
            Mk("D10G.2203", "Sistem Database I", 3, 2),
            // Semester III (20 SKS)
            Mk("D10A.3201", "Aljabar Linier", 3, 3),
            Mk("D10A.3202", "Metode Numerik", 3, 3),
            Mk("D10A.3203", "Matematika Diskrit", 3, 3),
            Mk("D10G.3201", "Sistem Operasi", 3, 3),
            Mk("D10G.3202", "Pemrograman Web", 3, 3),
            Mk("D10G.3203", "Pemrograman Berorientasi Objek", 3, 3),
            Mk("D10G.3204", "Sistem Database II", 2, 3),
            // Semester IV (20 SKS)
            Mk("UNX10.4001", "Bahasa Inggris II", 2, 4),
            Mk("D10A.4201", "Operasional Riset", 2, 4),
            Mk("D10G.4201", "Artificial Intelligence", 3, 4),
            Mk("D10G.4202", "Jaringan Komputer", 3, 4),
            Mk("D10G.4203", "Sistem Informasi", 3, 4),
            Mk("D10G.4204", "Kecakapan Antar Personal", 2, 4),
            Mk("D10G.4205", "Analisis Algoritma", 3, 4),
            Mk("D10G.4206", "Etika Profesi", 2, 4),
            // Semester V (wajib 10 SKS + 9 SKS pilihan)
            Mk("D10G.5201", "Telematika", 3, 5),
            Mk("D10G.5202", "Enterpreneurship", 2, 5),
            Mk("D10G.5203", "Rekayasa Perangkat Lunak", 3, 5),
            Mk("D10G.5204", "Metodologi Penelitian", 2, 5),
            // Semester VI (wajib 10 SKS + 9 SKS pilihan)
            Mk("D10G.6201", "Interaksi Manusia & Komputer", 3, 6),
            Mk("D10G.6202", "Teori Bahasa & Automata", 3, 6),
            Mk("D10G.6203", "Proyek Perangkat Lunak I", 2, 6),
            Mk("D10G.6204", "Data Warehouse", 2, 6),
            // Semester VII (wajib 9 SKS + 9 SKS pilihan)
            Mk("D10G.7201", "Data Mining", 3, 7),
            Mk("D10G.7202", "Proyek Perangkat Lunak II", 2, 7),
            Mk("D10G.7203", "Manajemen Project", 2, 7),
            Mk("UNX10.040701", "Magang", 2, 7),
            // Semester VIII
            Mk("UNX10.040801", "Seminar", 1, 8),
            Mk("UNX10.040802", "Skripsi", 6, 8),
            // Minat Ilmu Komputasi dan Metode Numerik (IKMN)
            Mk("D10G.52A1", "Pemodelan dan Simulasi", 3, 5, "IKMN"),
            Mk("D10G.52A2", "Analisis Numerik", 3, 5, "IKMN"),
            Mk("D10G.62A1", "Persamaan Diferensial", 3, 6, "IKMN"),
            Mk("D10G.62A2", "Optimization Problems", 3, 6, "IKMN"),
            Mk("D10G.72A1", "Kapita Selekta IKMN", 3, 7, "IKMN"),
            Mk("D10G.72A2", "High Performance Computing", 3, 7, "IKMN"),
            // Minat Sistem Informasi dan Sistem Multimedia (SISM)
            Mk("D10G.52B1", "Decision Support Systems", 3, 5, "SISM"),
            Mk("D10G.52B2", "Kriptografi", 3, 5, "SISM"),
            Mk("D10G.62B1", "Sistem Informasi Multimedia", 3, 6, "SISM"),
            Mk("D10G.62B2", "Semantik Web", 3, 6, "SISM"),
            Mk("D10G.72B1", "Kapita Selekta SISM", 3, 7, "SISM"),
            Mk("D10G.72B2", "Sistem Informasi Geografi", 3, 7, "SISM"),
            // Minat Sistem Cerdas dan Sistem Grafika (SCSG)
            Mk("D10G.52C1", "Grafika Komputer", 3, 5, "SCSG"),
            Mk("D10G.52C2", "Robotika", 3, 5, "SCSG"),
            Mk("D10G.62C1", "Pengolahan & Analisis Citra Digital", 3, 6, "SCSG"),
            Mk("D10G.62C2", "Soft Computing", 3, 6, "SCSG"),
            Mk("D10G.72C1", "Kapita Selekta SCSG", 3, 7, "SCSG"),
            Mk("D10G.72C2", "Machine Learning", 3, 7, "SCSG"),
            // Minat Jaringan Komputer dan Komunikasi Data (JKKD)
            Mk("D10G.52D1", "Sistem Keamanan Jaringan", 3, 5, "JKKD"),
            Mk("D10G.52D2", "Sistem Terdistribusi", 3, 5, "JKKD"),
            Mk("D10G.62D1", "Manajemen Jaringan", 3, 6, "JKKD"),
            Mk("D10G.62D2", "Mobile Computing", 3, 6, "JKKD"),
            Mk("D10K-7C01", "Kapita Selekta JKKD", 3, 7, "JKKD"),
            Mk("D10G.72D2", "Jaringan Komputer Lanjut", 3, 7, "JKKD"),
        )

    fun idUntuk(kode: String) = "mk-" + kode.lowercase().replace(Regex("[^a-z0-9]+"), "-")

    /** Baris Room siap disemai; updated_at dan sync_status diisi repository saat menyimpan. */
    val mataKuliah: List<MataKuliahEntity> =
        daftar.map { MataKuliahEntity(id = idUntuk(it.kode), nama = it.nama, kode = it.kode, sks = it.sks) }
}
