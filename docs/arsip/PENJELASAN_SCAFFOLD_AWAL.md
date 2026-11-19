# Penjelasan hasil minggu 2 PaduKampus

Disusun 19 November 2026 untuk periode minggu 2 pada rencana awal, yaitu 17–23 November 2026.

## Hasil yang dibuat

1. **Laporan Word delapan halaman** di `output/minggu-02/laporan/Laporan2_SmartCampus.docx`.
2. **Kerangka proyek Android** pada folder utama `smart-campus-mc`, yang dapat dibuka sebagai proyek Android Studio setelah SDK dan JDK disiapkan.
3. **Panduan memulai** di `README.md`, **pembagian kerja** di `docs/RENCANA_KERJA_MINGGU_02.md`, dan **test case serta kendala** di `docs/TEST_CASE_MINGGU_02.md`.
4. Skrip pembuatan laporan, pemeriksaan awal, dan render di `output/minggu-02/scripts/`.

## Dasar penyusunan

Konteks diambil dari `KONTEKS_KELOMPOK.md`, `Laporan1_SmartCampus.docx` pada folder induk, laporan yang sama di `output/minggu-01/laporan/`, ERD, diagram navigasi, dan wireframe minggu pertama. Kedua salinan laporan 1 identik saat diperiksa. Dokumen asli dan aset minggu 1 dipertahankan.

Nama proyek tetap **PaduKampus**, dengan tema Smart Campus Assistant untuk mahasiswa Unpad Jatinangor. Nama, NIM, dosen, kelas, periode kerja, pembagian peran, kode kebutuhan FR, tujuh tabel, serta penjadwalan Firebase pada minggu 3 mengikuti sumber tersebut. Isi dokumen sumber dipakai sebagai konteks proyek; arahan pengguna pada percakapan ini menentukan pekerjaan yang dilakukan.

Pengguna mengonfirmasi bahwa kode belum dibuat, kemudian meminta bantuan menyiapkan struktur kerja dan framework. Karena itu, laporan disusun sebagai **kemajuan awal minggu 2 setelah penambahan kerangka**, bukan laporan bahwa seluruh fitur minggu 2 sudah selesai. Tidak ada persentase kemajuan, screenshot aplikasi berjalan, respons API nyata, atau hasil pengujian perangkat yang dikarang.

## Isi laporan sesuai template

| Bagian pada gambar template | Isi yang disusun |
|---|---|
| 1 Ringkasan Kemajuan | Perbandingan target minggu 1 dengan kode kerangka dan pekerjaan yang masih terbuka |
| 2 Struktur Proyek di Android Studio | Package, pohon direktori, Activity, Fragment, library dan konfigurasi build |
| 3 Implementasi Navigasi dan Fragment | Enam destination, lima menu, alur tambah/ubah tugas, Bundle taskId, dan deep link; wireframe diberi label yang jelas |
| 4 Integrasi API Eksternal | Open-Meteo, endpoint GET, parameter, contoh JSON ilustratif, pemetaan DTO, cache dan penyajian per jam |
| 5 Implementasi Database Lokal SQLite | Tujuh entitas Room, relasi, metadata sinkronisasi, transaksi, dan contoh insert/update/read/soft delete |
| 6 Testing Awal dan Debugging | Sebelas pemeriksaan awal yang benar-benar dijalankan, skenario Android yang belum dijalankan, dan daftar kendala |
| 7 Refleksi dan Evaluasi Internal | Capaian, kekurangan, pembagian tindak lanjut, dan syarat menutup minggu 2 |

Format mengikuti laporan minggu 1: A4, margin 2,54 cm, Times New Roman, isi utama 12 pt dengan spasi 1,15, judul hitam, tabel ringkas, dan nomor halaman. Bagian teknis dipecah menjadi delapan halaman agar kode, tabel, dan gambar tetap terbaca. Word dirender menjadi gambar dan seluruh halaman diperiksa; PDF dan PNG pada folder `qa` hanya dipakai untuk pemeriksaan internal.

## Framework dan pola kerja yang dipilih

| Kebutuhan | Pilihan dan alasan |
|---|---|
| Bahasa dan UI | Kotlin, XML, Material Components; melanjutkan rancangan minggu 1 dan tugas penggunaan Fragment |
| Navigasi | Single Activity dengan Navigation Component dan lima menu bottom navigation |
| Database | Room 2.7.1 di atas SQLite; mengikuti tujuh tabel ERD |
| API | Retrofit 2.11.0 dan Gson; interface HTTP dipisahkan dari pengelolaan cache |
| Daftar | RecyclerView dan DiffUtil untuk daftar tugas |
| Status UI | ViewModel dan Flow pada daftar tugas; form dan cuaca masih perlu dipindahkan ke ViewModel |
| Pengingat | WorkManager dengan unique work berdasarkan UUID tugas; pengingat tidak menjamin ketepatan menit |
| Dependency injection | Penyedia dependency sederhana di PaduKampusApp; belum memakai Hilt |
| Build | AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20, KSP 2.1.20-1.0.32; JDK 17, SDK 35, minSdk 26 |

