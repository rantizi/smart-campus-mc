// Presentasi akhir PaduKampus (maksimal 10 slide), palet aplikasi dari Laporan 1 Tabel 5.
const path = require('path');
const pptxgen = require('pptxgenjs');
const { applyTheme } = require('/home/fadhli/.claude/skills/synced/a0e91ea0-2064-4d66-bd81-70d16438da5c_f8d2dc7a-b162-4a64-b4f4-8b9af12a6b2f/pptx/scripts/apply_theme.js');

const OUT = process.argv[2] || 'Presentasi_PaduKampus.pptx';
const IMG = (f) => path.join(__dirname, 'img', f);
const THEME = {
  name: 'PaduKampus',
  headFontFace: 'Cambria',
  bodyFontFace: 'Calibri',
  colors: {
    dk1: '1F1F1F', lt1: 'FFFFFF', dk2: '0F3550', lt2: 'F6F8FB',
    accent1: '1A5C8A', accent2: 'F2B705', accent3: '2E7D32', accent4: 'B3261E',
    accent5: '5F6368', accent6: 'DCE8F2', hlink: '1A5C8A', folHlink: '5F6368',
  },
};

const pres = new pptxgen();
pres.layout = 'LAYOUT_16x9'; // 10 x 5.625 in
pres.theme = { headFontFace: THEME.headFontFace, bodyFontFace: THEME.bodyFontFace };
pres.title = 'PaduKampus: Presentasi Akhir';
pres.author = 'Kelompok PaduKampus';
const C = pres.SchemeColor;

pres.defineSlideMaster({
  title: 'JUDUL_GELAP',
  background: { color: C.text2 },
  objects: [
    { placeholder: { options: { name: 'title', type: 'title', x: 0.6, y: 1.25, w: 5.6, h: 1.1,
      fontSize: 44, bold: true, color: C.background1, valign: 'bottom', align: 'left', margin: 0 }, text: '' } },
    { placeholder: { options: { name: 'body', type: 'body', x: 0.6, y: 2.45, w: 5.4, h: 2.4,
      fontSize: 16, color: C.background1, valign: 'top', margin: 0 }, text: '' } },
  ],
});
pres.defineSlideMaster({
  title: 'ISI',
  background: { color: C.background1 },
  objects: [
    { placeholder: { options: { name: 'title', type: 'title', x: 0.5, y: 0.3, w: 9.0, h: 0.75,
      fontSize: 30, bold: true, color: C.text2, valign: 'middle', align: 'left', margin: 0 }, text: '' } },
    { text: { text: 'PaduKampus · Mobile Computing Kelas B', options: { x: 0.5, y: 5.25, w: 5, h: 0.25,
      fontSize: 9, color: C.accent5, margin: 0 } } },
  ],
  slideNumber: { x: 9.1, y: 5.25, w: 0.4, h: 0.25, fontSize: 9, color: C.accent5, align: 'right' },
});

let section = '';
function slide(master, sec) {
  if (sec !== section) { pres.addSection({ title: sec }); section = sec; }
  return pres.addSlide({ masterName: master, sectionTitle: sec });
}
const card = (s, name, x, y, w, h, fill) => s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
  objectName: name, x, y, w, h, rectRadius: 0.08, fill: { color: fill || C.background2 }, line: { color: fill || C.background2 },
});
const num = (s, name, x, y, n, fill) => {
  s.addShape(pres.shapes.OVAL, { objectName: name, x, y, w: 0.42, h: 0.42, fill: { color: fill || C.accent1 }, line: { color: fill || C.accent1 } });
  s.addText(String(n), { objectName: name + ' angka', x, y, w: 0.42, h: 0.42, fontSize: 14, bold: true, color: fill === C.accent2 ? C.text2 : C.background1, align: 'center', valign: 'middle', margin: 0, isTextBox: true });
};
const phone = (s, name, file, x, y, w) => {
  const h = w * (803 / 420);
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, { objectName: name + ' bingkai', x: x - 0.05, y: y - 0.05, w: w + 0.1, h: h + 0.1,
    rectRadius: 0.08, fill: { color: 'FFFFFF' }, line: { color: 'C9D6E2', width: 1 },
    shadow: { type: 'outer', color: '000000', opacity: 0.18, blur: 6, offset: 2, angle: 90 } });
  s.addImage({ path: IMG(file), x, y, w, h, altText: name, objectName: name });
  return h;
};
const stat = (s, name, x, y, w, big, label, color) => {
  s.addText(big, { objectName: name, x, y, w, h: 0.75, fontSize: 44, bold: true, color: color || C.accent1, fontFace: 'Cambria', margin: 0, isTextBox: true });
  s.addText(label, { objectName: name + ' label', x, y: y + 0.75, w, h: 0.5, fontSize: 12, color: C.accent5, margin: 0, valign: 'top', isTextBox: true });
};

