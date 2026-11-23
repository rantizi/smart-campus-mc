package com.unpad.padukampus.data.remote.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/** Satu dokumen cloud: ID (= UUID lokal) dan isi field-nya. */
data class DokumenCloud(val id: String, val data: Map<String, Any?>)

/** Batas Firestore agar logika sinkronisasi bisa diuji dengan data palsu di unit test. */
interface SyncRemoteDataSource {
    /** Dokumen di users/{uid}/{koleksi} dengan updated_at lebih besar dari [sejak]. */
    suspend fun ambil(uid: String, koleksi: String, sejak: Long): List<DokumenCloud>

    suspend fun kirim(uid: String, koleksi: String, dokumen: List<DokumenCloud>)
}

class FirestoreSyncDataSource(private val firestore: FirebaseFirestore) : SyncRemoteDataSource {
    private fun koleksi(uid: String, nama: String) =
        firestore.collection("users").document(uid).collection(nama)

    override suspend fun ambil(uid: String, koleksi: String, sejak: Long): List<DokumenCloud> =
        koleksi(uid, koleksi)
            .whereGreaterThan("updated_at", sejak)
            .get(Source.SERVER)
            .await()
            .documents
            .map { DokumenCloud(it.id, it.data.orEmpty()) }

    override suspend fun kirim(uid: String, koleksi: String, dokumen: List<DokumenCloud>) {
        val ref = koleksi(uid, koleksi)
        // Batas WriteBatch Firestore 500 operasi.
        dokumen.chunked(400).forEach { bagian ->
            firestore
                .runBatch { batch -> bagian.forEach { batch.set(ref.document(it.id), it.data) } }
                .await()
        }
    }
}
