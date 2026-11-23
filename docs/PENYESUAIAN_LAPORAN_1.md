# Penyesuaian kode dengan Laporan 1

Pembaruan 19 November 2026. Acuan: `output/minggu-01/laporan/Laporan1_SmartCampus.pdf`.
**Belum di-build atau diuji di perangkat**; semua butir di bawah perlu diverifikasi lewat build Gradle dan HP (USB debugging).

| Bagian Laporan 1 | Sebelum | Sesudah |
|---|---|---|
| Tabel 5 – palet warna | Hanya warna primer, tema `DayNight` | `res/values/colors.xml` berisi 6 warna Tabel 5; tema `Light` agar rasio kontras berlaku |
| Tabel 5 – font | Isi 16 sp | `res/values/styles_text.xml`: judul layar 22 sp, judul kartu 16 sp tebal, isi 14 sp, keterangan 12 sp (Roboto = `sans-serif`) |
| Gambar 3a – Beranda | Teks polos | Kartu Cuaca, Kelas berikutnya (+ "Lihat di peta"), Jadwal hari ini, Tugas terdekat |
| Gambar 3b – Jadwal, FR-02 | Hanya daftar, tanpa form | Kartu jadwal + menu ⋮ (Ubah jadwal, Catat perubahan, Hapus) + tombol + → **Form Jadwal** |
| Gambar 3c – Form Perubahan, FR-04 | Belum ada | **Form Perubahan**: jadwal, tanggal, jenis (pindah ruang/ganti jam/batal), ruangan baru/jam baru, catatan, hapus perubahan |
| Gambar 3d – Tugas | Tanpa warna status | Kartu; deadline lewat = merah "terlambat", selesai = hijau |
| Gambar 3f, Tabel 6 – Peta, FR-08/09 | Daftar teks | **osmdroid + OpenStreetMap**: penanda dari tabel `gedung`, cari ruangan, kartu ruangan terpilih, "Pusatkan peta" |
| Gambar 2 – alur navigasi | 6 destination | 8 destination (+ Form Jadwal, Form Perubahan), Safe Args; bottom nav disembunyikan pada form |

## Yang sengaja belum diubah

- **Data gedung terisi dari sumber nyata** (`data/local/seed/KampusSeed.kt`, dijalankan saat aplikasi dibuka):

  | Kode | Gedung | Lat | Lon | Sumber |
  |---|---|---|---|---|
  | PPBS-A | PPBS Gedung A (Gedung Biru) – perkuliahan | -6.923388 | 107.773155 | OSM way 133952527 |
  | PPBS-B | PPBS Gedung B (Gedung Biru) – perkuliahan | -6.923265 | 107.773428 | OSM way 133952528 |
  | PPBS-C | PPBS Gedung C | -6.923973 | 107.773409 | OSM way 133956279 |
  | PPBS-D | PPBS Gedung D – laboratorium & manajemen | -6.923802 | 107.773778 | OSM way 133956278 |

  Koordinat = titik tengah poligon bangunan OpenStreetMap (© OpenStreetMap contributors, ODbL), diambil 19-11-2026. Fungsi gedung dari informatika.unpad.ac.id/s6. Koordinat cuaca bawaan = PPBS Gedung A (mengisi `[ISI: koordinat kampus Jatinangor]` di Laporan 1 Tabel 6), tetap bisa diganti di Profil.
- **Ruangan (pembaruan 23 Nov 2026)**: diisi dari kolom Ruangan portal akademik dan keterangan Fadhli. Format kode portal `0201-GGGG-RRRR`: `GGGG` gedung (0724 = PPBS A, 0726 = PPBS B, 0720 = PPBS C), `RRRR` nomor ruang, digit pertama nomor ruang = lantai. Total 12 ruangan di `KampusSeed.ruangan`: PPBS A (105, Aula 201, Aula 202, 204, 205, 207, 208), PPBS B (201, 204, 205, 206), PPBS C (309).
- Login/Profil (Gambar 3 layar Login, FR-01, FR-11) tetap pekerjaan minggu 3.
- Skema database tidak berubah (versi 1); hanya query DAO baru.

## File yang disentuh (untuk review pemilik file, lihat ALUR_KERJA_TIM.md)

