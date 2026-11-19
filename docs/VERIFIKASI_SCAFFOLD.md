# Hasil verifikasi scaffolding

Tanggal: 19 November 2026. Menggunakan JDK 17.0.21, Gradle 8.11.1, Android SDK Platform 35 dan Build Tools 35.0.0 dari folder alat lokal workspace.

## Hasil akhir

| Pemeriksaan | Hasil |
|---|---|
| Architecture guardrail | Lulus: 49 file target, 7 entitas, batas MVVM, navigasi, Safe Args, XML dan baseline build |
| Clean build `:app:assembleDebug` | Lulus; Safe Args, KSP/Room, Kotlin, Java, resource dan packaging diproses dari awal |
| `:app:testDebugUnitTest` | 26 test lulus; 0 gagal, 0 error, 0 dilewati |
| `:app:lintDebug` | Lulus; 0 error, 15 warning GradleDependency karena versi baseline sengaja dipertahankan |
| `:app:assembleDebugAndroidTest` | Lulus; APK instrumentation berhasil dikompilasi |
| Perbandingan skema | 7 tabel mempertahankan nama/urutan kolom, tipe, nullability, PK dan FK awal |
| Whitespace Git | Lulus untuk perubahan tracked |

## Cakupan unit test

| Suite | Jumlah | Cakupan |
|---|---|---|
| DateTimeUtilsTest | 7 | WIB, tanggal invalid, TTL, pengingat dan perubahan clock |
| JadwalResolverTest | 7 | Hari reguler, BATAL, GANTI_JAM, PINDAH_RUANG, tombstone dan kelas berikutnya |
| CuacaRepositoryTest | 7 | TTL, refresh ke penyimpanan fake, luring, galat HTTP, koordinat terpisah, payload invalid dan cancellation |
| TaskViewModelTest | 5 | Konversi form, tanggal/menit invalid, target edit hilang dan pemulihan draf |

Test cuaca memakai fake API/DAO/network, bukan HTTP langsung. Empat test `RepositoryRoomTest` memakai database in-memory Android; APK test berhasil dikompilasi, tetapi eksekusi tes tersebut belum dijalankan pada perangkat.

## Perintah verifikasi

```powershell
python scripts/verify_architecture.py
.\gradlew.bat :app:clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon --console=plain
```

Clean build terakhir berhasil setelah daemon verifikasi dihentikan untuk melepas file cache Lint yang terkunci di Windows. Setelah direktori ikon kosong dibersihkan, `:app:lintDebug` dijalankan kembali dan hanya menyisakan peringatan versi dependency. Platform Tools belum terpasang pada runtime alat lokal; build tetap berhasil, tetapi paket tersebut diperlukan untuk adb dan pengujian perangkat.

## Artefak lokal

- APK aplikasi: `app/build/outputs/apk/debug/app-debug.apk`.
- APK test: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Laporan unit test: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Laporan Lint: `app/build/reports/lint-results-debug.html`.
- Skema hasil KSP: `app/schemas/com.unpad.padukampus.data.local.db.PaduKampusDatabase/1.json`.
- Ringkasan mesin dan checksum APK: `output/minggu-02/qa/refactor-verification.json`.

Metadata APK diperiksa menggunakan aapt: application ID `com.unpad.padukampus`, minSdk 26, targetSdk dan compileSdk 35. Hasil build dan QA lokal diabaikan Git; JSON skema KSP disertakan sebagai dasar migration berikutnya.

## Batas verifikasi

Tampilan aktual, pengujian emulator/perangkat, pemulihan proses Android, notifikasi dan kebijakan baterai, persistensi perangkat, HTTP Open-Meteo nyata, serta GitHub Actions pada server belum dijalankan. Peta digital/seed kampus, form jadwal dan cloud masih pekerjaan lanjutan. Keberhasilan build tidak menyatakan fitur tersebut selesai.
