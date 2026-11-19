#!/usr/bin/env python3
"""
Compute R2 statistics, contact sheets, and expected counting table.
Build rules:
(a) Cluster WITHIN each class only.
(b) Dihedral perceptual hash, Hamming distance 8 within class (10 for tomato).
(c) Cross-class conflicting only if same photo appears in two different class folders
    at Hamming distance <= 2. Exclude only those. Print 10 example conflicting pairs.
(d) Split clusters 80/20. Train on ALL photos of train clusters up to 640 per class.
    Validate on ONE random photo per validation cluster. Cap total at 800.
(e) Report per class: source files, clusters, removed same-label duplicates,
    conflicting files, train photos, validation photos.
    Mark SMALL if < 150 clusters or < 30 validation photos.
"""

import os
import sys
import json
import random
import time
from pathlib import Path
from collections import defaultdict
from concurrent.futures import ProcessPoolExecutor
import numpy as np
from PIL import Image, ImageOps, ImageDraw, ImageFont

SEED = 42
DATA_DIR = Path(r"C:\crop-data\crop_doctor_512")
BENCHMARKS_DIR = Path(r"C:\crop-doctor-offline\benchmarks")
CONTACT_SHEETS_DIR = BENCHMARKS_DIR / "contact_sheets"

CLASS_NAMES = [
    "rice_bacterial_leaf_blight",
    "rice_leaf_blast",
    "rice_sheath_blight",
    "rice_healthy",
    "cotton_bacterial_blight",
    "cotton_fusarium_wilt",
    "cotton_leaf_curl_virus",
    "cotton_healthy",
    "chilli_leaf_curl_virus",
    "chilli_nutrition_deficiency",
    "chilli_healthy",
    "tomato_leaf_blight",
    "tomato_yellow_leaf_curl_virus",
    "tomato_septoria_leaf_spot",
    "tomato_healthy",
    "maize_gray_leaf_spot",
    "maize_common_rust",
    "maize_northern_leaf_blight",
    "maize_healthy",
    "weeds_not_a_crop",
]

def get_dct_matrix(N=32):
    n = np.arange(N)
    k = np.arange(N)[:, None]
    return 2.0 * np.cos(np.pi * k * (2 * n + 1) / (2.0 * N))

DCT_MAT = get_dct_matrix(32)

def phash_64(im, hash_size=8, highfreq_factor=4):
    img_size = hash_size * highfreq_factor
    g = im.convert('L').resize((img_size, img_size), Image.Resampling.LANCZOS)
    pix = np.asarray(g, dtype=np.float64)
    dct = DCT_MAT @ pix @ DCT_MAT.T
    dctlow = dct[:hash_size, :hash_size]
    med = np.median(dctlow)
    diff = dctlow > med
    h = 0
    for b in diff.flatten():
        h = (h << 1) | int(b)
    return h

def compute_dihedral_phash(path_str):
    try:
        with Image.open(path_str) as im0:
            im = ImageOps.exif_transpose(im0).convert('RGB')
            m = ImageOps.mirror(im)
            variants = [
                im,
                im.rotate(90, expand=True),
                im.rotate(180, expand=True),
                im.rotate(270, expand=True),
                m,
                m.rotate(90, expand=True),
                m.rotate(180, expand=True),
                m.rotate(270, expand=True),
            ]
            return min(phash_64(v) for v in variants)
    except Exception as e:
        print(f"Error hashing {path_str}: {e}")
        return 0

def hash_batch(paths):
    return [compute_dihedral_phash(p) for p in paths]

class UnionFind:
    def __init__(self, n):
        self.parent = list(range(n))
        self.rank = [0] * n
    def find(self, x):
        while self.parent[x] != x:
            self.parent[x] = self.parent[self.parent[x]]
            x = self.parent[x]
        return x
    def union(self, a, b):
        ra, rb = self.find(a), self.find(b)
        if ra == rb: return
        if self.rank[ra] < self.rank[rb]: ra, rb = rb, ra
        self.parent[rb] = ra
        if self.rank[ra] == self.rank[rb]: self.rank[ra] += 1

