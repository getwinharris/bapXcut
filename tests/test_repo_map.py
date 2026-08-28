from pathlib import Path
import tempfile
import unittest
from generate_repo_map_mmd import SOURCE, generate_mermaid


class RepoMapTest(unittest.TestCase):
    def test_deterministic_references_and_layer_classification(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / SOURCE
            (source / 'viewmodels').mkdir(parents=True)
            (source / 'viewmodels/Editor.kt').write_text('class Editor')
            (source / 'MainActivity.kt').write_text('class MainActivity { val model: Editor }')
            (source / 'Unrelated.kt').write_text('class Unrelated { val x: EditorFactory }')
            result = generate_mermaid(root)
            self.assertIn('subgraph UI\n        MainActivity[MainActivity]', result)
            self.assertIn('MainActivity --> Editor', result)
            self.assertNotIn('Unrelated --> Editor', result)
            self.assertEqual(result, generate_mermaid(root))

    def test_missing_sources_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                generate_mermaid(Path(tmp))

    def test_duplicate_node_names_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for folder in ('models', 'services'):
                p = root / SOURCE / folder
                p.mkdir(parents=True)
                (p / 'Duplicate.kt').write_text('class Duplicate')
            with self.assertRaises(ValueError):
                generate_mermaid(root)
