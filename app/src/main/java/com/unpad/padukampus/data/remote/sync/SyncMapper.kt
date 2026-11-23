package com.unpad.padukampus.data.remote.sync

import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.data.local.entity.JadwalEntity
import com.unpad.padukampus.data.local.entity.MataKuliahEntity
import com.unpad.padukampus.data.local.entity.PerubahanJadwalEntity
import com.unpad.padukampus.data.local.entity.TugasEntity

/**
 * Konversi baris Room <-> dokumen Firestore. Nama field sama dengan nama kolom SQLite, ID dokumen
 * sama dengan UUID lokal (Laporan 1 bagian 6.1). sync_status hanya ada di perangkat.
 */
object SyncMapper {
    const val MATA_KULIAH = "mata_kuliah"
    const val JADWAL = "jadwal"
    const val PERUBAHAN_JADWAL = "perubahan_jadwal"
    const val TUGAS = "tugas"

    /** Urutan pengambilan mengikuti foreign key: induk lebih dulu. */
    val KOLEKSI = listOf(MATA_KULIAH, JADWAL, PERUBAHAN_JADWAL, TUGAS)

    fun toMap(value: MataKuliahEntity): Map<String, Any?> =
        mapOf(
            "id" to value.id,
            "nama" to value.nama,
            "kode" to value.kode,
            "dosen" to value.dosen,
            "sks" to value.sks.toLong(),
            "updated_at" to value.updatedAt,
            "is_deleted" to value.isDeleted,
        )

    fun toMap(value: JadwalEntity): Map<String, Any?> =
        mapOf(
            "id" to value.id,
            "mata_kuliah_id" to value.mataKuliahId,
            "ruangan_id" to value.ruanganId,
            "hari" to value.hari.toLong(),
            "jam_mulai" to value.jamMulai,
            "jam_selesai" to value.jamSelesai,
            "pengingat_menit" to value.pengingatMenit.toLong(),
            "updated_at" to value.updatedAt,
            "is_deleted" to value.isDeleted,
        )

    fun toMap(value: PerubahanJadwalEntity): Map<String, Any?> =
        mapOf(
            "id" to value.id,
            "jadwal_id" to value.jadwalId,
            "ruangan_baru_id" to value.ruanganBaruId,
            "tanggal" to value.tanggal,
            "jenis" to value.jenis,
            "jam_mulai_baru" to value.jamMulaiBaru,
            "jam_selesai_baru" to value.jamSelesaiBaru,
            "catatan" to value.catatan,
            "updated_at" to value.updatedAt,
            "is_deleted" to value.isDeleted,
        )

    fun toMap(value: TugasEntity): Map<String, Any?> =
        mapOf(
            "id" to value.id,
            "mata_kuliah_id" to value.mataKuliahId,
            "judul" to value.judul,
            "deskripsi" to value.deskripsi,
            "deadline" to value.deadline,
            "sumber" to value.sumber,
            "selesai" to value.selesai,
            "pengingat_menit" to value.pengingatMenit.toLong(),
            "updated_at" to value.updatedAt,
            "is_deleted" to value.isDeleted,
        )

    fun mataKuliah(id: String, data: Map<String, Any?>) =
        MataKuliahEntity(
            id = id,
            nama = data.str("nama"),
            kode = data.str("kode"),
            dosen = data.str("dosen"),
            sks = data.long("sks").toInt(),
            updatedAt = data.long("updated_at"),
            syncStatus = AppConstants.SYNC_SYNCED,
            isDeleted = data.bool("is_deleted"),
        )

    fun jadwal(id: String, data: Map<String, Any?>) =
        JadwalEntity(
            id = id,
            mataKuliahId = data.str("mata_kuliah_id"),
            ruanganId = data.strOrNull("ruangan_id"),
            hari = data.long("hari").toInt(),
            jamMulai = data.str("jam_mulai"),
            jamSelesai = data.str("jam_selesai"),
            pengingatMenit = data.long("pengingat_menit", 15).toInt(),
            updatedAt = data.long("updated_at"),
            syncStatus = AppConstants.SYNC_SYNCED,
            isDeleted = data.bool("is_deleted"),
        )

    fun perubahan(id: String, data: Map<String, Any?>) =
        PerubahanJadwalEntity(
            id = id,
            jadwalId = data.str("jadwal_id"),
            ruanganBaruId = data.strOrNull("ruangan_baru_id"),
            tanggal = data.str("tanggal"),
            jenis = data.str("jenis"),
            jamMulaiBaru = data.strOrNull("jam_mulai_baru"),
            jamSelesaiBaru = data.strOrNull("jam_selesai_baru"),
            catatan = data.str("catatan"),
            updatedAt = data.long("updated_at"),
            syncStatus = AppConstants.SYNC_SYNCED,
            isDeleted = data.bool("is_deleted"),
        )

    fun tugas(id: String, data: Map<String, Any?>) =
        TugasEntity(
            id = id,
            mataKuliahId = data.str("mata_kuliah_id"),
            judul = data.str("judul"),
            deskripsi = data.str("deskripsi"),
            deadline = data.long("deadline"),
            sumber = data.str("sumber", "LAINNYA"),
            selesai = data.bool("selesai"),
            pengingatMenit = data.long("pengingat_menit", 1440).toInt(),
            updatedAt = data.long("updated_at"),
            syncStatus = AppConstants.SYNC_SYNCED,
            isDeleted = data.bool("is_deleted"),
        )

    /**
     * Last-write-wins (Laporan 1 bagian 6.1): versi cloud dipakai hanya bila lebih baru. Bila sama,
     * versi lokal dipertahankan agar perubahan yang belum terkirim tidak tertimpa.
     */
    fun remoteMenang(localUpdatedAt: Long?, remoteUpdatedAt: Long): Boolean =
        localUpdatedAt == null || remoteUpdatedAt > localUpdatedAt

    private fun Map<String, Any?>.str(key: String, default: String = ""): String =
        this[key] as? String ?: default

    private fun Map<String, Any?>.strOrNull(key: String): String? =
        (this[key] as? String)?.takeIf { it.isNotBlank() }

    private fun Map<String, Any?>.long(key: String, default: Long = 0): Long =
        (this[key] as? Number)?.toLong() ?: default

    private fun Map<String, Any?>.bool(key: String): Boolean = this[key] as? Boolean ?: false
}
