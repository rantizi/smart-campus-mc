"""Build the week 2 report from the week 1 document's page/style system."""
from pathlib import Path
from copy import deepcopy
import hashlib
import json
from zipfile import ZipFile
from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT/'output/minggu-02'
QA = OUT/'qa'
REFERENCE = ROOT.parent/'Laporan1_SmartCampus.docx'
FINAL = OUT/'laporan/Laporan2_SmartCampus.docx'
QA.mkdir(parents=True,exist_ok=True)
FINAL.parent.mkdir(parents=True,exist_ok=True)
baseline = hashlib.sha256(REFERENCE.read_bytes()).hexdigest()
doc = Document(REFERENCE)
# Preserve source styles, section geometry and footer; replace the week 1 content.
for child in list(doc._element.body):
    if child.tag != qn('w:sectPr'): doc._element.body.remove(child)
for name in ['Normal','Title','Subtitle','Heading 1','Heading 2','Heading 3']:
    style=doc.styles[name]
    style.font.name='Times New Roman'
    style.font.color.rgb=RGBColor(0,0,0)
    if style.element.rPr is not None:
        color=style.element.rPr.find(qn('w:color'))
        if color is not None:
            for key in ['themeColor','themeShade','themeTint']: color.attrib.pop(qn('w:'+key),None)
doc.styles['Title'].font.size=Pt(14)
doc.styles['Title'].font.bold=True
doc.styles['Title'].paragraph_format.space_after=Pt(5)
title_style=doc.styles['Title'].element
for node in list(title_style.iter(qn('w:pBdr'))): node.getparent().remove(node)
for fonts in title_style.iter(qn('w:rFonts')):
    for key in ['asciiTheme','eastAsiaTheme','hAnsiTheme','cstheme']: fonts.attrib.pop(qn('w:'+key),None)
for name in ['TitleChar']:
    if name in doc.styles:
        for node in list(doc.styles[name].element.iter(qn('w:pBdr'))): node.getparent().remove(node)
doc.core_properties.title='Laporan Minggu 2 PaduKampus Implementasi Fitur dan Integrasi API'
doc.core_properties.subject='Mobile Computing kelas B'
doc.core_properties.author='Kelompok PaduKampus'
doc.core_properties.comments=''

def font(run,size=None,bold=False):
    run.font.name='Times New Roman'; run.bold=bold
    run.font.color.rgb=RGBColor(0,0,0)
    if size: run.font.size=Pt(size)
    return run
def p(text='',size=12,bold=False,center=False,after=4):
    x=doc.add_paragraph()
    x.alignment=WD_ALIGN_PARAGRAPH.CENTER if center else WD_ALIGN_PARAGRAPH.JUSTIFY
    x.paragraph_format.space_after=Pt(after)
    font(x.add_run(text),size,bold)
    return x
def h(text,level=1):
    x=doc.add_heading(text,level=level)
    x.paragraph_format.keep_with_next=True
    return x
def page(): doc.add_page_break()
def code(text,size=9):
    x=doc.add_paragraph()
    x.paragraph_format.line_spacing=1
    x.paragraph_format.space_after=Pt(6)
    x.paragraph_format.keep_together=True
    r=x.add_run(text)
    r.font.name='Consolas'; r.font.size=Pt(size); r.font.color.rgb=RGBColor(0,0,0)
    return x
def caption(text):
    x=p(text,10,center=True,after=5)
    x.paragraph_format.line_spacing=1
    return x
