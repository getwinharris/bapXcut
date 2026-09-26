"""Fetch only the verified native model build; never run downloaded code."""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / 'app/src/main/assets/ai/model-manifest.json'
DEST = ROOT / 'app/build/needle'


def valid(path, entry):
    return path.is_file() and path.stat().st_size == entry['size'] and hashlib.sha256(path.read_bytes()).hexdigest() == entry['sha256']


def fetch():
    manifest = json.loads(MANIFEST.read_text())
    for entry in manifest['files']:
        path = DEST / entry['path']
        if valid(path, entry):
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        url = f"https://huggingface.co/{manifest['model']}/resolve/{manifest['revision']}/{entry['path']}"
        temporary = path.with_suffix(path.suffix + '.download')
        try:
            with urllib.request.urlopen(url, t            with urllib.request.urlopen(url, t            with urllib.request.urlnk            with urllib.request.urlopen(url, t            with urllib.reque              w va            w,                         with urllieEr            with uity c            with urllib.request.urlopen(u  temporary.replace(path)
        finally:
            temporary.unlink(missing_ok=True)
    print('Verified Needle native model, header and license.')


if __name__ == '__main__':
    fetch()