// 1. Judul
{
  const s = slide('JUDUL_GELAP', 'Pembuka');
  s.addText('PaduKampus', { placeholder: 'title' });
  s.addText([
    { text: 'Asisten kuliah harian mahasiswa Unpad Jatinangor berbasis Android', options: { fontSize: 18, breakLine: true } },
    { text: ' ', options: { fontSize: 10, breakLine: true } },
    { text: 'Proyek Mini Mobile Computing · Kelas B · Dosen: Deni Setiana', options: { fontSize: 13, color: C.accent6, breakLine: true } },
    { text: ' ', options: { fontSize: 10, breakLine: true } },
    { text: 'Alfarisy Nafaro Gymnastiar (Frontend)', options: { fontSize: 13, breakLine: true } },
    { text: 'Abdul Aziz Rantizi (Backend)', options: { fontSize: 13, breakLine: true } },
    { text: 'Muhammad Fadhli Ramadhan Aulia (Dokumentasi dan QA)', options: { fontSize: 13 } },
  ], { placeholder: 'body' });
  phone(s, 'Screenshot Beranda', '03-beranda.jpg', 7.05, 0.45, 2.3);
  s.addNotes('Assalamualaikum. Kami kelompok PaduKampus. PaduKampus adalah aplikasi Android yang mengumpulkan jadwal, perubahan kelas, tugas, ruangan, dan cuaca kampus Jatinangor dalam satu aplikasi yang tetap bisa dibuka tanpa sinyal. Presentasi ini membahas masalah, desain, implementasi, pengujian, lalu ditutup dengan demo langsung.');
}

// 2. Masalah dan solusi
{
  const s = slide('ISI', 'Masalah');
  s.addText('Masalah nyata yang kami alami di kampus', { placeholder: 'title' });
  const items = [
    ['Perubahan kelas tenggelam di grup WA', 'Catatan pindah ruang, ganti jam, batal per tanggal'],
    ['Portal akademik tidak nyaman di HP', 'Jadwal hari ini dan kelas berikutnya di Beranda'],
    ['Deadline tersebar di LMS, Classroom, WA', 'Satu daftar tugas dengan notifikasi pengingat'],
    ['Kode ruangan tidak jelas gedung dan lantainya', 'Peta kampus dan pencarian ruangan'],
    ['Hujan tiba-tiba saat pindah gedung', 'Cuaca dan peluang hujan per jam'],
    ['Sinyal lemah, catatan hilang saat ganti HP', 'Data luring dan sinkronisasi cloud per akun'],
  ];
  items.forEach(([m, f], i) => {
    const col = i % 3; const row = Math.floor(i / 3);
    const x = 0.5 + col * 3.07; const y = 1.3 + row * 1.9;
    card(s, `Kartu masalah ${i + 1}`, x, y, 2.86, 1.7);
    num(s, `Nomor masalah ${i + 1}`, x + 0.2, y + 0.2, i + 1);
    s.addText(m, { objectName: `Masalah ${i + 1}`, x: x + 0.75, y: y + 0.15, w: 1.95, h: 0.75, fontSize: 14, bold: true, color: C.text1, valign: 'top', margin: 0, isTextBox: true });
    s.addText(f, { objectName: `Solusi ${i + 1}`, x: x + 0.2, y: y + 0.95, w: 2.5, h: 0.65, fontSize: 12, color: C.accent1, valign: 'top', margin: 0, isTextBox: true });
  });
  s.addNotes('Masalah ini kami alami sendiri. Info perubahan kelas sering tenggelam di grup WhatsApp, portal akademik kurang nyaman di HP, tugas tersebar di banyak tempat, kode ruangan tidak menunjukkan gedung dan lantai, hujan turun tiba-tiba, dan sinyal kampus sering lemah. Tiap kartu menunjukkan fitur PaduKampus yang menjawab masalah tersebut.');
}

