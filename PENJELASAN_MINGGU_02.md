# Penjelasan refactor scaffolding PaduKampus

Pembaruan 19 November 2026. Dokumen ini menjelaskan keadaan kode setelah pembangunan ulang ke MVVM, Repository Pattern, dan offline-first. Penjelasan versi sebelumnya dipertahankan di `docs/arsip/PENJELASAN_SCAFFOLD_AWAL.md`.

## Perubahan utama

1. Namespace, application ID dan direktori Kotlin menjadi `com.unpad.padukampus`; sumber lama `id.ac.unpad.padukampus` dihapus dari source set aplikasi dan test.
2. Tujuh entitas dipisah per file. Nama tabel/kolom, relasi foreign key, metadata sinkronisasi, dan versi skema 1 dipertahankan. Properti Kotlin memakai camelCase dengan `@ColumnInfo`.
3. `CampusDao` diganti empat DAO domain. POJO `JadwalDetail` dan `TugasDetail` mengambil relasi Room secara transaksional, termasuk gedung dan ruangan pengganti.
4. Empat interface repository beserta implementasi offline menjadi batas kerja frontend/backend. Room menjadi sumber data tampilan; Retrofit hanya menulis pembaruan ke Room. Koordinat adalah pengaturan perangkat yang diakses melalui repository.
5. Setiap fitur mempunyai ViewModel dan UiState. Fragment tidak lagi membaca DAO, jaringan, atau SharedPreferences secara langsung. Pengamatan state mengikuti lifecycle view, binding dibersihkan, dan draf form memakai SavedStateHandle.
6. Safe Args Kotlin aktif. Daftar tugas memakai Directions, Form Tugas memakai navArgs, dan PendingIntent notifikasi memakai generated Args.
7. AppContainer menyediakan manual DI, Clock, NetworkMonitor, repository, API dan scheduler. PaduKampusApp menyiapkan NotificationChannel serta rekonsiliasi pengingat awal proses.
8. Pengingat dipisah menjadi scheduler, worker, dan NotificationHelper. Pekerjaan lama memeriksa revisi tugas; perubahan metadata monoton mencegah notifikasi dari revisi usang. Penanda revisi notifikasi membantu mencegah pengiriman ulang saat rekonsiliasi.
9. Resources dipisahkan per fitur. Ditambahkan aturan kepemilikan file, template PR, CI, schema JSON hasil KSP, architecture guardrail, unit test dan test Room untuk perangkat.

## Baseline yang dipertahankan

Kotlin 2.1.20, AGP 8.9.2, Gradle 8.11.1, KSP 2.1.20-1.0.32, compileSdk/targetSdk 35, minSdk 26, XML dan Material Components, Room 2.7.1, Retrofit 2.11.0 + Gson, Navigation 2.8.9 serta WorkManager 2.10.0. View Binding tetap aktif. Safe Args memakai versi Navigation yang sama.

JDK 17 dan komponen SDK 35 disiapkan secara lokal pada workspace untuk verifikasi, tanpa mengganti instalasi Java sistem. Folder alat dan hasil build tidak ikut masuk repository. Perubahan application ID berarti build ini merupakan aplikasi berbeda dari build hipotetis dengan ID lama; migrasi data antar application ID belum dibuat.

## Implementasi dan batas cakupan

Sudah ada implementasi awal alur tugas, pemilihan sumber dan filter status, jadwal efektif beserta kelas berikutnya, cache cuaca per koordinat, pengaturan koordinat, pencarian ruangan lokal, dan pengingat. Semua tetap membutuhkan pengujian perilaku pada perangkat.

Peta tile/marker, seed kampus terverifikasi, form CRUD jadwal/mata kuliah, login, isolasi akun, dan sinkronisasi Firestore belum selesai. CloudSyncManager secara eksplisit mengembalikan status belum diimplementasikan. Tidak ada kredensial, API key hardcode, data gedung fiktif, atau klaim respons API nyata.

Detail pemisahan lapisan, relasi, perubahan jadwal, cache dan worker ada di `docs/ARSITEKTUR.md`; aturan kerja tiga anggota ada di `docs/ALUR_KERJA_TIM.md`.

## Verifikasi

Build debug telah berhasil memproses Safe Args, KSP/Room, XML, Kotlin dan Java. Unit test mencakup zona waktu, tanggal tidak valid, TTL, pengingat, override jadwal, cache luring/kegagalan HTTP, propagasi cancellation, serta state/form ViewModel. Hasil akhir dan batas eksekusi dirangkum di `docs/VERIFIKASI_SCAFFOLD.md`.

APK pengujian Room dapat dikompilasi, tetapi pengujian instrumentation, notifikasi, persistensi perangkat dan HTTP Open-Meteo langsung belum dijalankan. Fixture pengujian sintetis hanya berada pada source set test, bukan seed produksi.

## Kedudukan laporan Word sebelumnya

File `output/minggu-02/laporan/Laporan2_SmartCampus.docx` dipertahankan sebagai snapshot sebelum refactor. Dokumen tersebut belum diperbarui untuk namespace, struktur paket, dan hasil build terbaru. Gunakan kode, README dan dokumentasi refactor ini sebagai acuan pekerjaan tim saat ini.
