# Firebase PaduKampus

Pembaruan 23 November 2026. Integrasi cloud minggu 3 sesuai Laporan 1 Tabel 6, FR-01, FR-11, dan NFR-03.

## Project

| Komponen | Nilai |
|---|---|
| Project ID | `padukampus-mc` (paket Spark, gratis) |
| Aplikasi Android | `com.unpad.padukampus` |
| Authentication | Email/Password |
| Firestore | Database `(default)`, lokasi `asia-southeast2` (Jakarta) |
| Aturan keamanan | `firestore.rules` di root repository |

Pemilik project adalah akun Google Fadhli. Anggota lain bisa ditambahkan lewat Firebase Console, menu Project settings, tab Users and permissions.

## Struktur data

```text
users/{uid}/mata_kuliah/{uuid}
users/{uid}/jadwal/{uuid}
users/{uid}/perubahan_jadwal/{uuid}
users/{uid}/tugas/{uuid}
```

ID dokumen sama dengan UUID baris Room, dan nama field sama dengan nama kolom SQLite. `sync_status` hanya ada di perangkat. Gedung dan ruangan adalah data referensi lokal dan tidak disinkronkan.

## Aturan keamanan

- Baca dan tulis hanya bila `request.auth.uid == uid` (NFR-03).
- Hanya empat koleksi di atas yang diizinkan. Path lain ditolak.
- Dokumen maksimal 30 field dan `updated_at` wajib bertipe integer.
- Hapus permanen dari aplikasi ditolak; aplikasi memakai hapus lunak `is_deleted`.

Hasil uji aturan (REST, 23 Nov 2026): baca tanpa login ditolak (`PERMISSION_DENIED`), akun menulis datanya sendiri diizinkan (200), akun menulis ke UID lain ditolak (403).

Memasang ulang aturan setelah diubah:

```bash
firebase deploy --only firestore:rules --project padukampus-mc
```

## google-services.json

File ini tidak di-commit (lihat `.gitignore`). Tanpa file itu aplikasi tetap ter-build dan berjalan dalam **mode lokal**: tanpa layar Login, tanpa sinkronisasi, data hanya di perangkat. Ini sengaja agar CI dan anggota yang belum punya file tetap bisa build.

Cara mendapatkan file:

1. Minta file ke Fadhli lewat chat pribadi, atau
2. Firebase Console, Project settings, bagian Your apps, unduh `google-services.json`, lalu simpan di `app/google-services.json`.

Isi file ini bukan rahasia seperti kata sandi (keamanan data dijaga oleh aturan Firestore), tetapi tetap tidak dipublikasikan di repository publik.

### CI

Bila repository memiliki secret `GOOGLE_SERVICES_JSON` (isi lengkap file), workflow memasangnya sebelum build sehingga APK artefak CI memakai Firebase. Tanpa secret, APK artefak berjalan dalam mode lokal. APK debug bisa diunduh dari tab Actions, artefak `padukampus-debug-apk`.

## Alur sinkronisasi

1. **Login/daftar** (`FirebaseAuthRepository`): setelah berhasil, sinkron awal dijalankan di scope aplikasi. Bila akun masih kosong, build debug mengisi jadwal awal.
2. **Ambil** (`FirestoreCloudSyncManager`): per koleksi, urut induk ke anak, dokumen dengan `updated_at` lebih besar dari batas terakhir (dikurangi toleransi jam 10 menit). Versi cloud dipakai hanya bila lebih baru (last-write-wins).
3. **Kirim**: baris `PENDING` dikirim dalam batch, lalu ditandai `SYNCED` hanya bila `updated_at` tidak berubah selama pengiriman.
4. **Pemicu**: setiap perubahan tabel (Room InvalidationTracker) menjadwalkan `SyncWorker` dengan syarat ada koneksi; ditambah sinkron berkala tiap 1 jam dan tombol "Sinkronkan sekarang" di Profil.
5. **Logout**: perubahan terakhir dicoba dikirim. Bila masih ada yang tertunda (luring), pengguna diminta konfirmasi. Data akun, pengingat, dan pekerjaan sinkron dihapus dari perangkat sebelum `signOut()`.

Room tetap menjadi sumber data tampilan (offline-first). Cache Firestore dibatasi di memori karena Room sudah menyimpan data luring.

## Akun uji

Dua akun uji dibuat di emulator untuk pengujian minggu 3: `uji1@padukampus.test` dan `uji2@padukampus.test`. Kata sandi dibagikan di grup kelompok, tidak ditulis di repository. Hapus kedua akun dari Firebase Console (Authentication, Users) setelah demo bila tidak dipakai lagi.
