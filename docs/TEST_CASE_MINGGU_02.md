# Test case minggu 2 setelah refactor

## Pemeriksaan otomatis

Hasil eksekusi akhir dan jumlah test ada di [VERIFIKASI_SCAFFOLD.md](VERIFIKASI_SCAFFOLD.md). `scripts/verify_architecture.py` menjaga struktur target, tujuh entitas, batas MVVM, View Binding, Safe Args, dan kecocokan destination/menu. Build Gradle memverifikasi Kotlin, generated Safe Args, KSP dan query Room yang sesungguhnya.

Unit test berada di `app/src/test/`: DateTimeUtilsTest, JadwalResolverTest, CuacaRepositoryTest, dan TaskViewModelTest. Test Room/perubahan data ada di `app/src/androidTest/` dan harus dijalankan pada emulator/perangkat; mengompilasi APK test bukan berarti test tersebut lulus.

## Skenario perangkat yang belum dijalankan

| ID | Skenario | Hasil yang diharapkan | Status |
|---|---|---|---|
| A01 | Lima menu, tambah/ubah tugas, Simpan/Batal/Back dan notifikasi deep link | Destination dan argumen Safe Args benar | Belum dijalankan |
| A02 | Tambah tugas; tutup proses; buka ulang dalam mode pesawat | Tugas dan relasi mata kuliah tetap tersedia | Belum dijalankan |
| A03 | Rotasi/rekreasi proses pada form dan ganti filter | Draf, filter dan tanggal pulih sesuai state | Belum dijalankan |
| A04 | Selesai/hapus/ubah deadline sebelum pengingat, restart perangkat | Tidak ada notifikasi revisi lama atau tugas selesai/dihapus | Belum dijalankan |
| A05 | Tolak lalu beri izin notifikasi API 33+ | Data tugas tetap tersimpan; pengingat menghormati izin | Belum dijalankan |
| A06 | Isi koordinat valid, ambil cuaca, ulangi dalam TTL, lalu luring | HTTP/cache/status lama sesuai kondisi, data tidak hilang | Belum dijalankan |
| A07 | Ganti koordinat setelah cache lokasi pertama | Data lokasi lama tidak tampil sebagai lokasi baru | Belum dijalankan |
| A08 | Room instrumentation RepositoryRoomTest | CRUD/metadata, nested relation, pemulihan override dan transaksi benar | APK test dikompilasi; eksekusi belum dijalankan |
| A09 | Seed jadwal dan override BATAL/GANTI_JAM/PINDAH_RUANG | Tampilan jadwal dan kelas berikutnya mengikuti hasil efektif | Logika unit diuji; UI perangkat belum |
| A10 | Peta digital dan marker kampus | Lokasi sesuai seed yang diverifikasi | Menunggu implementasi peta/seed |

## Bug dan kendala

Belum ada bug runtime perangkat yang dikonfirmasi. Catat reproduksi, versi Android, kondisi jaringan/izin/baterai, perbaikan dan hasil retest setelah eksekusi nyata. Kebutuhan data kampus, form jadwal, peta digital dan sinkronisasi adalah pekerjaan terbuka, bukan bug yang sudah diperbaiki.
