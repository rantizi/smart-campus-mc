# Naskah presentasi dan demo PaduKampus

Durasi total sekitar 15 menit: presentasi 10 slide (sekitar 9 menit) lalu demo langsung (sekitar 6 menit). Catatan pembicara per slide ada di `output/minggu-03/presentasi/Presentasi_PaduKampus.pptx` (menu View, Notes).

## Pembagian bicara (usulan, sesuaikan dengan tim)

| Bagian | Slide | Pembicara | Waktu |
|---|---|---|---|
| Pembuka dan masalah | 1-2 | Fadhli | 2 menit |
| Arsitektur dan fitur | 3-4 | Alfarisy | 2 menit |
| Cloud dan data kampus | 5-6 | Rantizi | 2 menit |
| Pengujian, optimasi, capaian | 7-9 | Fadhli | 2,5 menit |
| Kesimpulan, demo | 10 | Semua | 6,5 menit |

## Persiapan sebelum demo (H-1 dan 15 menit sebelum)

1. Pasang APK terbaru di HP demo (dari zip tim atau artefak CI dengan Firebase), login sekali dengan akun demo.
2. Pastikan HP online, notifikasi aplikasi diizinkan, mode Jangan Ganggu mati, kecerahan layar penuh.
3. Siapkan screen mirroring HP ke proyektor (misalnya `scrcpy` di laptop atau fitur cast HP). Uji sebelum giliran.
4. Buat satu tugas cadangan dengan deadline 8 menit setelah demo dimulai dan pengingat 5 menit, supaya notifikasi muncul saat demo.
5. Cadangan bila HP atau jaringan bermasalah: buka screenshot di `output/minggu-03/screenshot/` atau emulator.

## Alur demo (sekitar 6 menit)

| Langkah | Tindakan | Yang ditunjukkan |
|---|---|---|
| 1 | Profil, Keluar, lalu Login dengan akun demo | Validasi email salah, login berhasil, data tertarik dari cloud (FR-01, FR-11) |
| 2 | Beranda | Cuaca dan peluang hujan, kelas berikutnya dengan ruangan, tugas terdekat (FR-03, FR-10) |
| 3 | Jadwal, pilih Selasa | Jadwal asli semester ini dengan ruangan PPBS dan lantainya |
| 4 | Menu titik tiga, Catat perubahan, Pindah ruangan | Kartu berubah hanya untuk tanggal itu (FR-04); hapus perubahan lagi |
| 5 | Jadwal, tombol +, ketik "Data" | Dropdown 69 mata kuliah kurikulum, sandi dan SKS terisi otomatis (FR-02); Batal |
| 6 | Tugas, tambah tugas | Sumber tugas, deadline, menit pengingat (FR-05) |
| 7 | Tunggu notifikasi tugas cadangan | Notifikasi pengingat, ketuk membuka Form Tugas (FR-06) |
| 8 | Peta, cari "205" | Dua hasil, peta berpusat ke gedung, kartu ruangan (FR-08, FR-09) |
| 9 | Nyalakan mode pesawat, tandai tugas selesai, buka Profil | "1 menunggu"; data tetap bisa dipakai luring (NFR-06) |
| 10 | Matikan mode pesawat, Sinkronkan sekarang | "0 menunggu"; perubahan tersimpan di cloud |

## Pertanyaan yang mungkin muncul

| Pertanyaan | Jawaban singkat |
|---|---|
| Kenapa Room dan Firestore sekaligus? | Room membuat aplikasi tetap jalan tanpa sinyal (NFR-06); Firestore menjaga data tidak hilang saat ganti HP. Room menjadi sumber tampilan, Firestore cadangan. |
| Bagaimana jika data diubah di dua HP? | Last-write-wins berdasarkan `updated_at`; versi yang lebih baru menang. Sudah diuji lewat unit test dan uji hapus dari cloud, belum dengan dua HP sungguhan. |
| Apakah data pengguna lain bisa dibaca? | Tidak. Aturan Firestore hanya mengizinkan `request.auth.uid == uid`; diuji, akses lintas akun ditolak (HTTP 403). |
| Kenapa waktu buka 3,5 detik? | Itu di emulator pada laptop Celeron dengan grafis perangkat lunak; perlu diukur di HP fisik. |
| Apa yang belum selesai? | Hapus mata kuliah (FR-02 sebagian) dan pengingat sebelum kelas (FR-07, Should). |