// 3. Arsitektur
{
  const s = slide('ISI', 'Desain dan implementasi');
  s.addText('Arsitektur MVVM offline-first', { placeholder: 'title' });
  const layers = [
    ['UI', '9 Fragment, XML Material, Navigation + Safe Args', C.accent1],
    ['ViewModel', 'UiState per layar, draf form bertahan saat rotasi', C.accent1],
    ['Repository', 'Satu pintu data: Jadwal, Tugas, Kampus, Cuaca, Auth', C.accent1],
  ];
  layers.forEach(([t, d], i) => {
    const y = 1.25 + i * 0.95;
    card(s, `Lapisan ${t}`, 0.5, y, 5.2, 0.78, C.accent6);
    s.addText(t, { objectName: `Judul lapisan ${t}`, x: 0.7, y, w: 1.4, h: 0.78, fontSize: 16, bold: true, color: C.text2, valign: 'middle', margin: 0, isTextBox: true });
    s.addText(d, { objectName: `Isi lapisan ${t}`, x: 2.1, y, w: 3.5, h: 0.78, fontSize: 12, color: C.text1, valign: 'middle', margin: 0, isTextBox: true });
  });
  const src = [['Room (SQLite)', 'sumber data layar'], ['Open-Meteo', 'cuaca, cache 30 menit'], ['Firestore', 'cadangan per akun']];
  src.forEach(([t, d], i) => {
    const x = 0.5 + i * 1.77;
    card(s, `Sumber ${t}`, x, 4.15, 1.66, 0.85, C.text2);
    s.addText([{ text: t, options: { bold: true, fontSize: 13, breakLine: true } }, { text: d, options: { fontSize: 11 } }],
      { objectName: `Teks sumber ${t}`, x: x + 0.08, y: 4.15, w: 1.5, h: 0.85, color: C.background1, align: 'center', valign: 'middle', margin: 0, isTextBox: true });
  });
  card(s, 'Kartu teknologi', 6.1, 1.25, 3.4, 3.75);
  s.addText('Teknologi', { objectName: 'Judul teknologi', x: 6.35, y: 1.4, w: 3, h: 0.4, fontSize: 16, bold: true, color: C.text2, margin: 0, isTextBox: true });
  s.addText([
    'Kotlin 2.1, Android 8.0 ke atas', 'Room 2.7, Retrofit, Gson', 'Firebase Auth, Firestore', 'WorkManager (pengingat, sinkron)',
    'osmdroid + OpenStreetMap', 'JUnit, Lint, R8, GitHub Actions',
  ].map((t, i, a) => ({ text: t, options: { bullet: true, breakLine: i < a.length - 1 } })),
  { objectName: 'Daftar teknologi', x: 6.35, y: 1.85, w: 3.0, h: 3.0, fontSize: 13, color: C.text1, paraSpaceAfter: 6, valign: 'top', margin: 0, isTextBox: true });
  s.addNotes('Aplikasi memakai satu Activity dan sembilan Fragment. Layar hanya membaca UiState dari ViewModel, ViewModel memanggil repository, dan repository selalu membaca Room. Karena itu aplikasi tetap berjalan penuh saat luring. Jaringan, yaitu Open-Meteo untuk cuaca dan Firestore untuk cloud, hanya memperbarui Room.');
}

