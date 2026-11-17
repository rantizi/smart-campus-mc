"""Build Laporan 1 (Minggu 1) as .docx with python-docx.

Usage (from output/minggu-01):
    python scripts/render_assets.py   # PNG figures first
    python scripts/build_report.py    # -> laporan/Laporan1_SmartCampus.docx
    python scripts/export_pdf.py      # -> laporan/Laporan1_SmartCampus.pdf (via Microsoft Word)

All report text is in this file. Search for "[ISI:" to find placeholders.
Format: A4, Times New Roman 12, spasi 1,15, margin 2,54 cm. Tables use 9 pt (TABLE_PT).
"""
from pathlib import Path

from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "laporan" / "Laporan1_SmartCampus.docx"
FONT = "Times New Roman"
TABLE_PT = 9  # font size inside tables


# ---------------------------------------------------------------- helpers
def contrast(fg: str, bg: str) -> float:
    """WCAG 2.x contrast ratio between two hex colours."""
    def lum(h):
        h = h.lstrip("#")
        c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
        c = [x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
    a, b = sorted([lum(fg), lum(bg)], reverse=True)
    return (a + 0.05) / (b + 0.05)


def ratio_id(x: float) -> str:
    return f"{x:.2f}".replace(".", ",") + ":1"


def set_run_font(run, size=None, bold=None, italic=None):
    run.font.name = FONT
    run._element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
    if size:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def shade(cell, hex_color):
    tc_pr = cell._element.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color.lstrip("#"))
    tc_pr.append(shd)


def repeat_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    el = OxmlElement("w:tblHeader")
    el.set(qn("w:val"), "true")
    tr_pr.append(el)


def no_split(row):
    tr_pr = row._tr.get_or_add_trPr()
    tr_pr.append(OxmlElement("w:cantSplit"))


def remove_borders(table):
    tbl_pr = table._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), "nil")
        borders.append(el)
    tbl_pr.append(borders)


def cell_text(cell, text, size=TABLE_PT, bold=False, align=None):
    cell.text = ""
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.0
    if align:
        p.alignment = align
    lines = text.split("\n")
    for i, line in enumerate(lines):
        run = p.add_run(line)
        set_run_font(run, size=size, bold=bold)
        if i < len(lines) - 1:
            run.add_break()
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.TOP


def table(doc, header, rows, widths, bold_first_col=False, center_cols=()):
    t = doc.add_table(rows=1, cols=len(header))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    for i, h in enumerate(header):
        cell_text(t.rows[0].cells[i], h, bold=True)
        shade(t.rows[0].cells[i], "D9D9D9")
    repeat_header(t.rows[0])
    for r in rows:
        row = t.add_row()
        no_split(row)
        for i, v in enumerate(r):
            align = WD_ALIGN_PARAGRAPH.CENTER if i in center_cols else None
            cell_text(row.cells[i], v, bold=(bold_first_col and i == 0), align=align)
    fix_widths(t, widths)
    return t


def fix_widths(t, widths):
    """Word reads column widths from <w:tblGrid>, so set grid, cells and total width."""
    t.autofit = False
    for i, w in enumerate(widths):
        t.columns[i].width = Cm(w)
        for cell in t.columns[i].cells:
            cell.width = Cm(w)
    tbl_pr = t._tbl.tblPr
    tbl_w = tbl_pr.find(qn("w:tblW"))
    if tbl_w is None:
        tbl_w = OxmlElement("w:tblW")
        tbl_pr.append(tbl_w)
    tbl_w.set(qn("w:type"), "dxa")
    tbl_w.set(qn("w:w"), str(int(sum(widths) / 2.54 * 1440)))


def caption(doc, text, before=2, after=6):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(before)
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.line_spacing = 1.0
    label, _, rest = text.partition(". ")
    set_run_font(p.add_run(label + ". "), size=10, bold=True)
    set_run_font(p.add_run(rest), size=10)
    return p