def table(headers,rows,widths,size=9.5):
    t=doc.add_table(rows=1,cols=len(headers))
    t.style='Table Grid'; t.alignment=WD_TABLE_ALIGNMENT.CENTER; t.autofit=False
    for i,w in enumerate(widths): t.columns[i].width=Cm(w)
    prop=t._tbl.tblPr
    borders=OxmlElement('w:tblBorders')
    for edge in ['top','left','bottom','right','insideH','insideV']:
        node=OxmlElement('w:'+edge); node.set(qn('w:val'),'single'); node.set(qn('w:sz'),'4'); node.set(qn('w:color'),'D9D9D9'); borders.append(node)
    prop.append(borders)
    margins=OxmlElement('w:tblCellMar')
    for edge,value in [('top','65'),('bottom','65'),('left','80'),('right','80')]:
        node=OxmlElement('w:'+edge); node.set(qn('w:w'),value); node.set(qn('w:type'),'dxa'); margins.append(node)
    prop.append(margins)
    repeat=OxmlElement('w:tblHeader'); t.rows[0]._tr.get_or_add_trPr().append(repeat)
    for rindex,values in enumerate([headers]+rows):
        row=t.rows[0] if rindex==0 else t.add_row()
        row._tr.get_or_add_trPr().append(OxmlElement('w:cantSplit'))
        for i,value in enumerate(values):
            c=row.cells[i]; c.width=Cm(widths[i]); c.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
            cp=c.paragraphs[0]; cp.paragraph_format.space_after=Pt(0); cp.paragraph_format.line_spacing=1
            if headers[i] in ['NIM','Jumlah','Hasil']: cp.alignment=WD_ALIGN_PARAGRAPH.CENTER
            font(cp.add_run(str(value)),size,rindex==0)
            if rindex==0:
                shading=OxmlElement('w:shd'); shading.set(qn('w:fill'),'E6E6E6'); c._tc.get_or_add_tcPr().append(shading)
    doc.add_paragraph().paragraph_format.space_after=Pt(0)
    return t

# Distill the reference as a task-local execution contract.
with ZipFile(REFERENCE) as z:
    inventory={name:{'size':len(z.read(name)),'sha256':hashlib.sha256(z.read(name)).hexdigest()} for name in z.namelist()}
(QA/'reference-parts.json').write_text(json.dumps(inventory,indent=2),encoding='utf-8')
(QA/'artifact.md').write_text(f'''# Contract for week 2 report

Reference: `{REFERENCE}`. SHA256 `{baseline}`. Six rendered pages; one A4 portrait section.
Page system: 21 x 29.7 cm, margins 2.54 cm on all sides; source footer retained with centered PAGE field.
Typography: Times New Roman 12 pt body, 1.15 line spacing, 3 pt default after; black 12 pt bold Heading 1/2.
Source table pattern: repeated gray header, compact Times New Roman table text, variable row heights, explicit column widths.
Source figure pattern: centered image and 10 pt centered caption.
User change: week 2 screenshot controls the seven numbered section topics; the week 1 content is replaced in full.
Editable slots: word/document.xml body before sectPr; metadata; Title role; new figures; direct table borders/padding.
Preserve: section geometry, source footer, source numbering, other styles except Title role and explicit black heading colors.
Intentional adaptations: eight pages for technical detail; table text 9.5 pt and light gray borders for readability; formal title in Title style.
No evidence slot may present wireframes as screenshots or unexecuted tests as passing tests.
Full baseline package inventory: reference-parts.json. Rendered reference: reference-render/page-1.png to page-6.png.
''',encoding='utf-8')

# Page 1: identity and progress.
x=doc.add_paragraph('MINGGU 2 IMPLEMENTASI DAN INTEGRASI',style='Title'); x.alignment=WD_ALIGN_PARAGRAPH.CENTER
p('Implementasi Fitur dan Integrasi API',12,bold=True,center=True)
p('PaduKampus Aplikasi Asisten Kuliah Harian Mahasiswa Berbasis Android',11,center=True)
p('Mobile Computing Kelas B | Deni Setiana\nS1 Teknik Informatika Universitas Padjadjaran\nSemester Ganjil 2026/2027',10.5,center=True)
p('Periode minggu 2 17–23 November 2026 | Pembaruan 19 November 2026',10,center=True,after=8)
table(['Nama','NIM','Peran utama'],[
    ['Alfarisy Nafaro Gymnastiar','140810220020','Frontend dan UI/UX'],
    ['Abdul Aziz Rantizi','140810230038','Backend, database dan API'],
    ['Muhammad Fadhli Ramadhan Aulia','140810230056','Dokumentasi dan QA']], [6.6,3,6.32],9.5)
