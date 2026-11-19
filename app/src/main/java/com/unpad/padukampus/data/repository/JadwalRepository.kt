package com.unpad.padukampus.data.repository

import androidx.room.withTransaction
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.local.db.PaduKampusDatabase
import com.unpad.padukampus.data.local.entity.*
import com.unpad.padukampus.data.local.relation.JadwalDetail
import com.unpad.padukampus.data.local.relation.RuanganDetail
import com.unpad.padukampus.data.model.JenisPerubahan
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class KelasHarian(
    val id: String,
    val tanggal: LocalDate,
    val mataKuliah: String,
    val dosen: String,
    val jamMulai: LocalTime,
    val jamSelesai: LocalTime,
    val lokasi: RuanganDetail?,
    val dibatalkan: Boolean,
    val catatan: String,
    /** Jenis perubahan yang berlaku pada tanggal ini: PINDAH_RUANG, GANTI_JAM, BATAL, atau null. */
    val jenisPerubahan: String? = null,
) {
    val lokasiLabel: String
        get() =
            lokasi?.let { "${it.ruangan.kode} · ${it.gedung.nama} · Lantai ${it.ruangan.lantai}" }
                ?: "Ruangan belum ditentukan"
}

object JadwalResolver {
    fun resolve(detail: JadwalDetail, tanggal: LocalDate): KelasHarian? {
        if (detail.jadwal.isDeleted || detail.mataKuliah.isDeleted) return null
        val override =
            detail.perubahan.firstOrNull {
                !it.perubahan.isDeleted && it.perubahan.tanggal == tanggal.toString()
            }
        if (detail.jadwal.hari != tanggal.dayOfWeek.value && override == null) return null
        val change = override?.perubahan
        val changeTime = change?.jenis == JenisPerubahan.GANTI_JAM
        val start =
            if (changeTime) change?.jamMulaiBaru ?: detail.jadwal.jamMulai
            else detail.jadwal.jamMulai
        val finish =
            if (changeTime) change?.jamSelesaiBaru ?: detail.jadwal.jamSelesai
            else detail.jadwal.jamSelesai
        return KelasHarian(
            detail.jadwal.id,
            tanggal,
            detail.mataKuliah.nama,
            detail.mataKuliah.dosen,
            DateTimeUtils.parseClassTime(start),
            DateTimeUtils.parseClassTime(finish),
            if (change?.jenis == JenisPerubahan.PINDAH_RUANG) override.lokasiBaru
            else detail.lokasi,
            change?.jenis == JenisPerubahan.BATAL,
            change?.catatan.orEmpty(),
            change?.jenis,
        )
    }

    fun nextClass(classes: List<KelasHarian>, after: LocalTime): KelasHarian? =
        classes.filter { !it.dibatalkan && it.jamMulai > after }.minByOrNull { it.jamMulai }
}

/** Isian Form Jadwal (FR-02); mata kuliah dicari berdasarkan nama atau dibuat baru. */
data class JadwalInput(
    val id: String? = null,
    val mataKuliahNama: String,
    val kode: String,
    val dosen: String,
    val sks: Int,
    val hari: Int,
    val jamMulai: String,
    val jamSelesai: String,
    val ruanganId: String?,
    val pengingatMenit: Int,
)

interface JadwalRepository {
    fun observeJadwal(tanggal: LocalDate): Flow<List<KelasHarian>>

    fun observeKelasBerikutnya(tanggal: LocalDate, after: LocalTime): Flow<KelasHarian?>

    fun observeMataKuliah(): Flow<List<MataKuliahEntity>>

    suspend fun simpanMataKuliah(value: MataKuliahEntity)

    suspend fun simpanJadwal(value: JadwalEntity)

    suspend fun simpanPerubahan(value: PerubahanJadwalEntity)

    suspend fun hapusJadwal(id: String)

    fun observeSemuaJadwal(): Flow<List<JadwalDetail>>

    suspend fun getJadwalDetail(id: String): JadwalDetail?

    suspend fun simpanJadwal(input: JadwalInput): JadwalEntity

    /** Perubahan aktif (tidak terhapus) untuk jadwal dan tanggal tertentu. */
    suspend fun getPerubahan(jadwalId: String, tanggal: LocalDate): PerubahanJadwalEntity?

    suspend fun hapusPerubahan(jadwalId: String, tanggal: LocalDate)

    /** Isi jadwal awal hanya bila tabel jadwal masih kosong (tidak menimpa data pengguna). */
    suspend fun seedJadwalAwal(daftar: List<JadwalInput>)
}

