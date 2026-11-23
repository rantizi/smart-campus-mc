# Test case minggu 3: login, sinkronisasi, keamanan

Metode: black box di emulator (Android 11, API 30) dan white box lewat unit test JVM. Tanggal: 23 November 2026. Screenshot di `output/minggu-03/screenshot/`.

## Black box di emulator

| ID | Kebutuhan | Skenario | Hasil yang diharapkan | Hasil | Bukti |
|---|---|---|---|---|---|
| C01 | FR-01, Gambar 2 | Buka aplikasi tanpa sesi login | Layar Login tampil, menu bawah tersembunyi | Lulus | 01-login.png |
| C02 | NFR-04 | Email `bukan-email`, kata sandi `123`, tekan Masuk | Pesan "Format email tidak valid" dan "Kata sandi minimal 6 karakter"; Firebase tidak dipanggil | Lulus | 02-login-validasi.png |
| C03 | FR-01 | Daftar akun baru (uji2) | Masuk ke Beranda; build debug mengisi 7 jadwal awal; 7 mata kuliah dan 7 jadwal terkirim ke `users/{uid}` | Lulus | Firestore: 7 + 7 dokumen |
| C04 | FR-01, NFR-03 | Profil, Keluar | Kembali ke Login; tombol Back keluar dari aplikasi; penanda sinkron di perangkat kosong | Lulus | |
| C05 | NFR-03 | Login akun uji1 setelah uji2 logout | Tugas dan jadwal uji2 tidak terlihat | Lulus | |
| C06 | FR-11 | Login ulang uji2 | Tugas dan jadwal uji2 kembali dari cloud tanpa menekan tombol | Gagal (B-02), lulus setelah perbaikan | 04-jadwal.png, 05-tugas.png |
| C07 | FR-05, FR-11 | Tambah tugas lewat form saat online | Dokumen tugas muncul di Firestore kurang dari 25 detik tanpa sinkron manual | Lulus | |
| C08 | NFR-06 | Mode pesawat, tambah tugas | Tugas tersimpan dan tampil; Profil menunjukkan "1 menunggu" | Lulus | 08-profil-luring-menunggu.png |
| C09 | NFR-06, NFR-07 | Mode pesawat, tekan "Sinkronkan sekarang" | Pesan "Tidak ada koneksi. Perubahan tersimpan lokal dan dikirim saat online." | Lulus | |
| C10 | NFR-06 | Koneksi kembali | Tugas luring terkirim otomatis oleh WorkManager; Profil "0 menunggu" | Lulus | 07-profil-sinkron.png |
| C11 | NFR-03 | Mode pesawat, tandai tugas selesai, Keluar | Dialog "Perubahan belum terkirim" dengan jumlah perubahan; Batal mempertahankan sesi | Lulus | 09-dialog-logout.png |
| C12 | NFR-03 | Seperti C11, pilih Tetap keluar | Kembali ke Login; data lokal dan penanda sinkron dihapus | Lulus | |
| C13 | NFR-02 | Buka Beranda setelah cuaca diambil | Label "lebih dari 30 menit" tidak muncul untuk data baru | Gagal (B-01), lulus setelah perbaikan | 03-beranda.png |
| C14 | NFR-03 | REST tanpa login membaca `users/abc/tugas` | `PERMISSION_DENIED` | Lulus | |
| C15 | NFR-03 | REST akun A menulis ke `users/{uid B}/tugas` | HTTP 403 | Lulus | |
| C16 | Optimasi | APK release (R8 minify + shrink resources) diinstal, login, buka semua menu | Cuaca terbaca (Gson), data tertarik dari cloud, peta tampil, tidak ada crash | Lulus | 06-peta.png |
| C17 | FR-08 | Buka Peta | Penanda gedung PPBS A sampai D memakai ikon gedung warna primer | Lulus | 06-peta.png |

Tingkat keberhasilan akhir: 17 dari 17 skenario lulus. Dua skenario (C06, C13) gagal pada percobaan pertama dan lulus setelah perbaikan B-01 dan B-02.

## Optimasi dan analisis statis

| Pemeriksaan | Sebelum | Sesudah |
|---|---|---|
| Ukuran APK | 14,4 MB (debug, tanpa R8) | 3,6 MB (release, R8 + shrink resources), turun 75% |
| Android Lint | 21 warning (15 versi dependency, 6 lain) | 16 warning, semuanya versi dependency yang sengaja dipertahankan |

## White box (unit test JVM)

`./gradlew :app:testDebugUnitTest`: **49 test, 0 gagal**.

| Suite | Jumlah | Cakupan baru minggu 3 |
|---|---|---|
| CloudSyncManagerTest | 9 | Luring/tanpa login tidak mengirim, kirim lalu tandai SYNCED, baris yang diubah saat pengiriman tetap PENDING, last-write-wins dua arah, ruangan tak dikenal dikosongkan, induk hilang dilewati, pengingat dijadwalkan ulang, batas pengambilan maju, data UID lain tidak terbaca, pembersihan saat logout |
| SyncMapperTest | 6 | Konversi bolak-balik empat entitas, `sync_status` tidak dikirim, field rusak memakai nilai bawaan, aturan last-write-wins |
| LoginViewModelTest | 4 | Validasi tanpa memanggil Firebase, mode daftar, pemetaan galat Firebase, email/mode bertahan setelah process death tetapi kata sandi tidak |
| AuthValidatorTest | 3 | Format email, panjang kata sandi |
| Suite lama | 27 | Tidak berubah, tetap lulus |

## Belum diuji

- HP fisik (performa, penghemat baterai, notifikasi di perangkat asli).
- Dua perangkat aktif bersamaan dengan akun yang sama (konflik nyata antarperangkat); logika last-write-wins hanya diuji di unit test.
- `connectedDebugAndroidTest` (test Room di perangkat).
