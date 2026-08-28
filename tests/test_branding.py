from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


class BrandingTest(unittest.TestCase):
    def test_all_declared_localized_app_names_use_brand(self):
        for path in (ROOT / 'app/src/main/res').glob('values*/strings.xml'):
            with self.subTest(locale=path.parent.name):
                for item in ET.parse(path).getroot().findall("string[@name='app_name']"):
                    self.assertEqual('bapXcut', item.text)

    def test_no_upstream_identity_in_runtime_sources(self):
        for path in (ROOT / 'app/src').rglob('*'):
            if path.suffix not in {'.kt', '.java', '.xml'}:
                continue
            with self.subTest(path=str(path.relative_to(ROOT))):
                self.assertIsNone(re.search(r'librecuts|tharunbirla', path.read_text(), re.I))
