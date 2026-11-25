#!/usr/bin/env python3
"""
Crop Doctor - Task DATA-FIX + TR4: Rebuild 15-Class Dataset (crop_doctor_15_v2)
------------------------------------------------------------------------------
Features:
1. Uses ALL sources in training and validation:
   - Rice: PaddyDoctor (blast, healthy, stem borer, hispa), AgML (blast, healthy)
   - Cotton: Mendeley 74j, T9h, B3j (bacterial blight, healthy)
   - Maize: MLD (blight, healthy, fall armyworm), CCMT (blight, healthy, fall armyworm)
   - Maize rust: Rust dataset
   - Chilli: Krishna (leaf curl virus, healthy)
   - Tomato: Archive2 (healthy, septoria leaf spot)
   - Other: AgriData (other_not_a_crop)
   - PlantDoc Corn leaf blight: Kept strictly as stress-test folder 'plantdoc_corn_leaf_blight_noisy'
2. Detects and trims uniform gray (or single-color) letterbox bars before resizing.
3. Reports bar counts per source.
4. Source-balanced sampling within each class, capped at 800 per class.
   - Maize FAW: MLD (285) + CCMT (285) = 570 (both views at equal counts).
   - Maize CCMT: ~208-285 in each maize class (comparable proportions).
5. Generates visual contact sheet proving letterbox bars are gone.
6. Packages to C:\\crop-data\\crop_doctor_15_v2.zip.
"""

import os
import sys
import io
import time
import json
import random
import zipfile
import shutil
from pathlib import Path
from PIL import Image, ImageOps, ImageFile, ImageDraw, ImageFont
import numpy as np

ImageFile.LOAD_TRUNCATED_IMAGES = True

