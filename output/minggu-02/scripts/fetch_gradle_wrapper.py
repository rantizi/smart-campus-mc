"""Fetch the official Gradle 8.11.1 wrapper and verify its published checksum."""
from pathlib import Path
import hashlib
import requests

ROOT = Path(__file__).resolve().parents[3]
session = requests.Session()
files = ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar']
payloads = {}
for name in files:
    response = session.get('https://raw.githubusercontent.com/gradle/gradle/v8.11.1/' + name, timeout=25)
    response.raise_for_status()
    payloads[name] = response.content
response = session.get('https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar.sha256', timeout=25)
response.raise_for_status()
actual = hashlib.sha256(payloads[files[2]]).hexdigest()
assert actual == response.text.strip(), 'Gradle wrapper checksum mismatch'
response = session.get('https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256', timeout=25)
response.raise_for_status()
for name, payload in payloads.items():
    target = ROOT / name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(payload)
props = ROOT / 'gradle/wrapper/gradle-wrapper.properties'
lines = [line for line in props.read_text().splitlines() if not line.startswith('distributionSha256Sum=')]
props.write_text('\n'.join(lines) + '\ndistributionSha256Sum=' + response.text.strip() + '\n', encoding='utf-8')
print('Gradle wrapper downloaded; SHA256 verified:', actual)
