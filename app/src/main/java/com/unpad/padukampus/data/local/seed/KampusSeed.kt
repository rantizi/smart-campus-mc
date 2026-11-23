package com.unpad.padukampus.data.local.seed

import com.unpad.padukampus.data.local.entity.GedungEntity
import com.unpad.padukampus.data.local.entity.RuanganEntity
import com.unpad.padukampus.data.repository.KoordinatKampus

/**
 * Data awal kampus Unpad Jatinangor yang sudah diverifikasi (bukan contoh karangan).
 *
 * Koordinat gedung = titik tengah poligon bangunan di OpenStreetMap (© OpenStreetMap contributors,
 * ODbL), diambil 19 November 2026 lewat Overpass API; ID way OSM dicantumkan agar bisa dicek ulang.
 * Fungsi gedung: informatika.unpad.ac.id/s6 ("Gedung Biru", dahulu Gedung PPBS: Gedung A dan B
 * untuk perkuliahan, Gedung D untuk laboratorium dan manajemen).
 *
 * ID tetap (bukan UUID acak) agar seed idempoten: dijalankan ulang tidak membuat duplikat.
 * Tabel gedung/ruangan tidak disinkronkan ke Firestore (Laporan 1 Bagian 6.1).
 */
object KampusSeed {
    val gedung =
        listOf(
            // OSM way 133952527 "PPBS (Gedung A)"
            gedung("gedung-ppbs-a", "PPBS Gedung A (Gedung Biru)", "PPBS-A", -6.923388, 107.773155),
            // OSM way 133952528 "PPBS (Gedung B)"
            gedung("gedung-ppbs-b", "PPBS Gedung B (Gedung Biru)", "PPBS-B", -6.923265, 107.773428),
            // OSM way 133956279 "PPBS (Gedung C)"
            gedung("gedung-ppbs-c", "PPBS Gedung C", "PPBS-C", -6.923973, 107.773409),
            // OSM way 133956278 "PPBS (Gedung D)" – laboratorium dan manajemen
            gedung("gedung-ppbs-d", "PPBS Gedung D", "PPBS-D", -6.923802, 107.773778),
        )

    /**
     * Ruangan dari jadwal resmi portal akademik (semester ganjil 2026/2027) dan keterangan Fadhli
     * (23 November 2026). Format kode portal: 0201-GGGG-RRRR, dengan GGGG kode gedung (0724 = PPBS A,
     * 0726 = PPBS B, 0720 = PPBS C) dan RRRR nomor ruang; digit pertama nomor ruang = lantai.
     * Jangan menambahkan ruangan yang belum dicek di lokasi atau di jadwal resmi.
     */
    val ruangan: List<RuanganEntity> =
        listOf(
            // PPBS A: aula 201 dan 202 serta ruang 105 (keterangan Fadhli); 204, 205, 207, 208 (portal)
            ruang("gedung-ppbs-a", "0724", "0105", "PPBS A Ruang 105"),
            ruang("gedung-ppbs-a", "0724", "0201", "PPBS A Aula 201"),
            ruang("gedung-ppbs-a", "0724", "0202", "PPBS A Aula 202"),
            ruang("gedung-ppbs-a", "0724", "0204", "PPBS A Ruang 204"),
            ruang("gedung-ppbs-a", "0724", "0205", "PPBS A Ruang 205"),
            ruang("gedung-ppbs-a", "0724", "0207", "PPBS A Ruang 207"),
            ruang("gedung-ppbs-a", "0724", "0208", "PPBS A Ruang 208"),
            // PPBS B: 204, 205, 206 (keterangan Fadhli); 201 (portal)
            ruang("gedung-ppbs-b", "0726", "0201", "PPBS B Ruang 201"),
            ruang("gedung-ppbs-b", "0726", "0204", "PPBS B Ruang 204"),
            ruang("gedung-ppbs-b", "0726", "0205", "PPBS B Ruang 205"),
            ruang("gedung-ppbs-b", "0726", "0206", "PPBS B Ruang 206"),
            // PPBS C: 309 (portal)
            ruang("gedung-ppbs-c", "0720", "0309", "PPBS C Ruang 309"),
        )

    fun kodeRuang(gedung: String, nomor: String) = "0201-$gedung-$nomor"

    fun idRuang(gedung: String, nomor: String) = "ruang-$gedung-$nomor"

    private fun ruang(gedungId: String, gedung: String, nomor: String, nama: String) =
        RuanganEntity(
            id = idRuang(gedung, nomor),
            gedungId = gedungId,
            kode = kodeRuang(gedung, nomor),
            nama = nama,
            lantai = nomor.trimStart('0').first().digitToInt(),
        )

    private fun gedung(id: String, nama: String, kode: String, lat: Double, lon: Double) =
        GedungEntity(id = id, nama = nama, kode = kode, latitude = lat, longitude = lon)

    /** Koordinat cuaca bawaan (FR-10): PPBS Gedung A. Dapat diganti di layar Profil. */
    val koordinatBawaan = KoordinatKampus(-6.923388, 107.773155)
}
