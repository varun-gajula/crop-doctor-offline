#!/usr/bin/env python3
import json
import csv
from collections import Counter, defaultdict

def main():
    # 1. Manifest photo sources
    with open('benchmarks/p4_probe_manifest.json') as f:
        manifest = json.load(f)

    print("=== Photo Source Breakdown (from p4_probe_manifest.json) ===")
    for s in ['supported', 'unsupported', 'screen', 'nocrop', 'othercrop']:
        items = [m for m in manifest if m['set'] == s]
        sources = Counter()
        for m in items:
            src = m['source']
            if 'real_leaves' in src or 'realtest\\screen' in src or 'Camera imports' in src:
                sources['real phone photo'] += 1
            elif 'crop_doctor_512' in src or 'plantdoc' in src or 'rice shiva' in src or 'cotton' in src or 'Chili Leaf' in src or 'CCMT' in src:
                sources['web or dataset image'] += 1
            else:
                sources['other'] += 1
        print(f"Set '{s}' (total {len(items)}): {sources['real phone photo']} real phone photos, {sources['web or dataset image']} web or dataset images")

    # 2. Option B evaluation on p4_decision_table.csv
    with open('benchmarks/p4_decision_table.csv', encoding='utf-8') as f:
        rows = list(csv.DictReader(f))

    eth = -8.0
    cm = 0.60
    mg = 0.25
    bl = -9.0
    conf = 0.3434087

    sup_rows = [r for r in rows if r['set'] == 'supported']

    diag_rows = []
    dec_counts = Counter()
    per_class_total = Counter()
    per_class_diag = Counter()
    per_class_correct = Counter()

    for r in sup_rows:
        tc = r['true_class']
        per_class_total[tc] += 1
        reason = r['decision_reason']
        leaf_fail = 'LEAF_VALIDATOR_FAIL' in reason
        weeds_fail = 'GLOBAL_TOP1_WEEDS' in reason
        e = float(r['energy'])
        p1 = float(r['top1_prob'])
        m_c = float(r['crop_mass'])
        margin = float(r['margin'])

        if leaf_fail or weeds_fail or e > eth or p1 < conf:
            dec = "CAN'T TELL"
        elif m_c < cm or (margin < mg and e >= bl):
            dec = 'OUT OF CLASS'
        else:
            dec = 'DIAGNOSE'

        dec_counts[dec] += 1
        if dec == 'DIAGNOSE':
            diag_rows.append(r)
            per_class_diag[tc] += 1
            pred = r['top-1']
            if pred == tc:
                per_class_correct[tc] += 1

    print(f"\n=== Option B Decisions on Supported Set (N={len(sup_rows)}) ===")
    for k, v in dec_counts.items():
        print(f"  {k}: {v} ({v/len(sup_rows)*100:.1f}%)")

    total_diag = len(diag_rows)
    total_correct = sum(per_class_correct.values())
    overall_acc = total_correct / total_diag * 100 if total_diag > 0 else 0
    print(f"\nOverall DIAGNOSE Accuracy: {total_correct}/{total_diag} ({overall_acc:.1f}%)\n")

    print("=== Per-Class Accuracy on DIAGNOSE (Correct / Diagnosed) ===")
    for tc in sorted(per_class_total.keys()):
        corr = per_class_correct[tc]
        d_cnt = per_class_diag[tc]
        tot = per_class_total[tc]
        pct = corr / d_cnt * 100 if d_cnt > 0 else 0.0
        print(f"  {tc}: {corr}/{d_cnt} ({pct:.1f}%) [Total in set: {tot}]")

if __name__ == '__main__':
    main()
