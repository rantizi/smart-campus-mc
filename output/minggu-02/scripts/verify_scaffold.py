"""Compatibility entry point for the current architecture checks.
The original 11 desktop SQL checks were a historical pre-refactor snapshot.
"""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).resolve().parents[3] / 'scripts/verify_architecture.py'), run_name='__main__')