def table_caption(doc, text):
    p = caption(doc, text, before=6, after=2)
    p.paragraph_format.keep_with_next = True


def figure(doc, path, width_cm, text):
    # keep the introducing paragraph (and its heading) on the same page as the figure
    doc.paragraphs[-1].paragraph_format.keep_with_next = True
    doc.add_picture(str(path), width=Cm(width_cm))
    pic_par = doc.paragraphs[-1]
    pic_par.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pic_par.paragraph_format.space_after = Pt(0)
    pic_par.paragraph_format.keep_with_next = True
    caption(doc, text)


def para(doc, text, after=4, align=WD_ALIGN_PARAGRAPH.JUSTIFY, italic=False, size=None):
    p = doc.add_paragraph()
    p.alignment = align
    p.paragraph_format.space_after = Pt(after)
    set_run_font(p.add_run(text), italic=italic, size=size)
    return p


def numbered(doc, items, after=1):
    for i, text in enumerate(items, 1):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        pf = p.paragraph_format
        pf.left_indent = Cm(0.75)
        pf.first_line_indent = Cm(-0.75)
        pf.space_after = Pt(after)
        set_run_font(p.add_run(f"{i}.\t{text}"))
        pf.tab_stops.add_tab_stop(Cm(0.75))


def label_line(doc, label, value, after=0):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.left_indent = Cm(4.5)
    p.paragraph_format.first_line_indent = Cm(-4.5)
    p.paragraph_format.tab_stops.add_tab_stop(Cm(4.2))
    set_run_font(p.add_run(label), bold=True)
    set_run_font(p.add_run("\t: " + value))


def h1(doc, text):
    doc.add_heading(text, level=1)


def h2(doc, text):
    doc.add_heading(text, level=2)


def add_page_number(section):
    p = section.footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run()
    set_run_font(run, size=10)
    for kind, val in (("begin", None), ("instr", " PAGE "), ("end", None)):
        if kind == "instr":
            el = OxmlElement("w:instrText")
            el.set(qn("xml:space"), "preserve")
            el.text = val
        else:
            el = OxmlElement("w:fldChar")
            el.set(qn("w:fldCharType"), kind)
        run._r.append(el)


def setup(doc):
    sec = doc.sections[0]
    sec.orientation = WD_ORIENT.PORTRAIT
    sec.page_width, sec.page_height = Cm(21.0), Cm(29.7)
    for side in ("left_margin", "right_margin", "top_margin", "bottom_margin"):
        setattr(sec, side, Cm(2.54))
    sec.footer_distance = Cm(1.25)

    normal = doc.styles["Normal"]
    normal.font.name = FONT
    normal.font.size = Pt(12)
    normal.element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
    normal.paragraph_format.line_spacing = 1.15
    normal.paragraph_format.space_after = Pt(3)

    for name, size, before in (("Heading 1", 12, 8), ("Heading 2", 12, 4)):
        st = doc.styles[name]
        st.font.name = FONT
        st.font.size = Pt(size)
        st.font.bold = True
        st.font.italic = False
        st.font.color.rgb = RGBColor(0, 0, 0)
        rpr = st.element.get_or_add_rPr()
        fonts = rpr.find(qn("w:rFonts"))
        if fonts is None:
            fonts = OxmlElement("w:rFonts")
            rpr.append(fonts)
        for attr in ("w:ascii", "w:hAnsi", "w:eastAsia", "w:cs"):
            fonts.set(qn(attr), FONT)
        for attr in ("w:asciiTheme", "w:hAnsiTheme", "w:eastAsiaTheme", "w:cstheme"):
            if fonts.get(qn(attr)) is not None:
                del fonts.attrib[qn(attr)]
        pf = st.paragraph_format
        pf.space_before = Pt(before)
        pf.space_after = Pt(3)
        pf.line_spacing = 1.15
        pf.keep_with_next = True
    add_page_number(sec)


