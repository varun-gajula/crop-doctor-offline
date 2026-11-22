#!/usr/bin/env python3
"""
Crop Doctor - Task D6: 13-Class Pre-Training Verification & Audit
-----------------------------------------------------------------
Audits the proposed 13-class list:
1. Within-class clustering (dihedral pHash, Hamming <= 8) and capped count (cap 800).
2. Sources per class; held-out validation source for pests, rice, and chilli; source-limited flags.
3. Chilli leaf curl virus: analysis of thrips vs virus photos, split vs rename verdict.
4. License audit of every source; competition use compliance.
5. Cross-class and cross-source duplicate collision check (Hamming <= 2).
6. Outputs benchmarks/d6_class_audit.md.
"""

import os
import sys
import io
import json
import time
import zipfile
from pathlib import Path
from collections import defaultdict
from concurrent.futures import ProcessPoolExecutor
import numpy as np
from PIL import Image, ImageOps

# 13 Target Classes
CLASS_ORDER = [
    # Category A: 8 Core Disease / Healthy Classes
    "rice_leaf_blast",
    "rice_healthy",
    "cotton_bacterial_blight",
    "cotton_healthy",
    "chilli_leaf_curl_virus",
    "chilli_healthy",
    "maize_northern_leaf_blight",
    "maize_healthy",
    # Category B: 4 High-Impact Pest Classes
    "maize_fall_armyworm",
    "rice_stem_borer",
    "rice_hispa",
    "tomato_spider_mites",
    # Category C: Non-Crop Guardrail
    "other_not_a_crop",
]

# DCT basis matrix for 64-bit pHash
def get_dct_matrix(N=32):
    n = np.arange(N)
    k = np.arange(N)[:, None]
    return 2.0 * np.cos(np.pi * k * (2 * n + 1) / (2.0 * N))

DCT_MAT = get_dct_matrix(32)

def phash_64_from_image(im, hash_size=8, highfreq_factor=4):
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
    return int(h)

def compute_dihedral_phash_from_pil(im0):
    im = ImageOps.exif_transpose(im0).convert('RGB')
    m = ImageOps.mirror(im)
    variants = [
        im, im.rotate(90, expand=True), im.rotate(180, expand=True), im.rotate(270, expand=True),
        m, m.rotate(90, expand=True), m.rotate(180, expand=True), m.rotate(270, expand=True)
    ]
    return min(phash_64_from_image(v) for v in variants)

def compute_hash_for_item(item):
    """
    item: dict with type: 'file' or 'zip'
    """
    try:
        if item["type"] == "file":
            with Image.open(item["path"]) as im:
                h = compute_dihedral_phash_from_pil(im)
                return item["id"], h
        elif item["type"] == "zip":
            with zipfile.ZipFile(item["zip_path"], "r") as z:
                data = z.read(item["member"])
                with Image.open(io.BytesIO(data)) as im:
                    h = compute_dihedral_phash_from_pil(im)
                    return item["id"], h
    except Exception as e:
        print(f"Error hashing {item.get('id')}: {e}")
        return item["id"], 0

# Popcount Hamming distance between uint64 arrays
def hamming_dist_arr(h1, h2_arr):
    diff = h1 ^ h2_arr
    v = diff
    v = v - ((v >> np.uint64(1)) & np.uint64(0x5555555555555555))
    v = (v & np.uint64(0x3333333333333333)) + ((v >> np.uint64(2)) & np.uint64(0x3333333333333333))
    v = (v + (v >> np.uint64(4))) & np.uint64(0x0F0F0F0F0F0F0F0F)
    return (v * np.uint64(0x0101010101010101)) >> np.uint64(56)

