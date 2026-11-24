package com.unpad.padukampus.data.local.seed

import org.junit.Assert.*
import org.junit.Test

/** Memeriksa salinan kurikulum (Buku Pedoman 2021-2022) dan data ruangan dari portal akademik. */
class SeedDataTest {
    private val wajib = KurikulumTI.daftar.filter { it.kelompok == KurikulumTI.WAJIB }

    @Test
    fun curriculumHas45MandatoryAnd24ElectiveCourses() {
        assertEquals(45, wajib.size)
        assertEquals(69, KurikulumTI.daftar.size)
        listOf("IKMN", "SISM", "SCSG", "JKKD").forEach { minat ->
            assertEquals(minat, 6, KurikulumTI.daftar.count { it.kelompok == minat })
        }
    }

    @Test
    fun semesterCreditsMatchGuidebook() {
        // Bab IV.A: Semester I 21, II 21, III 20, IV 20 SKS (seluruhnya wajib).
        mapOf(1 to 21, 2 to 21, 3 to 20, 4 to 20).forEach { (semester, sks) ->
            assertEquals("Semester $semester", sks, wajib.filter { it.semester == semester }.sumOf { it.sks })
        }
    }

    @Test
    fun codesNamesAndIdsAreUnique() {
        val daftar = KurikulumTI.daftar
        assertEquals(daftar.size, daftar.map { it.kode }.toSet().size)
        assertEquals(daftar.size, daftar.map { it.nama.lowercase() }.toSet().size)
        assertEquals(daftar.size, KurikulumTI.mataKuliah.map { it.id }.toSet().size)
        assertTrue(KurikulumTI.mataKuliah.all { it.sks in 1..24 && it.nama.isNotBlank() })
    }

    @Test
    fun everyScheduledCourseIsInCurriculumWithSameCodeAndCredits() {
        JadwalAwal.daftar.forEach { jadwal ->
            val mk = KurikulumTI.daftar.firstOrNull { it.nama == jadwal.mataKuliahNama }
            assertNotNull(jadwal.mataKuliahNama, mk)
            assertEquals(jadwal.mataKuliahNama, mk!!.kode, jadwal.kode)
            assertEquals(jadwal.mataKuliahNama, mk.sks, jadwal.sks)
        }
    }

    @Test
    fun everyScheduledRoomExistsAndFollowsPortalCode() {
        val ruangan = KampusSeed.ruangan.associateBy { it.id }
        val gedung = KampusSeed.gedung.map { it.id }.toSet()
        assertEquals(ruangan.size, KampusSeed.ruangan.size)
        assertEquals(KampusSeed.ruangan.size, KampusSeed.ruangan.map { it.kode }.toSet().size)
        KampusSeed.ruangan.forEach {
            assertTrue(it.kode, it.kode.matches(Regex("0201-07(20|24|26)-0\\d{3}")))
            assertTrue(it.gedungId in gedung)
            assertEquals(it.kode, it.kode.last(3).first().digitToInt(), it.lantai)
        }
        JadwalAwal.daftar.forEach { assertNotNull(it.mataKuliahNama, ruangan[it.ruanganId]) }
        // Contoh dari portal: Mobile Computing 0201-0724-0205 = PPBS A ruang 205 lantai 2.
        val mobile = ruangan[JadwalAwal.daftar.first { it.mataKuliahNama == "Mobile Computing" }.ruanganId]!!
        assertEquals("0201-0724-0205", mobile.kode)
        assertEquals("gedung-ppbs-a", mobile.gedungId)
        assertEquals(2, mobile.lantai)
    }

    private fun String.last(n: Int) = takeLast(n)
}
