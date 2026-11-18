#!/usr/bin/env python3
r"""
Crop Doctor - Task D4: Unique-Photo Audit of Tomato Datasets
------------------------------------------------------------
Audits unique photos, internal duplicate rates, and cross-dataset overlap
between:
  1) C:\Users\<USERNAME>\Downloads\tomato siva\augmented_dataset
  2) C:\Users\<USERNAME>\Downloads\archive (2) (tomato folders)

Perceptual Hash:
  Dihedral (8-transformation) canonical minimum 64-bit dHash.
Clustering:
  Single-linkage connected components at Hamming distance <= 6.
Deliverables:
  - benchmarks/tomato_unique_audit.csv
  - C:\crop-data\final19\tomato_groups.csv
  - Printed report & 5 example clusters per class
"""

import os
import sys
import time
import csv
from pathlib import Path
from concurrent.futures import ProcessPoolExecutor
import numpy as np
from PIL import Image, ImageOps

CLASS_MAPPING = {
    'Tomato_Bacterial_spot': 'Tomato_Bacterial_spot',
    'Tomato_Early_blight': 'Tomato_Early_blight',
    'Tomato_healthy': 'Tomato_healthy',
    'Tomato_Late_blight': 'Tomato_Late_blight',
    'Tomato_Leaf_Mold': 'Tomato_Leaf_Mold',
    'Tomato_Septoria_leaf_spot': 'Tomato_Septoria_leaf_spot',
    'Tomato_Spider_mites_Two_spotted_spider_mite': 'Tomato_Spider_mites Two-spotted_spider_mite',
    'Tomato__Target_Spot': 'Tomato_Target_Spot',
    'Tomato__Tomato_mosaic_virus': 'Tomato_Tomato_mosaic_virus',
    'Tomato__Tomato_YellowLeaf__Curl_Virus': 'Tomato_Tomato_Yellow_Leaf_Curl_Virus'
}

VALID_EXTS = {'.jpg', '.jpeg', '.png', '.bmp', '.webp'}

def compute_dhash_64(gray_image, hash_size=8):
    resized = gray_image.resize((hash_size + 1, hash_size), Image.Resampling.LANCZOS)
    pixels = np.array(resized)
    diff = pixels[:, 1:] > pixels[:, :-1]
    h = 0
    for bit in diff.flatten():
        h = (h << 1) | int(bit)
    return h

def get_canonical_dihedral_hash(img_path):
    try:
        with Image.open(img_path) as img:
            g = img.convert('L')
            variants = [
                g,
                g.rotate(90, expand=True),
                g.rotate(180, expand=True),
                g.rotate(270, expand=True),
                ImageOps.mirror(g),
                ImageOps.mirror(g).rotate(90, expand=True),
                ImageOps.mirror(g).rotate(180, expand=True),
                ImageOps.mirror(g).rotate(270, expand=True)
            ]
            return min(compute_dhash_64(v) for v in variants)
    except Exception as e:
        return 0

def hash_batch(paths):
    return [get_canonical_dihedral_hash(p) for p in paths]

def cluster_hashes(hashes, max_dist=6):
    """
    Cluster hashes using connected components where Hamming distance <= max_dist.
    Returns: list of clusters, each cluster is a list of indices into `hashes`.
    """
    N = len(hashes)
    if N == 0:
        return []
    
    h_arr = np.array(hashes, dtype=np.uint64)
    adj = [[] for _ in range(N)]
    
    # Chunked pairwise distance matrix computation
    chunk_size = 500
    for i in range(0, N, chunk_size):
        sub = h_arr[i : min(i + chunk_size, N)]
        sub_len = len(sub)
        diff = sub[:, None] ^ h_arr[None, :]
        dist = np.bitwise_count(diff)
        
        # Only pairs where j > (i + r)
        rows, cols = np.where(dist <= max_dist)
        global_rows = rows + i
        valid_mask = global_rows < cols
        for u, v in zip(global_rows[valid_mask], cols[valid_mask]):
            adj[u].append(v)
            adj[v].append(u)
            
    visited = [False] * N
    clusters = []
    for i in range(N):
        if not visited[i]:
            cluster = []
            queue = [i]
            visited[i] = True
            while queue:
                curr = queue.pop()
                cluster.append(curr)
                for neighbor in adj[curr]:
                    if not visited[neighbor]:
                        visited[neighbor] = True
                        queue.append(neighbor)
            clusters.append(cluster)
            
    return clusters

