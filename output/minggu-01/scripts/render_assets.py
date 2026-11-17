"""Render diagram SVGs and wireframe HTML to PNG with Playwright (Chromium).

Usage (from output/minggu-01):  python scripts/render_assets.py

Outputs:
  diagram/<name>.png                for every diagram/src/*.svg
  wireframe/wf-XX-<screen>.png      one per <section class="phone" id="wf-...">
  wireframe/wireframe_semua.png     all screens in one grid (with captions)
"""
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent
DIAGRAM_SRC = ROOT / "diagram" / "src"
WIREFRAME_HTML = ROOT / "wireframe" / "src" / "wireframes.html"
SCALE = 3  # device pixel ratio -> sharp PNGs when printed


def render_svgs(browser):
    page = browser.new_page(device_scale_factor=SCALE)
    for svg_path in sorted(DIAGRAM_SRC.glob("*.svg")):
        page.goto(svg_path.as_uri())
        out = ROOT / "diagram" / f"{svg_path.stem}.png"
        page.locator("svg").first.screenshot(path=str(out))
        print("diagram  ->", out.relative_to(ROOT))
    page.close()


def render_wireframes(browser):
    page = browser.new_page(device_scale_factor=SCALE, viewport={"width": 1600, "height": 1500})
    page.goto(WIREFRAME_HTML.as_uri())
    phones = page.locator("section.phone")
    for i in range(phones.count()):
        phone = phones.nth(i)
        out = ROOT / "wireframe" / f"{phone.get_attribute('id')}.png"
        phone.screenshot(path=str(out))
        print("wireframe->", out.relative_to(ROOT))
    out = ROOT / "wireframe" / "wireframe_semua.png"
    page.locator("#grid").screenshot(path=str(out))
    print("wireframe->", out.relative_to(ROOT))
    page.close()


if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch()
        render_svgs(browser)
        render_wireframes(browser)
        browser.close()
