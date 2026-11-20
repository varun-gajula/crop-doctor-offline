import json
import numpy as np

with open('benchmarks/p1_benchmark_raw.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for set_key, items in data.items():
    print(f"### {set_key} details (Total: {len(items)})")
    for x in items:
        status = x.get('decision')
        code = x.get('reason_code')
        energy = x.get('gate2_energy')
        prob = x.get('gate3_prob')
        cls = x.get('gate3_class')
        print(f"- {x['file_name']}: {status} ({code}) | Energy: {energy:.4f} | Prob: {prob:.4f} | Class: {cls}")
    print()
