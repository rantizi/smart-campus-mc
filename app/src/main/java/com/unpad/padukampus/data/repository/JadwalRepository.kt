package com.unpad.padukampus.data.repository

import androidx.room.withTransaction
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.local.db.PaduKampusDatabase
import com.unpad.padukampus.data.local.entity.*
import com.unpad.padukampus.data.local.relation.JadwalDetail
import com.unpad.padukampus.data.local.relation.RuanganDetail
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
        val changeTime = change?.jenis == "GANTI_JAM"
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
            if (change?.jenis == "PINDAH_RUANG") override.lokasiBaru else detail.lokasi,
            change?.jenis == "BATAL",
            change?.catatan.orEmpty(),
        )
    }

    fun nextClass(classes: List<KelasHarian>, after: LocalTime): KelasHarian? =
        classes.filter { !it.dibatalkan && it.jamMulai > after }.minByOrNull { it.jamMulai }
}

interface JadwalRepository {
    fun observeJadwal(tanggal: LocalDate): Flow<List<KelasHarian>>

    fun observeKelasBerikutnya(tanggal: LocalDate, after: LocalTime): Flow<KelasHarian?>

    fun observeMataKuliah(): Flow<List<MataKuliahEntity>>

    suspend fun simpanMataKuliah(value: MataKuliahEntity)

    suspend fun simpanJadwal(value: JadwalEntity)

    suspend fun simpanPerubahan(value: PerubahanJadwalEntity)

    suspend fun hapusJadwal(id: String)
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
            require(value.jenis in setOf("BATAL", "GANTI_JAM", "PINDAH_RUANG")) {
                "Jenis perubahan tidak valid"
            }
            if (value.jenis == "GANTI_JAM") {
                require(
                    value.jamMulaiBaru != null &&
                        value.jamSelesaiBaru != null &&
                        DateTimeUtils.parseClassTime(value.jamSelesaiBaru) >
                            DateTimeUtils.parseClassTime(value.jamMulaiBaru)
                ) {
                    "Jam perubahan tidak valid"
                }
            }
            if (value.jenis == "PINDAH_RUANG")
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
}