def main():
    CONTACT_SHEETS_DIR.mkdir(parents=True, exist_ok=True)
    random.seed(SEED)
    rng = random.Random(SEED)

    print("Collecting files per class...")
    class_files = {}
    all_records = []
    for c in CLASS_NAMES:
        c_dir = DATA_DIR / c
        if not c_dir.exists():
            print(f"Warning: {c_dir} does not exist!")
            continue
        files = sorted([f for f in c_dir.iterdir() if f.is_file()])
        class_files[c] = files
        for f in files:
            all_records.append({
                "class_name": c,
                "file_name": f.name,
                "path": str(f),
            })
    print(f"Total files: {len(all_records)} across {len(class_files)} classes.")

    # 1. Compute hashes in parallel
    print("Computing dihedral perceptual hashes in parallel...")
    t0 = time.time()
    paths = [r["path"] for r in all_records]
    batch_size = 200
    batches = [paths[i:i + batch_size] for i in range(0, len(paths), batch_size)]
    
    num_workers = min(12, os.cpu_count() or 4)
    with ProcessPoolExecutor(max_workers=num_workers) as executor:
        results = list(executor.map(hash_batch, batches))
    
    hashes = [h for sub in results for h in sub]
    for r, h in zip(all_records, hashes):
        r["hash"] = h
    print(f"Hashes computed in {time.time() - t0:.2f} s.")

    # 2. Check cross-class conflicts: Hamming distance <= 2 across different classes
    print("\nChecking cross-class conflicts (Hamming distance <= 2 across different classes)...")
    hash_arr = np.array(hashes, dtype=np.uint64)
    class_indices = {c: [i for i, r in enumerate(all_records) if r["class_name"] == c] for c in CLASS_NAMES}
    
    conflicting_indices = set()
    conflicting_pairs_by_class = defaultdict(list)

    # For each class, compare its hashes against all other classes
    for c1_idx, c1 in enumerate(CLASS_NAMES):
        idx1 = class_indices[c1]
        if not idx1: continue
        h1 = hash_arr[idx1]
        
        for c2_idx in range(c1_idx + 1, len(CLASS_NAMES)):
            c2 = CLASS_NAMES[c2_idx]
            idx2 = class_indices[c2]
            if not idx2: continue
            h2 = hash_arr[idx2]
            
            # XOR diff
            diff = h1[:, None] ^ h2[None, :]
            # bit count
            # in numpy, count bits in uint64
            # bit_count is available in python int or unpackbits
            # Let's do bitwise count using popcount logic in numpy
            v = diff
            v = v - ((v >> np.uint64(1)) & np.uint64(0x5555555555555555))
            v = (v & np.uint64(0x3333333333333333)) + ((v >> np.uint64(2)) & np.uint64(0x3333333333333333))
            v = (v + (v >> np.uint64(4))) & np.uint64(0x0F0F0F0F0F0F0F0F)
            dist_matrix = (v * np.uint64(0x0101010101010101)) >> np.uint64(56)
            
            # matches <= 2
            matches = np.argwhere(dist_matrix <= 2)
            for m in matches:
                i1 = idx1[m[0]]
                i2 = idx2[m[1]]
                d = int(dist_matrix[m[0], m[1]])
                conflicting_indices.add(i1)
                conflicting_indices.add(i2)
                r1, r2 = all_records[i1], all_records[i2]
                conflicting_pairs_by_class[c1].append((r1["file_name"], c2, r2["file_name"], d))
                conflicting_pairs_by_class[c2].append((r2["file_name"], c1, r1["file_name"], d))

    print(f"Total conflicting files excluded: {len(conflicting_indices)}")
    for c in CLASS_NAMES:
        pairs = conflicting_pairs_by_class[c]
        if pairs:
            print(f"  Class {c}: {len(pairs)} conflict occurrences (example first 2: {pairs[:2]})")

    # 3. Within-class clustering (Hamming <= 8, or <= 10 for tomato)
    print("\nClustering within each class (Hamming <= 8; tomato <= 10)...")
    class_stats = []
    
    for c in CLASS_NAMES:
        c_records = [r for i, r in enumerate(all_records) if r["class_name"] == c]
        source_count = len(c_records)
        c_conflicts = [r for i, r in enumerate(all_records) if r["class_name"] == c and i in conflicting_indices]
        conflict_count = len(c_conflicts)
        
        # Surviving records for this class
        surviving = [r for i, r in enumerate(all_records) if r["class_name"] == c and i not in conflicting_indices]
        
        threshold = 10 if c.startswith("tomato_") else 8
        
        # Cluster surviving
        N = len(surviving)
        if N == 0:
            class_stats.append({
                "class": c,
                "source_files": source_count,
                "clusters": 0,
                "removed_duplicates": 0,
                "conflicting_files": conflict_count,
                "train_photos": 0,
                "val_photos": 0,
                "small": True
            })
            continue
            
        uf = UnionFind(N)
        s_hashes = np.array([r["hash"] for r in surviving], dtype=np.uint64)
        
        diff = s_hashes[:, None] ^ s_hashes[None, :]
        v = diff
        v = v - ((v >> np.uint64(1)) & np.uint64(0x5555555555555555))
        v = (v & np.uint64(0x3333333333333333)) + ((v >> np.uint64(2)) & np.uint64(0x3333333333333333))
        v = (v + (v >> np.uint64(4))) & np.uint64(0x0F0F0F0F0F0F0F0F)
        dist_matrix = (v * np.uint64(0x0101010101010101)) >> np.uint64(56)
        
        matches = np.argwhere(dist_matrix <= threshold)
        for m in matches:
            if m[0] < m[1]:
                uf.union(m[0], m[1])
                
        clusters = defaultdict(list)
        for idx in range(N):
            clusters[uf.find(idx)].append(surviving[idx])
            
        cluster_list = list(clusters.values())
        cluster_count = len(cluster_list)
        
        # Split clusters 80/20
        # RNG seeded
        rng.shuffle(cluster_list)
        if cluster_count >= 2:
            n_val_clusters = max(1, int(round(0.20 * cluster_count)))
            n_val_clusters = min(n_val_clusters, cluster_count - 1)
        else:
            n_val_clusters = 0
            
        val_clusters = cluster_list[:n_val_clusters]
        train_clusters = cluster_list[n_val_clusters:]
        
        # Train on ALL photos of train clusters up to 640
        train_photos_all = [p for cl in train_clusters for p in cl]
        rng.shuffle(train_photos_all)
        train_photos = train_photos_all[:640]
        
        # Validate on ONE random photo per validation cluster
        val_photos = [rng.choice(cl) for cl in val_clusters]
        
        # Cap class total at 800: train <= 640, val <= 160
        # Total kept photos = len(train_photos) + len(val_photos)
        # Removed same-label duplicates:
        # Source files - conflicting files - (train_photos + val_photos)
        removed_same_label = source_count - conflict_count - (len(train_photos) + len(val_photos))
        
        is_small = (cluster_count < 150) or (len(val_photos) < 30)
        
        class_stats.append({
            "class": c,
            "source_files": source_count,
            "clusters": cluster_count,
            "removed_duplicates": removed_same_label,
            "conflicting_files": conflict_count,
            "train_photos": len(train_photos),
            "val_photos": len(val_photos),
            "total_kept": len(train_photos) + len(val_photos),
            "small": is_small
        })

    # Print Table
    print("\n" + "="*110)
    print(f"{'Class':<30} | {'Source':<7} | {'Clusters':<8} | {'SameDup':<7} | {'Conflict':<8} | {'Train':<6} | {'Val':<5} | {'Kept':<6} | {'Status'}")
    print("="*110)
    total_src = sum(s["source_files"] for s in class_stats)
    total_cl = sum(s["clusters"] for s in class_stats)
    total_dup = sum(s["removed_duplicates"] for s in class_stats)
    total_conf = sum(s["conflicting_files"] for s in class_stats)
    total_tr = sum(s["train_photos"] for s in class_stats)
    total_va = sum(s["val_photos"] for s in class_stats)
    total_kp = sum(s["total_kept"] for s in class_stats)
    
    for s in class_stats:
        status = "SMALL" if s["small"] else "OK"
        print(f"{s['class']:<30} | {s['source_files']:<7} | {s['clusters']:<8} | {s['removed_duplicates']:<7} | {s['conflicting_files']:<8} | {s['train_photos']:<6} | {s['val_photos']:<5} | {s['total_kept']:<6} | {status}")
    print("="*110)
    print(f"{'TOTAL':<30} | {total_src:<7} | {total_cl:<8} | {total_dup:<7} | {total_conf:<8} | {total_tr:<6} | {total_va:<5} | {total_kp:<6} |")
    print("="*110)

    # 4. Generate contact sheets (30 random photos per class, 6x5 montage)
    print("\nGenerating contact sheets (30 photos per class montage)...")
    for c in CLASS_NAMES:
        files = class_files[c]
        sample_files = rng.sample(files, min(30, len(files)))
        # Create a 6 cols x 5 rows grid of 160x160 thumbnails
        cols, rows = 6, 5
        cell_w, cell_h = 160, 160
        montage = Image.new("RGB", (cols * cell_w, rows * cell_h + 40), color=(240, 240, 240))
        draw = ImageDraw.Draw(montage)
        draw.text((10, 10), f"Class: {c} (30 samples out of {len(files)})", fill=(0, 0, 0))
        
        for idx, f in enumerate(sample_files):
            r_idx = idx // cols
            c_idx = idx % cols
            x = c_idx * cell_w
            y = 40 + r_idx * cell_h
            try:
                with Image.open(f) as img:
                    im = ImageOps.exif_transpose(img).convert("RGB")
                    im.thumbnail((cell_w - 4, cell_h - 4), Image.Resampling.LANCZOS)
                    # paste centered
                    px = x + (cell_w - im.width) // 2
                    py = y + (cell_h - im.height) // 2
                    montage.paste(im, (px, py))
            except Exception as e:
                print(f"Error loading {f}: {e}")
                
        out_path = CONTACT_SHEETS_DIR / f"{c}.jpg"
        montage.save(out_path, quality=85)
        print(f"  Saved {out_path.name}")
        
    print("\nDone!")

if __name__ == "__main__":
    main()
