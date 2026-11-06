"""Reject private files, credentials and local machine/device identifiers in Git files."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode().split('\0')
blocked_dirs = {'keys', 'work', 'shots', 'backups', '.gradle', 'build'}
blocked_suffixes = {'.apk', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx', '.log'}
# Assemble signatures so this scanner does not flag its own source.
patterns = [
    re.compile(rb'-----BEGIN ' + rb'(?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    re.compile(rb'(?:gh[pousr]_' + rb'[A-Za-z0-9]{30,}|github_pat_' + rb'[A-Za-z0-9_]{40,})'),
    re.compile(rb'(?:AKIA|ASIA)' + rb'[A-Z0-9]{16}'),
    re.compile(rb'/(?:Users|home)/' + rb'[A-Za-z0-9_.-]+/'),
    re.compile(rb'\b10\.' + rb'\d{1,3}\.\d{1,3}\.\d{1,3}\b'),
    re.compile(rb'\b192\.168\.' + rb'\d{1,3}\.\d{1,3}\b'),
]
failures = []
for relative in filter(None, paths):
    path = root / relative
    if not path.exists():
        continue
    if path.is_symlink():
        failures.append((relative, 'symlink requires review'))
        continue
    if (blocked_dirs.intersection(path.relative_to(root).parts)
            or path.suffix.lower() in blocked_suffixes
            or path.name in {'local.properties', 'signing.properties', '.env'}
            or path.name.startswith(('adbkey', '.env.'))):
        failures.append((relative, 'private/generated file'))
        continue
    data = path.read_bytes()
    if any(pattern.search(data) for pattern in patterns):
        failures.append((relative, 'credential/local identifier signature'))
if failures:
    for name, reason in failures:
        print(f'FAIL {name}: {reason}')
    raise SystemExit(1)
print('PASS public Git files: no private artifacts, known credential signatures or local paths/device IPs')