class OfflineJadwalRepository(private val database: PaduKampusDatabase, private val clock: Clock) :
    JadwalRepository {
    private val dao = database.jadwalDao()

    override fun observeJadwal(tanggal: LocalDate) =
        dao.observeJadwalHarian(tanggal.dayOfWeek.value, tanggal.toString()).map { rows ->
            rows.mapNotNull { JadwalResolver.resolve(it, tanggal) }.sortedBy { it.jamMulai }
        }

    override fun observeKelasBerikutnya(tanggal: LocalDate, after: LocalTime) =
        observeJadwal(tanggal).map { JadwalResolver.nextClass(it, after) }

    override fun observeMataKuliah() = dao.observeMataKuliah()

    override suspend fun simpanMataKuliah(value: MataKuliahEntity) {
        require(value.nama.isNotBlank() && value.sks in 0..24) { "Mata kuliah tidak valid" }
        val old = dao.getMataKuliah(value.id)
        dao.simpanMataKuliah(
            value.copy(
                updatedAt = maxOf(clock.millis(), (old?.updatedAt ?: 0) + 1),
                syncStatus = AppConstants.SYNC_PENDING,
            )
        )
    }

    override suspend fun simpanJadwal(value: JadwalEntity) {
        require(
            value.hari in 1..7 &&
                DateTimeUtils.parseClassTime(value.jamSelesai) >
                    DateTimeUtils.parseClassTime(value.jamMulai)
        ) {
            "Hari atau rentang jam jadwal tidak valid"
        }
        require(dao.getMataKuliah(value.mataKuliahId) != null) { "Mata kuliah tidak ditemukan" }
        val old = dao.getJadwal(value.id)
        dao.simpanJadwal(
            value.copy(
                updatedAt = maxOf(clock.millis(), (old?.updatedAt ?: 0) + 1),
                syncStatus = AppConstants.SYNC_PENDING,
            )
        )
    }

    override suspend fun simpanPerubahan(value: PerubahanJadwalEntity) =
        database.withTransaction {
            LocalDate.parse(value.tanggal)
            require(dao.getJadwal(value.jadwalId) != null) { "Jadwal tidak ditemukan" }
            require(value.jenis in JenisPerubahan.semua) {
                "Jenis perubahan tidak valid"
            }
            if (value.jenis == JenisPerubahan.GANTI_JAM) {
                require(
                    value.jamMulaiBaru != null &&
                        value.jamSelesaiBaru != null &&
                        DateTimeUtils.parseClassTime(value.jamSelesaiBaru) >
                            DateTimeUtils.parseClassTime(value.jamMulaiBaru)
                ) {
                    "Jam perubahan tidak valid"
                }
            }
            if (value.jenis == JenisPerubahan.PINDAH_RUANG)
                require(value.ruanganBaruId != null) { "Pilih ruangan pengganti" }
            val old = dao.getPerubahan(value.jadwalId, value.tanggal)
            dao.simpanPerubahan(
                value.copy(
                    id = old?.id ?: value.id,
                    updatedAt = maxOf(clock.millis(), (old?.updatedAt ?: 0) + 1),
                    syncStatus = AppConstants.SYNC_PENDING,
                )
            )
        }

    override suspend fun hapusJadwal(id: String) {
        val old = dao.getJadwal(id) ?: return
        dao.hapusJadwal(id, maxOf(clock.millis(), old.updatedAt + 1))
    }

    override fun observeSemuaJadwal() = dao.observeSemuaJadwal()

    override suspend fun getJadwalDetail(id: String) =
        dao.getJadwalDetail(id)?.takeUnless { it.mataKuliah.isDeleted }

    override suspend fun simpanJadwal(input: JadwalInput): JadwalEntity {
        val nama = input.mataKuliahNama.trim()
        require(nama.isNotBlank()) { "Nama mata kuliah wajib diisi" }
        require(input.sks in 0..24) { "SKS harus 0–24" }
        require(input.pengingatMenit in 0..AppConstants.MAX_REMINDER_MINUTES) {
            "Menit pengingat tidak valid"
        }
        require(input.hari in 1..7) { "Pilih hari kuliah" }
        val mulai = DateTimeUtils.parseClassTime(input.jamMulai.trim())
        val selesai = DateTimeUtils.parseClassTime(input.jamSelesai.trim())
        require(selesai > mulai) { "Jam selesai harus setelah jam mulai" }
        return database.withTransaction {
            val old = input.id?.let { dao.getJadwal(it) }
            require(input.id == null || old != null) { "Jadwal telah dihapus atau tidak ditemukan" }
            val oldCourse = old?.let { dao.getMataKuliah(it.mataKuliahId) }
            val sameName = dao.getMataKuliahByNama(nama)
            val courseBase =
                when {
                    sameName != null -> sameName
                    // Ganti nama mata kuliah yang sedang diedit, bukan membuat duplikat.
                    oldCourse != null -> oldCourse.copy(nama = nama)
                    else -> MataKuliahEntity(nama = nama)
                }
            val course =
                courseBase.copy(
                    nama = nama,
                    kode = input.kode.trim(),
                    dosen = input.dosen.trim(),
                    sks = input.sks,
                    updatedAt = maxOf(clock.millis(), courseBase.updatedAt + 1),
                    syncStatus = AppConstants.SYNC_PENDING,
                )
            dao.simpanMataKuliah(course)
            val jadwal =
                (old
                        ?: JadwalEntity(
                            mataKuliahId = course.id,
                            ruanganId = null,
                            hari = input.hari,
                            jamMulai = input.jamMulai,
                            jamSelesai = input.jamSelesai,
                        ))
                    .copy(
                        mataKuliahId = course.id,
                        ruanganId = input.ruanganId,
                        hari = input.hari,
                        jamMulai = input.jamMulai.trim(),
                        jamSelesai = input.jamSelesai.trim(),
                        pengingatMenit = input.pengingatMenit,
                        updatedAt = maxOf(clock.millis(), (old?.updatedAt ?: 0) + 1),
                        syncStatus = AppConstants.SYNC_PENDING,
                    )
            dao.simpanJadwal(jadwal)
            jadwal
        }
    }

    override suspend fun getPerubahan(jadwalId: String, tanggal: LocalDate) =
        dao.getPerubahan(jadwalId, tanggal.toString())?.takeUnless { it.isDeleted }

    override suspend fun hapusPerubahan(jadwalId: String, tanggal: LocalDate) {
        val old = dao.getPerubahan(jadwalId, tanggal.toString()) ?: return
        dao.hapusPerubahan(old.id, maxOf(clock.millis(), old.updatedAt + 1))
    }

    override suspend fun seedJadwalAwal(daftar: List<JadwalInput>) {
        if (daftar.isEmpty() || dao.hitungJadwal() > 0) return
        daftar.forEach { simpanJadwal(it) }
    }
}
