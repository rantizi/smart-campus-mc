"""Automated checks for Laporan 1. Run after export_pdf.py.

Usage (from output/minggu-01):  python scripts/verify_report.py

Checks:
  1. PDF: 3-6 pages, <= 5 MB, the 8 template sections present and in order.
  2. Every FR (FR-01..FR-11) appears in the navigation flow or the wireframes.
  3. Every table in the ERD is labelled with at least one existing FR.
  4. Common English function words in the PDF text (should be none).
  5. Lists every [ISI...] placeholder found in the report, diagrams and wireframes.
"""
import re
import sys
from pathlib import Path

import pymupdf

ROOT = Path(__file__).resolve().parent.parent
PDF = ROOT / "laporan" / "Laporan1_SmartCampus.pdf"
NAV = ROOT / "diagram" / "src" / "nav_flow.svg"
WF = ROOT / "wireframe" / "src" / "wireframes.html"
ERD = ROOT / "diagram" / "src" / "erd.svg"

SECTIONS = ["1. Identitas Kelompok", "2. Latar Belakang", "3. Tujuan Proyek", "4. Analisis Kebutuhan",
            "5. Desain UI/UX Awal", "6. Perancangan Database", "7. Rencana Pengembangan",
            "8. Refleksi & Rencana Perbaikan"]
ENGLISH = {"the", "and", "of", "with", "is", "are", "for", "this", "that", "will", "from", "should be",
           "which", "when", "has", "have", "into", "user", "users"}
ok = True


def check(cond, msg):
    global ok
    print(("PASS  " if cond else "FAIL  ") + msg)
    ok &= bool(cond)


# 1. PDF basics
doc = pymupdf.open(PDF)
text = "\n".join(p.get_text() for p in doc)
size_mb = PDF.stat().st_size / 1024 / 1024
check(3 <= len(doc) <= 6, f"page count = {len(doc)} (3-6)")
check(size_mb <= 5, f"PDF size = {size_mb:.2f} MB (<= 5 MB)")
flat = re.sub(r"\s+", " ", text)
positions = [flat.find(s) for s in SECTIONS]
check(all(p >= 0 for p in positions), "all 8 sections present: " + ", ".join(
    s.split(".")[0] for s, p in zip(SECTIONS, positions) if p >= 0))
check(positions == sorted(positions), "sections in template order")

# 2. FR coverage in UI design
frs = sorted(set(re.findall(r"FR-\d{2}", flat)))
check(frs == [f"FR-{i:02d}" for i in range(1, 12)], f"FR list in report = {frs[0]}..{frs[-1]} ({len(frs)})")
ui = NAV.read_text(encoding="utf-8") + WF.read_text(encoding="utf-8")
missing = [fr for fr in frs if fr not in ui]
check(not missing, "every FR appears in nav flow or wireframes" + (f" (missing: {missing})" if missing else ""))

# 3. Every ERD table is used by at least one FR
erd = ERD.read_text(encoding="utf-8")
tables = re.findall(r'class="tn">([a-z_]+)</text><text [^>]*class="frs">([^<]+)</text>', erd)
check(len(tables) == 7, f"ERD tables with FR label = {len(tables)} (expected 7)")
for name, fr_text in tables:
    used = re.findall(r"FR-\d{2}", fr_text)
    check(used and all(u in frs for u in used), f"table {name:<17} used by {', '.join(used)}")

# 4. English leftovers (whole-word match, lower case)
# identifiers such as is_deleted or users/{uid} are code, not English prose -> skipped
words = [w for w in re.findall(r"[a-zA-Z_/{}.]+", text.lower()) if not re.search(r"[_/{}.]", w)]
hits = sorted({w for w in words if w in ENGLISH})
check(not hits, "no common English words in PDF text" + (f" (found: {hits})" if hits else ""))

# 5. Placeholders
print("\nPlaceholders [ISI...]:")
for path in (PDF, NAV, WF, ERD):
    src = text if path == PDF else path.read_text(encoding="utf-8")
    found = sorted(set(re.findall(r"\[ISI[^\]]*\]", re.sub(r"\s+", " ", src))))
    print(f"  {path.relative_to(ROOT)}: {found if found else '-'}")

print("\nRESULT:", "ALL CHECKS PASSED" if ok else "SOME CHECKS FAILED")
sys.exit(0 if ok else 1)