Versi di atas adalah baseline yang dipatok, bukan klaim versi terbaru. Gradle Wrapper resmi disertakan; checksum JAR telah dicocokkan dengan checksum resmi Gradle dan checksum distribusi ditulis pada properties.

## Kode yang sudah disiapkan

- `MainActivity`, `nav_graph.xml`, dan enam Fragment untuk Beranda, Jadwal, Tugas, Peta, Profil, dan Form Tugas.
- Tujuh entitas `gedung`, `ruangan`, `mata_kuliah`, `jadwal`, `perubahan_jadwal`, `tugas`, dan `cache_cuaca`, berikut `CampusDatabase` dan `CampusDao`.
- Daftar tugas menggunakan Flow/RecyclerView, form tambah dan ubah, transaksi mata kuliah/tugas, penandaan selesai, dan soft delete. Sumber tugas saat ini memakai `LAINNYA`; form pemilihan sumber belum dibuat.
- `WeatherApi`, DTO, `WeatherRepository`, cache 30 menit per pasangan koordinat, penanganan kegagalan jaringan, dan penyajian peluang hujan per jam. Koordinat diisi melalui Profil setelah kelompok memverifikasinya.
- Scheduler, worker notifikasi, permintaan izin Android 13+, deep link, pembatalan pengingat saat tugas selesai/dihapus, dan empat unit test kebijakan waktu pengingat.

**Belum lengkap:** CRUD jadwal, JOIN nama mata kuliah/gedung, penerapan perubahan per tanggal, peta dan seed lokasi, metadata mata kuliah pada form khusus, pemilihan sumber tugas, login, serta sinkronisasi Firebase. Safe Args belum dipasang; taskId menggunakan Bundle. Layar Jadwal masih menampilkan jadwal reguler dengan ID mata kuliah jika datanya ada. Peta baru merupakan layar awal.

## Pemeriksaan dan batas validasi

Sebelas pemeriksaan statis XML dan SQL SQLite desktop **lulus**, termasuk sembilan query DAO, insert/read, update status, soft delete, foreign key, keunikan perubahan jadwal, serta cache. Hasil terperinci tersimpan pada `output/minggu-02/qa/verification.json`.

Skema pemeriksaan diturunkan dari properti entitas untuk validasi SQL desktop; belum merupakan skema yang dihasilkan compiler Room. Pemeriksaan tersebut tidak membuktikan Kotlin atau KSP sudah dapat dikompilasi. **Build APK, lint, empat unit test JUnit, HTTP API langsung, pengujian emulator/perangkat, persistensi Android, dan notifikasi belum dijalankan.** Lingkungan pengerjaan belum memiliki SDK Android dan JDK 17 yang siap dipakai; Java yang ditemukan adalah versi 26.

Laporan tidak menganggap kendala lingkungan atau fitur yang belum dibuat sebagai bug runtime yang telah diperbaiki. Wireframe dalam laporan berasal dari minggu 1 dan tidak disebut sebagai screenshot implementasi. Contoh JSON hanya menjelaskan format, bukan data cuaca aktual.

## Langkah pertama kelompok

1. Buka `smart-campus-mc` di Android Studio; pasang SDK 35 dan pilih JDK 17 sebagai Gradle JDK.
2. Lakukan Sync, lalu jalankan `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` dari PowerShell.
3. Jalankan aplikasi pada Android API 26 ke atas; mulai dari alur tugas, kemudian isi koordinat terverifikasi dan uji cuaca.
4. Lanjutkan jadwal dan peta mengikuti rencana kerja. Catat bug hanya setelah dapat direproduksi.
5. Ambil screenshot dan log pengujian nyata, lalu perbarui status laporan sebelum dipakai sebagai laporan akhir minggu 2.

## Referensi teknis

- [Kompatibilitas AGP 8.9 dan kebutuhan Gradle/JDK](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [Navigation Component](https://developer.android.com/guide/navigation/use-graph/navigate)
- [Room dan SQLite](https://developer.android.com/training/data-storage/room)
- [Open-Meteo Forecast API](https://open-meteo.com/en/docs)
- [WorkManager dan waktu eksekusi](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Izin notifikasi Android](https://developer.android.com/develop/ui/views/notifications/notification-permission)
