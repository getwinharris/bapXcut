"""Generate a deterministic Kotlin file-reference overview, not a runtime call graph."""
import argparse
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
SOURCE = Path('app/src/main/java/com/getwinharris/bapxcut')
LAYERS = {'UI': {'activities', 'customviews'}, 'VM': {'viewmodels'},
          'Service': {'services'}, 'Model': {'models'}, 'Command': {'commands'}}
IGNORE = {'ScratchTest.kt', 'ViewExtensions.kt', 'ErrorCode.kt'}


def generate_mermaid(root=ROOT):
    source = root / SOURCE
    files = sorted(p for p in source.rglob('*.kt') if p.name not in IGNORE)
    if not files:
        raise ValueError(f'No Kotlin sources found at {source}')
    nodes = {}
    layers = {name: [] for name in (*LAYERS, 'Other')}
    for path in files:
        name = path.stem
        if not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', name) or name in nodes:
            raise ValueError(f'Invalid or duplicate map node: {name}')
        nodes[name] = path
        folders = set(path.relative_to(source).parts[:-1])
        layer = next((key for key, values in LAYERS.items() if folders & values), 'Other')
        if layer == 'Other' and name.endswith('Activity'):
            layer = 'UI'
        layers[layer].append(name)
    lines = ['graph TD', '    %% Auto-generated bapXcut Kotlin file-reference overview.',
             '    %% Lexical references may include comments or strings; not a runtime call graph.']
    for layer, names in layers.items():
        if names:
            lines.append(f'    subgraph {layer}')
            lines.extend(f'        {name}[{name}]' for name in sorted(names))
            lines.append('    end')
    for name, path in sorted(nodes.items()):
        tokens = set(re.findall(r'\b[A-Za-z_][A-Za-z0-9_]*\b', path.read_text()))
        lines.extend(f'    {name} --> {other}' for other in sorted(tokens & nodes.keys()) if other != name)
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Fail if map.mmd differs; do not write')
    args = parser.parse_args()
    expected = generate_mermaid()
    target = ROOT / 'map.mmd'
    if args.check:
        if not target.exists() or target.read_text() != expected:
            parser.exit(1, 'map.mmd is stale; run python3 generate_repo_map_mmd.py\n')
        print('map.mmd is current.')
    else:
        target.write_text(expected)
        print('map.mmd generated successfully.')


if __name__ == '__main__':
    main()
