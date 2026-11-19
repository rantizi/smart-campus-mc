# PaduKampus

Kerangka aplikasi Android asisten kuliah mahasiswa Unpad Jatinangor untuk Mobile Computing kelas B. Konteks anggota dan kebutuhan awal ada di [KONTEKS_KELOMPOK.md](KONTEKS_KELOMPOK.md).

Arsitektur memakai **MVVM + Repository + offline-first**, package `com.unpad.padukampus`. Lima menu utama, Form Tugas, Form Jadwal dan Form Perubahan menggunakan Fragment, XML, Material Components, View Binding, dan Navigation Safe Args.

## Mulai menjalankan

1. Buka folder `smart-campus-mc` di Android Studio yang mendukung AGP 8.9.2.
2. Gunakan **JDK 17** sebagai Gradle JDK. Pasang Android SDK Platform 35, Build Tools 35.0.0, dan Platform Tools melalui SDK Manager.
3. Lakukan Gradle Sync; Android Studio membuat `local.properties` untuk lokasi SDK masing-masing laptop.
4. Pilih emulator/perangkat Android API 26 ke atas, lalu jalankan modul `app`.

```powershell
python scripts/verify_architecture.py
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
# Hanya jika perangkat/emulator sudah tersambung:
.\gradlew.bat :app:connectedDebugAndroidTest
```

Pada Linux/macOS gunakan `bash ./gradlew`. APK debug dihasilkan pada `app/build/outputs/apk/debug/app-debug.apk`. Folder build tidak masuk Git. CI menjalankan build, unit test, lint, architecture guardrail dan kompilasi test APK.

## Baseline build

| Komponen | Versi |
|---|---|
| Kotlin / KSP | 2.1.20 / 2.1.20-1.0.32 |
| AGP / Gradle | 8.9.2 / 8.11.1 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |
| Room / Retrofit + Gson | 2.7.1 / 2.11.0 |
| Navigation dan Safe Args / WorkManager | 2.8.9 / 2.10.0 |

Versi dipertahankan sesuai kesepakatan proyek. Rekomendasi pembaruan versi dari Lint tidak diterapkan otomatis. Gradle Wrapper resmi serta checksum distribusinya tetap disertakan.

## Struktur kode

```text
app/src/main/java/com/unpad/padukampus/
  PaduKampusApp.kt
  core/          # AppResult, konstanta, tanggal/waktu, NetworkMonitor
  di/            # AppContainer dan factory ViewModel
  data/
    local/       # db, 7 entity, relation, dan 4 DAO
    remote/      # OpenMeteoApi, DTO, kontrak CloudSyncManager
    repository/  # interface dan implementasi offline per domain
  worker/        # scheduler, worker dan NotificationHelper
  ui/
    main/        # Activity tunggal
    home/        # Fragment, ViewModel dan UiState
    schedule/    # Fragment, ViewModel, UiState dan adapter
    tasks/       # daftar/form, ViewModel, UiState dan adapter
    map/         # Fragment, ViewModel dan UiState
    profile/     # Fragment, ViewModel dan UiState
```

## Perilaku yang disiapkan

- **Tugas**: tambah/ubah, sumber tugas, filter status, selesai dan soft delete. Draf form disimpan di SavedStateHandle. Mata kuliah baru dapat dibuat dari nama input; kode/dosen/SKS tetap kosong atau 0 sampai diisi melalui pengelolaan mata kuliah.
- **Jadwal**: observasi Room beserta nama mata kuliah dan lokasi, penerapan pembatalan/ganti jam/pindah ruang pada tanggal tertentu, serta kelas berikutnya setelah perubahan. Form Jadwal (FR-02) dan Form Perubahan (FR-04) tersedia dari layar Jadwal.
- **Cuaca**: masukkan koordinat terverifikasi melalui Profil. Repository menulis respons API ke Room; Beranda mengamati cache, tetap menampilkan data luring, dan memberi status cache lama. Tidak ada koordinat kampus fiktif atau izin lokasi perangkat.
- **Pengingat**: unique work per UUID, pemeriksaan revisi/data terkini, penanganan izin notifikasi, dan deep link Safe Args. Waktu tampil tidak dijamin tepat oleh WorkManager; perilaku perangkat belum diuji.
- **Peta**: osmdroid + OpenStreetMap dengan penanda dari tabel gedung dan pencarian ruangan lokal; data awal PPBS Gedung A–D (koordinat OpenStreetMap) ada di `data/local/seed/KampusSeed.kt`; kode ruangan menunggu jadwal resmi.
- **Cloud**: interface/stub minggu 3, belum login atau sinkronisasi Firebase. Tidak ada API key atau kredensial yang dikarang.

## Bekerja bertiga

Baca [alur kerja dan kepemilikan file](docs/ALUR_KERJA_TIM.md) sebelum membuat branch. Frontend memiliki UI/XML/navigasi, backend memiliki data/core/DI/build, dan QA memiliki worker/test/docs/CI. File bersama mempunyai satu pemilik; perubahan interface repository disepakati lewat PR.

Dokumentasi lain:

- [Arsitektur dan kontrak data](docs/ARSITEKTUR.md)
- [Penjelasan hasil refactor](PENJELASAN_MINGGU_02.md)
- [Test case dan batas verifikasi](docs/TEST_CASE_MINGGU_02.md)
- [Rencana kerja minggu 2](docs/RENCANA_KERJA_MINGGU_02.md)
- [Penyesuaian kode dengan Laporan 1](docs/PENYESUAIAN_LAPORAN_1.md)

Laporan Word pada `output/minggu-02/laporan/` adalah snapshot sebelum refactor ini dan dipertahankan sebagai arsip. Jangan menganggap namespace, struktur, atau status build di snapshot tersebut sebagai keadaan kode terbaru.
