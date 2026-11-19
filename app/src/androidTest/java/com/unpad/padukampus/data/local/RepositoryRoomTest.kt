package com.unpad.padukampus.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unpad.padukampus.data.local.db.PaduKampusDatabase
import com.unpad.padukampus.data.local.entity.*
import com.unpad.padukampus.data.repository.*
import com.unpad.padukampus.worker.TaskReminderScheduler
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room/SQLite tests; require a device or emulator, not desktop SQLite. */
@RunWith(AndroidJUnit4::class)
class RepositoryRoomTest {
    private lateinit var database: PaduKampusDatabase
    private val clock = Clock.fixed(Instant.parse("2026-11-19T06:00:00Z"), ZoneOffset.UTC)

    private class FakeScheduler : TaskReminderScheduler {
        val scheduled = mutableListOf<TugasEntity>()
        val cancelled = mutableListOf<String>()

        override fun schedule(task: TugasEntity) {
            scheduled += task
        }

        override fun cancel(id: String) {
            cancelled += id
        }
    }

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    PaduKampusDatabase::class.java,
                )
                .build()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun taskMutationsAreLocalAndMaintainMonotonicRevision() = runBlocking {
        val scheduler = FakeScheduler()
        val repository = OfflineTugasRepository(database, scheduler, clock)
        val task =
            repository.simpan(
                TugasInput(
                    mataKuliahNama = "Mata kuliah uji",
                    judul = "Tugas uji",
                    deskripsi = "",
                    deadline = clock.millis() + 3_600_000,
                    sumber = "LMS",
                    pengingatMenit = 30,
                )
            )
        assertEquals(1, repository.observeTugas().first().size)
        repository.setSelesai(task.id, true)
        val done = repository.getTugas(task.id)!!.tugas
        assertTrue(done.selesai)
        assertTrue(done.updatedAt > task.updatedAt)
        assertEquals("PENDING", done.syncStatus)
        assertTrue(scheduler.scheduled.last().selesai)
        repository.hapus(task.id)
        assertTrue(repository.observeTugas().first().isEmpty())
        assertTrue(database.tugasDao().getEntity(task.id)!!.isDeleted)
        assertEquals(listOf(task.id), scheduler.cancelled)
    }

    @Test
    fun nestedScheduleRelationsIncludeReplacementBuilding() = runBlocking {
        val course = MataKuliahEntity(id = "course-test", nama = "Mata kuliah uji")
        val building =
            GedungEntity(
                id = "building-test",
                nama = "Gedung uji",
                kode = "TEST",
                latitude = 0.0,
                longitude = 0.0,
            )
        val room =
            RuanganEntity(
                id = "room-test",
                gedungId = building.id,
                kode = "TEST-1",
                nama = "Ruang uji",
                lantai = 1,
            )
        database.kampusDao().simpanGedung(building)
        database.kampusDao().simpanRuangan(room)
        val repository = OfflineJadwalRepository(database, clock)
        repository.simpanMataKuliah(course)
        repository.simpanJadwal(
            JadwalEntity(
                id = "schedule-test",
                mataKuliahId = course.id,
                ruanganId = null,
                hari = 1,
                jamMulai = "08:00",
                jamSelesai = "10:00",
            )
        )
        repository.simpanPerubahan(
            PerubahanJadwalEntity(
                jadwalId = "schedule-test",
                tanggal = "2026-11-23",
                jenis = "PINDAH_RUANG",
                ruanganBaruId = room.id,
                jamMulaiBaru = null,
                jamSelesaiBaru = null,
            )
        )
        val detail = repository.observeJadwal(LocalDate.parse("2026-11-23")).first().single()
        assertEquals(building.nama, detail.lokasi!!.gedung.nama)
        assertEquals(course.nama, detail.mataKuliah)
    }

    @Test
    fun restoringOverrideReusesUniqueScheduleDateRow() = runBlocking {
        val repository = OfflineJadwalRepository(database, clock)
        repository.simpanMataKuliah(MataKuliahEntity(id = "course-test", nama = "Uji"))
        repository.simpanJadwal(
            JadwalEntity(
                id = "schedule-test",
                mataKuliahId = "course-test",
                ruanganId = null,
                hari = 1,
                jamMulai = "08:00",
                jamSelesai = "10:00",
            )
        )
        val first =
            PerubahanJadwalEntity(
                id = "change-test",
                jadwalId = "schedule-test",
                ruanganBaruId = null,
                tanggal = "2026-11-23",
                jenis = "BATAL",
                jamMulaiBaru = null,
                jamSelesaiBaru = null,
                isDeleted = true,
            )
        repository.simpanPerubahan(first)
        repository.simpanPerubahan(first.copy(id = "different-test", isDeleted = false))
        assertEquals(
            "change-test",
            database.jadwalDao().getPerubahan("schedule-test", "2026-11-23")!!.id,
        )
        assertTrue(
            repository.observeJadwal(LocalDate.parse("2026-11-23")).first().single().dibatalkan
        )
    }

    @Test
    fun invalidTaskDoesNotCreatePartialCourseRecord() = runBlocking {
        val repository = OfflineTugasRepository(database, FakeScheduler(), clock)
        try {
            repository.simpan(
                TugasInput(
                    mataKuliahNama = "Uji",
                    judul = "",
                    deskripsi = "",
                    deadline = 100,
                    sumber = "LMS",
                    pengingatMenit = 0,
                )
            )
            fail("Blank title must be rejected")
        } catch (_: IllegalArgumentException) {}
        assertTrue(database.jadwalDao().observeMataKuliah().first().isEmpty())
    }
}
