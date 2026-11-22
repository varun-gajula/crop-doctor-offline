#!/usr/bin/env python3
"""
Crop Doctor - Task D7: Tomato Classes Audit, Contact Sheet & 15-Class Table
---------------------------------------------------------------------------
1. Cluster within tomato_healthy and tomato_septoria_leaf_spot at Hamming <= 10.
2. Report unique photos per class, cap at 800, flag if < 300 unique photos.
3. Cross-class collision check between the two tomato classes (Hamming <= 2).
4. Build contact sheet of 40 random photos per class in benchmarks/d7_tomato_contact_sheet.jpg.
5. Keep tomato_spider_mites OUT.
6. Generate 15-class table and save benchmarks/d7_checkpoint.md.
"""

import os
import sys
import json
import time
import random
from pathlib import Path
from collections import defaultdict
from concurrent.futures import ProcessPoolExecutor
import numpy as np
from PIL import Image, ImageOps, ImageDraw, ImageFont

SEED = 42
random.seed(SEED)
np.random.seed(SEED)

TOMATO_SOURCES = {
    "tomato_healthy": [
        (r"C:\Users\<USERNAME>\Downloads\archive (2)\tomato_healthy", "archive_2"),
    ],
    "tomato_septoria_leaf_spot": [
        (r"C:\Users\<USERNAME>\Downloads\archive (2)\tomato_septoria leaf spot", "archive_2"),
    ]
}

# Optional fallback/supplemental if tomato siva exists
if os.path.exists(r"C:\Users\<USERNAME>\Downloads\tomato siva\augmented_dataset"):
    TOMATO_SOURCES["tomato_healthy"].append(
        (r"C:\Users\<USERNAME>\Downloads\tomato siva\augmented_dataset\Tomato_healthy", "tomato_siva")
    )
    TOMATO_SOURCES["tomato_septoria_leaf_spot"].append(
        (r"C:\Users\<USERNAME>\Downloads\tomato siva\augmented_dataset\Tomato_Septoria_leaf_spot", "tomato_siva")
    )

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

def compute_dihedral_phash(path_str):
    try:
        with Image.open(path_str) as im0:
            im = ImageOps.exif_transpose(im0).convert('RGB')
            m = ImageOps.mirror(im)
            variants = [
                im, im.rotate(90, expand=True), im.rotate(180, expand=True), im.rotate(270, expand=True),
                m, m.rotate(90, expand=True), m.rotate(180, expand=True), m.rotate(270, expand=True)
            ]
            return min(phash_64_from_image(v) for v in variants)
    except Exception as e:
        print(f"Error hashing {path_str}: {e}")
        return 0

def hash_batch(paths):
    return [compute_dihedral_phash(p) for p in paths]

def hamming_dist_arr(h1, h2_arr):
    diff = h1 ^ h2_arr
    v = diff
    v = v - ((v >> np.uint64(1)) & np.uint64(0x5555555555555555))
    v = (v & np.uint64(0x3333333333333333)) + ((v >> np.uint64(2)) & np.uint64(0x3333333333333333))
    v = (v + (v >> np.uint64(4))) & np.uint64(0x0F0F0F0F0F0F0F0F)
    return (v * np.uint64(0x0101010101010101)) >> np.uint64(56)