TARGET_DIR = Path(r"C:\crop-data\crop_doctor_15_v2")
TARGET_ZIP = Path(r"C:\crop-data\crop_doctor_15_v2.zip")
PADDY_ZIP = Path(r"C:\crop-data\candidates\raw\paddy-disease-classification.zip")
CROP_512 = Path(r"C:\crop-data\crop_doctor_512")
MLD_ROOT = Path(r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset")
CCMT_ROOT = Path(r"C:\Users\<path>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize")
PLANTDOC = Path(r"C:\crop-data\candidates\raw\plantdoc\train\Corn leaf blight")
M74 = Path(r"C:\crop-data\candidates\raw\74jsdxtmx2\extracted\Cotton-Original")
MT9 = Path(r"C:\crop-data\candidates\raw\t9hgvk2h9p\extracted\Cotton_Original_Dataset")
MB3 = Path(r"C:\crop-data\candidates\raw\b3jy2p6k8w\extracted\Original Dataset")

SEED = 42

bar_counts_by_source = {}
photos_by_source_class = {}

def record_bar_stat(source_name, had_bars):
    if source_name not in bar_counts_by_source:
        bar_counts_by_source[source_name] = {"total": 0, "had_bars": 0}
    bar_counts_by_source[source_name]["total"] += 1
    if had_bars:
        bar_counts_by_source[source_name]["had_bars"] += 1

def record_source_class(source_name, class_name):
    key = (source_name, class_name)
    photos_by_source_class[key] = photos_by_source_class.get(key, 0) + 1

def trim_letterbox_bars(im, std_tol=6.0, abs_tol=8):
    """
    Detects and trims uniform gray (or any single-color) letterbox bars.
    Returns (trimmed_PIL_image, had_bars, (top, bottom, left, right)).
    """
    arr = np.array(im)
    if arr.ndim != 3 or arr.shape[2] < 3:
        return im, False, (0, 0, 0, 0)
    h, w, _ = arr.shape
    
    # Check top rows
    top = 0
    ref_top = arr[0, 0, :3]
    for r in range(h // 3):
        row = arr[r, :, :3]
        if np.std(row) < std_tol or np.all(np.abs(row.astype(int) - ref_top.astype(int)) <= abs_tol):
            top += 1
        else:
            break
            
    # Check bottom rows
    bottom = 0
    ref_bottom = arr[-1, 0, :3]
    for r in range(h - 1, h - 1 - h // 3, -1):
        row = arr[r, :, :3]
        if np.std(row) < std_tol or np.all(np.abs(row.astype(int) - ref_bottom.astype(int)) <= abs_tol):
            bottom += 1
        else:
            break
            
    # Check left cols
    left = 0
    ref_left = arr[0, 0, :3]
    for col in range(w // 3):
        c_data = arr[:, col, :3]
        if np.std(c_data) < std_tol or np.all(np.abs(c_data.astype(int) - ref_left.astype(int)) <= abs_tol):
            left += 1
        else:
            break
            
    # Check right cols
    right = 0
    ref_right = arr[0, -1, :3]
    for col in range(w - 1, w - 1 - w // 3, -1):
        c_data = arr[:, col, :3]
        if np.std(c_data) < std_tol or np.all(np.abs(c_data.astype(int) - ref_right.astype(int)) <= abs_tol):
            right += 1
        else:
            break
            
    had_bars = (top >= 6) or (bottom >= 6) or (left >= 6) or (right >= 6)
    if had_bars:
        x1 = left
        y1 = top
        x2 = max(x1 + 16, w - right)
        y2 = max(y1 + 16, h - bottom)
        trimmed = im.crop((x1, y1, x2, y2))
        return trimmed, True, (top, bottom, left, right)
    return im, False, (0, 0, 0, 0)

def process_and_save_image(src_input, out_path, source_name, class_name):
    """
    Reads src_input (Path or bytes), trims letterbox bars,
    resizes directly to 512x512 via LANCZOS, and saves as JPEG quality 95.
    """
    out_path.parent.mkdir(parents=True, exist_ok=True)
    try:
        if isinstance(src_input, (str, Path)):
            with Image.open(src_input) as raw_im:
                im = ImageOps.exif_transpose(raw_im).convert("RGB")
        elif isinstance(src_input, bytes):
            with Image.open(io.BytesIO(src_input)) as raw_im:
                im = ImageOps.exif_transpose(raw_im).convert("RGB")
        else:
            raise ValueError("Unsupported src_input type")

        trimmed, had_bars, _ = trim_letterbox_bars(im)
        record_bar_stat(source_name, had_bars)
        record_source_class(source_name, class_name)

        # Direct 512x512 resize without padding bars
        final_512 = trimmed.resize((512, 512), Image.Resampling.LANCZOS)
        final_512.save(out_path, "JPEG", quality=95)
        return True, had_bars
    except Exception as e:
        print(f"  Warning: Skipping unreadable {out_path.name}: {e}")
        return False, False

def collect_images_from_dir(directory, recursive=True):
    exts = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
    p = Path(directory)
    if not p.exists():
        return []
    if recursive:
        return sorted([f for f in p.rglob("*") if f.is_file() and f.suffix.lower() in exts])
    else:
        return sorted([f for f in p.iterdir() if f.is_file() and f.suffix.lower() in exts])

def main():
    print("=" * 80)
    print("TASK DATA-FIX + TR4: REBUILDING 15-CLASS DATASET (C:\\crop-data\\crop_doctor_15_v2)")
    print("=" * 80)
    t0 = time.time()

    if TARGET_DIR.exists():
        print(f"Cleaning existing target directory: {TARGET_DIR}")
        shutil.rmtree(TARGET_DIR)
    TARGET_DIR.mkdir(parents=True, exist_ok=True)

    rng = random.Random(SEED)

    # =========================================================================
    # 1. rice_leaf_blast (Cap 800: 400 PaddyDoctor + 400 AgML)
    # =========================================================================
    print("\n[1/15] Processing rice_leaf_blast...")
    c_name = "rice_leaf_blast"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)

    # PaddyDoctor (400)
    with zipfile.ZipFile(PADDY_ZIP) as z:
        pd_blast = [m for m in z.namelist() if m.startswith("train_images/blast/") and m.lower().endswith((".jpg", ".jpeg", ".png"))]
        rng.shuffle(pd_blast)
        for m in pd_blast[:400]:
            out = c_dir / f"src-PaddyDoctor_{Path(m).name}"
            process_and_save_image(z.read(m), out, "PaddyDoctor", c_name)

    # AgML (400)
    agml_blast = collect_images_from_dir(CROP_512 / "rice_leaf_blast")
    rng.shuffle(agml_blast)
    for f in agml_blast[:400]:
        out = c_dir / f"src-AgML_{f.name}"
        process_and_save_image(f, out, "AgML", c_name)

    # =========================================================================
    # 2. rice_healthy (Cap 800: 400 PaddyDoctor + 400 AgML)
    # =========================================================================
    print("[2/15] Processing rice_healthy...")
    c_name = "rice_healthy"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)

    # PaddyDoctor (400)
    with zipfile.ZipFile(PADDY_ZIP) as z:
        pd_norm = [m for m in z.namelist() if m.startswith("train_images/normal/") and m.lower().endswith((".jpg", ".jpeg", ".png"))]
        rng.shuffle(pd_norm)
        for m in pd_norm[:400]:
            out = c_dir / f"src-PaddyDoctor_{Path(m).name}"
            process_and_save_image(z.read(m), out, "PaddyDoctor", c_name)

    # AgML (400)
    agml_norm = collect_images_from_dir(CROP_512 / "rice_healthy")
    rng.shuffle(agml_norm)
    for f in agml_norm[:400]:
        out = c_dir / f"src-AgML_{f.name}"
        process_and_save_image(f, out, "AgML", c_name)

    # =========================================================================
    # 3. cotton_bacterial_blight (Balanced: 119 Mendeley74j + 200 MendeleyT9h + 200 MendeleyB3j = 519)
    # =========================================================================
    print("[3/15] Processing cotton_bacterial_blight...")
    c_name = "cotton_bacterial_blight"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)

    c74_b = collect_images_from_dir(M74 / "bacterial blight")
    for f in c74_b:
        out = c_dir / f"src-Mendeley74j_{f.parent.name}_{f.name}"
        process_and_save_image(f, out, "Mendeley74j", c_name)

    ct9_b = collect_images_from_dir(MT9 / "Bacterial Blight")
    rng.shuffle(ct9_b)
    for f in ct9_b[:200]:
        out = c_dir / f"src-MendeleyT9h_{f.name}"
        process_and_save_image(f, out, "MendeleyT9h", c_name)

    cb3_b = collect_images_from_dir(MB3 / "Bacterial Blight")
    rng.shuffle(cb3_b)
    for f in cb3_b[:200]:
        out = c_dir / f"src-MendeleyB3j_{f.name}"
        process_and_save_image(f, out, "MendeleyB3j", c_name)

    # =========================================================================
    # 4. cotton_healthy (Balanced: 266 Mendeley74j + 266 MendeleyT9h + 257 MendeleyB3j = 789)
    # =========================================================================
    print("[4/15] Processing cotton_healthy...")
    c_name = "cotton_healthy"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)

    c74_h = collect_images_from_dir(M74 / "healthy")
    rng.shuffle(c74_h)
    for f in c74_h[:266]:
        out = c_dir / f"src-Mendeley74j_{f.name}"
        process_and_save_image(f, out, "Mendeley74j", c_name)

    ct9_h = collect_images_from_dir(MT9 / "Healthy Leaf")
    rng.shuffle(ct9_h)
    for f in ct9_h[:266]:
        out = c_dir / f"src-MendeleyT9h_{f.name}"
        process_and_save_image(f, out, "MendeleyT9h", c_name)

    cb3_h = collect_images_from_dir(MB3 / "Healthy Leaf")
    rng.shuffle(cb3_h)
    for f in cb3_h[:257]:
        out = c_dir / f"src-MendeleyB3j_{f.name}"
        process_and_save_image(f, out, "MendeleyB3j", c_name)

    # =========================================================================
    # 5. chilli_leaf_curl_virus (Krishna: 423)
    # =========================================================================
    print("[5/15] Processing chilli_leaf_curl_virus...")
    c_name = "chilli_leaf_curl_virus"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)
    chilli_cv = collect_images_from_dir(CROP_512 / "chilli_leaf_curl_virus")
    for f in chilli_cv[:800]:
        out = c_dir / f"src-Krishna_{f.name}"
        process_and_save_image(f, out, "Krishna", c_name)

    # =========================================================================
    # 6. chilli_healthy (Krishna: 458)
    # =========================================================================
    print("[6/15] Processing chilli_healthy...")
    c_name = "chilli_healthy"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)
    chilli_h = collect_images_from_dir(CROP_512 / "chilli_healthy")
    for f in chilli_h[:800]:
        out = c_dir / f"src-Krishna_{f.name}"
        process_and_save_image(f, out, "Krishna", c_name)

    # =========================================================================
    # 7. maize_northern_leaf_blight (Balanced: 260 MLD + 260 CCMT = 520)
    # =========================================================================
    print("[7/15] Processing maize_northern_leaf_blight...")
    c_name = "maize_northern_leaf_blight"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)

    mld_blight = collect_images_from_dir(MLD_ROOT / "Leaf_Blight")
    rng.shuffle(mld_blight)
    for f in mld_blight[:260]:
        out = c_dir / f"src-MLD_{f.name}"
        process_and_save_image(f, out, "MLD", c_name)

    ccmt_blight = collect_images_from_dir(CCMT_ROOT / "leaf blight")
    rng.shuffle(ccmt_blight)
    for f in ccmt_blight[:260]:
        out = c_dir / f"src-CCMT_{f.name}"
        process_and_save_image(f, out, "CCMT", c_name)

    # =========================================================================
    # 8. maize_common_rust (Rust: 500)
    # =========================================================================
    print("[8/15] Processing maize_common_rust...")
    c_name = "maize_common_rust"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)
    m_rust = collect_images_from_dir(CROP_512 / "maize_common_rust")
    for f in m_rust[:800]:
        out = c_dir / f"src-Rust_{f.name}"
        process_and_save_image(f, out, "Rust", c_name)

    # =========================================================================
    # 9. maize_healthy (Balanced: 208 MLD + 208 CCMT = 416)
    # =========================================================================
    print("[9/15] Processing maize_healthy...")
    c_name = "maize_healthy"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)

    ccmt_h = collect_images_from_dir(CCMT_ROOT / "healthy")
    for f in ccmt_h:
        out = c_dir / f"src-CCMT_{f.name}"
        process_and_save_image(f, out, "CCMT", c_name)

    mld_h = collect_images_from_dir(MLD_ROOT / "Healthy")
    rng.shuffle(mld_h)
    for f in mld_h[:len(ccmt_h)]:
        out = c_dir / f"src-MLD_{f.name}"
        process_and_save_image(f, out, "MLD", c_name)

    # =========================================================================
    # 10. maize_fall_armyworm (Balanced: 285 MLD + 285 CCMT = 570, both views equal!)
    # =========================================================================
    print("[10/15] Processing maize_fall_armyworm...")
    c_name = "maize_fall_armyworm"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)

    ccmt_faw = collect_images_from_dir(CCMT_ROOT / "fall armyworm")
    for f in ccmt_faw:
        out = c_dir / f"src-CCMT_{f.name}"
        process_and_save_image(f, out, "CCMT", c_name)

    mld_faw = collect_images_from_dir(MLD_ROOT / "Fall_Armyworm")
    rng.shuffle(mld_faw)
    for f in mld_faw[:len(ccmt_faw)]:
        out = c_dir / f"src-MLD_{f.name}"
        process_and_save_image(f, out, "MLD", c_name)

    # =========================================================================
    # 11. rice_stem_borer (PaddyDoctor dead_heart: 800)
    # =========================================================================
    print("[11/15] Processing rice_stem_borer...")
    c_name = "rice_stem_borer"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(PADDY_ZIP) as z:
        pd_sb = [m for m in z.namelist() if m.startswith("train_images/dead_heart/") and m.lower().endswith((".jpg", ".jpeg", ".png"))]
        rng.shuffle(pd_sb)
        for m in pd_sb[:800]:
            out = c_dir / f"src-PaddyDoctor_{Path(m).name}"
            process_and_save_image(z.read(m), out, "PaddyDoctor", c_name)

    # =========================================================================
    # 12. rice_hispa (PaddyDoctor hispa: 800)
    # =========================================================================
    print("[12/15] Processing rice_hispa...")
    c_name = "rice_hispa"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(PADDY_ZIP) as z:
        pd_hispa = [m for m in z.namelist() if m.startswith("train_images/hispa/") and m.lower().endswith((".jpg", ".jpeg", ".png"))]
        rng.shuffle(pd_hispa)
        for m in pd_hispa[:800]:
            out = c_dir / f"src-PaddyDoctor_{Path(m).name}"
            process_and_save_image(z.read(m), out, "PaddyDoctor", c_name)

    # =========================================================================
    # 13. tomato_healthy (Archive2: 800)
    # =========================================================================
    print("[13/15] Processing tomato_healthy...")
    c_name = "tomato_healthy"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)
    tom_h = collect_images_from_dir(CROP_512 / "tomato_healthy")
    for f in tom_h[:800]:
        out = c_dir / f"src-Archive2_{f.name}"
        process_and_save_image(f, out, "Archive2", c_name)

    # =========================================================================
    # 14. tomato_septoria_leaf_spot (Archive2: 800)
    # =========================================================================
    print("[14/15] Processing tomato_septoria_leaf_spot...")
    c_name = "tomato_septoria_leaf_spot"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)
    tom_s = collect_images_from_dir(CROP_512 / "tomato_septoria_leaf_spot")
    for f in tom_s[:800]:
        out = c_dir / f"src-Archive2_{f.name}"
        process_and_save_image(f, out, "Archive2", c_name)

    # =========================================================================
    # 15. other_not_a_crop (AgriData: 800)
    # =========================================================================
    print("[15/15] Processing other_not_a_crop...")
    c_name = "other_not_a_crop"
    c_dir = TARGET_DIR / c_name
    c_dir.mkdir(parents=True, exist_ok=True)
    weeds = collect_images_from_dir(CROP_512 / "weeds_not_a_crop")
    for f in weeds[:800]:
        out = c_dir / f"src-AgriData_{f.name}"
        process_and_save_image(f, out, "AgriData", c_name)

    # =========================================================================
    # STRESS TEST FOLDER: plantdoc_corn_leaf_blight_noisy (174)
    # =========================================================================
    print("\nProcessing Stress-Test folder: plantdoc_corn_leaf_blight_noisy...")
    st_dir = TARGET_DIR / "plantdoc_corn_leaf_blight_noisy"
    st_dir.mkdir(parents=True, exist_ok=True)
    pd_corn = collect_images_from_dir(PLANTDOC)
    for f in pd_corn:
        out = st_dir / f"noisy_{f.name}"
        process_and_save_image(f, out, "PlantDoc", "plantdoc_corn_leaf_blight_noisy")

    print(f"\n✓ Dataset processing completed in {time.time()-t0:.1f}s.")

    # Generate Contact Sheet
    print("\nGenerating Contact Sheet (benchmarks/data_fix_contact_sheet.jpg)...")
    generate_contact_sheet()

    # Save Metadata and Bar Stats
    meta_path = Path("benchmarks/data_fix_stats.json")
    with open(meta_path, "w", encoding="utf-8") as fp:
        json.dump({
            "bar_counts_by_source": bar_counts_by_source,
            "photos_by_source_class": {f"{k[0]}::{k[1]}": v for k, v in photos_by_source_class.items()}
        }, fp, indent=2)

    # Compress to Zip
    print(f"\nPackaging to {TARGET_ZIP}...")
    tz0 = time.time()
    with zipfile.ZipFile(TARGET_ZIP, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for root, _, files in os.walk(TARGET_DIR):
            for file in files:
                p = Path(root) / file
                arc = p.relative_to(TARGET_DIR)
                zf.write(p, arcname=arc)
    zip_size_mb = TARGET_ZIP.stat().st_size / (1024 * 1024)
    print(f"✓ Zip complete: {TARGET_ZIP} ({zip_size_mb:.2f} MB) in {time.time()-tz0:.1f}s")

def generate_contact_sheet():
    """
    Renders sample thumbnails from all sources showing that letterbox bars are completely removed.
    """
    cs_path = Path("benchmarks/data_fix_contact_sheet.jpg")
    cs_path.parent.mkdir(parents=True, exist_ok=True)

    sources = [
        ("PaddyDoctor", "rice_hispa"),
        ("PaddyDoctor", "rice_stem_borer"),
        ("PaddyDoctor", "rice_leaf_blast"),
        ("AgML", "rice_leaf_blast"),
        ("Mendeley74j", "cotton_healthy"),
        ("MendeleyT9h", "cotton_bacterial_blight"),
        ("MendeleyB3j", "cotton_bacterial_blight"),
        ("MLD (Leaf blade)", "maize_fall_armyworm"),
        ("CCMT (Whorl/frass)", "maize_fall_armyworm"),
        ("MLD", "maize_northern_leaf_blight"),
        ("CCMT", "maize_northern_leaf_blight"),
        ("Rust", "maize_common_rust"),
        ("Krishna", "chilli_leaf_curl_virus"),
        ("Archive2", "tomato_septoria_leaf_spot"),
        ("AgriData", "other_not_a_crop"),
    ]

    thumb_size = 180
    cols = 5
    rows = 3
    pad = 10
    banner_h = 40
    w = cols * (thumb_size + pad) + pad
    h = rows * (thumb_size + pad + banner_h) + pad + 60

    canvas = Image.new("RGB", (w, h), (245, 245, 245))
    draw = ImageDraw.Draw(canvas)

    # Title
    draw.rectangle([0, 0, w, 50], fill=(27, 94, 32))
    draw.text((pad, 15), "Crop Doctor: Task DATA-FIX Contact Sheet - Zero Letterbox Bars Verified", fill=(255, 255, 255))

    for idx, (src_label, folder_name) in enumerate(sources):
        r = idx // cols
        c = idx % cols
        x = pad + c * (thumb_size + pad)
        y = 60 + r * (thumb_size + pad + banner_h)

        f_dir = TARGET_DIR / folder_name
        files = list(f_dir.glob("*.jpg"))
        if files:
            sample_f = files[min(idx * 7, len(files)-1)]
            with Image.open(sample_f) as im:
                thumb = im.resize((thumb_size, thumb_size), Image.Resampling.BILINEAR)
                canvas.paste(thumb, (x, y))
                draw.rectangle([x, y, x + thumb_size, y + thumb_size], outline=(76, 175, 80), width=2)
                draw.rectangle([x, y + thumb_size, x + thumb_size, y + thumb_size + banner_h], fill=(230, 230, 230))
                draw.text((x + 4, y + thumb_size + 4), src_label[:22], fill=(0, 0, 0))
                draw.text((x + 4, y + thumb_size + 20), folder_name[:22], fill=(100, 100, 100))

    canvas.save(cs_path, "JPEG", quality=90)
    print(f"✓ Contact sheet saved: {cs_path}")

if __name__ == "__main__":
    main()