// 4. Fitur inti
{
  const s = slide('ISI', 'Desain dan implementasi');
  s.addText('Fitur inti dalam lima menu', { placeholder: 'title' });
  const shots = [
    ['03-beranda.jpg', 'Beranda', 'Cuaca, kelas berikutnya, tugas terdekat'],
    ['10-jadwal-ruangan.jpg', 'Jadwal', 'Ruangan dan perubahan per tanggal'],
    ['05-tugas.jpg', 'Tugas', 'Sumber, deadline, pengingat'],
    ['13-cari-ruangan.jpg', 'Peta', 'Gedung PPBS dan cari ruangan'],
  ];
  shots.forEach(([f, t, d], i) => {
    const x = 0.75 + i * 2.25;
    phone(s, `Screenshot ${t}`, f, x, 1.2, 1.55);
    s.addText([{ text: t, options: { bold: true, fontSize: 14, breakLine: true } }, { text: d, options: { fontSize: 11, color: C.accent5 } }],
      { objectName: `Keterangan ${t}`, x: x - 0.2, y: 4.25, w: 1.95, h: 0.8, color: C.text1, align: 'center', valign: 'top', margin: 0, isTextBox: true });
  });
  s.addNotes('Beranda menampilkan cuaca, kelas berikutnya beserta lokasinya, jadwal hari ini, dan tugas terdekat. Di Jadwal, setiap kelas punya ruangan, dan perubahan seperti pindah ruang, ganti jam, atau batal hanya berlaku pada tanggalnya. Tugas mencatat sumbernya dan mengirim pengingat. Peta menampilkan gedung PPBS dan pencarian ruangan.');
}

// 5. Cloud
{
  const s = slide('ISI', 'Desain dan implementasi');
  s.addText('Akun dan sinkronisasi cloud', { placeholder: 'title' });
  s.addImage({ path: IMG('diagram-sinkron.png'), objectName: 'Diagram sinkronisasi', altText: 'Alur Room, SyncWorker, Firestore', x: 0.5, y: 1.15, w: 5.9, h: 5.9 * 640 / 1600 });
  const pts = [
    ['Login Firebase', 'Email dan kata sandi, validasi sebelum dikirim'],
    ['Offline-first', 'Ubah data saat luring, terkirim otomatis saat online'],
    ['Last-write-wins', 'Versi terbaru menang, hapus lewat penanda is_deleted'],
    ['Logout aman', 'Konfirmasi bila ada perubahan tertunda, data akun dihapus'],
  ];
  pts.forEach(([t, d], i) => {
    const y = 1.15 + i * 0.95;
    num(s, `Poin cloud ${i + 1}`, 6.7, y + 0.05, i + 1, C.accent2);
    s.addText([{ text: t, options: { bold: true, fontSize: 14, breakLine: true } }, { text: d, options: { fontSize: 12, color: C.accent5 } }],
      { objectName: `Teks poin cloud ${i + 1}`, x: 7.25, y, w: 2.3, h: 0.85, color: C.text1, valign: 'top', margin: 0, isTextBox: true });
  });
  card(s, 'Kartu aturan', 0.5, 3.75, 5.9, 1.25, C.accent6);
  s.addText([
    { text: 'Aturan keamanan Firestore', options: { bold: true, fontSize: 13, breakLine: true } },
    { text: 'users/{uid}/... hanya bisa dibaca dan diubah bila request.auth.uid == uid. Diuji: tanpa login ditolak, akun lain ditolak (HTTP 403).', options: { fontSize: 12 } },
  ], { objectName: 'Teks aturan', x: 0.7, y: 3.8, w: 5.5, h: 1.15, color: C.text1, valign: 'middle', margin: 0, isTextBox: true });
  s.addNotes('Untuk cloud kami memakai Firebase Authentication dan Cloud Firestore di Jakarta. Setiap perubahan di Room memicu SyncWorker yang hanya jalan saat ada koneksi. Sinkron mengambil dulu dokumen cloud yang lebih baru, lalu mengirim perubahan lokal. Aturan Firestore memastikan tiap akun hanya bisa membaca datanya sendiri, dan ini sudah kami uji.');
}

