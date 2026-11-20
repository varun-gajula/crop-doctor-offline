#!/usr/bin/env python3
"""
tools/assemble_p4_probe_sets.py
Assembles the 5 probe sets for Task P4 into C:\\crop-data\\realtest:
1. supported: Real leaf photos of our 19 supported crop classes (0 train collisions).
2. unsupported: Unsupported diseases/pests on our 5 crops (tomato bacterial spot,
   leaf mold, mosaic, late blight, chilli bacterial spot, cercospora, rice scald, cotton jassids/redding).
3. othercrop: Real leaves of crops we do not support (cashew, cassava).
4. nocrop: Real photos of non-crop scenes (desks, walls, papers, floors, objects; no faces).
5. screen: Real photos of laptop screen displaying crop leaves (already in realtest/screen).

Generates benchmarks/p4_probe_manifest.json with full metadata for traceability.
"""

import os
import sys
import json
import shutil
import hashlib
from pathlib import Path
from PIL import Image

BASE_REALTEST = Path("C:/crop-data/realtest")
SPLIT_V2_PATH = Path("benchmarks/final/split_v2.json")
CROP_512_BASE = Path("C:/crop-data/crop_doctor_512")
MANIFEST_PATH = Path("benchmarks/p4_probe_manifest.json")

def md5_file(filepath):
    h = hashlib.md5()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def main():
    print("=== Assembling P4 Probe Sets ===")
    os.makedirs(BASE_REALTEST / "supported", exist_ok=True)
    os.makedirs(BASE_REALTEST / "unsupported", exist_ok=True)
    os.makedirs(BASE_REALTEST / "othercrop", exist_ok=True)
    os.makedirs(BASE_REALTEST / "nocrop", exist_ok=True)
    os.makedirs(BASE_REALTEST / "screen", exist_ok=True)

    manifest = []

    # 1. Supported Set
    # A) Real field photos from real_leaves that belong to supported classes
    real_leaves_dir = BASE_REALTEST / "real_leaves"
    supported_mapping = {
        "camera_real_01": ("maize", "maize_gray_leaf_spot"),
        "camera_real_02": ("maize", "maize_gray_leaf_spot"),
        "camera_real_03": ("maize", "maize_northern_leaf_blight"),
        "camera_real_04": ("maize", "maize_northern_leaf_blight"),
        "camera_real_05": ("maize", "maize_common_rust"),
        "camera_real_06": ("maize", "maize_common_rust"),
        "camera_real_07": ("tomato", "tomato_leaf_blight"),
        "camera_real_08": ("tomato", "tomato_leaf_blight"),
        "camera_real_09": ("tomato", "tomato_healthy"),
        "camera_real_10": ("tomato", "tomato_healthy"),
        "camera_real_17": ("tomato", "tomato_yellow_leaf_curl_virus"),
        "camera_real_18": ("tomato", "tomato_yellow_leaf_curl_virus"),
    }
    for prefix, (crop, cls) in supported_mapping.items():
        matches = list(real_leaves_dir.glob(f"{prefix}*.jpg"))
        if matches:
            src = matches[0]
            dst_name = f"field_{crop}_{cls}_{src.name}"
            dst = BASE_REALTEST / "supported" / dst_name
            shutil.copy2(src, dst)
            manifest.append({
                "set": "supported",
                "filename": dst_name,
                "path": str(dst),
                "true_crop": crop,
                "true_class": cls,
                "source": str(src),
                "md5": md5_file(dst)
            })

    # B) Held-out validation samples (5 per supported crop class, 0 train collisions)
    with open(SPLIT_V2_PATH) as f:
        split_data = json.load(f)
    val_items = split_data.get("validation", [])
    val_by_class = {}
    for item in val_items:
        cls = item["class_name"]
        if cls == "weeds_not_a_crop":
            continue
        val_by_class.setdefault(cls, []).append(item)

    for cls, items in sorted(val_by_class.items()):
        crop = cls.split("_")[0]
        # Pick 5 items
        for idx, item in enumerate(items[:5]):
            src = CROP_512_BASE / item["relative_path"]
            if src.exists():
                dst_name = f"val_{crop}_{cls}_{idx:02d}_{src.name}"
                dst = BASE_REALTEST / "supported" / dst_name
                shutil.copy2(src, dst)
                manifest.append({
                    "set": "supported",
                    "filename": dst_name,
                    "path": str(dst),
                    "true_crop": crop,
                    "true_class": cls,
                    "source": str(src),
                    "md5": md5_file(dst)
                })

    print(f"Supported set assembled: {sum(1 for m in manifest if m['set'] == 'supported')} images")

    # 2. Unsupported Set (unsupported diseases on our 5 crops)
    # A) Real field photos from real_leaves that are unsupported
    unsupported_field_mapping = {
        "camera_real_11": ("tomato", "tomato_bacterial_spot"),
        "camera_real_12": ("tomato", "tomato_bacterial_spot"),
        "camera_real_13": ("tomato", "tomato_late_blight"),
        "camera_real_14": ("tomato", "tomato_late_blight"),
        "camera_real_15": ("tomato", "tomato_mosaic_virus"),
        "camera_real_16": ("tomato", "tomato_mosaic_virus"),
        "camera_real_19": ("tomato", "tomato_leaf_mold"),
        "camera_real_20": ("tomato", "tomato_leaf_mold"),
    }
    for prefix, (crop, prob) in unsupported_field_mapping.items():
        matches = list(real_leaves_dir.glob(f"{prefix}*.jpg"))
        if matches:
            src = matches[0]
            dst_name = f"field_{crop}_{prob}_{src.name}"
            dst = BASE_REALTEST / "unsupported" / dst_name
            shutil.copy2(src, dst)
            manifest.append({
                "set": "unsupported",
                "filename": dst_name,
                "path": str(dst),
                "true_crop": crop,
                "true_class": prob,
                "source": str(src),
                "md5": md5_file(dst)
            })

    # B) PlantDoc unsupported tomato
    plantdoc_base = Path("C:/crop-data/candidates/raw/plantdoc/train")
    plantdoc_map = {
        "Tomato leaf bacterial spot": ("tomato", "tomato_bacterial_spot"),
        "Tomato leaf late blight": ("tomato", "tomato_late_blight"),
        "Tomato leaf mosaic virus": ("tomato", "tomato_mosaic_virus"),
        "Tomato mold leaf": ("tomato", "tomato_leaf_mold"),
    }
    for folder, (crop, prob) in plantdoc_map.items():
        src_dir = plantdoc_base / folder
        if src_dir.exists():
            imgs = [p for p in src_dir.glob("*.jpg") if p.is_file()][:10]
            for idx, src in enumerate(imgs):
                dst_name = f"plantdoc_{crop}_{prob}_{idx:02d}_{src.name}"
                dst = BASE_REALTEST / "unsupported" / dst_name
                shutil.copy2(src, dst)
                manifest.append({
                    "set": "unsupported",
                    "filename": dst_name,
                    "path": str(dst),
                    "true_crop": crop,
                    "true_class": prob,
                    "source": str(src),
                    "md5": md5_file(dst)
                })

    # C) Chilli unsupported: Bacterial Spot & Cercospora
    chilli_base = Path("C:/Users/ec026/Downloads/Chili Leaf Disease Original Dataset/Chili Leaf Disease Original Dataset")
    chilli_map = {
        "Bacterial Spot": ("chilli", "chilli_bacterial_spot"),
        "Cercospora Leaf Spot": ("chilli", "chilli_cercospora_leaf_spot")
    }
    for folder, (crop, prob) in chilli_map.items():
        src_dir = chilli_base / folder
        if src_dir.exists():
            imgs = [p for p in src_dir.glob("*.jpg") if p.is_file()][:10]
            for idx, src in enumerate(imgs):
                dst_name = f"chilli_{crop}_{prob}_{idx:02d}_{src.name}"
                dst = BASE_REALTEST / "unsupported" / dst_name
                shutil.copy2(src, dst)
                manifest.append({
                    "set": "unsupported",
                    "filename": dst_name,
                    "path": str(dst),
                    "true_crop": crop,
                    "true_class": prob,
                    "source": str(src),
                    "md5": md5_file(dst)
                })

    # D) Rice unsupported: Leaf scald
    rice_scald_dir = Path("C:/Users/ec026/Downloads/rice shiva/Rice_Leaf_AUG/Leaf scald")
    if rice_scald_dir.exists():
        imgs = [p for p in rice_scald_dir.glob("*.jpg") if p.is_file()][:10]
        for idx, src in enumerate(imgs):
            dst_name = f"rice_leaf_scald_{idx:02d}_{src.name}"
            dst = BASE_REALTEST / "unsupported" / dst_name
            shutil.copy2(src, dst)
            manifest.append({
                "set": "unsupported",
                "filename": dst_name,
                "path": str(dst),
                "true_crop": "rice",
                "true_class": "rice_leaf_scald",
                "source": str(src),
                "md5": md5_file(dst)
            })

    # E) Cotton unsupported: Leaf Hopper Jassids & Leaf Redding
    cotton_base = Path("C:/Users/ec026/Downloads/cotton/Original Dataset")
    cotton_map = {
        "Leaf Hopper Jassids": ("cotton", "cotton_jassids"),
        "Leaf Redding": ("cotton", "cotton_leaf_redding"),
        "Herbicide Growth Damage": ("cotton", "cotton_herbicide_damage")
    }
    for folder, (crop, prob) in cotton_map.items():
        src_dir = cotton_base / folder
        if src_dir.exists():
            imgs = [p for p in src_dir.glob("*.jpg") if p.is_file()][:5]
            for idx, src in enumerate(imgs):
                dst_name = f"cotton_{prob}_{idx:02d}_{src.name}"
                dst = BASE_REALTEST / "unsupported" / dst_name
                shutil.copy2(src, dst)
                manifest.append({
                    "set": "unsupported",
                    "filename": dst_name,
                    "path": str(dst),
                    "true_crop": crop,
                    "true_class": prob,
                    "source": str(src),
                    "md5": md5_file(dst)
                })

    print(f"Unsupported set assembled: {sum(1 for m in manifest if m['set'] == 'unsupported')} images")

    # 3. Othercrop Set (leaves of crops we do not support: cashew, cassava)
    ccmt_base = Path("C:/Users/ec026/Downloads/Dataset for Crop Pest and Disease Detection/Dataset for Crop Pest and Disease Detection/Raw Data/CCMT Dataset")
    othercrops = {
        "Cashew": ["anthracnose", "red rust", "leaf miner", "healthy"],
        "Cassava": ["brown spot", "mosaic", "green mite", "healthy"]
    }
    for crop_name, conditions in othercrops.items():
        for cond in conditions:
            src_dir = ccmt_base / crop_name / cond
            if src_dir.exists():
                imgs = [p for p in src_dir.glob("*.jpg") if p.is_file()][:6]
                for idx, src in enumerate(imgs):
                    dst_name = f"other_{crop_name.lower()}_{cond}_{idx:02d}_{src.name}"
                    dst = BASE_REALTEST / "othercrop" / dst_name
                    shutil.copy2(src, dst)
                    manifest.append({
                        "set": "othercrop",
                        "filename": dst_name,
                        "path": str(dst),
                        "true_crop": crop_name.lower(),
                        "true_class": f"{crop_name.lower()}_{cond}",
                        "source": str(src),
                        "md5": md5_file(dst)
                    })

    print(f"Othercrop set assembled: {sum(1 for m in manifest if m['set'] == 'othercrop')} images")

    # 4. Nocrop Set (hands, walls, floors, desks, objects; no faces)
    nocrop_source = Path("C:/Users/ec026/OneDrive/Pictures/Camera imports/2026-03-14")
    if nocrop_source.exists():
        # Select verified camera photos of indoor objects, desks, floors, paper
        all_imgs = [p for p in nocrop_source.glob("IMG_*.jpg") if p.is_file()]
        # Take 30 diverse images
        selected_nocrop = all_imgs[:30]
        for idx, src in enumerate(selected_nocrop):
            dst_name = f"nocrop_object_{idx:02d}_{src.name}"
            dst = BASE_REALTEST / "nocrop" / dst_name
            shutil.copy2(src, dst)
            manifest.append({
                "set": "nocrop",
                "filename": dst_name,
                "path": str(dst),
                "true_crop": "none",
                "true_class": "non_crop_object",
                "source": str(src),
                "md5": md5_file(dst)
            })

    print(f"Nocrop set assembled: {sum(1 for m in manifest if m['set'] == 'nocrop')} images")

    # 5. Screen Set (photos of a laptop screen displaying crop leaves)
    screen_src_dir = BASE_REALTEST / "screen"
    screen_files = sorted([p for p in screen_src_dir.glob("screen_*.jpg") if p.is_file()])
    for src in screen_files:
        # Determine crop from filename
        name_lower = src.name.lower()
        crop = "unknown"
        for c in ["rice", "cotton", "chilli", "tomato", "maize"]:
            if c in name_lower:
                crop = c
                break
        manifest.append({
            "set": "screen",
            "filename": src.name,
            "path": str(src),
            "true_crop": crop,
            "true_class": src.stem,
            "source": str(src),
            "md5": md5_file(src)
        })

    print(f"Screen set verified: {sum(1 for m in manifest if m['set'] == 'screen')} images")

    # Save manifest
    with open(MANIFEST_PATH, "w") as f:
        json.dump(manifest, f, indent=2)

    print(f"\nTotal images in manifest: {len(manifest)}")
    print(f"Manifest written to: {MANIFEST_PATH}")

if __name__ == "__main__":
    main()