- **Alfarisy (UI):** `ui/home/*`, `ui/schedule/*` (+ `ScheduleFormFragment/ViewModel`, `ChangeFormFragment/ViewModel`), `ui/map/*` (+ `RoomAdapter`), `ui/tasks/TaskAdapter.kt`, `ui/main/MainActivity.kt`, semua `res/layout`, `res/navigation/nav_graph.xml`, `res/values/{themes,colors,styles_text,strings_*}.xml`
- **Rantizi (data/build):** `JadwalDao.kt`, `JadwalRepository.kt` (`JadwalInput`, `simpanJadwal(input)`, `observeSemuaJadwal`, `getJadwalDetail`, `getPerubahan`, `hapusPerubahan`, field `KelasHarian.jenisPerubahan`), `AppContainer.kt`, `PaduKampusApp.kt` (konfigurasi osmdroid), `app/build.gradle.kts` (osmdroid 6.1.20)
- **Fadhli (QA):** `scripts/verify_architecture.py`, `JadwalResolverTest.kt`, dokumen ini, `TEST_CASE_MINGGU_02.md`

## Skenario uji tambahan

Lihat B01–B08 di [TEST_CASE_MINGGU_02.md](TEST_CASE_MINGGU_02.md).

## Jadwal awal untuk uji di HP (build debug saja)

`app/src/debug/.../data/local/seed/JadwalAwal.kt` berisi 7 jadwal kuliah nyata Fadhli (Selasa: PPL II, Mobile Computing, SIG; Rabu: Kapita Selekta JKKD, Seminar; Kamis: Skripsi; Sabtu: Magang). Data ini dimasukkan sekali saat tabel jadwal masih kosong. Build release memakai `app/src/release/.../JadwalAwal.kt` yang kosong, jadi data pribadi tidak ikut ke pengguna lain.

Kode `0201-07xx-0xxx` di jadwal sumber ternyata kode ruangan portal (dikonfirmasi Fadhli, 23 Nov 2026). Ketujuh jadwal sekarang memakai ruangan tersebut; jadwal yang sudah tersimpan tanpa ruangan dilengkapi otomatis saat sinkron awal.

## Kurikulum (pembaruan 23 Nov 2026)

`data/local/seed/KurikulumTI.kt` berisi 69 mata kuliah S-1 Teknik Informatika dari Buku Pedoman Prodi 2021-2022 (Revisi 2022 OBE) halaman 28-30: 45 wajib semester I-VIII dan 24 pilihan dari 4 peminatan (IKMN, SISM, SCSG, JKKD). Empat mata kuliah memakai sandi portal karena berbeda dengan buku: Kapita Selekta JKKD `D10K-7C01`, Magang `UNX10.040701`, Seminar `UNX10.040801`, Skripsi 6 SKS `UNX10.040802`. Kurikulum disemai ke setiap akun setelah sinkron awal (ID tetap dari sandi sehingga tidak ganda di cloud), dan menjadi pilihan dropdown "Mata kuliah" di Form Jadwal dan Form Tugas.

## Perapian kode (standar Android/Kotlin)

- **Pesan UI di `strings.xml`**: ViewModel tidak lagi menulis teks langsung. Pesan milik aplikasi memakai `UiText.Res(R.string.…)` (`core/common/UiText.kt`, daftar di `res/values/strings_errors.xml`); Fragment menampilkannya dengan `asString(context)`. Pesan validasi dari repository (`require`) dan galat API cuaca tetap dibawa sebagai `UiText.Raw`.
- **Jenis perubahan jadwal**: literal `"BATAL"`, `"GANTI_JAM"`, `"PINDAH_RUANG"` diganti konstanta `data/model/JenisPerubahan`. Kolom tetap TEXT, jadi skema Room versi 1 dan Firestore tidak berubah. `enum` + TypeConverter belum dipakai agar skema dan test lama tidak perlu diubah tanpa build.
- **Gaya kode**: import diurutkan, tidak ada baris di atas 100 karakter pada file yang diubah. Tetap jalankan *Code → Reformat Code* di Android Studio sebelum commit.
- **Belum diubah**: DI manual (`AppContainer`) belum diganti Hilt, karena itu perubahan Gradle/DI besar yang harus diuji dengan build.
