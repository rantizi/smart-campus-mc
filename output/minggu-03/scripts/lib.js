// Gaya bersama laporan PaduKampus (meniru Laporan 1/2: Times New Roman, tabel abu-abu, A4).
const fs = require('fs');
const path = require('path');
const d = require('docx');
const {
  Paragraph, TextRun, Table, TableRow, TableCell, WidthType, AlignmentType, BorderStyle,
  ShadingType, ImageRun, PageBreak, Footer, PageNumber, HeadingLevel, LevelFormat, VerticalAlign,
} = d;

const FONT = 'Times New Roman';
const PAGE_W = 11906; // A4
const MARGIN = 1247; // 2,2 cm
const CONTENT_W = PAGE_W - 2 * MARGIN; // 9412 DXA
const IMG = path.join(__dirname, 'img');
const GRID = { style: BorderStyle.SINGLE, size: 4, color: 'BFBFBF' };
const BORDERS = { top: GRID, bottom: GRID, left: GRID, right: GRID };

let tableNo = 0;
let figureNo = 0;

function runs(text, opts = {}) {
  // **tebal** dan `kode` di dalam teks
  const out = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let last = 0; let m;
  while ((m = re.exec(text))) {
    if (m.index > last) out.push(new TextRun({ text: text.slice(last, m.index), font: FONT, ...opts }));
    const t = m[0];
    if (t.startsWith('**')) out.push(new TextRun({ text: t.slice(2, -2), font: FONT, ...opts, bold: true }));
    else out.push(new TextRun({ text: t.slice(1, -1), font: 'Courier New', size: (opts.size || 22) - 2 }));
    last = m.index + t.length;
  }
  if (last < text.length) out.push(new TextRun({ text: text.slice(last), font: FONT, ...opts }));
  return out;
}

const p = (text, o = {}) => new Paragraph({
  children: runs(text, { size: o.size || 22, bold: o.bold, italics: o.italics }),
  alignment: o.align || AlignmentType.JUSTIFIED,
  spacing: { after: o.after ?? 100, before: o.before ?? 0, line: 264 },
  keepNext: o.keepNext,
});
const center = (text, o = {}) => p(text, { ...o, align: AlignmentType.CENTER, after: o.after ?? 40 });
const h1 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text, font: FONT })] });
const h2 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun({ text, font: FONT })] });
const bullet = (text) => new Paragraph({
  children: runs(text, { size: 22 }), numbering: { reference: 'bullets', level: 0 },
  spacing: { after: 40, line: 252 }, alignment: AlignmentType.JUSTIFIED,
});
const pageBreak = () => new Paragraph({ children: [new PageBreak()] });

function caption(kind, text) {
  const n = kind === 'Tabel' ? ++tableNo : ++figureNo;
  return new Paragraph({
    children: [new TextRun({ text: `${kind} ${n}. ${text}`, font: FONT, size: 20 })],
    alignment: AlignmentType.CENTER, spacing: { before: kind === 'Gambar' ? 40 : 120, after: 60 },
    keepNext: kind === 'Tabel',
  });
}

// widths: proporsi kolom (jumlah bebas), dinormalkan ke lebar isi.
function table(headers, rows, widths, o = {}) {
  const total = widths.reduce((a, b) => a + b, 0);
  const w = widths.map((x) => Math.floor((x / total) * CONTENT_W));
  w[w.length - 1] += CONTENT_W - w.reduce((a, b) => a + b, 0);
  const size = o.size || 19;
  const cell = (text, i, head) => new TableCell({
    borders: BORDERS, width: { size: w[i], type: WidthType.DXA },
    shading: head ? { fill: 'F2F2F2', type: ShadingType.CLEAR, color: 'auto' } : undefined,
    margins: { top: 50, bottom: 50, left: 90, right: 90 }, verticalAlign: VerticalAlign.CENTER,
    children: String(text).split('\n').map((line) => new Paragraph({
      children: runs(line, { size, bold: head }), spacing: { after: 0, line: 240 },
    })),
  });
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA }, columnWidths: w,
    rows: [
      new TableRow({ tableHeader: true, cantSplit: true, children: headers.map((t, i) => cell(t, i, true)) }),
      ...rows.map((r) => new TableRow({ cantSplit: true, children: r.map((t, i) => cell(t, i, false)) })),
    ],
  });
}

