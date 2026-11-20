#!/usr/bin/env python3
"""
tools/analyze_p4_distributions.py
Analyzes benchmarks/p4_decision_table.csv and sweeps candidate thresholds
for crop_mass, margin, and energy to meet the targets:
1. Supported: >= 90% DIAGNOSE or CAN'T TELL (< 10% OUT OF CLASS).
2. Nocrop: >= 80% CAN'T TELL.
3. Othercrop: >= 60% OUT OF CLASS or CAN'T TELL.
4. Unsupported: Report share wrongly DIAGNOSED.
5. Screen photos: Report decisions and compare energy distribution to real photos.
6. The two P2 wrong-crop cases (camera_real_11, camera_real_17).
"""

import csv
import json
import numpy as np
from pathlib import Path
from collections import defaultdict, Counter

CSV_PATH = Path("benchmarks/p4_decision_table.csv")

def load_data():
    rows = []
    with open(CSV_PATH, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append({
                "file": r["file"],
                "set": r["set"],
                "selected_crop": r["selected crop"],
                "energy": float(r["energy"]),
                "crop_mass": float(r["crop_mass"]),
                "top1": r["top-1"],
                "top2": r["top-2"],
                "margin": float(r["margin"]),
                "top1_prob": float(r["top1_prob"]),
                "top2_prob": float(r["top2_prob"]),
                "decision": r["decision"],
                "decision_reason": r["decision_reason"],
                "true_crop": r["true_crop"],
                "true_class": r["true_class"]
            })
    return rows

def print_distribution(name, values):
    arr = np.array(values)
    print(f"--- {name} (N={len(arr)}) ---")
    print(f"  Min:    {np.min(arr):.4f}")
    print(f"  P10:    {np.percentile(arr, 10):.4f}")
    print(f"  P25:    {np.percentile(arr, 25):.4f}")
    print(f"  Median: {np.median(arr):.4f}")
    print(f"  Mean:   {np.mean(arr):.4f}")
    print(f"  P75:    {np.percentile(arr, 75):.4f}")
    print(f"  P90:    {np.percentile(arr, 90):.4f}")
    print(f"  P95:    {np.percentile(arr, 95):.4f}")
    print(f"  Max:    {np.max(arr):.4f}")

def evaluate_rules(rows, energy_thresh, conf_thresh, crop_mass_thresh, margin_thresh, borderline_energy):
    by_set = defaultdict(list)
    for r in rows:
        reason = r["decision_reason"]
        leaf_fail = "LEAF_VALIDATOR_FAIL" in reason
        weeds_fail = "GLOBAL_TOP1_WEEDS" in reason

        e = r["energy"]
        p1 = r["top1_prob"]
        m_c = r["crop_mass"]
        margin = r["margin"]

        if leaf_fail:
            dec = "CAN'T TELL"
            code = "LEAF_VALIDATOR_FAIL"
        elif weeds_fail:
            dec = "CAN'T TELL"
            code = "GLOBAL_TOP1_WEEDS"
        elif energy_thresh is not None and e > energy_thresh:
            dec = "CAN'T TELL"
            code = "HIGH_ENERGY"
        elif p1 < conf_thresh:
            dec = "CAN'T TELL"
            code = "LOW_CONFIDENCE"
        elif m_c < crop_mass_thresh:
            dec = "OUT OF CLASS"
            code = "LOW_CROP_MASS"
        elif margin < margin_thresh and e >= borderline_energy:
            dec = "OUT OF CLASS"
            code = "BORDERLINE_AMBIGUOUS"
        else:
            dec = "DIAGNOSE"
            code = "ACCEPTED"

        by_set[r["set"]].append((dec, code, r))

    return by_set

def main():
    rows = load_data()
    print(f"Loaded {len(rows)} decision rows.")

    # 1. Metric Distributions per Set
    sets = sorted(list(set(r["set"] for r in rows)))
    for s in sets:
        s_rows = [r for r in rows if r["set"] == s]
        print(f"\n================ SET: {s.upper()} ================")
        print_distribution(f"Free Energy: {s}", [r["energy"] for r in s_rows])
        print_distribution(f"Crop Mass: {s}", [r["crop_mass"] for r in s_rows])
        print_distribution(f"Within-crop Margin: {s}", [r["margin"] for r in s_rows])
        print_distribution(f"In-crop Top1 Prob: {s}", [r["top1_prob"] for r in s_rows])

    # 2. Compare Screen vs Supported Real Photos Energy
    real_supported = [r["energy"] for r in rows if r["set"] == "supported"]
    screen_photos = [r["energy"] for r in rows if r["set"] == "screen"]
    print("\n================ ENERGY COMPARISON: REAL vs SCREEN ================")
    print_distribution("Real Supported Leaves Energy", real_supported)
    print_distribution("Screen Photos Energy", screen_photos)

    # 3. Two P2 wrong-crop cases with Tomato selected
    print("\n================ P2 WRONG-CROP CASES (WITH TOMATO SELECTED) ================")
    for case_file in ["camera_real_11", "camera_real_17"]:
        case_rows = [r for r in rows if case_file in r["file"] and r["selected_crop"] == "tomato"]
        for cr in case_rows:
            print(f"File: {cr['file']}")
            print(f"  Selected Crop: {cr['selected_crop']}")
            print(f"  True Class:    {cr['true_class']}")
            print(f"  Energy:        {cr['energy']:.4f}")
            print(f"  Crop Mass:     {cr['crop_mass']:.4f}")
            print(f"  In-crop Top1:  {cr['top1']} (prob={cr['top1_prob']:.4f})")
            print(f"  In-crop Top2:  {cr['top2']} (prob={cr['top2_prob']:.4f})")
            print(f"  Margin:        {cr['margin']:.4f}")
            print(f"  Decision:      {cr['decision']} ({cr['decision_reason']})")

    # 4. Sweep Candidate Thresholds
    print("\n================ THRESHOLD SWEEP ================")
    conf_thresh = 0.3434087  # fixed from thresholds.json
    energy_cand = [-9.3050365, -8.0, -7.0, None]
    crop_mass_cands = [0.30, 0.40, 0.50, 0.60]
    margin_cands = [0.10, 0.15, 0.20, 0.25]
    borderline_cands = [-12.0, -11.0, -10.0, -9.0]

    best_configs = []

    for eth in energy_cand:
        for cm in crop_mass_cands:
            for mg in margin_cands:
                for bl in borderline_cands:
                    by_set = evaluate_rules(rows, eth, conf_thresh, cm, mg, bl)

                    # Rates
                    # Supported: % DIAGNOSE + CAN'T TELL
                    sup_decs = [d[0] for d in by_set["supported"]]
                    sup_diag_or_cant = sum(1 for d in sup_decs if d in ["DIAGNOSE", "CAN'T TELL"]) / len(sup_decs) * 100
                    sup_out_of_class = sum(1 for d in sup_decs if d == "OUT OF CLASS") / len(sup_decs) * 100
                    sup_diag = sum(1 for d in sup_decs if d == "DIAGNOSE") / len(sup_decs) * 100

                    # Nocrop: % CAN'T TELL
                    noc_decs = [d[0] for d in by_set["nocrop"]]
                    noc_cant = sum(1 for d in noc_decs if d == "CAN'T TELL") / len(noc_decs) * 100

                    # Othercrop: % OUT OF CLASS + CAN'T TELL
                    oth_decs = [d[0] for d in by_set["othercrop"]]
                    oth_out_or_cant = sum(1 for d in oth_decs if d in ["OUT OF CLASS", "CAN'T TELL"]) / len(oth_decs) * 100

                    # Unsupported: % wrongly DIAGNOSED
                    uns_decs = [d[0] for d in by_set["unsupported"]]
                    uns_diag = sum(1 for d in uns_decs if d == "DIAGNOSE") / len(uns_decs) * 100

                    # Targets:
                    # sup_diag_or_cant >= 90.0 (sup_out_of_class <= 10.0)
                    # noc_cant >= 80.0
                    # oth_out_or_cant >= 60.0
                    if sup_diag_or_cant >= 90.0 and noc_cant >= 80.0 and oth_out_or_cant >= 60.0:
                        best_configs.append({
                            "energy_thresh": eth,
                            "crop_mass_thresh": cm,
                            "margin_thresh": mg,
                            "borderline_energy": bl,
                            "sup_diag_or_cant": sup_diag_or_cant,
                            "sup_diag": sup_diag,
                            "sup_out_of_class": sup_out_of_class,
                            "noc_cant": noc_cant,
                            "oth_out_or_cant": oth_out_or_cant,
                            "uns_diag": uns_diag
                        })

    print(f"Found {len(best_configs)} valid configurations satisfying all 3 target constraints!")
    # Sort by: high sup_diag, high oth_out_or_cant, low uns_diag
    best_configs.sort(key=lambda c: (c["sup_diag"], c["oth_out_or_cant"], -c["uns_diag"]), reverse=True)

    print("\nTop 5 Candidate Configurations:")
    for idx, cfg in enumerate(best_configs[:5]):
        print(f"\n[Option {idx+1}]:")
        print(f"  energy_threshold:    {cfg['energy_thresh']}")
        print(f"  crop_mass_threshold: {cfg['crop_mass_thresh']}")
        print(f"  margin_threshold:    {cfg['margin_thresh']}")
        print(f"  borderline_energy:   {cfg['borderline_energy']}")
        print(f"  Supported:   {cfg['sup_diag_or_cant']:.1f}% DIAGNOSE or CAN'T TELL (DIAGNOSE={cfg['sup_diag']:.1f}%, OUT_OF_CLASS={cfg['sup_out_of_class']:.1f}%) [Target >=90%]")
        print(f"  Nocrop:      {cfg['noc_cant']:.1f}% CAN'T TELL [Target >=80%]")
        print(f"  Othercrop:   {cfg['oth_out_or_cant']:.1f}% OUT OF CLASS or CAN'T TELL [Target >=60%]")
        print(f"  Unsupported: {cfg['uns_diag']:.1f}% wrongly DIAGNOSED")

if __name__ == "__main__":
    main()