# ---------------------------------------------------------------- content
def build():
    doc = Document()
    setup(doc)

    # ---------- Judul
    for text, size, bold, after in (
        ("LAPORAN 1: Analisis Kebutuhan dan Desain Awal Aplikasi Mobile", 14, True, 2),
        ("PaduKampus: Aplikasi Asisten Kuliah Harian Mahasiswa Berbasis Android", 12, False, 2),
        ("Mata Kuliah Mobile Computing, Kelas B, Semester Ganjil 2026/2027", 10, False, 0),
        ("Program Studi S1 Teknik Informatika, Universitas Padjadjaran · 17 November 2026", 10, False, 4),
    ):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(after)
        set_run_font(p.add_run(text), size=size, bold=bold)

    # ---------- 1
    h1(doc, "1. Identitas Kelompok")
    label_line(doc, "Judul proyek mini", "PaduKampus: Aplikasi Asisten Kuliah Harian Mahasiswa Berbasis Android")
    label_line(doc, "Tema", "Tema 1 – Smart Campus Assistant")
    label_line(doc, "Kelas / dosen", "B / Deni Setiana", after=2)
    table_caption(doc, "Tabel 1. Anggota kelompok")
    table(doc, ["No", "Nama", "NIM", "Peran utama"], [
        ["1", "Alfarisy Nafaro Gymnastiar", "140810220020", "Frontend (UI/UX & Android UI)"],
        ["2", "Abdul Aziz Rantizi", "140810230038", "Backend (Database, API & Cloud)"],
        ["3", "Muhammad Fadhli Ramadhan Aulia", "140810230056", "Dokumentasi & QA (Testing)"],
    ], [1.0, 6.0, 3.2, 5.8], center_cols=(0,))

    # ---------- 2
    h1(doc, "2. Latar Belakang")
    para(doc,
         "Informasi akademik mahasiswa tersebar di portal akademik, LMS, Google Classroom, grup WhatsApp, dan "
         "pengumuman lisan. Tabel 2 merangkum masalah yang dialami sendiri oleh anggota kelompok; daftar ini "
         "berasal dari pengalaman, bukan survei, sehingga perlu divalidasi kepada mahasiswa lain saat pengujian.")
    table_caption(doc, "Tabel 2. Masalah pengguna dan fitur yang menjawabnya")
    table(doc, ["No", "Masalah dan dampaknya", "Fitur PaduKampus"], [
        ["1", "Info dosen berhalangan, ganti jam, atau pindah ruangan dikirim mendadak di grup WA dan tenggelam di antara pesan lain; mahasiswa baru tahu setelah tiba di kampus atau menunggu di ruangan yang salah.",
         "Catatan perubahan jadwal pribadi (FR-04); menjawab sebagian."],
        ["2", "Portal akademik kurang nyaman di HP dan lambat saat KRS, sehingga jadwal disimpan sebagai screenshot yang tidak ikut berubah.",
         "Jadwal hari ini di beranda, tersimpan di perangkat (FR-02, FR-03)."],
        ["3", "Tugas diumumkan di LMS, Google Classroom, grup WA, atau lisan; deadline terlewat saat tugas menumpuk.",
         "Satu daftar tugas dengan sumber dan pengingat (FR-05, FR-06)."],
        ["4", "Kode ruangan tidak menunjukkan gedung dan lantai, peta umum hanya sampai gedung; ruangan sulit ditemukan.",
         "Peta kampus dan pencarian ruangan (FR-08, FR-09)."],
        ["5", "Jeda antarkelas singkat sementara gedungnya berjauhan; mahasiswa terlambat masuk kelas.",
         "Kelas berikutnya dan lokasinya, pengingat kelas (FR-03, FR-07)."],
        ["6", "Hujan deras sering turun siang atau sore saat harus pindah gedung atau pulang; mahasiswa kehujanan.",
         "Cuaca dan peluang hujan per jam di kampus (FR-10)."],
        ["7", "Sinyal dan Wi-Fi kampus tidak stabil pada jam sibuk; portal dan LMS tidak bisa dibuka untuk mengecek jadwal atau tugas.",
         "Data tersimpan di perangkat dan dapat dibuka luring (FR-11)."],
        ["8", "Catatan jadwal dan tugas di aplikasi catatan HP hilang saat ganti HP atau reset.",
         "Login dan sinkronisasi cloud (FR-01, FR-11)."],
    ], [0.8, 10.0, 5.2], center_cols=(0,))
    para(doc,
         "Masalah nomor 1 hanya terjawab sebagian karena perubahan masih dicatat sendiri oleh tiap mahasiswa dan "
         "belum disebarkan ke seluruh kelas; solusinya dicatat sebagai pengembangan lanjut (Bagian 8).",
         after=2).paragraph_format.space_before = Pt(4)

    # ---------- 3
    h1(doc, "3. Tujuan Proyek")
    numbered(doc, [
        "Menyediakan jadwal hari ini, kelas berikutnya, dan lokasi ruangannya dalam satu layar tanpa membuka portal akademik.",
        "Mengumpulkan tugas dari berbagai sumber dalam satu daftar dan mengingatkan deadline melalui notifikasi.",
        "Menampilkan peluang hujan di area kampus Jatinangor agar mahasiswa dapat bersiap sebelum berpindah gedung atau pulang.",
        "Menjaga data tetap dapat dibuka tanpa koneksi dan tidak hilang saat berganti perangkat melalui sinkronisasi cloud.",
    ])
    p = para(doc, "", after=2)
    p.paragraph_format.space_before = Pt(4)
    set_run_font(p.add_run("Target pengguna: "), bold=True)
    set_run_font(p.add_run(
        "mahasiswa S1 aktif di kampus Unpad Jatinangor, terutama mahasiswa baru dan yang kuliah di gedung atau "
        "fakultas lain, dengan ponsel Android pribadi (minimal Android 8.0) yang sering dipakai sambil berjalan. "
        "Dosen dan admin akademik belum menjadi pengguna langsung pada MVP."))

    # ---------- 4
    h1(doc, "4. Analisis Kebutuhan")
    h2(doc, "4.1 Kebutuhan Fungsional")
    table_caption(doc, "Tabel 3. Kebutuhan fungsional (Must = wajib ada pada MVP; Should = boleh ditunda)")
    table(doc, ["Kode", "Kebutuhan fungsional", "Prioritas", "Masalah"], [
        ["FR-01", "Sistem menyediakan pendaftaran akun, login, dan logout dengan email dan kata sandi.", "Must", "8"],
        ["FR-02", "Sistem memungkinkan pengguna menambah, mengubah, dan menghapus mata kuliah beserta jadwal mingguannya (hari, jam, ruangan, dosen).", "Must", "2"],
        ["FR-03", "Sistem menampilkan jadwal hari ini serta kelas berikutnya lengkap dengan ruangan, gedung, dan lantainya.", "Must", "2, 5"],
        ["FR-04", "Sistem memungkinkan pengguna mencatat perubahan jadwal pada tanggal tertentu (pindah ruangan, ganti jam, atau kelas batal) yang menggantikan jadwal reguler pada tanggal itu.", "Must", "1"],
        ["FR-05", "Sistem memungkinkan pengguna menambah, mengubah, menghapus, dan menandai selesai tugas, termasuk mata kuliah, deadline, dan sumber tugas.", "Must", "3"],
        ["FR-06", "Sistem mengirim notifikasi pengingat sebelum deadline tugas sesuai waktu pengingat yang dipilih pengguna.", "Must", "3"],
        ["FR-07", "Sistem mengirim notifikasi beberapa menit sebelum kelas dimulai yang memuat mata kuliah dan ruangannya.", "Should", "5"],
        ["FR-08", "Sistem menampilkan peta kampus Jatinangor dengan penanda lokasi gedung perkuliahan.", "Must", "4"],
        ["FR-09", "Sistem memungkinkan pengguna mencari ruangan berdasarkan kode atau nama, lalu menampilkan gedung, lantai, dan lokasinya di peta.", "Should", "4"],
        ["FR-10", "Sistem menampilkan cuaca terkini dan peluang hujan per jam di lokasi kampus dari API publik, serta data terakhir yang tersimpan saat luring.", "Must", "6"],
        ["FR-11", "Sistem menyimpan jadwal, perubahan jadwal, dan tugas di perangkat agar dapat dibuka tanpa koneksi, lalu menyinkronkannya ke cloud saat terhubung internet.", "Must", "7, 8"],
    ], [1.3, 11.5, 1.6, 1.6], bold_first_col=True, center_cols=(2, 3))

    h2(doc, "4.2 Kebutuhan Non-Fungsional")
    table_caption(doc, "Tabel 4. Kebutuhan non-fungsional")
    table(doc, ["Kode", "Kategori", "Kebutuhan"], [
        ["NFR-01", "Kinerja", "Beranda menampilkan jadwal dan tugas dari database lokal paling lama 2 detik setelah aplikasi dibuka, tanpa menunggu jaringan."],
        ["NFR-02", "Kinerja", "Data cuaca disimpan di cache lokal dan API tidak dipanggil ulang selama data berumur kurang dari 30 menit; daftar memakai RecyclerView dengan DiffUtil."],
        ["NFR-03", "Keamanan", "Data cloud hanya dapat dibaca dan diubah oleh pemilik akun (aturan keamanan Firestore berbasis UID); data lokal dihapus saat logout."],
        ["NFR-04", "Keamanan", "Masukan formulir divalidasi: kolom wajib, format email, kata sandi minimal 6 karakter, jam selesai setelah jam mulai."],
        ["NFR-05", "Keamanan", "Komunikasi jaringan hanya melalui HTTPS, tidak ada kunci rahasia di kode sumber, dan aplikasi tidak meminta izin lokasi."],
        ["NFR-06", "Keandalan", "Fitur jadwal dan tugas berjalan penuh dalam mode pesawat; perubahan luring terkirim ke cloud saat koneksi kembali tanpa kehilangan data."],
        ["NFR-07", "Keandalan", "Kegagalan atau timeout API cuaca tidak membuat aplikasi berhenti; aplikasi menampilkan pesan yang jelas dan data cache terakhir."],
        ["NFR-08", "Estetika", "Material Design dengan warna dan font konsisten (Tabel 5), target sentuh minimal 48 dp, kontras teks minimal 4,5:1."],
        ["NFR-09", "Kompatibilitas", "Aplikasi berjalan pada Android 8.0 (API 26) hingga versi terbaru dalam orientasi potret."],
    ], [1.6, 2.4, 12.0], bold_first_col=True)

    h2(doc, "4.3 Use Case Diagram")
    para(doc,
         "Aktor utama adalah mahasiswa; layanan eksternal digambarkan sebagai aktor sistem. Kode FR sesuai Tabel 3.", after=2)
    figure(doc, ROOT / "diagram" / "use_case.png", 13.5,"Gambar 1. Use case diagram PaduKampus")

    # ---------- 5
    h1(doc, "5. Desain UI/UX Awal")
    h2(doc, "5.1 Alur Navigasi")
    para(doc,
         "Aplikasi memakai satu Activity dengan NavHostFragment dan BottomNavigationView untuk lima tujuan utama. "
         "Antarhalaman hanya dikirim ID data (Safe Args), dan setelah logout back stack dibersihkan agar layar "
         "sebelumnya tidak dapat dibuka kembali.", after=2)
    figure(doc, ROOT / "diagram" / "nav_flow.png", 14.0, "Gambar 2. Alur navigasi aplikasi")

    h2(doc, "5.2 Wireframe")
    para(doc,
         "Navigasi utama dan tombol tambah (+) berada di zona jangkauan ibu jari; elemen interaktif minimal 48 dp. "
         "Layar Login dan Profil mengikuti pola yang sama (lihat Gambar 2).", after=2)
    shots = [
        ("wf-01-beranda.png", "a. Beranda (FR-03, FR-10)"),
        ("wf-02-jadwal.png", "b. Jadwal (FR-02, FR-04)"),
        ("wf-03-form-perubahan.png", "c. Form Perubahan (FR-04)"),
        ("wf-04-tugas.png", "d. Tugas (FR-05)"),
        ("wf-05-form-tugas.png", "e. Form Tugas (FR-05, FR-06)"),
        ("wf-06-peta.png", "f. Peta (FR-08, FR-09)"),
    ]
    grid = doc.add_table(rows=2, cols=3)
    grid.alignment = WD_TABLE_ALIGNMENT.CENTER
    remove_borders(grid)
    for idx, (fname, cap) in enumerate(shots):
        cell = grid.rows[idx // 3].cells[idx % 3]
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.line_spacing = 1.0
        p.paragraph_format.keep_with_next = True
        p.add_run().add_picture(str(ROOT / "wireframe" / fname), width=Cm(4.9))
        cp = cell.add_paragraph()
        cp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        cp.paragraph_format.space_after = Pt(3)
        cp.paragraph_format.line_spacing = 1.0
        set_run_font(cp.add_run(cap), size=10)
    fix_widths(grid, [5.3, 5.3, 5.3])
    for row in grid.rows:
        no_split(row)
    caption(doc, "Gambar 3. Wireframe low-fidelity layar utama ([ISI] = data gedung/ruangan, diisi minggu 2)")

    h2(doc, "5.3 Panduan Warna dan Font")
    colors = [
        ("Primer", "#1A5C8A", "#FFFFFF", "App bar, tombol utama, item navigasi aktif"),
        ("Peringatan", "#F2B705", "#1C1B1F", "Deadline dekat, peluang hujan tinggi"),
        ("Galat", "#B3261E", "#FFFFFF", "Pesan validasi, kelas batal, tugas terlambat"),
        ("Berhasil", "#2E7D32", "#FFFFFF", "Tugas selesai, data tersinkron"),
        ("Latar", "#F6F8FB", "#1C1B1F", "Latar layar (kartu memakai putih #FFFFFF)"),
        ("Teks sekunder", "#5F6368", "#FFFFFF", "Keterangan dan label kecil"),
    ]
    table_caption(doc, "Tabel 5. Panduan warna dan font (kontras terhadap warna teks di atasnya, rumus WCAG 2.x, minimal 4,5:1)")
    rows = [[n, hx, use, ratio_id(contrast(hx, fg))] for n, hx, fg, use in colors]
    rows += [
        ["Font", "Roboto", "Judul layar 22 sp, judul kartu 16 sp tebal, isi 14 sp, keterangan 12 sp", "–"],
        ["Ukuran", "48 dp / 8 dp", "Tinggi minimal elemen yang dapat disentuh / jarak antarelemen kelipatan 8 dp", "–"],
    ]
    t = table(doc, ["Elemen", "Nilai", "Penggunaan", "Kontras"], rows, [2.5, 2.3, 9.6, 1.6],
              bold_first_col=True, center_cols=(3,))
    for i, (_, hx, fg, _) in enumerate(colors, start=1):
        cell = t.rows[i].cells[1]
        shade(cell, hx)
        for run in cell.paragraphs[0].runs:
            run.font.color.rgb = RGBColor.from_string(fg.lstrip("#"))

    # ---------- 6
    h1(doc, "6. Perancangan Database")
    h2(doc, "6.1 Skema SQLite (Room)")
    para(doc,
         "Room di atas SQLite menjadi sumber data utama tampilan (offline-first). Kunci primer berupa UUID (TEXT) "
         "yang juga dipakai sebagai ID dokumen Firestore, sehingga relasi tetap konsisten di perangkat lain. Tabel "
         "yang disinkronkan memiliki updated_at (epoch ms), sync_status (SYNCED/PENDING), dan is_deleted (hapus "
         "lunak); konflik diselesaikan dengan last-write-wins berdasarkan updated_at.", after=2)
    figure(doc, ROOT / "diagram" / "erd.png", 14.0,"Gambar 4. ERD dan skema tabel SQLite")
    para(doc,
         "Kode FR di kanan nama tabel menunjukkan kebutuhan yang memakai tabel itu. Format nilai: hari 1 = Senin "
         "s.d. 7 = Minggu, jam HH:mm, tanggal yyyy-MM-dd, jenis perubahan PINDAH_RUANG/GANTI_JAM/BATAL, deadline "
         "dalam epoch ms, sumber tugas LMS/CLASSROOM/WA/LISAN/LAINNYA. Tabel gedung dan ruangan berisi data awal "
         "aplikasi [ISI: data gedung dan ruangan kampus Jatinangor] dan tidak disinkronkan.", after=2)

    h2(doc, "6.2 Rancangan Integrasi Cloud dan API")
    table_caption(doc, "Tabel 6. Rancangan integrasi data")
    table(doc, ["Komponen", "Rancangan"], [
        ["Firebase Authentication", "Login email dan kata sandi; UID menjadi kunci data di cloud."],
        ["Cloud Firestore (paket Spark)", "Koleksi users/{uid}/mata_kuliah, /jadwal, /perubahan_jadwal, /tugas; ID dokumen = UUID lokal. Aturan keamanan: baca/tulis hanya jika request.auth.uid == uid."],
        ["Sinkronisasi", "WorkManager berjalan saat ada koneksi: mengirim baris PENDING, lalu mengambil dokumen yang updated_at-nya lebih baru."],
        ["Open-Meteo (cuaca)", "GET https://api.open-meteo.com/v1/forecast dengan parameter latitude dan longitude [ISI: koordinat kampus Jatinangor], current = temperature_2m, weather_code; hourly = precipitation_probability; timezone = Asia/Jakarta. Tanpa API key; via Retrofit, cache 30 menit."],
        ["osmdroid + OpenStreetMap", "Peta tanpa API key dan tanpa akun penagihan; penanda dari tabel gedung. Tile perlu internet, info gedung/ruangan tetap tersedia luring."],
    ], [4.2, 11.8], bold_first_col=True)

    # ---------- 7
    h1(doc, "7. Rencana Pengembangan")
    h2(doc, "7.1 Pembagian Tugas")
    table_caption(doc, "Tabel 7. Pembagian tugas per minggu")
    t = table(doc, ["Minggu", "Alfarisy (Frontend)", "Rantizi (Backend)", "Fadhli (Dokumentasi & QA)"], [
        ["1 – Analisis & Desain",
         "Wireframe/mockup, diagram alur navigasi, panduan warna & font",
         "Skema database SQLite (ERD), pemilihan API publik, rancangan integrasi Firebase",
         "Analisis kebutuhan (FR/NFR), use case diagram, latar belakang & tujuan, menyusun Laporan 1"],
        ["2 – Implementasi & Integrasi",
         "Layout XML, Fragment, Navigation Component, RecyclerView",
         "Room/SQLite (DAO & query), integrasi API dengan Retrofit, Repository",
         "Fitur notifikasi pengingat tugas, test case black box awal, log bug, menyusun Laporan 2"],
        ["3 – Optimasi & Finalisasi",
         "Perbaikan UI, lazy loading, screenshot hasil, slide presentasi",
         "Firebase Auth + Firestore, sinkronisasi data, keamanan (login, API key), caching",
         "Pengujian akhir, rekap bug & perbaikan, Laporan 3 & laporan akhir"],
        ["Semua anggota",
         "Commit rutin ke GitHub, review laporan bersama, latihan demo & presentasi", "", ""],
    ], [3.1, 4.1, 4.4, 4.4], bold_first_col=True)
    last = t.rows[-1]
    merged = last.cells[1].merge(last.cells[3])
    cell_text(merged, "Commit rutin ke GitHub, review laporan bersama, latihan demo & presentasi")

    h2(doc, "7.2 Jadwal Kerja Mingguan")
    table_caption(doc, "Tabel 8. Jadwal kerja tiga minggu (bila waktu kurang, FR-07 dan FR-09 ditunda)")
    table(doc, ["Minggu", "Kegiatan utama", "Fitur target", "Luaran"], [
        ["1 (10–16 Nov)", "Analisis kebutuhan, use case, wireframe, alur navigasi, rancangan database dan integrasi.",
         "–", "Laporan 1 (17 Nov)"],
        ["2 (17–23 Nov)", "Proyek Android Studio (Kotlin), layout XML, Fragment dan Navigation Component, Room, Retrofit ke Open-Meteo, notifikasi tugas, uji black box awal.",
         "Must: FR-02 s.d. FR-06, FR-08, FR-10", "Laporan 2"],
        ["3 (24–30 Nov)", "Firebase Auth dan Firestore, sinkronisasi, aturan keamanan, caching dan lazy loading, perbaikan UI, pengujian akhir.",
         "Must: FR-01, FR-11; Should: FR-07, FR-09", "Laporan 3, laporan akhir, presentasi, demo"],
    ], [2.5, 7.5, 3.3, 2.7], bold_first_col=True)

    # ---------- 8
    h1(doc, "8. Refleksi & Rencana Perbaikan")
    table_caption(doc, "Tabel 9. Hambatan awal dan solusi sementara")
    table(doc, ["No", "Hambatan awal", "Solusi sementara"], [
        ["1", "Nama gedung, kode ruangan, lantai, dan koordinat gedung di kampus Jatinangor belum tersedia dalam bentuk siap pakai (FR-03, FR-08, FR-09).",
         "Desain memakai penanda [ISI]; data dikumpulkan dan diisi kelompok pada minggu 2 sebagai data awal aplikasi."],
        ["2", "Sejak Android 13 (API 33), notifikasi memerlukan izin runtime POST_NOTIFICATIONS; bila ditolak, pengingat FR-06 dan FR-07 tidak tampil.",
         "Izin diminta saat pengguna pertama kali mengaktifkan pengingat, dengan penjelasan singkat; status izin ditampilkan di Profil."],
        ["3", "Alarm tepat waktu dibatasi: sejak Android 12 perlu izin SCHEDULE_EXACT_ALARM, yang sejak Android 14 tidak lagi diberikan otomatis untuk aplikasi baru; Doze dan penghemat baterai dapat menunda pengingat beberapa menit.",
         "Memakai WorkManager/alarm tidak tepat (inexact) tanpa izin khusus, jarak pengingat longgar (bawaan 15 menit sebelum kelas, H-1 untuk tugas), dijadwalkan ulang setelah restart; ketepatan diuji di perangkat nyata minggu 2."],
        ["4", "Sinkronisasi dua arah dapat menimbulkan konflik bila data yang sama diubah di dua perangkat sebelum tersinkron.",
         "Last-write-wins berdasarkan updated_at; MVP diuji dengan asumsi satu perangkat aktif per akun."],
        ["5", "Layanan gratis memiliki batas (Open-Meteo untuk penggunaan non-komersial, kuota harian Firestore paket Spark).",
         "Cache cuaca 30 menit dan sinkronisasi hanya untuk data yang berubah."],
    ], [0.8, 7.6, 7.6], center_cols=(0,))
    p = para(doc, "", after=0)
    p.paragraph_format.space_before = Pt(5)
    set_run_font(p.add_run("Pengembangan lanjut (di luar MVP): "), bold=True)
    set_run_font(p.add_run(
        "(1) kanal kelas bersama, yaitu ketua kelas mencatat perubahan jadwal ke koleksi kelas di Firestore dan "
        "anggota kelas menerimanya saat sinkronisasi beserta notifikasi, sehingga masalah nomor 1 terjawab penuh; "
        "(2) presensi digital; (3) akses dari laptop (versi web)."))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc.save(OUT)
    print("saved", OUT.relative_to(ROOT))


if __name__ == "__main__":
    build()
