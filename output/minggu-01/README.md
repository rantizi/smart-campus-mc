# Minggu 1 – Analisis & Desain (PaduKampus)

Checklist internal kelompok untuk Laporan 1. File ini **tidak** dikumpulkan; yang dikumpulkan adalah
`laporan/Laporan1_SmartCampus.pdf` (6 halaman, ±0,9 MB, batas 5 MB).

## Isi folder

```
output/minggu-01/
├── README.md                     ← file ini
├── laporan/
│   ├── Laporan1_SmartCampus.docx ← hasil scripts/build_report.py (boleh diedit di Word)
│   └── Laporan1_SmartCampus.pdf  ← hasil scripts/export_pdf.py
├── diagram/
│   ├── src/use_case.svg, nav_flow.svg, erd.svg   ← sumber (edit label di tag <text>)
│   └── use_case.png, nav_flow.png, erd.png       ← hasil render
├── wireframe/
│   ├── src/wireframes.html       ← sumber 8 layar (satu <section class="phone"> per layar)
│   ├── wf-01-beranda.png … wf-08-profil.png
│   └── wireframe_semua.png       ← 8 layar dalam satu gambar
└── scripts/
    ├── render_assets.py          ← SVG/HTML → PNG (Playwright + Chromium)
    ├── build_report.py           ← semua teks laporan ada di sini → .docx
    ├── export_pdf.py             ← .docx → .pdf lewat Microsoft Word
    ├── verify_report.py          ← cek otomatis (halaman, ukuran, urutan bagian, FR, placeholder)
    └── preview_pages.py          ← render halaman PDF ke PNG untuk dicek visual
```

## Checklist 8 bagian template (dengan penanggung jawab)

| No | Bagian template | Isi di laporan | Penanggung jawab | Status |
|---|---|---|---|---|
| 1 | Identitas Kelompok | Judul, tema, kelas/dosen, Tabel 1 anggota | Fadhli | Selesai |
| 2 | Latar Belakang | Tabel 2 (8 masalah → fitur), catatan masalah no. 1 hanya terjawab sebagian | Fadhli | Selesai |
| 3 | Tujuan Proyek | 4 tujuan + target pengguna | Fadhli | Selesai |
| 4 | Analisis Kebutuhan | Tabel 3 FR-01..FR-11 (Must/Should), Tabel 4 NFR-01..NFR-09, Gambar 1 use case | Fadhli | Selesai |
| 5 | Desain UI/UX Awal | 5.1 alur navigasi (Gambar 2), 5.2 wireframe 6 layar (Gambar 3), 5.3 warna & font (Tabel 5) | Alfarisy | Selesai, **perlu direview Alfarisy** |
| 6 | Perancangan Database | Gambar 4 ERD (tipe data, PK/FK, relasi, FR pemakai), format nilai, Tabel 6 integrasi Firebase/API | Rantizi | Selesai, **perlu direview Rantizi** |
| 7 | Rencana Pengembangan | Tabel 7 pembagian tugas (salinan persis KONTEKS_KELOMPOK.md), Tabel 8 jadwal 3 minggu | Semua | Selesai |
| 8 | Refleksi & Rencana Perbaikan | Tabel 9 (5 hambatan + solusi), pengembangan lanjut (kanal kelas, presensi, versi web) | Semua | Selesai |

Use case diagram (wajib menurut timeline brief) ada di Bagian 4.3.

## Pemetaan 7 kriteria teknis brief

| # | Kriteria | Dipenuhi oleh | Minggu |
|---|---|---|---|
| 1 | UI/UX | Material Components, bottom navigation, FAB, target sentuh ≥ 48 dp, warna kontras ≥ 4,5:1 (Tabel 5), wireframe | 2–3 |
| 2 | Navigasi & ≥ 3 fragment | Single activity + Navigation Component: Beranda, Jadwal, Tugas, Peta, Profil + Form Jadwal/Perubahan/Tugas + Detail Ruangan | 2 |
| 3 | Database lokal SQLite | Room (7 tabel, Gambar 4), DAO + Flow → Repository → ViewModel | 2 |
| 4 | API eksternal | Open-Meteo Forecast API via Retrofit (FR-10) | 2 |
| 5 | Cloud | Firebase Auth email/kata sandi (FR-01) + Firestore `users/{uid}/…` (FR-11) | 3 |
| 6 | Testing & debugging | Test case black box per FR, unit test logika pengingat/waktu, uji DAO Room in-memory, log bug | 2–3 |
| 7 | Optimasi + keamanan | Optimasi: cache cuaca 30 menit di Room, RecyclerView + DiffUtil, lazy loading. Keamanan: Firestore Rules pemilik saja, validasi input, HTTPS saja, tanpa secret di kode, tidak minta izin lokasi, data lokal dihapus saat logout | 3 |

