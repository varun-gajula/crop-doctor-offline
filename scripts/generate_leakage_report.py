#!/usr/bin/env python3
"""
Crop Doctor - Generate Leakage Report Locally (Rule H3)
-------------------------------------------------------
Computes dihedral perceptual hashes, performs baseline 80/20 stratified split,
and audits near-duplicate leakage (Hamming distance <= 6) across all 25 classes.
Outputs: leakage_report.json and benchmarks/leakage_report.json
"""

import os
import sys
import json
import time
import random
from pathlib import Path
from concurrent.futures import ProcessPoolExecutor
import numpy as np
from PIL import Image, ImageOps
import yaml

def compute_dhash_64(image, hash_size=8):
    gray = image.convert('L').resize((hash_size + 1, hash_size), Image.Resampling.LANCZOS)
    pixels = np.array(gray)
    diff = pixels[:, 1:] > pixels[:, :-1]
    h = 0
    for bit in diff.flatten():
        h = (h << 1) | int(bit)
    return h

def get_canonical_dihedral_hash(img_path):
    try:
        with Image.open(img_path) as img:
            img = img.convert('RGB')
            variants = [
                img,
                img.rotate(90, expand=True),
                img.rotate(180, expand=True),
                img.rotate(270, expand=True),
                ImageOps.mirror(img),
                ImageOps.mirror(img).rotate(90, expand=True),
                ImageOps.mirror(img).rotate(180, expand=True),
                ImageOps.mirror(img).rotate(270, expand=True)
            ]
            return min(compute_dhash_64(v) for v in variants)
    except Exception:
        return 0

def hamming_dist(h1, h2):
    return bin(h1 ^ h2).count('1')

def hash_batch(paths):
    return [get_canonical_dihedral_hash(p) for p in paths]

def main():
    repo_root = Path(__file__).resolve().parent.parent
    data_dir = Path("C:/crop-data/small")
    class_map_path = repo_root / "data" / "class_map.yaml"

    if not data_dir.exists():
        data_dir = Path("C:/crop-data/by_class")

    print(f"Loading class map from: {class_map_path}")
    with open(class_map_path, "r", encoding="utf-8") as f:
        class_meta = yaml.safe_load(f)["classes"]

    valid_exts = {".jpg", ".jpeg", ".png", ".webp"}
    samples = []

    print(f"Scanning dataset from: {data_dir}")
    for idx, info in class_meta.items():
        cid = info["class_id"]
        cdir = data_dir / cid
        if not cdir.exists():
            matches = list(data_dir.glob(f"{idx:02d}_*"))
            if matches:
                cdir = matches[0]
            else:
                continue

        for f in cdir.rglob("*"):
            if f.is_file() and f.suffix.lower() in valid_exts:
                samples.append({
                    "path": str(f),
                    "class_idx": idx,
                    "class_id": cid
                })

    print(f"Total valid images scanned: {len(samples)}")
    if len(samples) == 0:
        print("ERROR: No images found!")
        sys.exit(1)

    # Multi-core perceptual hashing
    t0 = time.time()
    num_workers = min(12, os.cpu_count() or 4)
    print(f"Computing dihedral perceptual hashes across {num_workers} CPU worker processes...")
    
    batch_size = 200
    batches = [samples[i:i + batch_size] for i in range(0, len(samples), batch_size)]
    batch_paths = [[s["path"] for s in b] for b in batches]

    with ProcessPoolExecutor(max_workers=num_workers) as executor:
        results = list(executor.map(hash_batch, batch_paths))

    all_hashes = [h for sublist in results for h in sublist]
    for s, h in zip(samples, all_hashes):
        s["phash"] = h

    elapsed = time.time() - t0
    print(f"Done computing hashes in {elapsed:.1f}s ({len(samples)/elapsed:.1f} img/s)!")

    # Baseline 80/20 Stratified Split with random_state=42
    rng = random.Random(42)
    train_samples = []
    val_samples = []

    for c_idx in sorted(list(set(s["class_idx"] for s in samples))):
        c_samples = [s for s in samples if s["class_idx"] == c_idx]
        rng.shuffle(c_samples)
        split_idx = int(len(c_samples) * 0.80)
        train_samples.extend(c_samples[:split_idx])
        val_samples.extend(c_samples[split_idx:])

    print(f"Split completed: Train={len(train_samples)}, Val={len(val_samples)}")

    # Audit near-duplicate leakage
    leakage_per_class = {}
    total_leaked_val = 0
    total_val_count = len(val_samples)

    print("\nAuditing near-duplicate leakage in baseline random split (seed 42)...")
    for c_idx in sorted(list(set(s["class_idx"] for s in samples))):
        train_hashes = [s["phash"] for s in train_samples if s["class_idx"] == c_idx]
        val_c_samples = [s for s in val_samples if s["class_idx"] == c_idx]
        leaked_in_class = 0
        for vs in val_c_samples:
            vh = vs["phash"]
            if any(hamming_dist(vh, th) <= 6 for th in train_hashes):
                leaked_in_class += 1

        cid = val_c_samples[0]["class_id"] if val_c_samples else str(c_idx)
        val_c_count = len(val_c_samples)
        pct = (leaked_in_class / val_c_count * 100.0) if val_c_count > 0 else 0.0
        leakage_per_class[cid] = {
            "class_idx": c_idx,
            "val_photos": val_c_count,
            "leaked_photos": leaked_in_class,
            "leakage_percentage": round(pct, 2)
        }
        total_leaked_val += leaked_in_class
        print(f"  Class {c_idx:02d} ({cid[:25]:<25}): {leaked_in_class:3d} / {val_c_count:3d} val photos leaked ({pct:5.1f}%)")

    overall_leak_pct = (total_leaked_val / total_val_count * 100.0) if total_val_count > 0 else 0.0
    leakage_report = {
        "description": "Near-duplicate leakage report for baseline random 80/20 split (seed 42)",
        "total_val_photos": total_val_count,
        "total_leaked_val_photos": total_leaked_val,
        "overall_leakage_percentage": round(overall_leak_pct, 2),
        "hamming_distance_threshold": 6,
        "leakage_by_class": leakage_per_class
    }

    out_file1 = repo_root / "leakage_report.json"
    out_file2 = repo_root / "benchmarks" / "leakage_report.json"
    out_file2.parent.mkdir(parents=True, exist_ok=True)

    with open(out_file1, "w", encoding="utf-8") as f:
        json.dump(leakage_report, f, indent=2)
    with open(out_file2, "w", encoding="utf-8") as f:
        json.dump(leakage_report, f, indent=2)

    print(f"\n=== LEAKAGE AUDIT SUMMARY ===")
    print(f"Total validation photos:     {total_val_count}")
    print(f"Photos with train duplicate: {total_leaked_val}")
    print(f"Overall Leakage Rate:        {overall_leak_pct:.2f}%")
    print(f"[SUCCESS] Saved leakage report to:")
    print(f"  1. {out_file1}")
    print(f"  2. {out_file2}")

if __name__ == "__main__":
    main()
