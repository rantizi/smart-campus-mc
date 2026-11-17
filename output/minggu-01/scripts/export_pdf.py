"""Export laporan/Laporan1_SmartCampus.docx to PDF using Microsoft Word (Windows, pywin32).

Usage (from output/minggu-01):  python scripts/export_pdf.py
Alternative without Word: open the .docx in Word/LibreOffice and use "Save as PDF".
"""
from pathlib import Path

import win32com.client

ROOT = Path(__file__).resolve().parent.parent
DOCX = ROOT / "laporan" / "Laporan1_SmartCampus.docx"
PDF = DOCX.with_suffix(".pdf")
WD_EXPORT_FORMAT_PDF = 17

word = win32com.client.DispatchEx("Word.Application")
word.Visible = False
word.DisplayAlerts = 0
try:
    doc = word.Documents.Open(str(DOCX), ReadOnly=True)
    doc.ExportAsFixedFormat(str(PDF), WD_EXPORT_FORMAT_PDF)
    doc.Close(False)
finally:
    word.Quit()
print("saved", PDF.relative_to(ROOT), f"{PDF.stat().st_size / 1024:.0f} KB")
