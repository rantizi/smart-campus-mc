package com.unpad.padukampus.data.repository

import com.unpad.padukampus.data.local.entity.*
import com.unpad.padukampus.data.local.relation.*
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class JadwalResolverTest {
    private val date = LocalDate.parse("2026-11-23") // Monday, synthetic fixture.
    private val course = MataKuliahEntity(id = "course", nama = "Mata kuliah uji")
    private val schedule =
        JadwalEntity(
            id = "schedule",
            mataKuliahId = "course",
            ruanganId = null,
            hari = 1,
            jamMulai = "08:00",
            jamSelesai = "10:00",
        )

    private fun detail(change: PerubahanJadwalDetail? = null) =
        JadwalDetail(schedule, course, null, listOfNotNull(change))

    private fun change(type: String, day: String = date.toString(), deleted: Boolean = false) =
        PerubahanJadwalDetail(
            PerubahanJadwalEntity(
                jadwalId = "schedule",
                ruanganBaruId = null,
                tanggal = day,
                jenis = type,
                jamMulaiBaru = "13:00",
                jamSelesaiBaru = "15:00",
                isDeleted = deleted,
            ),
            null,
        )

    @Test
    fun regularScheduleIncludesCourseName() {
        assertEquals(course.nama, JadwalResolver.resolve(detail(), date)!!.mataKuliah)
    }

    @Test
    fun changeTypeIsExposedForScheduleLabels() {
        assertNull(JadwalResolver.resolve(detail(), date)!!.jenisPerubahan)
        val moved = JadwalResolver.resolve(detail(change("PINDAH_RUANG")), date)!!
        assertEquals("PINDAH_RUANG", moved.jenisPerubahan)
        val restored = JadwalResolver.resolve(detail(change("GANTI_JAM", deleted = true)), date)!!
        assertNull(restored.jenisPerubahan)
    }

    @Test
    fun wrongWeekdayHasNoRegularClass() {
        assertNull(JadwalResolver.resolve(detail(), date.plusDays(1)))
    }

    @Test
    fun cancelledClassIsExcludedFromNextClass() {
        val cancelled = JadwalResolver.resolve(detail(change("BATAL")), date)!!
        assertTrue(cancelled.dibatalkan)
        assertNull(JadwalResolver.nextClass(listOf(cancelled), LocalTime.of(7, 0)))
    }

    @Test
    fun replacementTimeIsUsedBeforeSelectingNextClass() {
        val moved = JadwalResolver.resolve(detail(change("GANTI_JAM")), date)!!
        assertEquals(LocalTime.of(13, 0), moved.jamMulai)
        assertEquals(moved, JadwalResolver.nextClass(listOf(moved), LocalTime.of(9, 0)))
    }

    @Test
    fun replacementRoomIncludesItsBuilding() {
        val building =
            GedungEntity(
                id = "building-test",
                nama = "Gedung uji",
                kode = "TEST",
                latitude = 0.0,
                longitude = 0.0,
            )
        val room =
            RuanganDetail(
                RuanganEntity(
                    id = "room-test",
                    gedungId = building.id,
                    kode = "TEST-1",
                    nama = "Ruang uji",
                    lantai = 1,
                ),
                building,
            )
        val change =
            change("PINDAH_RUANG").let {
                it.copy(
                    perubahan = it.perubahan.copy(ruanganBaruId = room.ruangan.id),
                    lokasiBaru = room,
                )
            }
        assertEquals(room, JadwalResolver.resolve(detail(change), date)!!.lokasi)
    }

    @Test
    fun deletedOrDifferentDateOverrideIsIgnored() {
        assertFalse(
            JadwalResolver.resolve(detail(change("BATAL", deleted = true)), date)!!.dibatalkan
        )
        assertFalse(
            JadwalResolver.resolve(detail(change("BATAL", day = "2026-11-30")), date)!!.dibatalkan
        )
    }

    @Test
    fun deletedCourseIsNotDisplayed() {
        assertNull(
            JadwalResolver.resolve(detail().copy(mataKuliah = course.copy(isDeleted = true)), date)
        )
    }
}