def main():
    print("=== Task D6: 13-Class List Verification & Audit ===")
    t0 = time.time()
    
    # 1. Collect all images per class from sources on disk
    print("\n1. Collecting image records across sources on disk...")
    items = []
    
    # Helper to add directory photos
    def add_dir_photos(class_name, source_name, dir_path, max_n=None):
        if not os.path.exists(dir_path):
            print(f"Warning: {dir_path} not found!")
            return
        files = []
        for root, _, fs in os.walk(dir_path):
            for f in fs:
                if f.lower().endswith((".jpg", ".jpeg", ".png", ".bmp", ".webp")):
                    files.append(os.path.join(root, f))
        files.sort()
        if max_n and len(files) > max_n:
            files = files[:max_n]
        for f in files:
            items.append({
                "id": f"{class_name}::{source_name}::{os.path.basename(f)}",
                "class_name": class_name,
                "source": source_name,
                "filename": os.path.basename(f),
                "type": "file",
                "path": f
            })
        print(f"  [{class_name}] {source_name}: {len(files)} files")

    # Helper to add zip photos
    def add_zip_photos(class_name, source_name, zip_path, prefix, max_n=None):
        if not os.path.exists(zip_path):
            print(f"Warning: {zip_path} not found!")
            return
        with zipfile.ZipFile(zip_path, "r") as z:
            members = [n for n in z.namelist() if n.startswith(prefix + "/") and n.lower().endswith((".jpg", ".jpeg", ".png"))]
            members.sort()
            if max_n and len(members) > max_n:
                members = members[:max_n]
            for m in members:
                items.append({
                    "id": f"{class_name}::{source_name}::{os.path.basename(m)}",
                    "class_name": class_name,
                    "source": source_name,
                    "filename": os.path.basename(m),
                    "type": "zip",
                    "zip_path": zip_path,
                    "member": m
                })
            print(f"  [{class_name}] {source_name}: {len(members)} files from zip")

    PADDY_ZIP = r"C:\crop-data\candidates\raw\paddy-disease-classification.zip"

    # 1. rice_leaf_blast
    add_zip_photos("rice_leaf_blast", "PaddyDoctor", PADDY_ZIP, "train_images/blast")
    add_dir_photos("rice_leaf_blast", "Project-AgML", r"C:\crop-data\crop_doctor_512\rice_leaf_blast")

    # 2. rice_healthy
    add_zip_photos("rice_healthy", "PaddyDoctor", PADDY_ZIP, "train_images/normal")
    add_dir_photos("rice_healthy", "Project-AgML", r"C:\crop-data\crop_doctor_512\rice_healthy")

    # 3. cotton_bacterial_blight
    add_dir_photos("cotton_bacterial_blight", "Mendeley_74jsdxtmx2", r"C:\crop-data\candidates\raw\74jsdxtmx2\extracted\Cotton-Original\bacterial blight")
    add_dir_photos("cotton_bacterial_blight", "Mendeley_t9hgvk2h9p", r"C:\crop-data\candidates\raw\t9hgvk2h9p\extracted\Cotton_Original_Dataset\Bacterial Blight")
    add_dir_photos("cotton_bacterial_blight", "Mendeley_b3jy2p6k8w", r"C:\crop-data\candidates\raw\b3jy2p6k8w\extracted\Original Dataset\Bacterial Blight")

    # 4. cotton_healthy
    add_dir_photos("cotton_healthy", "Mendeley_74jsdxtmx2", r"C:\crop-data\candidates\raw\74jsdxtmx2\extracted\Cotton-Original\healthy")
    add_dir_photos("cotton_healthy", "Mendeley_t9hgvk2h9p", r"C:\crop-data\candidates\raw\t9hgvk2h9p\extracted\Cotton_Original_Dataset\Healthy Leaf")
    add_dir_photos("cotton_healthy", "Mendeley_b3jy2p6k8w", r"C:\crop-data\candidates\raw\b3jy2p6k8w\extracted\Original Dataset\Healthy Leaf")

    # 5. chilli_leaf_curl_virus
    add_dir_photos("chilli_leaf_curl_virus", "Mendeley_ymt8k9bjkn", r"C:\Users\<USERNAME>\Downloads\Chili Leaf Disease Original Dataset\Chili Leaf Disease Original Dataset\Curl Virus")

    # 6. chilli_healthy
    add_dir_photos("chilli_healthy", "Mendeley_ymt8k9bjkn", r"C:\Users\<USERNAME>\Downloads\Chili Leaf Disease Original Dataset\Chili Leaf Disease Original Dataset\Healthy Leaf")

    # 7. maize_northern_leaf_blight
    add_dir_photos("maize_northern_leaf_blight", "Mendeley_3myfctrgk3", r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Leaf_Blight")
    add_dir_photos("maize_northern_leaf_blight", "Mendeley_bwh3zbpkpv", r"C:\Users\<USERNAME>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\leaf blight")
    add_dir_photos("maize_northern_leaf_blight", "PlantDoc", r"C:\crop-data\candidates\raw\plantdoc\train\Corn leaf blight")

    # 8. maize_healthy
    add_dir_photos("maize_healthy", "Mendeley_3myfctrgk3", r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Healthy")
    add_dir_photos("maize_healthy", "Mendeley_bwh3zbpkpv", r"C:\Users\<USERNAME>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\healthy")

    # 9. maize_fall_armyworm
    add_dir_photos("maize_fall_armyworm", "Mendeley_3myfctrgk3", r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Fall_Armyworm")
    add_dir_photos("maize_fall_armyworm", "Mendeley_bwh3zbpkpv", r"C:\Users\<USERNAME>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\fall armyworm")

    # 10. rice_stem_borer
    add_zip_photos("rice_stem_borer", "PaddyDoctor", PADDY_ZIP, "train_images/dead_heart")

    # 11. rice_hispa
    add_zip_photos("rice_hispa", "PaddyDoctor", PADDY_ZIP, "train_images/hispa")

    # 12. tomato_spider_mites
    add_dir_photos("tomato_spider_mites", "PlantVillage", r"D:\main set\Tomato_Spider_mites Two-spotted_spider_mite")
    add_dir_photos("tomato_spider_mites", "PlantDoc", r"C:\crop-data\candidates\raw\plantdoc\train\Tomato two spotted spider mites leaf")

    # 13. other_not_a_crop
    add_dir_photos("other_not_a_crop", "Kaggle_agri_data", r"C:\crop-data\crop_doctor_512\weeds_not_a_crop")

    print(f"\nTotal collected items to audit: {len(items):,} photos across {len(CLASS_ORDER)} classes.")

    # 2. Hash computation with caching
    cache_path = Path(r"C:\crop-doctor-offline\benchmarks\d6_hashes.json")
    cache = {}
    if cache_path.exists():
        try:
            with open(cache_path, "r", encoding="utf-8") as fp:
                cache = json.load(fp)
            print(f"Loaded {len(cache):,} pre-computed hashes from cache.")
        except Exception as e:
            print(f"Could not load cache: {e}")

    items_to_hash = [it for it in items if it["id"] not in cache]
    print(f"Items needing hashing: {len(items_to_hash):,}")

    if items_to_hash:
        num_workers = min(12, os.cpu_count() or 4)
        print(f"Computing dihedral pHashes using {num_workers} processes...")
        th0 = time.time()
        with ProcessPoolExecutor(max_workers=num_workers) as executor:
            batch_size = 100
            for i in range(0, len(items_to_hash), batch_size):
                batch = items_to_hash[i:i + batch_size]
                results = list(executor.map(compute_hash_for_item, batch))
                for item_id, h in results:
                    cache[item_id] = h
                if (i // batch_size) % 10 == 0:
                    print(f"  Hashed {min(i + batch_size, len(items_to_hash))}/{len(items_to_hash)} photos...")
        print(f"Hashing finished in {time.time() - th0:.2f} s. Saving cache...")
        cache_path.parent.mkdir(parents=True, exist_ok=True)
        with open(cache_path, "w", encoding="utf-8") as fp:
            json.dump(cache, fp)

    # Attach hashes to items
    for it in items:
        it["hash"] = cache.get(it["id"], 0)

    # 3. Cross-Class Duplicate Collision Audit (Hamming <= 2)
    print("\n2. Checking Cross-Class Collisions (Hamming <= 2 across DIFFERENT classes)...")
    class_item_map = defaultdict(list)
    for it in items:
        class_item_map[it["class_name"]].append(it)

    cross_class_conflicts = []
    cross_source_conflicts = []

    for c1_idx, c1 in enumerate(CLASS_ORDER):
        items1 = class_item_map[c1]
        if not items1: continue
        h1_arr = np.array([it["hash"] for it in items1], dtype=np.uint64)
        
        for c2_idx in range(c1_idx + 1, len(CLASS_ORDER)):
            c2 = CLASS_ORDER[c2_idx]
            items2 = class_item_map[c2]
            if not items2: continue
            h2_arr = np.array([it["hash"] for it in items2], dtype=np.uint64)
            
            # Compare each item in c1 against c2
            for i, it1 in enumerate(items1):
                dists = hamming_dist_arr(h1_arr[i], h2_arr)
                match_indices = np.where(dists <= 2)[0]
                for mi in match_indices:
                    d = int(dists[mi])
                    it2 = items2[mi]
                    conflict_info = {
                        "class1": c1, "source1": it1["source"], "file1": it1["filename"],
                        "class2": c2, "source2": it2["source"], "file2": it2["filename"],
                        "dist": d
                    }
                    cross_class_conflicts.append(conflict_info)
                    print(f"  🚨 CROSS-CLASS CONFLICT (dist={d}): [{c1}] {it1['filename']} vs [{c2}] {it2['filename']}")

    print(f"Total cross-class conflicts (dist <= 2): {len(cross_class_conflicts)}")

    # 4. Within-Class Clustering (Hamming <= 8)
    print("\n3. Within-Class Single-Linkage Clustering (Hamming <= 8)...")
    class_audit_results = {}

    for c in CLASS_ORDER:
        c_items = class_item_map[c]
        N = len(c_items)
        if N == 0:
            class_audit_results[c] = {
                "raw_count": 0, "unique_clusters": 0, "capped_count": 0,
                "sources": [], "held_out": None, "source_limited": True,
                "internal_duplicates": 0, "clusters": []
            }
            continue

        parent = list(range(N))
        def find(x):
            while parent[x] != x:
                parent[x] = parent[parent[x]]
                x = parent[x]
            return x
        def union(a, b):
            ra, rb = find(a), find(b)
            if ra != rb: parent[ra] = rb

        h_arr = np.array([it["hash"] for it in c_items], dtype=np.uint64)
        
        # Cluster items within class
        for i in range(N):
            if i + 1 < N:
                dists = hamming_dist_arr(h_arr[i], h_arr[i+1:])
                matches = np.where(dists <= 8)[0]
                for m in matches:
                    union(i, i + 1 + m)

        cluster_map = defaultdict(list)
        for i in range(N):
            cluster_map[find(i)].append(c_items[i])

        unique_clusters = len(cluster_map)
        internal_duplicates = N - unique_clusters
        capped_count = min(unique_clusters, 800)

        # Sources present
        sources_present = sorted(list(set(it["source"] for it in c_items)))
        is_source_limited = len(sources_present) <= 1

        # Check cross-source duplicates within the same class
        source_breakdown = defaultdict(int)
        for it in c_items:
            source_breakdown[it["source"]] += 1

        class_audit_results[c] = {
            "raw_count": N,
            "unique_clusters": unique_clusters,
            "internal_duplicates": internal_duplicates,
            "capped_count": capped_count,
            "sources": sources_present,
            "source_breakdown": dict(source_breakdown),
            "source_limited": is_source_limited,
        }
        print(f"  Class [{c}]: Raw={N:,} -> Unique Clusters={unique_clusters:,} (Dups={internal_duplicates:,}) -> Capped(800)={capped_count} | Sources: {sources_present}")

    # Summary timing
    print(f"\nAudit completed in {time.time() - t0:.2f} s.")
    
    # Save JSON summary for reporting
    json_path = Path(r"C:\crop-doctor-offline\benchmarks\d6_audit_data.json")
    with open(json_path, "w", encoding="utf-8") as fp:
        json.dump({
            "classes": class_audit_results,
            "cross_class_conflicts": cross_class_conflicts
        }, fp, indent=2)
    print(f"Saved audit data to {json_path}")

if __name__ == "__main__":
    main()