// 6. Data kampus nyata
{
  const s = slide('ISI', 'Desain dan implementasi');
  s.addText('Data kampus nyata, bukan contoh', { placeholder: 'title' });
  stat(s, 'Jumlah mata kuliah', 0.5, 1.2, 2.4, '69', 'mata kuliah kurikulum S-1 TI (45 wajib, 24 peminatan)');
  stat(s, 'Jumlah ruangan', 3.0, 1.2, 2.4, '12', 'ruangan PPBS A, B, C dari portal akademik');
  card(s, 'Kartu kode ruangan', 0.5, 2.75, 4.9, 2.25);
  s.addText('Membaca kode ruangan portal', { objectName: 'Judul kode', x: 0.7, y: 2.85, w: 4.5, h: 0.4, fontSize: 15, bold: true, color: C.text2, margin: 0, isTextBox: true });
  s.addText([
    { text: '0201 - ', options: { color: C.accent5 } }, { text: '0724', options: { color: C.accent1, bold: true } },
    { text: ' - ', options: { color: C.accent5 } }, { text: '0205', options: { color: C.accent4, bold: true } },
  ], { objectName: 'Contoh kode', x: 0.7, y: 3.3, w: 4.5, h: 0.6, fontSize: 26, fontFace: 'Courier New', margin: 0, isTextBox: true });
  s.addText([
    { text: '0724 = PPBS A, 0726 = PPBS B, 0720 = PPBS C', options: { breakLine: true } },
    { text: '0205 = ruang 205, digit pertama = lantai 2', options: { breakLine: true } },
    { text: 'Mobile Computing: PPBS A Ruang 205 · Lantai 2', options: { bold: true } },
  ], { objectName: 'Arti kode', x: 0.7, y: 3.95, w: 4.6, h: 1.0, fontSize: 12, color: C.text1, valign: 'top', margin: 0, isTextBox: true });
  phone(s, 'Screenshot dropdown kurikulum', '11-dropdown-matkul.jpg', 5.95, 1.15, 1.6);
  phone(s, 'Screenshot cari ruangan', '13-cari-ruangan.jpg', 7.85, 1.15, 1.6);
  s.addNotes('Semua data kampus nyata. 69 mata kuliah diambil dari Buku Pedoman Prodi S-1 TI dan muncul sebagai dropdown yang otomatis mengisi sandi dan SKS. Ruangan diambil dari portal akademik. Kodenya kami uraikan: bagian tengah adalah gedung dan bagian akhir adalah nomor ruang, digit pertamanya lantai.');
}

// 7. Pengujian
{
  const s = slide('ISI', 'Pengujian');
  s.addText('Pengujian: semua skenario lulus', { placeholder: 'title' });
  stat(s, 'Skenario black box', 0.5, 1.2, 2.8, '30/30', 'skenario black box di emulator Android 11', C.accent3);
  stat(s, 'Unit test', 3.5, 1.2, 2.8, '54', 'unit test JVM lulus, otomatis di CI', C.accent3);
  stat(s, 'Bug diperbaiki', 6.5, 1.2, 3.0, '3', 'bug ditemukan, diperbaiki, diuji ulang', C.accent4);
  const bugs = [
    ['B-01', 'Cuaca baru dianggap basi', 'Umur cache dihitung dengan jam terkini'],
    ['B-02', 'Data akun kadang tidak ditarik setelah login', 'Login dijalankan di scope aplikasi'],
    ['B-03', 'Label dropdown bertumpuk', 'Gaya ExposedDropdownMenu'],
  ];
  s.addTable([
    [{ text: 'Bug', options: { bold: true } }, { text: 'Gejala', options: { bold: true } }, { text: 'Perbaikan', options: { bold: true } }],
    ...bugs,
  ], { objectName: 'Tabel bug', x: 0.5, y: 2.85, w: 9.0, colW: [0.9, 4.0, 4.1], fontSize: 12, color: '1F1F1F', fontFace: 'Calibri',
    fill: { color: 'FFFFFF' }, border: { type: 'solid', pt: 0.75, color: 'C9D6E2' }, rowH: 0.42, valign: 'middle' });
  s.addText('Belum diuji: HP fisik dan dua perangkat aktif bersamaan.', { objectName: 'Catatan uji', x: 0.5, y: 4.65, w: 9, h: 0.35, fontSize: 12, italic: true, color: C.accent5, margin: 0, isTextBox: true });
  s.addNotes('Kami menguji 30 skenario black box di emulator, termasuk mode pesawat, isolasi akun, notifikasi, dan APK release. Semua lulus. Ada 54 unit test yang juga dijalankan otomatis di GitHub Actions. Tiga bug ditemukan dan diperbaiki, yang paling penting B-02: setelah login data kadang tidak ditarik karena coroutine ikut batal saat layar Login ditutup.');
}

