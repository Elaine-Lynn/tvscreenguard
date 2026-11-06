"""Standalone extraction invariants, translations, asset integrity and APK boundaries."""
from pathlib import Path
import xml.etree.ElementTree as E
import json,hashlib
r=Path(__file__).resolve().parents[1];src=r/'app/src/main';a='{http://schemas.android.com/apk/res/android}'
m=E.parse(src/'AndroidManifest.xml').getroot()
build=(r/'app/build.gradle').read_text()
assert "applicationId 'com.tv.screenguard'" in build
assert "namespace 'com.tv.screenguard'" in build
assert "buildConfigField 'boolean', 'TIMER_TEST_OPTION', 'false'" in build
assert E.parse(src/'res/values/strings.xml').getroot().find("string[@name='app_name']").text == 'TV ScreenGuard'
assert E.parse(src/'res/values-zh-rCN/strings.xml').getroot().find("string[@name='app_name']").text == '电视防沉迷系统'
assert [x.get(a+'name') for x in m.findall('uses-permission')]==['android.permission.INTERNET']
assert not m.findall('.//category[@'+a+'name="android.intent.category.HOME"]')
for p in (src/'java').rglob('*.java'):
 s=p.read_text();assert all(x not in s for x in ['com.gentv.tv','MainActivity','OpenOnStart','DesktopStore']),p
base=E.parse(src/'res/values/child_lock_strings.xml').getroot();zh=E.parse(src/'res/values-zh-rCN/child_lock_strings.xml').getroot()
assert {e.get('name') for e in base}=={e.get('name') for e in zh}
assets=src/'assets/child-questions';manifest=json.loads((assets/'manifest.json').read_text())
print('PASS manifest permissions, independent entry, test timer build isolation and localization parity')
for name,meta in manifest['files'].items():
 data=(assets/name).read_bytes();assert len(data)==meta['bytes'] and hashlib.sha256(data).hexdigest()==meta['sha256'],name
print('PASS all offline pack SHA-256 checksums')
for p in assets.glob('*.jsonl'):
 for line in p.read_text().splitlines():json.loads(line)
print('PASS all offline question JSONL parses')
assert (r/'LICENSE').exists() and (r/'THIRD_PARTY_NOTICES.md').exists()
print('PASS inherited licence and notices retained')