h('1 Ringkasan Kemajuan')
p('Minggu pertama menghasilkan analisis kebutuhan, wireframe, alur navigasi, dan ERD PaduKampus. Pada awal minggu kedua, hasil desain tersebut diterjemahkan menjadi kerangka proyek Android agar pengembangan antarmuka, data lokal, API, dan pengingat tugas dapat dimulai dengan struktur yang sama.')
p('Capaian saat pembaruan ini adalah tersedianya kode kerangka dan validasi awal XML serta SQL. Build APK, pengujian perangkat, dan integrasi HTTP langsung belum dijalankan. Karena itu, fitur pada Tabel 1 belum dinyatakan selesai atau lulus uji Android.',size=11)
caption('Tabel 1 Perbandingan target minggu pertama dan kemajuan minggu kedua')
table(['Target','Kemajuan yang tersedia','Pekerjaan berikutnya'],[
 ['Navigasi dan UI','MainActivity, lima menu utama, dan Form Tugas','Build, uji Back, dan rapikan UI'],
 ['Jadwal FR-02/03/04','Entitas, DAO, pembacaan jadwal reguler hari ini','Form CRUD, JOIN nama, dan resolver perubahan'],
 ['Tugas FR-05','Kode tambah, ubah, selesai, dan soft delete','Uji persistensi dan lengkapi sumber tugas'],
 ['Pengingat FR-06','Scheduler, worker, izin dan deep link tugas','Uji notifikasi dan perubahan deadline'],
 ['Peta FR-08','Destination dan layar awal','Library peta, seed gedung dan penanda'],
 ['Cuaca FR-10','Retrofit, DTO, cache dan tampilan per jam','Koordinat terverifikasi dan uji HTTP'],
 ['Cloud FR-01/11','Metadata sinkronisasi disiapkan di entitas','Login dan sinkronisasi pada minggu 3']
], [3.1,6.4,6.42],9.3)

# Page 2: actual tree and technology selection.
page(); h('2 Struktur Proyek di Android Studio')
p('Proyek menggunakan package id.ac.unpad.padukampus. Satu modul app memisahkan antarmuka, data, aturan waktu pengingat, dan pekerjaan latar belakang. Struktur ini melanjutkan pilihan Kotlin dan XML pada laporan minggu pertama.')
code('''smart-campus-mc/
  settings.gradle.kts, build.gradle.kts, gradlew.bat
  app/build.gradle.kts
  app/src/main/
    AndroidManifest.xml
    java/id/ac/unpad/padukampus/
      PaduKampusApp.kt
      data/local/       Entities, CampusDao, CampusDatabase
      data/remote/      WeatherApi dan DTO
      data/repository/  TaskRepository, WeatherRepository
      domain/           ReminderPolicy
      ui/               MainActivity
        beranda/, jadwal/, tugas/, peta/, profil/
      worker/           ReminderScheduler, TaskReminderWorker
    res/layout/, res/menu/, res/navigation/, res/values/
  app/src/test/         ReminderPolicyTest
  docs/                Rencana kerja dan test case''',9.5)
caption('Tabel 2 Teknologi dan tanggung jawab komponen')
table(['Komponen','Teknologi','Fungsi dalam kerangka'],[
 ['Antarmuka','Kotlin, XML, Material Components','Layout, form, serta bottom navigation'],
 ['Perpindahan halaman','Navigation Component','NavHostFragment, graph, dan taskId'],
 ['Daftar tugas','RecyclerView dan DiffUtil','Pembaruan item berdasarkan UUID dan isi'],
 ['Status tampilan','ViewModel dan Flow','Daftar tugas mengamati perubahan Room'],
 ['Penyimpanan','Room di atas SQLite','Tujuh entitas, DAO, dan transaksi tugas'],
 ['Jaringan','Retrofit dan Gson','GET Open-Meteo dan pemetaan JSON'],
 ['Pengingat','WorkManager','Unique work per UUID tugas']
], [3.3,4.5,8.12],10)
p('PaduKampusApp menyediakan database dan repository melalui properti lazy. Alur daftar tugas mengikuti Fragment → ViewModel → Repository → DAO. Pada kerangka awal, logika Form Tugas dan cuaca masih berada di Fragment dan perlu dipindahkan ke ViewModel masing-masing saat pengembangan dilanjutkan.',size=11)
p('Konfigurasi baseline: AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20, KSP 2.1.20-1.0.32, minSdk 26, compileSdk dan targetSdk 35, serta Gradle JDK 17. Versi dipatok untuk baseline proyek, bukan sebagai klaim versi terbaru. Gradle Wrapper resmi telah disertakan dan checksum JAR diverifikasi. [R1]',size=10.5)