// 8. Optimasi dan keamanan
{
  const s = slide('ISI', 'Pengujian');
  s.addText('Optimasi dan keamanan', { placeholder: 'title' });
  s.addChart(pres.charts.BAR, [{ name: 'Ukuran APK (MB)', labels: ['Debug', 'Release R8'], values: [14.4, 3.6] }], {
    objectName: 'Grafik ukuran APK', x: 0.5, y: 1.15, w: 4.3, h: 3.85, barDir: 'col', chartColors: ['1A5C8A'],
    showTitle: true, title: 'Ukuran APK turun 75% (MB)', titleFontSize: 13, titleColor: '0F3550', titleFontFace: '+mj-lt',
    showValue: true, dataLabelPosition: 'outEnd', dataLabelFormatCode: '0.0', dataLabelFontSize: 12, dataLabelColor: '1F1F1F', dataLabelFontFace: '+mn-lt',
    catAxisLabelColor: '5F6368', catAxisLabelFontSize: 12, catAxisLabelFontFace: '+mn-lt', valAxisHidden: true,
    valGridLine: { style: 'none' }, catGridLine: { style: 'none' }, showLegend: false, barGapWidthPct: 80,
  });
  const rows = [
    ['Cache dan luring', 'Room sebagai cache, cuaca 30 menit, ambil yang lebih baru saja'],
    ['Lazy loading', 'Komponen dibuat saat dipakai, RecyclerView + DiffUtil'],
    ['Akses data', 'Aturan Firestore per UID, hapus permanen ditolak'],
    ['Validasi input', 'Email, sandi minimal 6, jam, deadline'],
    ['Data dan koneksi', 'HTTPS saja, tanpa API key di kode, data dihapus saat logout'],
  ];
  rows.forEach(([t, d], i) => {
    const y = 1.15 + i * 0.78;
    card(s, `Kartu ${t}`, 5.2, y, 4.3, 0.68, i < 2 ? C.accent6 : C.background2);
    s.addText([{ text: t, options: { bold: true, fontSize: 13, breakLine: true } }, { text: d, options: { fontSize: 11, color: C.accent5 } }],
      { objectName: `Teks ${t}`, x: 5.35, y, w: 4.05, h: 0.68, color: C.text1, valign: 'middle', margin: 0, isTextBox: true });
  });
  s.addNotes('Untuk optimasi, Room menjadi cache semua data, cuaca tidak diminta ulang selama 30 menit, dan sinkron hanya mengambil data yang lebih baru. Build release memakai R8 sehingga APK turun dari 14,4 MB menjadi 3,6 MB. Untuk keamanan: aturan Firestore per akun, validasi input, hanya HTTPS, tanpa API key di kode, dan data dihapus dari HP saat logout.');
}

