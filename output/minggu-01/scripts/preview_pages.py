"""Render every PDF page to PNG and a contact sheet, for visual checking.

Usage (from output/minggu-01):  python scripts/preview_pages.py [out_dir] [dpi]
Default out_dir: laporan/preview (not part of the deliverable; safe to delete).
"""
import sys
from pathlib import Path

import pymupdf
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
PDF = ROOT / "laporan" / "Laporan1_SmartCampus.pdf"
out = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "laporan" / "preview"
dpi = int(sys.argv[2]) if len(sys.argv) > 2 else 60
out.mkdir(parents=True, exist_ok=True)

doc = pymupdf.open(PDF)
images = []
for i, page in enumerate(doc, 1):
    path = out / f"page{i}.png"
    page.get_pixmap(dpi=dpi).save(path)
    images.append(Image.open(path))
w, h = images[0].size
cols = 4
rows = (len(images) + cols - 1) // cols
sheet = Image.new("RGB", (cols * (w + 10), rows * (h + 10)), "gray")
for i, im in enumerate(images):
    sheet.paste(im, ((i % cols) * (w + 10), (i // cols) * (h + 10)))
sheet.save(out / "sheet.png")
print(f"pages={len(doc)} size={PDF.stat().st_size / 1024:.0f} KB -> {out}")
