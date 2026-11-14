#!/usr/bin/env python3
import json
import sys

def check_syntax(notebook_path):
    with open(notebook_path, 'r', encoding='utf-8') as f:
        nb = json.load(f)

    print(f"Checking notebook: {notebook_path} ({len(nb['cells'])} cells)")
    errors = 0
    for idx, cell in enumerate(nb['cells']):
        ctype = cell['cell_type']
        if ctype == 'code':
            source = ''.join(cell['source'])
            lines = [
                line if not line.strip().startswith('!') and not line.strip().startswith('%')
                else '# ' + line
                for line in source.splitlines()
            ]
            clean_code = '\n'.join(lines)
            try:
                compile(clean_code, f'Cell_{idx}', 'exec')
                print(f"  Cell {idx:02d} [CODE]: SYNTAX OK ({len(lines)} lines)")
            except SyntaxError as e:
                print(f"  Cell {idx:02d} [CODE]: SYNTAX ERROR on line {e.lineno}: {e.msg}")
                errors += 1
        else:
            first_line = cell['source'][0].strip() if cell['source'] else ''
            print(f"  Cell {idx:02d} [MD]:   OK ('{first_line[:40]}...')")

    if errors == 0:
        print("\nAll notebook cells passed Python syntax verification successfully!")
        return 0
    else:
        print(f"\nFound {errors} syntax error(s)!")
        return 1

if __name__ == '__main__':
    path = sys.argv[1] if len(sys.argv) > 1 else 'notebooks/03_grouped_split_and_retrain.ipynb'
    sys.exit(check_syntax(path))