# Page 3: real navigation versus reference wireframes.
page(); h('3 Implementasi Navigasi dan Fragment')
h('3.1 Logika perpindahan antarhalaman',2)
p('MainActivity memasang NavHostFragment dan menghubungkan BottomNavigationView melalui setupWithNavController. Graph awal langsung membuka Beranda karena login Firebase dijadwalkan pada minggu ketiga. Lima ID menu cocok dengan destination di nav_graph.xml. [R2]',size=11)
table(['Destination','Aksi dan perilaku dalam kode'],[
 ['Beranda','Menampilkan jumlah tugas aktif dan tombol pembaruan cuaca'],
 ['Jadwal','Membaca jadwal reguler hari ini; form dan override belum tersedia'],
 ['Tugas','Menampilkan RecyclerView; tambah membuka Form Tugas'],
 ['Form Tugas','taskId kosong untuk tambah; taskId terisi untuk ubah; Simpan/Batal kembali'],
 ['Peta','Layar awal; penanda gedung belum diimplementasikan'],
 ['Profil','Menyimpan koordinat untuk permintaan cuaca, belum login akun']
],[3.2,12.72],10)
p('Ketukan judul tugas mengirim UUID melalui Bundle. Worker menyiapkan deep link menuju Form Tugas dengan UUID yang sama. Safe Args pada rancangan minggu pertama belum dipasang; jika ditambahkan, kontrak taskId tetap dipertahankan. Perilaku Back dan deep link masih menunggu pengujian perangkat.',size=11)
h('3.2 Bukti tampilan dan acuan antarmuka',2)
p('Bukti implementasi saat ini berupa file XML, kelas Fragment, dan graph navigasi. Screenshot emulator/perangkat belum tersedia. Gambar 1 mempertahankan wireframe minggu pertama sebagai acuan UI; gambar ini tidak menunjukkan aplikasi yang telah dijalankan.',size=11)
x=doc.add_paragraph(); x.alignment=WD_ALIGN_PARAGRAPH.CENTER
for filename in ['wf-04-tugas.png','wf-05-form-tugas.png']:
    r=x.add_run(); r.add_picture(str(ROOT/'output/minggu-01/wireframe'/filename),width=Cm(4.0))
    x.add_run('   ')
x.paragraph_format.keep_with_next=True
caption('Gambar 1 Wireframe Tugas dan Form Tugas dari minggu pertama')

# Page 4: API contract, honest example.
page(); h('4 Integrasi API Eksternal')
h('4.1 API dan cara pengambilan data',2)
p('Layanan yang dipakai adalah Open-Meteo Forecast API. WeatherApi mendefinisikan GET v1/forecast pada base URL https://api.open-meteo.com/. Latitude dan longitude dibaca dari pengaturan Profil, tanpa meminta lokasi perangkat. Koordinat kampus belum ditetapkan sebagai data awal. [R3]',size=11)
code('''GET /v1/forecast
  ?latitude={lat}&longitude={lon}
  &current=temperature_2m,weather_code
  &hourly=precipitation_probability
  &timezone=Asia/Jakarta&forecast_days=1''',9)
p('WeatherRepository mengubah respons menjadi DTO, memeriksa kesamaan panjang array waktu dan probabilitas, lalu menyimpan hasil ke cache_cuaca. Cache berlaku 30 menit. Jika permintaan gagal, data terakhir untuk koordinat yang sama dipakai dengan penanda pembaruan gagal; tanpa cache, Beranda menampilkan pesan galat.',size=11)
h('4.2 Struktur respons JSON dan pemetaan',2)
p('Payload berikut adalah contoh ilustratif untuk menjelaskan kontrak data. Nilainya bukan hasil pengukuran atau respons HTTP yang sudah diuji.',size=10.5)
code('''{
  "timezone": "Asia/Jakarta",
  "current": {"temperature_2m": 25.5, "weather_code": 3},
  "hourly": {
    "time": ["2026-11-19T13:00", "2026-11-19T14:00",
             "2026-11-19T15:00"],
    "precipitation_probability": [40, 65, 70]
  }
}''',9)
table(['Field JSON','Tujuan lokal atau tampilan'],[
 ['current.temperature_2m','cache_cuaca.suhu; ditampilkan dalam °C'],
 ['current.weather_code','cache_cuaca.kode_cuaca; awalnya ditampilkan sebagai kode numerik'],
 ['hourly.time dan hourly.precipitation_probability','Disimpan bersama sebagai JSON dalam hujan_per_jam'],
 ['Waktu pengambilan lokal','diambil_pada berupa epoch milidetik untuk menghitung umur cache']
],[5.4,10.52],10)
h('4.3 Hasil integrasi dan penyajian data',2)
p('Kode Beranda disiapkan untuk menampilkan suhu, kode cuaca, dan tiga baris peluang hujan mulai jam berjalan dalam WIB. Status cache lama ditampilkan saat jaringan gagal. Respons asli, keberhasilan HTTP, dan tampilan pada perangkat belum diverifikasi; validasi tersebut menjadi syarat penutupan FR-10.',size=11)