## Placeholder yang harus diisi

| Placeholder | Lokasi | Kapan |
|---|---|---|
| `[ISI: koordinat kampus Jatinangor]` (latitude/longitude) | Laporan Tabel 6 (Open-Meteo) | Minggu 2 |
| `[ISI: data gedung dan ruangan kampus Jatinangor]` (nama, kode, koordinat gedung; kode & lantai ruangan) | Laporan 6.1; seed tabel `gedung`/`ruangan` | Minggu 2 |
| `Gedung [ISI]`, `R. [ISI]`, `Lantai [ISI]`, `[kode]` | Wireframe Beranda, Jadwal, Peta (Gambar 3) | Minggu 2 (opsional di wireframe) |
| `[nama dosen]`, `[mata kuliah]`, `[judul tugas]`, `[Nama pengguna]`, `[email]` | Wireframe (contoh isi, bukan data asli) | Tidak wajib diisi |
| `Catatan lain dari dosen: [isi]` | KONTEKS_KELOMPOK.md, belum dipakai di laporan | Bila ada masukan dosen |

Contoh nilai di wireframe (`xx°C`, tanggal, jam) hanya ilustrasi tata letak, bukan data.

## Keputusan dan asumsi yang perlu diketahui kelompok

- **FR-08 dipecah**: FR-08 peta + penanda gedung (Must), FR-09 cari ruangan (Should). Akibatnya cuaca = FR-10, sinkronisasi = FR-11.
- **Urutan Bagian 5**: alur navigasi (5.1) sebelum wireframe (5.2), lalu warna & font (5.3). Alasannya mengikuti urutan UCD modul minggu 4 (user flow → wireframe), dan agar laporan muat 6 halaman.
- **Wireframe di laporan hanya 6 layar** (3 × 2 agar tetap terbaca saat dicetak). Layar Login dan Profil ada di `wireframe/` dan pada alur navigasi.
- **Kunci primer UUID (TEXT)** sekaligus ID dokumen Firestore; tidak ada kolom `remote_id`.
- **minSdk 26 (Android 8.0)** diasumsikan; ubah NFR-09 bila kelompok memilih lain.
- **Palet warna dan ukuran font (Tabel 5) adalah usulan awal**; rasio kontras dihitung otomatis oleh `build_report.py` (rumus WCAG). Alfarisy bebas menggantinya.
- **Latar belakang berasal dari pengalaman kelompok**, bukan survei (dinyatakan jelas di laporan). Tidak ada angka, kutipan, atau referensi yang dikarang.
- **Format**: A4, Times New Roman 12, spasi 1,15, margin 2,54 cm. Isi tabel 9 pt dan keterangan gambar/tabel 10 pt agar muat 6 halaman.
- **Sebaiknya dicek ulang sebelum minggu 2**: aturan alarm tepat waktu Android 12/14, ketentuan penggunaan Open-Meteo (non-komersial), dan kuota paket Spark Firestore. Ketiganya belum dicek ulang ke dokumentasi terbaru saat laporan dibuat dan bisa berubah.

## Cara membuat ulang

Prasyarat (sudah terpasang di laptop ini): Python 3 dengan `python-docx`, `pymupdf`, `Pillow`,
`playwright` (+ Chromium), `pywin32`, dan Microsoft Word untuk ekspor PDF.

```bash
cd output/minggu-01
python scripts/render_assets.py   # 1. SVG/HTML -> PNG
python scripts/build_report.py    # 2. teks + gambar -> laporan/Laporan1_SmartCampus.docx
python scripts/export_pdf.py      # 3. .docx -> .pdf (Word)
python scripts/verify_report.py   # 4. cek otomatis
python scripts/preview_pages.py   # 5. (opsional) laporan/preview/*.png untuk cek visual
```

Bila mengedit langsung di Word, ekspor PDF manual (File → Save As → PDF) dan jalankan langkah 4.
Perhatikan: menjalankan `build_report.py` lagi akan menimpa .docx hasil edit manual.
