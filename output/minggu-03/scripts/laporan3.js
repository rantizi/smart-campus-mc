// Laporan 3 PaduKampus: Optimasi, Keamanan, dan Finalisasi (template Minggu 3, 3-6 halaman).
const L = require('./lib');
const { p, center, h1, h2, bullet, caption, table, figure, gallery, code, members } = L;

const out = process.argv[2] || 'Laporan3_SmartCampus.docx';

const body = [
  center('MINGGU 3 OPTIMASI, KEAMANAN, DAN FINALISASI', { bold: true, size: 28, after: 60 }),
  center('Optimasi, Keamanan, dan Finalisasi Aplikasi Mobile', { bold: true, size: 24 }),
  center('PaduKampus Aplikasi Asisten Kuliah Harian Mahasiswa Berbasis Android'),
  center('Mobile Computing Kelas B | Deni Setiana'),
  center('S1 Teknik Informatika Universitas Padjadjaran'),
  center('Semester Ganjil 2026/2027'),
  center('Periode minggu 3: 24-30 November 2026 | Pembaruan 24 November 2026', { after: 140 }),
  members(),

  h1('1 Fitur Tambahan dan Perbaikan'),
  p('Minggu ketiga menindaklanjuti pekerjaan terbuka pada Laporan 2, terutama FR-01 (akun) dan FR-11 (sinkronisasi cloud), lalu melengkapi data kampus yang sebelumnya masih berupa penanda [ISI]. Seluruh perubahan dikerjakan di branch `feat/firebase-auth-sync` dan diajukan lewat Pull Request #2 dengan CI yang lulus.'),
  caption('Tabel', 'Fitur baru dan perbaikan minggu ketiga'),
  table(['Fitur', 'Kebutuhan', 'Hasil'], [
    ['Login, daftar, logout', 'FR-01', 'Firebase Authentication email dan kata sandi; layar awal ditentukan oleh "Sudah login?" sesuai alur navigasi Gambar 2 Laporan 1'],
    ['Sinkronisasi cloud', 'FR-11', 'Room ke Firestore dan sebaliknya, offline-first, otomatis saat online, berkala tiap 1 jam, dan tombol "Sinkronkan sekarang"'],
    ['Profil', 'FR-01, FR-11', 'Email akun, status "Terakhir sinkron · N menunggu", tombol logout sesuai wireframe wf-08'],
    ['Kurikulum lengkap', 'FR-02', '69 mata kuliah S-1 TI (45 wajib, 24 peminatan) dari Buku Pedoman Prodi 2021-2022; dropdown di Form Jadwal dan Form Tugas; di Form Jadwal, sandi dan SKS terisi otomatis'],
    ['Data ruangan', 'FR-03, FR-09', '12 ruangan PPBS A, B, C berkode portal `0201-GGGG-RRRR` (0724 = A, 0726 = B, 0720 = C); tujuh jadwal kuliah terhubung ke ruangannya'],
    ['Peta dan cuaca', 'FR-08, FR-10', 'Ikon penanda gedung, kartu ruangan berisi nama, lantai dan kode; peluang hujan 70% ke atas disorot warna peringatan'],
  ], [20, 14, 66]),
  caption('Tabel', 'Bug yang ditemukan dan diperbaiki'),
  table(['ID', 'Bug', 'Penyebab dan perbaikan'], [
    ['B-01', 'Cuaca yang baru diambil langsung ditandai "lebih dari 30 menit"', 'Umur cache dihitung dari detak waktu 30 detik sehingga bernilai negatif. Umur sekarang dihitung dengan jam terkini.'],
    ['B-02', 'Setelah login, data akun kadang tidak ditarik dari cloud', 'Layar Login ditutup saat login berhasil sehingga coroutine-nya ikut batal. Proses login dipindah ke scope aplikasi, ditambah percobaan ulang.'],
    ['B-03', 'Label "Nama mata kuliah" bertumpuk dengan teks', 'Kolom dropdown belum memakai gaya ExposedDropdownMenu. Gaya ditambahkan pada TextInputLayout.'],
  ], [8, 34, 58]),

  h1('2 Integrasi Cloud (Firebase)'),
  p('Aplikasi memakai project Firebase `padukampus-mc` paket Spark (gratis): Authentication metode Email/Password dan Cloud Firestore di lokasi `asia-southeast2` (Jakarta). Struktur data mengikuti rancangan Tabel 6 Laporan 1: `users/{uid}/mata_kuliah`, `/jadwal`, `/perubahan_jadwal`, dan `/tugas`, dengan ID dokumen sama dengan UUID baris Room sehingga relasi tetap utuh di perangkat lain.'),
  caption('Tabel', 'Fungsi yang dihubungkan ke cloud'),
  table(['Fungsi aplikasi', 'Layanan', 'Data'], [
    ['Daftar, login, logout (FR-01)', 'Firebase Authentication', 'Email dan kata sandi; UID menjadi kunci data'],
    ['Cadangan dan pemulihan data (FR-11)', 'Cloud Firestore', 'Mata kuliah, jadwal, perubahan jadwal, dan tugas'],
    ['Pembatasan akses (NFR-03)', 'Firestore Security Rules', 'Hanya pemilik UID yang dapat membaca dan menulis'],
  ], [36, 28, 36]),
  ...L.figure('diagram-sinkron.png', 560, 'Alur autentikasi dan sinkronisasi Room dengan Firestore'),
  p('**Autentikasi.** Email divalidasi formatnya dan kata sandi minimal 6 karakter sebelum dikirim ke Firebase. Pesan galat Firebase diterjemahkan menjadi pesan yang jelas (email sudah terdaftar, kata sandi salah, tidak ada koneksi). Setelah login, aplikasi menarik data akun, menambahkan kurikulum bila belum ada, lalu menjadwalkan sinkron berkala. Tanpa berkas `google-services.json` aplikasi tetap berjalan dalam mode lokal sehingga CI dan anggota yang belum memiliki berkas tidak terhambat.'),
  p('**Sinkronisasi.** Setiap perubahan tabel Room memicu `SyncWorker` (WorkManager) yang hanya berjalan saat ada koneksi. Sinkron mengambil dokumen yang `updated_at`-nya lebih baru lebih dulu, lalu mengirim baris `PENDING`. Konflik diselesaikan dengan last-write-wins, dan hapus memakai tombstone `is_deleted` agar penghapusan ikut tersinkron.'),

  h1('3 Optimasi Performa'),
  caption('Tabel', 'Teknik optimasi yang diterapkan'),
  table(['Teknik', 'Penerapan', 'Dampak'], [
    ['Offline-first (caching)', 'Room menjadi sumber data layar; Firestore hanya cadangan dan cache Firestore dibatasi di memori', 'Beranda tampil tanpa menunggu jaringan; tidak ada data ganda di penyimpanan'],
    ['Cache cuaca 30 menit', 'Respons Open-Meteo disimpan di tabel `cache_cuaca` dan tidak diminta ulang selama masih segar', 'Hemat kuota; cuaca tetap tampil saat luring (NFR-02, NFR-07)'],
    ['Pengambilan bertahap', 'Hanya dokumen dengan `updated_at` di atas batas terakhir yang diambil', 'Sinkron berikutnya tidak mengunduh ulang semua data'],
    ['Penulisan batch', 'Dokumen dikirim dalam WriteBatch hingga 400 dokumen per permintaan', 'Jumlah permintaan jaringan berkurang'],
    ['Penjadwalan hemat baterai', 'WorkManager dengan syarat koneksi dan jeda 3 detik untuk menggabungkan perubahan beruntun', 'Tidak ada percobaan jaringan saat luring'],
    ['Lazy loading', 'Database, API, dan Firestore dibuat saat pertama dipakai (`by lazy`); daftar memakai RecyclerView dengan DiffUtil', 'Komponen yang belum dipakai tidak dibuat; hanya baris yang berubah digambar ulang'],
    ['R8 dan shrink resources', 'Build release mengecilkan dan mengaburkan kode serta menghapus resource tak terpakai', 'APK 14,4 MB (debug) menjadi 3,6 MB (release), turun 75%'],
  ], [22, 46, 32]),

  h1('4 Keamanan Aplikasi'),
  caption('Tabel', 'Penerapan keamanan dan buktinya'),
  table(['Aspek', 'Penerapan', 'Bukti uji'], [
    ['Akses data cloud', 'Aturan Firestore: baca/tulis hanya bila `request.auth.uid == uid`; hanya empat koleksi yang diizinkan; hapus permanen ditolak; maksimal 30 field per dokumen', 'Baca tanpa login ditolak; akun A menulis ke UID B ditolak (HTTP 403)'],
    ['Validasi input', 'Format email, kata sandi minimal 6 karakter, jam selesai setelah jam mulai, format deadline (NFR-04)', 'Pesan galat tampil di emulator; Firebase tidak dipanggil (unit test)'],
    ['Proteksi data pengguna', 'Logout menghapus data akun dan pengingat dari perangkat sebelum `signOut()`; konfirmasi bila ada perubahan belum terkirim; kata sandi tidak ditulis ke SavedStateHandle', 'Dialog konfirmasi muncul saat luring; pembersihan data dan isolasi UID lulus unit test; uji isolasi dua akun di emulator perlu diulang'],
    ['Koneksi dan kredensial', 'HTTPS saja (`usesCleartextTraffic=false`), `allowBackup=false`, tidak ada API key di kode, `google-services.json` tidak di-commit, tidak meminta izin lokasi', 'Pemeriksaan manifest dan pencarian kode; larangan commit berkas diperiksa CI'],
    ['Kode release', 'R8 mengaburkan nama kelas', 'APK release dijalankan di emulator: login, Beranda, Jadwal, Tugas, Peta'],
  ], [18, 52, 30]),
  ...code([
    "match /users/{uid}/{koleksi}/{docId} {",
    "  // koleksi: mata_kuliah, jadwal, perubahan_jadwal, tugas",
    "  allow read: if isOwner(uid) && koleksi in [...];",
    "  allow create, update: if isOwner(uid) && koleksi in [...] && validDoc();",
    "  allow delete: if false;  // hapus lunak lewat is_deleted",
    "}",
  ]),
  caption('Gambar', 'Inti aturan keamanan Firestore (firestore.rules)'),

  h1('5 Testing dan Debugging Akhir'),
  p('Pengujian pada 23 November 2026 memakai tiga lapis: **black box** di emulator Android 11 (API 30, profil Pixel 4) dengan akun uji, **white box** berupa unit test JVM untuk logika sinkronisasi, login, dan data kurikulum, serta **analisis statis** (Android Lint dan pemeriksaan arsitektur) yang dijalankan otomatis oleh GitHub Actions pada setiap Pull Request.'),
  caption('Tabel', 'Ringkasan hasil uji black box minggu ketiga'),
  table(['Kelompok', 'Skenario', 'Lulus', 'Catatan'], [
    ['Login, daftar, logout', '5', '4', 'Validasi input dan back stack lulus; C05 (isolasi akun) perlu uji ulang karena dijalankan sebelum B-02 diperbaiki'],
    ['Sinkronisasi dan luring', '8', '8', 'Mode pesawat, kirim otomatis, login ulang, konfirmasi logout, hapus dari cloud; satu gagal awal (B-02)'],
    ['Aturan keamanan Firestore', '2', '2', 'Tanpa login dan lintas UID ditolak'],
    ['Pengingat tugas', '2', '2', 'Notifikasi muncul sekitar 3 menit sebelum deadline (ketelitian pengamatan sekitar 1 menit); ketuk membuka Form Tugas'],
    ['Kurikulum, ruangan, peta', '6', '6', 'Semai kurikulum, ruangan jadwal, dropdown, isian otomatis, cari "205"; satu gagal awal (B-03)'],
    ['Jadwal dan perubahan jadwal', '5', '5', 'Validasi jam, tambah jadwal, pindah ruang, ganti jam, batal, hapus perubahan dan jadwal'],
    ['Cuaca', '1', '1', 'Label basi salah (B-01), lulus setelah perbaikan'],
    ['APK release (R8)', '1', '1', 'Build sebelum data kurikulum: login, Beranda, Jadwal, Tugas, dan Peta tanpa crash'],
    ['**Jumlah**', '**30**', '**29**', 'Tingkat keberhasilan 96,7%; detail di `docs/TEST_CASE_MINGGU_03.md`'],
  ], [28, 12, 10, 50]),
  p('Unit test berjumlah **54 dan seluruhnya lulus**, termasuk 27 test baru untuk sinkronisasi (last-write-wins dua arah, baris yang berubah saat pengiriman tetap `PENDING`, data UID lain tidak terbaca), login, dan kecocokan data kurikulum dengan buku pedoman. Android Lint tidak menyisakan peringatan selain versi dependency yang sengaja dipertahankan. Di emulator (laptop Celeron, grafis perangkat lunak) aplikasi terbuka dari nol dalam 3,5 sampai 3,7 detik dengan memori 85 MB; target 2 detik pada NFR-01 belum tercapai di lingkungan ini. Waktu di HP fisik diperkirakan lebih singkat, tetapi belum diukur. Uji yang belum dilakukan: HP fisik, dua perangkat aktif bersamaan, dan ulangan C05.', { before: 80 }),

  h1('6 Dokumentasi Akhir'),
  ...L.figure('nav_flow.jpg', 430, 'Flowchart navigasi (rancangan Laporan 1; notifikasi kelas FR-07 belum dibuat, Detail Ruangan berupa kartu di Peta)'),
  gallery([
    ['01-login.jpg', 'Login'], ['10-jadwal-ruangan.jpg', 'Jadwal dan ruangan'],
    ['08-profil-luring-menunggu.jpg', 'Profil: 1 menunggu'], ['09-dialog-logout.jpg', 'Konfirmasi logout'],
  ], 112),
  caption('Gambar', 'Layar login, jadwal berruangan, status sinkron, dan konfirmasi logout'),
  gallery([
    ['11-dropdown-matkul.jpg', 'Dropdown kurikulum'], ['13-cari-ruangan.jpg', 'Cari ruangan'],
    ['03-beranda.jpg', 'Beranda'], ['14-notifikasi-tugas.jpg', 'Notifikasi pengingat'],
  ], 112),
  caption('Gambar', 'Dropdown mata kuliah, pencarian ruangan, Beranda, dan notifikasi pengingat'),
  p('Dokumentasi teknis tersedia di repository: `docs/FIREBASE.md` (project, aturan, alur sinkron), `docs/TEST_CASE_MINGGU_03.md`, `docs/LOG_BUG_MINGGU_03.md`, serta seluruh screenshot di `output/minggu-03/screenshot/`.'),

  h1('7 Refleksi Akhir'),
  h2('7.1 Evaluasi Hasil Proyek'),
  caption('Tabel', 'Status kebutuhan fungsional dibandingkan Laporan 1'),
  table(['Kode', 'Kebutuhan', 'Status'], [
    ['FR-01', 'Daftar, login, logout', 'Selesai'],
    ['FR-02', 'Kelola mata kuliah dan jadwal', 'Sebagian: tambah/ubah lewat Form Jadwal, hapus jadwal; hapus mata kuliah belum'],
    ['FR-03, FR-04', 'Jadwal hari ini, kelas berikutnya, perubahan jadwal', 'Selesai'],
    ['FR-05, FR-06', 'Tugas dan notifikasi pengingat', 'Selesai'],
    ['FR-07 (Should)', 'Notifikasi sebelum kelas dimulai', 'Belum, ditunda'],
    ['FR-08, FR-09', 'Peta kampus dan cari ruangan', 'Selesai (gedung PPBS A-D, 12 ruangan)'],
    ['FR-10, FR-11', 'Cuaca dan sinkronisasi cloud', 'Selesai'],
  ], [18, 52, 30]),
  p('Sembilan dari sebelas kebutuhan fungsional selesai penuh, FR-02 terpenuhi sebagian karena mata kuliah belum dapat dihapus dari aplikasi, dan FR-07 (Should) ditunda sesuai prioritas Tabel 8 Laporan 1. Hambatan utama minggu ini adalah keterbatasan RAM laptop penguji yang membuat build dan emulator tidak dapat berjalan bersamaan, serta kode ruangan portal yang baru dapat dipetakan setelah dikonfirmasi dari jadwal akademik.', { before: 80 }),
  h2('7.2 Pembagian Peran'),
  caption('Tabel', 'Pembagian tugas minggu ketiga menurut rencana kerja (Laporan 1 Tabel 7)'),
  table(['Anggota', 'Peran', 'Tugas minggu ketiga'], [
    ['Alfarisy Nafaro Gymnastiar', 'Frontend', 'Perbaikan UI, lazy loading, screenshot hasil, slide presentasi'],
    ['Abdul Aziz Rantizi', 'Backend', 'Firebase Auth dan Firestore, sinkronisasi data, keamanan (login, API key), caching'],
    ['Muhammad Fadhli Ramadhan Aulia', 'Dokumentasi dan QA', 'Pengujian akhir, rekap bug dan perbaikan, Laporan 3 dan laporan akhir'],
  ], [32, 18, 50]),
  h2('7.3 Rencana Pengembangan'),
  bullet('FR-02: menu hapus mata kuliah (hapus lunak) beserta jadwal dan tugasnya.'),
  bullet('FR-07: notifikasi beberapa menit sebelum kelas dimulai, memakai mekanisme yang sama dengan pengingat tugas.'),
  bullet('Uji di HP fisik, termasuk penghemat baterai dan izin notifikasi Android 13 ke atas.'),
  bullet('Uji dua perangkat aktif bersamaan untuk memastikan last-write-wins di kondisi nyata.'),
  bullet('Presensi digital (di luar MVP) dan data ruangan untuk gedung selain PPBS.'),
];

L.save(L.document(body, { title: 'Laporan 3 PaduKampus' }), out).then(() => console.log('tersimpan', out));