def main():
    print("=== Task D7: Tomato Classes Audit & Verification ===")
    t0 = time.time()
    
    # 1. Collect files
    records = []
    for cls, src_list in TOMATO_SOURCES.items():
        for dir_path, src_tag in src_list:
            if not os.path.exists(dir_path):
                print(f"  Note: {dir_path} not found on disk, skipping.")
                continue
            fs = sorted([f for f in os.listdir(dir_path) if f.lower().endswith(('.jpg', '.jpeg', '.png', '.bmp'))])
            print(f"Found {len(fs)} photos in [{cls}] from {src_tag}")
            for f in fs:
                records.append({
                    "class_name": cls,
                    "source": src_tag,
                    "filename": f,
                    "path": os.path.join(dir_path, f)
                })

    print(f"Total tomato photos collected: {len(records):,}")

    # 2. Compute hashes in parallel
    paths = [r["path"] for r in records]
    num_workers = min(12, os.cpu_count() or 4)
    print(f"Computing dihedral 64-bit pHashes across {num_workers} processes...")
    th0 = time.time()
    batch_size = 100
    batches = [paths[i:i + batch_size] for i in range(0, len(paths), batch_size)]
    with ProcessPoolExecutor(max_workers=num_workers) as executor:
        results = list(executor.map(hash_batch, batches))
    hashes = [h for sub in results for h in sub]
    for r, h in zip(records, hashes):
        r["hash"] = h
    print(f"Hashes computed in {time.time() - th0:.2f} s.")

    # 3. Check Cross-Class Conflicts (Hamming <= 2) between tomato_healthy and tomato_septoria_leaf_spot
    print("\n--- Checking Cross-Class Duplicates (Hamming <= 2) ---")
    healthy_recs = [r for r in records if r["class_name"] == "tomato_healthy" and r["hash"] != 0]
    septoria_recs = [r for r in records if r["class_name"] == "tomato_septoria_leaf_spot" and r["hash"] != 0]

    h_healthy = np.array([r["hash"] for r in healthy_recs], dtype=np.uint64)
    h_septoria = np.array([r["hash"] for r in septoria_recs], dtype=np.uint64)

    cross_duplicates = []
    for i, rh in enumerate(healthy_recs):
        dists = hamming_dist_arr(h_healthy[i], h_septoria)
        matches = np.where(dists <= 2)[0]
        for mi in matches:
            d = int(dists[mi])
            rs = septoria_recs[mi]
            cross_duplicates.append({
                "healthy_file": rh["filename"],
                "septoria_file": rs["filename"],
                "distance": d
            })
            print(f"  🚨 CROSS-CLASS MATCH (dist={d}): [healthy] {rh['filename']} <-> [septoria] {rs['filename']}")

    print(f"Total cross-class duplicates between tomato_healthy and tomato_septoria_leaf_spot: {len(cross_duplicates)}")

    # 4. Within-class clustering at Hamming <= 10 (Rule 1)
    print("\n--- Within-Class Single-Linkage Clustering (Hamming <= 10) ---")
    results_by_class = {}
    for cls in ["tomato_healthy", "tomato_septoria_leaf_spot"]:
        c_recs = [r for r in records if r["class_name"] == cls and r["hash"] != 0]
        N = len(c_recs)
        parent = list(range(N))
        def find(x):
            while parent[x] != x:
                parent[x] = parent[parent[x]]
                x = parent[x]
            return x
        def union(a, b):
            ra, rb = find(a), find(b)
            if ra != rb: parent[ra] = rb

        h_arr = np.array([r["hash"] for r in c_recs], dtype=np.uint64)
        for i in range(N):
            if i + 1 < N:
                dists = hamming_dist_arr(h_arr[i], h_arr[i+1:])
                matches = np.where(dists <= 10)[0]
                for m in matches:
                    union(i, i + 1 + m)

        clusters = defaultdict(list)
        for i in range(N):
            clusters[find(i)].append(c_recs[i])

        unique_clusters = len(clusters)
        capped_count = min(unique_clusters, 800)
        dups = N - unique_clusters
        is_under_300 = unique_clusters < 300

        results_by_class[cls] = {
            "raw_count": N,
            "unique_clusters": unique_clusters,
            "internal_duplicates": dups,
            "capped_count": capped_count,
            "under_300": is_under_300,
            "records": c_recs,
            "clusters": list(clusters.values())
        }
        status_str = "WARNING: UNDER 300 PHOTOS!" if is_under_300 else "PASS (>= 300)"
        print(f"  [{cls}]: Raw={N:,} | Unique Clusters (Hamming<=10)={unique_clusters:,} | Duplicates={dups:,} | Capped(800)={capped_count} | {status_str}")

    # 5. Build Contact Sheet (Rule 2)
    # 40 random photos per class in benchmarks/d7_tomato_contact_sheet.jpg
    print("\n--- Generating Contact Sheet of 40 Random Photos Per Class ---")
    contact_sheet_path = Path(r"C:\crop-doctor-offline\benchmarks\d7_tomato_contact_sheet.jpg")
    contact_sheet_path.parent.mkdir(parents=True, exist_ok=True)

    rng = random.Random(SEED)
    sample_healthy = rng.sample(healthy_recs, min(40, len(healthy_recs)))
    sample_septoria = rng.sample(septoria_recs, min(40, len(septoria_recs)))

    thumb_w, thumb_h = 160, 160
    cols = 8
    rows_per_class = 5  # 8 * 5 = 40 photos per class
    header_h = 40
    margin = 8
    total_w = cols * thumb_w + (cols + 1) * margin
    # Section 1: Septoria (header + 5 rows)
    # Section 2: Healthy (header + 5 rows)
    total_h = 2 * header_h + 2 * (rows_per_class * thumb_h + (rows_per_class + 1) * margin) + 60

    canvas = Image.new("RGB", (total_w, total_h), color=(245, 245, 245))
    draw = ImageDraw.Draw(canvas)
    
    try:
        font_large = ImageFont.truetype("arial.ttf", 22)
        font_sub = ImageFont.truetype("arial.ttf", 14)
    except:
        font_large = ImageFont.load_default()
        font_sub = ImageFont.load_default()

    curr_y = 15
    # Overall Title
    draw.text((margin, curr_y), "Crop Doctor - Task D7: Tomato Inspection Contact Sheet (40 Photos / Class)", fill=(20, 20, 20), font=font_large)
    curr_y += 35

    def draw_section(title, subtitle, samples, start_y, border_color):
        y = start_y
        draw.rectangle([(margin, y), (total_w - margin, y + header_h)], fill=(230, 230, 230))
        draw.text((margin + 10, y + 4), title, fill=(0, 0, 0), font=font_large)
        draw.text((margin + 360, y + 10), subtitle, fill=(80, 80, 80), font=font_sub)
        y += header_h + margin

        for idx, rec in enumerate(samples):
            r = idx // cols
            c = idx % cols
            x = margin + c * (thumb_w + margin)
            thumb_y = y + r * (thumb_h + margin)

            try:
                with Image.open(rec["path"]) as im:
                    thumb = ImageOps.fit(im.convert("RGB"), (thumb_w, thumb_h), Image.Resampling.LANCZOS)
                    canvas.paste(thumb, (x, thumb_y))
            except Exception as e:
                draw.rectangle([(x, thumb_y), (x + thumb_w, thumb_y + thumb_h)], fill=(200, 200, 200))

            # Border
            draw.rectangle([(x, thumb_y), (x + thumb_w, thumb_y + thumb_h)], outline=border_color, width=2)
            # Label
            draw.text((x + 4, thumb_y + thumb_h - 18), f"#{idx+1}", fill=(255, 255, 0), font=font_sub)

        return y + rows_per_class * (thumb_h + margin) + margin

    # Draw Septoria first
    curr_y = draw_section(
        "tomato_septoria_leaf_spot (Septoria lycopersici)",
        "Check: Small circular dark brown lesions with gray/tan centers & pycnidia",
        sample_septoria,
        curr_y,
        border_color=(180, 50, 50)
    )

    # Draw Healthy
    curr_y = draw_section(
        "tomato_healthy (Healthy Foliage)",
        "Check: Intact green leaf lamina, no necrosis or fungal spots",
        sample_healthy,
        curr_y,
        border_color=(30, 140, 30)
    )

    canvas.save(contact_sheet_path, "JPEG", quality=90)
    print(f"Contact sheet saved to: {contact_sheet_path} ({os.path.getsize(contact_sheet_path):,} bytes)")

    # 6. Save JSON Data
    d7_data = {
        "tomato_healthy": {
            "raw_count": results_by_class["tomato_healthy"]["raw_count"],
            "unique_clusters_hamming_10": results_by_class["tomato_healthy"]["unique_clusters"],
            "internal_duplicates": results_by_class["tomato_healthy"]["internal_duplicates"],
            "capped_count": results_by_class["tomato_healthy"]["capped_count"],
            "under_300": results_by_class["tomato_healthy"]["under_300"],
        },
        "tomato_septoria_leaf_spot": {
            "raw_count": results_by_class["tomato_septoria_leaf_spot"]["raw_count"],
            "unique_clusters_hamming_10": results_by_class["tomato_septoria_leaf_spot"]["unique_clusters"],
            "internal_duplicates": results_by_class["tomato_septoria_leaf_spot"]["internal_duplicates"],
            "capped_count": results_by_class["tomato_septoria_leaf_spot"]["capped_count"],
            "under_300": results_by_class["tomato_septoria_leaf_spot"]["under_300"],
        },
        "cross_class_duplicates": cross_duplicates
    }
    with open(r"C:\crop-doctor-offline\benchmarks\d7_tomato_audit.json", "w") as fp:
        json.dump(d7_data, fp, indent=2)
    print("Saved d7_tomato_audit.json")

if __name__ == "__main__":
    main()
