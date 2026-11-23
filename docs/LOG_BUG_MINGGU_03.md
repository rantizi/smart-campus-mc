# Log bug minggu 3

Pengujian: emulator Android 11 (API 30, x86_64, profil Pixel 4), build debug `0.1.0`, Firebase `padukampus-mc`, 23 November 2026. Penguji: Fadhli (QA).

| ID | Temuan | Langkah reproduksi | Penyebab | Perbaikan | Status |
|---|---|---|---|---|---|
| B-01 | Cuaca yang baru diambil langsung ditandai "Data tersimpan sudah lebih dari 30 menit" | Buka Beranda saat cache cuaca kosong; cuaca diambil, label basi muncul | `HomeViewModel` menghitung umur cache dengan detak waktu yang diperbarui tiap 30 detik. Bila cuaca diambil setelah detak terakhir, umur menjadi negatif dan `isCacheFresh` menganggapnya tidak segar | Umur dihitung dengan `max(detak, jam terkini)`; aturan `isCacheFresh` untuk jam yang dimundurkan tetap dipertahankan | Diperbaiki, diuji ulang lulus |
| B-02 | Setelah login, data akun tidak ditarik dari cloud dan jadwal awal tidak terisi; data baru muncul setelah menekan "Sinkronkan sekarang" | Logout, lalu login dengan akun yang sudah punya data; buka Tugas | Firebase memberi tahu login berhasil, `MainActivity` menutup layar Login, `LoginViewModel` dibatalkan, dan coroutine login ikut batal sebelum memulai sinkron awal. Terjadi tidak selalu (bergantung urutan), sehingga pendaftaran akun uji2 sempat lolos | Proses login dijalankan di scope aplikasi (`applicationScope.async`), sinkron awal diluncurkan sebagai coroutine terpisah, ditambah percobaan ulang 3 kali dan log `PaduKampusSync` | Diperbaiki, diuji ulang lulus |
| B-03 | Label "Nama mata kuliah" bertumpuk dengan teks yang diketik di Form Jadwal/Tugas | Form Jadwal, ketik nama mata kuliah | Kolom diganti `MaterialAutoCompleteTextView` agar bisa dropdown, tetapi `TextInputLayout` masih memakai gaya bawaan sehingga padding teks tidak diatur | Gaya `FilledBox.ExposedDropdownMenu` pada `TextInputLayout` | Diperbaiki, diuji ulang lulus |

## Kendala lingkungan (bukan bug aplikasi)

| Kendala | Dampak | Penanganan |
|---|---|---|
| RAM laptop 7,6 GB; Gradle, emulator, dan browser berjalan bersamaan | Kernel mematikan proses Java (build gagal) atau emulator (exit 137); swap 2 GB penuh | Tutup browser/aplikasi berat saat uji; build dan emulator bergantian; Gradle memakai `-Pkotlin.compiler.execution.strategy=in-process -Dorg.gradle.workers.max=1` |
| Emulator baru menyala | Dialog "System UI isn't responding" | Pilih Wait, tunggu sekitar 1 menit setelah boot sebelum menguji |
| Peta: penanda gedung memakai ikon bawaan osmdroid | Tampilan kurang rapi (bukan kegagalan fungsi) | Diganti ikon gedung warna primer |