// 9. Capaian
{
  const s = slide('ISI', 'Penutup');
  s.addText('Capaian kebutuhan fungsional', { placeholder: 'title' });
  const fr = [
    ['FR-01', 'Akun', 'ok'], ['FR-02', 'Kelola mata kuliah', 'part'], ['FR-03', 'Jadwal hari ini', 'ok'], ['FR-04', 'Perubahan jadwal', 'ok'],
    ['FR-05', 'Tugas', 'ok'], ['FR-06', 'Pengingat tugas', 'ok'], ['FR-07', 'Pengingat kelas', 'no'], ['FR-08', 'Peta kampus', 'ok'],
    ['FR-09', 'Cari ruangan', 'ok'], ['FR-10', 'Cuaca', 'ok'], ['FR-11', 'Sinkron cloud', 'ok'],
  ];
  const color = { ok: C.accent3, part: C.accent2, no: C.accent5 };
  const label = { ok: 'Terpenuhi', part: 'Sebagian', no: 'Ditunda' };
  fr.forEach(([k, t, st], i) => {
    const col = i % 4; const row = Math.floor(i / 4);
    const x = 0.5 + col * 2.27; const y = 1.2 + row * 1.0;
    card(s, `Kartu ${k}`, x, y, 2.1, 0.85);
    s.addShape(pres.shapes.OVAL, { objectName: `Status ${k}`, x: x + 0.15, y: y + 0.27, w: 0.3, h: 0.3, fill: { color: color[st] }, line: { color: color[st] } });
    s.addText([{ text: `${k} ${t}`, options: { bold: true, fontSize: 12, breakLine: true } }, { text: label[st], options: { fontSize: 11, color: C.accent5 } }],
      { objectName: `Teks ${k}`, x: x + 0.55, y, w: 1.5, h: 0.85, color: C.text1, valign: 'middle', margin: 0, isTextBox: true });
  });
  s.addText('9 terpenuhi penuh · FR-02 belum bisa menghapus mata kuliah · FR-07 (Should) ditunda', {
    objectName: 'Ringkasan capaian', x: 0.5, y: 4.25, w: 9, h: 0.35, fontSize: 13, bold: true, color: C.text2, margin: 0, isTextBox: true });
  s.addText('Kinerja di emulator (Celeron, grafis perangkat lunak): buka 3,5 detik, memori 85 MB; perlu diukur ulang di HP fisik.', {
    objectName: 'Catatan kinerja', x: 0.5, y: 4.62, w: 9, h: 0.35, fontSize: 12, italic: true, color: C.accent5, margin: 0, isTextBox: true });
  s.addNotes('Dari sebelas kebutuhan fungsional, sembilan terpenuhi penuh. FR-02 sebagian karena mata kuliah belum bisa dihapus dari aplikasi, dan FR-07, pengingat sebelum kelas, ditunda karena prioritasnya Should. Waktu buka di emulator 3,5 detik, di atas target 2 detik, tetapi emulator kami berjalan di laptop Celeron dengan grafis perangkat lunak, jadi perlu diukur ulang di HP.');
}

// 10. Penutup
{
  const s = slide('JUDUL_GELAP', 'Penutup');
  s.addText('Kesimpulan', { placeholder: 'title' });
  s.addText([
    { text: 'Aplikasi lengkap: UI, SQLite, API cuaca, Firebase, pengujian, optimasi', options: { bullet: true, breakLine: true } },
    { text: 'Data tetap ada saat luring dan pulih di HP lain setelah login', options: { bullet: true, breakLine: true } },
    { text: 'Rencana: hapus mata kuliah, pengingat kelas, uji HP fisik, presensi', options: { bullet: true, breakLine: true } },
    { text: ' ', options: { fontSize: 8, breakLine: true } },
    { text: 'Demo langsung: login, jadwal, perubahan kelas, tugas dan pengingat, peta, mode pesawat', options: { fontSize: 14, color: C.accent2, bold: true } },
  ], { placeholder: 'body' });
  phone(s, 'Screenshot Profil sinkron', '07-profil-sinkron.jpg', 7.05, 0.45, 2.3);
  s.addNotes('Kesimpulannya, PaduKampus memenuhi semua komponen teknis tugas: UI dan navigasi, SQLite, API eksternal, Firebase, pengujian, serta optimasi dan keamanan. Selanjutnya kami demokan langsung. Urutan demo ada di docs/NASKAH_DEMO.md. Terima kasih.');
}

(async () => {
  await pres.writeFile({ fileName: OUT });
  await applyTheme(OUT, THEME);
  console.log('tersimpan', OUT);
})();