# Page 5: database schema.
page(); h('5 Implementasi Database Lokal SQLite')
h('5.1 Skema tabel yang digunakan',2)
p('Room menampung tujuh entitas sesuai ERD minggu pertama. CampusDatabase menggunakan versi skema 1 dan exportSchema. File skema resmi Room baru akan dihasilkan ketika KSP dijalankan saat build. Tabel 3 merangkum kolom yang telah ditulis pada Entities.kt. [R4]',size=11)
caption('Tabel 3 Pemetaan entitas ke skema SQLite')
table(['Tabel','Kolom utama dan tipe data'],[
 ['gedung','id TEXT PK; nama, kode TEXT; latitude, longitude REAL'],
 ['ruangan','id TEXT PK; gedung_id TEXT FK; kode, nama TEXT; lantai INTEGER'],
 ['mata_kuliah','id TEXT PK; nama, kode, dosen TEXT; sks INTEGER; metadata sinkronisasi'],
 ['jadwal','id TEXT PK; mata_kuliah_id, ruangan_id TEXT FK; hari INTEGER; jam_mulai, jam_selesai TEXT; pengingat_menit INTEGER; metadata sinkronisasi'],
 ['perubahan_jadwal','id TEXT PK; jadwal_id, ruangan_baru_id TEXT FK; tanggal, jenis TEXT; jam_mulai_baru, jam_selesai_baru TEXT; catatan TEXT; metadata sinkronisasi'],
 ['tugas','id TEXT PK; mata_kuliah_id TEXT FK; judul, deskripsi TEXT; deadline INTEGER; sumber TEXT; selesai, pengingat_menit INTEGER; metadata sinkronisasi'],
 ['cache_cuaca','id TEXT PK berupa pasangan koordinat; suhu REAL; kode_cuaca INTEGER; hujan_per_jam TEXT JSON; diambil_pada INTEGER']
],[3.5,12.42],10)
p('Metadata sinkronisasi terdiri atas updated_at INTEGER, sync_status TEXT, dan is_deleted INTEGER pada mata_kuliah, jadwal, perubahan_jadwal, serta tugas. Boolean Kotlin disimpan sebagai INTEGER. UUID dipakai untuk kunci data pribadi; cache menggunakan kunci lokasi agar tidak tertukar antarpermintaan.',size=11)
h('5.2 Relasi dan aturan konsistensi',2)
p('Relasi utama adalah gedung ke ruangan, mata_kuliah ke jadwal dan tugas, serta jadwal ke perubahan_jadwal. Lokasi jadwal dan lokasi pengganti dapat kosong ketika ruangan belum ditentukan. Foreign key menjaga relasi; indeks unik jadwal_id dan tanggal membatasi satu perubahan untuk satu pertemuan.',size=11)
p('Tugas dan mata kuliah baru disimpan dalam satu transaksi. Penghapusan tugas mengubah is_deleted menjadi true; query daftar selalu menyaring is_deleted = 0. Perubahan data pribadi ditandai PENDING, tetapi belum dikirim ke cloud. Data awal gedung/ruangan belum diisi sebelum verifikasi lapangan.',size=11)
p('Format nilai melanjutkan laporan minggu pertama: hari 1–7, jam HH:mm, tanggal yyyy-MM-dd, dan deadline dalam epoch milidetik. Logika penerapan BATAL, GANTI_JAM, dan PINDAH_RUANG pada jadwal efektif masih menjadi pekerjaan lanjutan.',size=11)

