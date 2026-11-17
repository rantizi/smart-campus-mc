# Konteks Kelompok
- Mata kuliah / kelas: Mobile Computing / B
- Dosen: Deni Setiana
- Kampus / prodi: Universitas Padjadjaran / S1 Teknik Informatika
- Semester: Ganjil 2026/2027
- Deadline Laporan 1: 17-11-2026
- Tanggal mulai proyek (Minggu 1): 10-11-2026

## Anggota
| No | Nama | NIM | Peran Utama |
|---|---|---|---|
| 1 | Alfarisy Nafaro Gymnastiar | 140810220020 | Frontend (UI/UX & Android UI) |
| 2 | Abdul Aziz Rantizi | 140810230038 | Backend (Database, API & Cloud) |
| 3 | Muhammad Fadhli Ramadhan Aulia | 140810230056 | Dokumentasi & QA (Testing) |

## Pembagian Tugas per Minggu
| Minggu | Alfarisy (Frontend) | Rantizi (Backend) | Fadhli (Dokumentasi & QA) |
|---|---|---|---|
| 1 – Analisis & Desain | Wireframe/mockup, diagram alur navigasi, panduan warna & font | Skema database SQLite (ERD), pemilihan API publik, rancangan integrasi Firebase | Analisis kebutuhan (FR/NFR), use case diagram, latar belakang & tujuan, menyusun Laporan 1 |
| 2 – Implementasi & Integrasi | Layout XML, Fragment, Navigation Component, RecyclerView | Room/SQLite (DAO & query), integrasi API dengan Retrofit, Repository | Fitur notifikasi pengingat tugas, test case black box awal, log bug, menyusun Laporan 2 |
| 3 – Optimasi & Finalisasi | Perbaikan UI, lazy loading, screenshot hasil, slide presentasi | Firebase Auth + Firestore, sinkronisasi data, keamanan (login, API key), caching | Pengujian akhir, rekap bug & perbaikan, Laporan 3 & laporan akhir |
| Semua anggota | Commit rutin ke GitHub, review laporan bersama, latihan demo & presentasi | | |

## Masalah nyata yang kami alami di kampus
1. Info perubahan kelas sering telat sampai
   Kalau dosen berhalangan, kelas diganti jam, atau pindah ruangan, infonya biasanya dikirim
   mendadak lewat grup WA kelas atau ketua kelas. Pesannya sering tenggelam di antara chat lain,
   jadi ada yang baru tahu setelah sampai di kampus atau sudah menunggu di ruangan yang salah.
   → terkait fitur: jadwal + notifikasi perubahan

2. Jadwal kuliah tidak praktis dicek dari HP
   Jadwal resmi ada di portal akademik yang tampilannya kurang nyaman di HP dan kadang lambat,
   apalagi saat masa KRS. Akhirnya banyak mahasiswa cuma screenshot jadwal dan menyimpannya
   di galeri. Kalau ada perubahan, screenshot itu sudah tidak sesuai lagi.
   → terkait fitur: jadwal hari ini di beranda, data tersimpan lokal

3. Deadline tugas tersebar di banyak tempat
   Tugas diumumkan di tempat yang berbeda-beda: LMS kampus, Google Classroom, grup WA, atau
   hanya disebut lisan di kelas. Tidak ada satu tempat untuk melihat semuanya, sehingga deadline
   mudah terlewat, terutama saat beberapa mata kuliah memberi tugas di minggu yang sama.
   → terkait fitur: daftar tugas + pengingat deadline

4. Susah mencari gedung dan ruangan
   Mahasiswa baru, atau mahasiswa yang mengambil kelas di gedung/fakultas lain, sering bingung
   mencari ruangan. Kode ruangan tidak selalu jelas menunjukkan gedung dan lantainya, dan
   aplikasi peta umum biasanya hanya menunjukkan gedung, tidak sampai ruangannya.
   → terkait fitur: peta kampus digital + info lokasi ruangan

5. Waktu pindah antar kelas mepet
   Kadang jeda antar kelas singkat, sementara gedungnya berjauhan. Kalau tidak tahu lokasi
   ruangan berikutnya dari awal, mahasiswa jadi terlambat masuk kelas.
   → terkait fitur: info kelas berikutnya + lokasinya di beranda

6. Hujan datang tiba-tiba saat harus pindah gedung atau pulang
   Di musim hujan, hujan deras sering turun siang atau sore, tepat saat mahasiswa harus
   berjalan ke gedung lain atau pulang naik motor. Banyak yang baru sadar setelah sudah keluar
   gedung dan tidak membawa jas hujan atau payung.
   → terkait fitur: info cuaca di lokasi kampus (API cuaca)

7. Sinyal dan Wi-Fi kampus tidak selalu bisa diandalkan
   Di beberapa ruang kelas sinyalnya lemah, dan Wi-Fi kampus penuh saat jam sibuk. Akibatnya
   mahasiswa tidak bisa membuka portal atau LMS untuk sekadar mengecek jadwal atau tugas.
   → terkait fitur: data tetap bisa dibuka offline (SQLite), sinkron saat online

8. Catatan pribadi hilang atau tidak sinkron antar perangkat
   Banyak mahasiswa mencatat jadwal dan tugas di aplikasi notes HP. Catatan itu bisa hilang
   saat ganti HP atau reset, dan tidak bisa dibuka dari laptop.
   → terkait fitur: login + sinkronisasi cloud (Firebase)

9. (Opsional, pengembangan lanjut) Presensi manual memakan waktu
   Di beberapa kelas presensi masih pakai kertas yang diedarkan atau dipanggil satu per satu.
   Cara ini memakan waktu kuliah dan masih memungkinkan titip absen.
   → terkait fitur: presensi digital (di luar MVP)

## Catatan lain dari dosen
- [isi]