function image(file, widthPx) {
  const buf = fs.readFileSync(path.join(IMG, file));
  const { w, h } = size(buf);
  return new ImageRun({
    type: file.endsWith('.png') ? 'png' : 'jpg', data: buf,
    transformation: { width: widthPx, height: Math.round((h * widthPx) / w) },
    altText: { title: file, description: file, name: file },
  });
}
function size(buf) {
  if (buf[0] === 0x89) return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
  let i = 2; // JPEG SOF
  while (i < buf.length) {
    const marker = buf[i + 1]; const len = buf.readUInt16BE(i + 2);
    if (marker >= 0xc0 && marker <= 0xc3) return { h: buf.readUInt16BE(i + 5), w: buf.readUInt16BE(i + 7) };
    i += 2 + len;
  }
  throw new Error('ukuran gambar tidak terbaca');
}
const figure = (file, widthPx, text) => [
  new Paragraph({ children: [image(file, widthPx)], alignment: AlignmentType.CENTER, spacing: { before: 140, after: 0 }, keepNext: true }),
  caption('Gambar', text),
];
// Deretan screenshot berlabel dalam tabel tanpa garis.
function gallery(items, widthPx) {
  const none = { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' };
  const nb = { top: none, bottom: none, left: none, right: none };
  const cw = Math.floor(CONTENT_W / items.length);
  return new Table({
    width: { size: cw * items.length, type: WidthType.DXA }, columnWidths: items.map(() => cw),
    alignment: AlignmentType.CENTER,
    rows: [new TableRow({ cantSplit: true, children: items.map(([file, label]) => new TableCell({
      borders: nb, width: { size: cw, type: WidthType.DXA },
      children: [
        new Paragraph({ children: [image(file, widthPx)], alignment: AlignmentType.CENTER, spacing: { after: 20 } }),
        new Paragraph({ children: [new TextRun({ text: label, font: FONT, size: 18 })], alignment: AlignmentType.CENTER, spacing: { after: 0 } }),
      ],
    })) })],
  });
}
function code(lines) {
  return lines.map((l, i) => new Paragraph({
    children: [new TextRun({ text: l || ' ', font: 'Courier New', size: 17 })],
    shading: { fill: 'F5F5F5', type: ShadingType.CLEAR, color: 'auto' },
    spacing: { after: 0, line: 228 }, keepNext: i < lines.length - 1,
    indent: { left: 120, right: 120 },
  }));
}

const members = () => table(['Nama', 'NIM', 'Peran utama'], [
  ['Alfarisy Nafaro Gymnastiar', '140810220020', 'Frontend dan UI/UX'],
  ['Abdul Aziz Rantizi', '140810230038', 'Backend, database dan API'],
  ['Muhammad Fadhli Ramadhan Aulia', '140810230056', 'Dokumentasi dan QA'],
], [45, 18, 37]);

function document(children, opts = {}) {
  return new d.Document({
    creator: 'Kelompok PaduKampus', title: opts.title,
    styles: {
      default: { document: { run: { font: FONT, size: 22 } } },
      paragraphStyles: [
        { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
          run: { font: FONT, size: 26, bold: true, color: '000000' },
          paragraph: { spacing: { before: 220, after: 100 }, keepNext: true, outlineLevel: 0 } },
        { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
          run: { font: FONT, size: 23, bold: true, color: '000000' },
          paragraph: { spacing: { before: 160, after: 80 }, keepNext: true, outlineLevel: 1 } },
      ],
    },
    numbering: { config: [{ reference: 'bullets', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•',
      alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 400, hanging: 260 } } } }] }] },
    sections: [{
      properties: { page: { size: { width: PAGE_W, height: 16838 }, margin: { top: MARGIN, bottom: MARGIN, left: MARGIN, right: MARGIN } } },
      footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
        children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 20 })] })] }) },
      children,
    }],
  });
}

async function save(doc, out) {
  fs.writeFileSync(out, await d.Packer.toBuffer(doc));
}

module.exports = { p, center, h1, h2, bullet, pageBreak, caption, table, figure, gallery, code, members, document, save, AlignmentType };