# Page 6: concrete query examples.
page(); h('5.3 Contoh query utama',2)
p('Kode aplikasi memakai @Upsert untuk penyimpanan entitas dan @Query untuk pembacaan, update status, serta soft delete. Contoh INSERT di bawah menunjukkan bentuk SQL ekuivalen; parameter harus diikat, bukan digabungkan dari teks pengguna.',size=11)
p('a Tambah tugas',11,bold=True)
code('''INSERT INTO tugas (
  id, mata_kuliah_id, judul, deskripsi, deadline,
  sumber, selesai, pengingat_menit,
  updated_at, sync_status, is_deleted
) VALUES (
  :id, :mataKuliahId, :judul, :deskripsi, :deadline,
  :sumber, 0, :pengingatMenit, :now, 'PENDING', 0
);''',9.5)
p('b Ubah status selesai',11,bold=True)
code('''UPDATE tugas
SET selesai = :done, updated_at = :now,
    sync_status = 'PENDING'
WHERE id = :id AND is_deleted = 0;''',9.5)
p('c Baca daftar tugas',11,bold=True)
code('''SELECT * FROM tugas
WHERE is_deleted = 0
ORDER BY selesai, deadline;''',9.5)
p('d Hapus tugas secara lunak',11,bold=True)
code('''UPDATE tugas
SET is_deleted = 1, updated_at = :now,
    sync_status = 'PENDING'
WHERE id = :id;''',9.5)
p('Pada alur tugas, perubahan Room diamati melalui Flow. RecyclerView menerima daftar terbaru melalui ListAdapter dan DiffUtil. Setelah tugas selesai atau dihapus, ReminderScheduler membatalkan unique work dan notifikasi yang berkaitan dengan UUID tersebut.',size=11)
h('5.4 Validasi awal query',2)
p('Sembilan query DAO diperiksa menggunakan EXPLAIN pada SQLite desktop. Pengujian tambahan menjalankan insert/read, update status, soft delete, foreign key yang tidak valid, keunikan perubahan jadwal, dan pembacaan cache. Semua pemeriksaan SQL tersebut lulus. Hasil ini belum menggantikan verifikasi KSP, uji DAO Room, atau pengujian persistensi Android.',size=11)

# Page 7: honest testing and debugging.
page(); h('6 Testing Awal dan Debugging')
h('6.1 Pemeriksaan yang telah dijalankan',2)
p('Sebelas pemeriksaan awal lulus menggunakan verify_scaffold.py. Lingkupnya adalah struktur sumber dan SQL di SQLite desktop, dengan hasil rinci tersimpan pada qa/verification.json.',size=11)
table(['Kelompok pemeriksaan','Jumlah','Hasil'],[
 ['XML dapat diparse, kelas Fragment ada, dan ID lima menu cocok','3','Lulus'],
 ['Tujuh tabel dari properti entitas dan sembilan query DAO valid','2','Lulus'],
 ['Insert/read tugas, update status dan metadata, soft delete','3','Lulus'],
 ['Penolakan foreign key, keunikan perubahan, pembacaan cache','3','Lulus'],
 ['Total pemeriksaan statis dan SQLite desktop','11','Lulus']
],[11.2,1.7,3.02],10)
h('6.2 Skenario uji aplikasi berikutnya',2)
table(['Skenario','Hasil yang diharapkan','Status'],[
 ['Build, JUnit, dan lint','Gradle/KSP berhasil; empat test ReminderPolicy lulus','Belum dijalankan'],
 ['Navigasi dan Back','Menu dan form kembali ke destination yang benar','Belum dijalankan'],
 ['CRUD dan mode pesawat','Data tugas tetap tersimpan setelah proses ditutup','Belum dijalankan'],
 ['Izin dan pengingat','Penolakan izin aman; edit/selesai/hapus tidak menimbulkan pengingat ganda','Belum dijalankan'],
 ['Cuaca online dan cache','HTTP nyata berhasil; cache dan galat tampil sesuai kondisi','Belum dijalankan'],
 ['Jadwal efektif dan peta','Perubahan tanggal benar dan penanda sesuai kampus','Menunggu fitur lengkap']
],[4.1,8.7,3.12],9.5)
h('6.3 Daftar kendala dan penanganan',2)
table(['Temuan','Penanganan berikutnya'],[
 ['Lingkungan belum memiliki SDK Android dan JDK 17 yang siap dipakai','Pasang SDK 35 dan pilih JDK 17, lalu simpan log build'],
 ['Koordinat dan data gedung/ruangan belum diverifikasi','Validasi data kampus sebelum seed dan uji API'],
 ['Form jadwal, resolver perubahan, dan peta belum lengkap','Lanjutkan implementasi; jangan tandai FR terkait selesai'],
 ['Form tugas belum memiliki pilihan sumber dan metadata mata kuliah lengkap','Tambahkan pengelolaan mata kuliah dan pilihan sumber']
],[7.7,8.22],9.5)
p('Belum ada bug runtime Android yang dikonfirmasi. Daftar di atas adalah kendala lingkungan dan fitur terbuka, bukan perbaikan yang sudah lulus retest. WorkManager juga tidak menjamin waktu eksekusi tepat; hasil notifikasi perlu diuji pada perangkat nyata. [R5]',size=10.5)

