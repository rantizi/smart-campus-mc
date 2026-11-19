"""Use the packaged render_docx rasterizer with native Word PDF conversion on Windows.

The bundled LibreOffice renderer is unavailable on this Windows runtime.
Only the converter is replaced; the canonical render_docx.py rasterizer is reused.
Inputs are opened read-only and are never saved by Word.
"""
from pathlib import Path
import importlib.util
import os
import sys
import win32com.client

SKILL = Path(r'C:\Users\Alfarisy\.codex\plugins\cache\openai-primary-runtime\documents\26.1118.11504\skills\documents')
DEPS = Path(sys.executable).parents[1]
poppler = next((DEPS / 'native/poppler').rglob('pdftoppm.exe')).parent
os.environ['PATH'] = str(poppler) + os.pathsep + os.environ.get('PATH', '')
spec = importlib.util.spec_from_file_location('packaged_render_docx', SKILL / 'render_docx.py')
renderer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(renderer)

def word_pdf(doc_path, user_profile, convert_tmp_dir, stem, verbose):
    target = Path(convert_tmp_dir) / (stem + '.pdf')
    word = win32com.client.DispatchEx('Word.Application')
    word.Visible = False
    word.DisplayAlerts = 0
    try:
        document = word.Documents.Open(str(Path(doc_path).resolve()), ReadOnly=True, AddToRecentFiles=False)
        document.Repaginate()
        document.ExportAsFixedFormat(str(target), 17)
        document.Close(False)
    finally:
        word.Quit()
    return str(target), 'PDF exported with Microsoft Word; source opened read-only.'

renderer.convert_to_pdf = word_pdf
if __name__ == '__main__':
    renderer.main()
