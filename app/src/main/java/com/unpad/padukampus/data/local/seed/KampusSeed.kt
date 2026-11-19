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
     * Kode dan lantai ruangan tidak dipublikasikan; diisi dari jadwal kuliah resmi kelompok.
     * Jangan menambahkan ruangan yang belum dicek di lokasi atau di jadwal resmi.
     */
    val ruangan: List<RuanganEntity> = emptyList()

    private fun gedung(id: String, nama: String, kode: String, lat: Double, lon: Double) =
        GedungEntity(id = id, nama = nama, kode = kode, latitude = lat, longitude = lon)

    /** Koordinat cuaca bawaan (FR-10): PPBS Gedung A. Dapat diganti di layar Profil. */
    val koordinatBawaan = KoordinatKampus(-6.923388, 107.773155)
}