# Page 8: reflection, work plan and sources.
page(); h('7 Refleksi dan Evaluasi Internal')
h('7.1 Hal yang sudah berjalan baik',2)
p('Rancangan minggu pertama dapat diterjemahkan ke package, entitas, dan tujuan navigasi yang konsisten. UUID serta metadata sinkronisasi tetap dipertahankan sehingga pengembangan cloud minggu ketiga memiliki dasar yang sama. Pemeriksaan SQL juga menunjukkan bahwa operasi tugas dan aturan relasi dasar dapat dijalankan pada SQLite desktop.',size=11)
h('7.2 Hal yang perlu diperbaiki',2)
p('Ketersediaan kode belum membuktikan aplikasi berjalan. Prioritas pertama adalah menyiapkan lingkungan build, menjalankan kerangka, lalu menutup alur tugas sebelum memperluas jadwal dan peta. Logika form serta cuaca perlu dipisahkan ke ViewModel, kode cuaca perlu diterjemahkan menjadi label yang mudah dipahami, dan masukan tanggal perlu memakai pemilih tanggal/waktu.',size=11)
p('Perubahan jadwal masih bersifat pribadi sesuai MVP, sehingga belum menyelesaikan penyebaran informasi perubahan kelas. Login, sinkronisasi dua arah, isolasi data per akun, dan pembersihan data saat logout tetap menjadi target minggu ketiga, bukan capaian minggu kedua.',size=11)
caption('Tabel 4 Pembagian tindak lanjut kelompok')
table(['Anggota','Fokus kerja berikutnya'],[
 ['Alfarisy','Uji UI/navigasi, form mata kuliah dan jadwal, konsistensi layout, serta screenshot perangkat'],
 ['Rantizi','Verifikasi Room/KSP, JOIN dan resolver jadwal, koordinat/seed kampus, serta uji Retrofit dan cache'],
 ['Fadhli','Uji pengingat dan izin, jalankan test case, catat bug/retest, lalu perbarui bukti pada laporan']
],[3.3,12.62],10)
p('Minggu kedua dapat dinyatakan selesai setelah build debug berhasil, alur prioritas berjalan pada perangkat, data tetap tersedia luring, cuaca dan pengingat memiliki bukti uji, serta screenshot implementasi menggantikan acuan wireframe. Rencana kerja rinci dan test case disediakan bersama kerangka proyek.',size=11)
h('7.3 Acuan teknis',2)
for text in [
 '[R1] Android Developers. AGP 8.9 compatibility. developer.android.com/build/releases/agp-8-9-0-release-notes',
 '[R2] Android Developers. Navigate to a destination. developer.android.com/guide/navigation/use-graph/navigate',
 '[R3] Open-Meteo. Weather Forecast API. open-meteo.com/en/docs',
 '[R4] Android Developers. Save data in a local database using Room. developer.android.com/training/data-storage/room',
 '[R5] Android Developers. Define work requests. developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work'
]:
    p(text,9,after=4)

doc.save(FINAL)
assert hashlib.sha256(REFERENCE.read_bytes()).hexdigest()==baseline
final = Document(FINAL)
assert len(final.sections)==1
with ZipFile(REFERENCE) as original, ZipFile(FINAL) as made:
    for name in ['word/footer1.xml','word/numbering.xml']:
        assert original.read(name)==made.read(name), f'Preserve-only part changed: {name}'
print('Saved',FINAL)
print('Source unchanged; footer and numbering preserved. Sections:',len(final.sections))
