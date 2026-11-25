#!/usr/bin/env python3
"""
Crop Doctor - Dataset Contact Sheet & Collage Generator
-------------------------------------------------------
Extracts representative photos from C:\\crop-data\\crop_doctor_15_v2
for all 15 classes and builds:
1. 30-sample contact sheets (6x5 grid, 960x840) for every class in benchmarks/contact_sheets/
2. Master 15-class poster collage in benchmarks/crop_doctor_15_classes.png
3. Diagnosis screens collage in benchmarks/diagnosis_screens_collage.png
4. Mirrors all collages to C:\\crop-data\\collages\\
"""

import os
import random
import shutil
import subprocess
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw, ImageFont

SEED = 42
random.seed(SEED)

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = Path(r"C:\crop-data\crop_doctor_15_v2")
BENCHMARKS_DIR = PROJECT_ROOT / "benchmarks"
CONTACT_SHEETS_DIR = BENCHMARKS_DIR / "contact_sheets"
CONTACT_SHEETS_DIR.mkdir(parents=True, exist_ok=True)

CROP_DATA_COLLAGES = Path(r"C:\crop-data\collages")
CROP_DATA_COLLAGES.mkdir(parents=True, exist_ok=True)

CLASSES = [
    "rice_leaf_blast",
    "rice_healthy",
    "cotton_bacterial_blight",
    "cotton_healthy",
    "chilli_leaf_curl_virus",
    "chilli_healthy",
    "maize_northern_leaf_blight",
    "maize_common_rust",
    "maize_healthy",
    "maize_fall_armyworm",
    "rice_stem_borer",
    "rice_hispa",
    "tomato_healthy",
    "tomato_septoria_leaf_spot",
    "other_not_a_crop",
]

def generate_contact_sheets():
    cols, rows = 6, 5
    cell_w, cell_h = 160, 160
    total_w = cols * cell_w
    total_h = rows * cell_h + 40

    print("Generating 15 dataset class contact sheets...")
    for c in CLASSES:
        class_dir = DATA_DIR / c
        if not class_dir.exists():
            print(f"Warning: Directory not found: {class_dir}")
            continue
        files = [f for f in class_dir.glob("*.*") if f.suffix.lower() in (".jpg", ".jpeg", ".png")]
        sample_files = random.sample(files, min(30, len(files)))

        montage = Image.new("RGB", (total_w, total_h), color=(240, 240, 240))
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
                    px = x + (cell_w - im.width) // 2
                    py = y + (cell_h - im.height) // 2
                    montage.paste(im, (px, py))
            except Exception as e:
                print(f"Error loading {f}: {e}")

        out_path = CONTACT_SHEETS_DIR / f"{c}.jpg"
        montage.save(out_path, quality=88)
        shutil.copy2(out_path, CROP_DATA_COLLAGES / f"{c}.jpg")
        if c == "other_not_a_crop":
            montage.save(CONTACT_SHEETS_DIR / "weeds_not_a_crop.jpg", quality=88)
            shutil.copy2(CONTACT_SHEETS_DIR / "weeds_not_a_crop.jpg", CROP_DATA_COLLAGES / "weeds_not_a_crop.jpg")
        print(f"  ✓ {out_path.name} ({len(files)} source images)")

def main():
    generate_contact_sheets()
    
    # Run master 15 classes poster generator
    poster_script = PROJECT_ROOT / "scripts" / "generate_15_classes_poster.py"
    if poster_script.exists():
        print("\nUpdating master 15-class poster...")
        subprocess.run(["python", str(poster_script)], check=True)
        shutil.copy2(BENCHMARKS_DIR / "crop_doctor_15_classes.png", CROP_DATA_COLLAGES / "crop_doctor_15_classes.png")
        shutil.copy2(BENCHMARKS_DIR / "crop_doctor_15_classes.jpg", CROP_DATA_COLLAGES / "crop_doctor_15_classes.jpg")

    # Run gallery generator
    gallery_script = PROJECT_ROOT / "scripts" / "generate_test_gallery.py"
    if gallery_script.exists():
        print("\nUpdating HTML test gallery...")
        subprocess.run(["python", str(gallery_script)], check=True)

    print("\nAll collages and contact sheets successfully generated on disk!")

if __name__ == "__main__":
    main()
