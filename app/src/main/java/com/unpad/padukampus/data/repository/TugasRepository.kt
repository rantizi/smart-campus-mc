package com.unpad.padukampus.data.repository

import androidx.room.withTransaction
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.data.local.db.PaduKampusDatabase
import com.unpad.padukampus.data.local.entity.MataKuliahEntity
import com.unpad.padukampus.data.local.entity.TugasEntity
import com.unpad.padukampus.data.local.relation.TugasDetail
import com.unpad.padukampus.worker.TaskReminderScheduler
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TugasFilter(val selesai: Boolean?) {
    SEMUA(null),
    BELUM_SELESAI(false),
    SELESAI(true),
}

data class TugasInput(
    val id: String? = null,
    val mataKuliahNama: String,
    val judul: String,
    val deskripsi: String,
    val deadline: Long,
    val sumber: String,
    val pengingatMenit: Int,
)

interface TugasRepository {
    fun observeTugas(filter: TugasFilter = TugasFilter.SEMUA): Flow<List<TugasDetail>>

    suspend fun getTugas(id: String): TugasDetail?

    suspend fun simpan(input: TugasInput): TugasEntity

    suspend fun setSelesai(id: String, selesai: Boolean)

    suspend fun hapus(id: String)

    suspend fun reconcileReminders()

    /** Pilihan mata kuliah di Form Tugas (kurikulum + mata kuliah buatan pengguna). */
    fun observeMataKuliah(): Flow<List<MataKuliahEntity>> = flowOf(emptyList())
}

class OfflineTugasRepository(
    private val database: PaduKampusDatabase,
    private val scheduler: TaskReminderScheduler,
    private val clock: Clock,
) : TugasRepository {
    private val dao = database.tugasDao()
    private val writes = Mutex()

    override fun observeTugas(filter: TugasFilter) = dao.observeTugas(filter.selesai)

    override fun observeMataKuliah() = dao.observeMataKuliah()

    override suspend fun getTugas(id: String) =
        dao.getDetail(id)?.takeUnless { it.mataKuliah.isDeleted }

    override suspend fun simpan(input: TugasInput): TugasEntity =
        writes.withLock {
            require(input.judul.isNotBlank() && input.mataKuliahNama.isNotBlank()) {
                "Judul dan mata kuliah wajib diisi"
            }
            require(input.pengingatMenit in 0..AppConstants.MAX_REMINDER_MINUTES) {
                "Menit pengingat tidak valid"
            }
            require(input.sumber in setOf("LMS", "CLASSROOM", "WA", "LISAN", "LAINNYA")) {
                "Sumber tugas tidak valid"
            }
            val saved =
                database.withTransaction {
                    val old = input.id?.let { dao.getEntity(it) }
                    require(input.id == null || (old != null && !old.isDeleted)) {
                        "Tugas telah dihapus atau tidak ditemukan"
                    }
                    val now = maxOf(clock.millis(), (old?.updatedAt ?: 0) + 1)
                    val course =
                        dao.getMataKuliahByNama(input.mataKuliahNama.trim())
                            ?: MataKuliahEntity(nama = input.mataKuliahNama.trim(), updatedAt = now)
                                .also { dao.simpanMataKuliah(it) }
                    val task =
                        (old
                                ?: TugasEntity(
                                    mataKuliahId = course.id,
                                    judul = input.judul,
                                    deadline = input.deadline,
                                ))
                            .copy(
                                mataKuliahId = course.id,
                                judul = input.judul.trim(),
                                deskripsi = input.deskripsi.trim(),
                                deadline = input.deadline,
                                sumber = input.sumber,
                                pengingatMenit = input.pengingatMenit,
                                updatedAt = now,
                                syncStatus = AppConstants.SYNC_PENDING,
                            )
                    dao.simpanTugas(task)
                    task
                }
            scheduler.schedule(saved)
            saved
        }

    override suspend fun setSelesai(id: String, selesai: Boolean) =
        writes.withLock {
            val updated =
                database.withTransaction {
                    val old =
                        dao.getEntity(id)?.takeUnless { it.isDeleted }
                            ?: return@withTransaction null
                    val now = maxOf(clock.millis(), old.updatedAt + 1)
                    dao.setSelesai(id, selesai, now)
                    old.copy(
                        selesai = selesai,
                        updatedAt = now,
                        syncStatus = AppConstants.SYNC_PENDING,
                    )
                }
            updated?.let(scheduler::schedule)
            Unit
        }

    override suspend fun hapus(id: String) =
        writes.withLock {
            database.withTransaction {
                val old = dao.getEntity(id) ?: return@withTransaction
                dao.hapus(id, maxOf(clock.millis(), old.updatedAt + 1))
            }
            scheduler.cancel(id)
        }

    override suspend fun reconcileReminders() =
        writes.withLock { dao.getReminderTasks().forEach(scheduler::schedule) }
}
