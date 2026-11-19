# Rencana kerja minggu 2 setelah refactor

Periode awal proyek: 17–23 November 2026. Ikuti [kepemilikan file dan aturan Git](ALUR_KERJA_TIM.md) serta [kontrak arsitektur](ARSITEKTUR.md).

| Urutan | Penanggung jawab | Pekerjaan berikutnya | Bukti selesai |
|---|---|---|---|
| 1 | Semua anggota | Sync baseline dan jalankan pemeriksaan lokal | Build/unit test/lint lulus di laptop anggota |
| 2 | Alfarisy | Uji tampilan, Back, pemulihan draf, insets dan form tugas | Screenshot dan hasil uji perangkat |
| 3 | Rantizi | Lengkapi pengelolaan mata kuliah/jadwal melalui kontrak repository | CRUD dan override per tanggal diuji dengan Room |
| 4 | Rantizi + Alfarisy | Verifikasi koordinat, uji HTTP cuaca, integrasi UI cache | Respons nyata dan perilaku luring terdokumentasi |
| 5 | Fadhli | Jalankan instrumentation dan test notifikasi/izin/restart | Hasil test serta log bug/retest |
| 6 | Rantizi + Alfarisy | Kumpulkan seed kampus dan implementasikan peta | Penanda sesuai data lokasi terverifikasi |
| 7 | Fadhli + semua | Perbarui laporan dari snapshot lama dengan bukti terbaru | Laporan mencerminkan hasil aktual, bukan target |

Minggu 3 tetap menangani Firebase Auth/Firestore, isolasi data per akun, pembersihan data saat logout, sinkronisasi, konflik data dan finalisasi. Interface CloudSyncManager belum merupakan implementasi cloud.