def check_overlap(cluster_indices, siva_hashes, arch_hashes, max_dist=6):
    """
    Check if a cluster in siva matches any image in archive (2).
    Returns True if at least one photo in the cluster has Hamming distance <= max_dist
    to any photo in archive (2).
    """
    if len(arch_hashes) == 0:
        return False
    
    arch_arr = np.array(arch_hashes, dtype=np.uint64)
    c_hashes = np.array([siva_hashes[idx] for idx in cluster_indices], dtype=np.uint64)
    
    diff = c_hashes[:, None] ^ arch_arr[None, :]
    dist = np.bitwise_count(diff)
    return np.any(dist <= max_dist)

def main():
    repo_root = Path(r"C:\crop-doctor-offline")
    siva_root = Path(r"C:\Users\<USERNAME>\Downloads\tomato siva\augmented_dataset")
    arch_root = Path(r"C:\Users\<USERNAME>\Downloads\archive (2)")
    final19_dir = Path(r"C:\crop-data\final19")
    final19_dir.mkdir(parents=True, exist_ok=True)
    
    print("=" * 80)
    print("TASK D4: UNIQUE-PHOTO AUDIT OF TOMATO DATASETS")
    print(f"Dataset 1 (Siva):    {siva_root}")
    print(f"Dataset 2 (Archive): {arch_root}")
    print("=" * 80)
    
    if not siva_root.exists():
        print(f"ERROR: Siva root not found: {siva_root}")
        sys.exit(1)
    if not arch_root.exists():
        print(f"ERROR: Archive root not found: {arch_root}")
        sys.exit(1)
        
    num_workers = min(12, os.cpu_count() or 4)
    print(f"Using {num_workers} worker processes for parallel dihedral perceptual hashing.\n")
    
    audit_results = []
    example_clusters_per_class = {}
    all_group_records = []
    global_group_counter = 0
    
    for s_cls, a_cls in CLASS_MAPPING.items():
        s_dir = siva_root / s_cls
        a_dir = arch_root / a_cls
        
        print(f"Processing Class: {s_cls} ...")
        t0 = time.time()
        
        # 1. Scan Siva files
        s_files = sorted([f for f in s_dir.iterdir() if f.is_file() and f.suffix.lower() in VALID_EXTS])
        a_files = sorted([f for f in a_dir.iterdir() if f.is_file() and f.suffix.lower() in VALID_EXTS]) if a_dir.exists() else []
        
        print(f"  Scanned {len(s_files)} Siva files, {len(a_files)} Archive files.")
        
        # 2. Compute Siva hashes in parallel
        batch_size = 250
        s_batches = [s_files[i:i + batch_size] for i in range(0, len(s_files), batch_size)]
        with ProcessPoolExecutor(max_workers=num_workers) as executor:
            s_results = list(executor.map(hash_batch, [[str(p) for p in b] for b in s_batches]))
        s_hashes = [h for sub in s_results for h in sub]
        
        # 3. Compute Archive hashes in parallel
        if a_files:
            a_batches = [a_files[i:i + batch_size] for i in range(0, len(a_files), batch_size)]
            with ProcessPoolExecutor(max_workers=num_workers) as executor:
                a_results = list(executor.map(hash_batch, [[str(p) for p in b] for b in a_batches]))
            a_hashes = [h for sub in a_results for h in sub]
        else:
            a_hashes = []
            
        t_hash = time.time() - t0
        print(f"  Hashes computed in {t_hash:.1f}s.")
        
        # 4. Cluster Siva photos (Hamming distance <= 6)
        t_cl_start = time.time()
        clusters = cluster_hashes(s_hashes, max_dist=6)
        t_cl = time.time() - t_cl_start
        
        total_files = len(s_files)
        unique_clusters = len(clusters)
        duplicate_files = total_files - unique_clusters
        duplicate_pct = round((duplicate_files / total_files * 100.0), 2) if total_files > 0 else 0.0
        
        print(f"  Clustering: {total_files} files -> {unique_clusters} unique photos ({duplicate_pct}% duplicate files) in {t_cl:.2f}s.")
        
        # 5. Check cross-folder overlap with archive (2)
        overlap_count = 0
        if a_hashes:
            for c in clusters:
                if check_overlap(c, s_hashes, a_hashes, max_dist=6):
                    overlap_count += 1
                    
        print(f"  Cross-folder overlap with archive (2): {overlap_count} / {unique_clusters} unique photos match.")
        
        audit_results.append({
            'class': s_cls,
            'files': total_files,
            'unique': unique_clusters,
            'duplicate_percent': duplicate_pct,
            'overlap_with_archive2': overlap_count
        })
        
        # 6. Extract 5 example clusters
        # Sort clusters by size descending to show rich duplicate groups
        multi_clusters = [c for c in clusters if len(c) > 1]
        multi_clusters.sort(key=lambda c: len(c), reverse=True)
        chosen_clusters = multi_clusters[:5]
        
        example_clusters_per_class[s_cls] = []
        for idx, c in enumerate(chosen_clusters):
            file_names = [s_files[img_idx].name for img_idx in c]
            example_clusters_per_class[s_cls].append({
                'example_id': idx + 1,
                'cluster_size': len(c),
                'files': file_names
            })
            
        # 7. Record group_id for tomato_groups.csv
        for c in clusters:
            gid = global_group_counter
            global_group_counter += 1
            for img_idx in c:
                fpath = s_files[img_idx]
                all_group_records.append({
                    'file': fpath.name,
                    'class': s_cls,
                    'group_id': gid,
                    'filepath': str(fpath)
                })
                
        print()

    # 8. Write benchmarks/tomato_unique_audit.csv
    csv_path = repo_root / "benchmarks" / "tomato_unique_audit.csv"
    csv_path.parent.mkdir(parents=True, exist_ok=True)
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=['class', 'files', 'unique', 'duplicate_percent', 'overlap_with_archive2'])
        writer.writeheader()
        for row in audit_results:
            writer.writerow(row)
    print(f"Saved audit table to: {csv_path}")

    # Also save to current directory if running in datasets arm
    cwd_csv = Path(r"C:\Users\<USERNAME>\datasets arm\benchmarks\tomato_unique_audit.csv")
    cwd_csv.parent.mkdir(parents=True, exist_ok=True)
    with open(cwd_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=['class', 'files', 'unique', 'duplicate_percent', 'overlap_with_archive2'])
        writer.writeheader()
        for row in audit_results:
            writer.writerow(row)

    # 9. Write C:\crop-data\final19\tomato_groups.csv
    groups_path = final19_dir / "tomato_groups.csv"
    with open(groups_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=['file', 'class', 'group_id', 'filepath'])
        writer.writeheader()
        for row in all_group_records:
            writer.writerow(row)
    print(f"Saved group assignments ({len(all_group_records)} photos, {global_group_counter} groups) to: {groups_path}")

    # 10. Print Summary Table
    print("\n" + "=" * 95)
    print(f"{'Class':<45} | {'Files':<7} | {'Unique':<7} | {'Dup %':<7} | {'Overlap Arch2':<13}")
    print("-" * 95)
    tot_files = sum(r['files'] for r in audit_results)
    tot_unique = sum(r['unique'] for r in audit_results)
    tot_overlap = sum(r['overlap_with_archive2'] for r in audit_results)
    overall_dup_pct = round((tot_files - tot_unique) / tot_files * 100.0, 2) if tot_files > 0 else 0.0

    for r in audit_results:
        print(f"{r['class']:<45} | {r['files']:<7} | {r['unique']:<7} | {r['duplicate_percent']:<7.2f} | {r['overlap_with_archive2']:<13}")
    print("-" * 95)
    print(f"{'TOTAL':<45} | {tot_files:<7} | {tot_unique:<7} | {overall_dup_pct:<7.2f} | {tot_overlap:<13}")
    print("=" * 95)

    # 11. Print Example Clusters per Class
    print("\n" + "=" * 95)
    print("EXAMPLE CLUSTERS (FILE NAMES ONLY)")
    print("=" * 95)
    for cls_name, ex_list in example_clusters_per_class.items():
        print(f"\nClass: {cls_name} ({len(ex_list)} example clusters shown):")
        for ex in ex_list:
            files_str = ", ".join(ex['files'][:10])
            if len(ex['files']) > 10:
                files_str += f", ... (+{len(ex['files'])-10} more)"
            print(f"  Cluster (size {ex['cluster_size']}): [{files_str}]")

if __name__ == "__main__":
    main()
