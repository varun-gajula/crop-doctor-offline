#!/usr/bin/env python3
"""
Crop Doctor - Prepare Unified 15-Class Dataset for Kaggle Training
------------------------------------------------------------------
Builds a unified, self-contained 15-class directory:
  C:\\crop-data\\crop_doctor_15class
and packages it into:
  C:\\crop-data\\crop_doctor_15class.zip

Features:
- Encodes held-out source tags directly into filenames:
    'src-AgML_...', 'src-PaddyDoctor_...', 'src-PlantDoc_...',
    'src-CCMT_...', 'src-MendeleyT9h_...', 'src-MendeleyB3j_...'
- Resizes raw images (pests, PlantDoc) to uniform 512x512 with centered gray padding.
- Copies existing 512x512 images directly.
- Maps 'weeds_not_a_crop' -> 'other_not_a_crop'.
- Capped at 800 images per source pool.
"""

import os
import sys
import io
import time
import zipfile
import shutil
from pathlib import Path
from PIL import Image, ImageOps

TARGET_DIR = Path(r"C:\crop-data\crop_doctor_15class")
TARGET_ZIP = Path(r"C:\crop-data\crop_doctor_15class.zip")
PADDY_ZIP = Path(r"C:\crop-data\candidates\raw\paddy-disease-classification.zip")
CROP_512 = Path(r"C:\crop-data\crop_doctor_512")

def resize_and_pad(im, size=512, pad_color=(114, 114, 114)):
    im = ImageOps.exif_transpose(im).convert("RGB")
    w, h = im.size
    scale = size / max(w, h)
    nw, nh = int(round(w * scale)), int(round(h * scale))
    resized = im.resize((nw, nh), Image.Resampling.LANCZOS)
    canvas = Image.new("RGB", (size, size), pad_color)
    paste_x = (size - nw) // 2
    paste_y = (size - nh) // 2
    canvas.paste(resized, (paste_x, paste_y))
    return canvas

def process_and_save(src_img, out_path):
    if out_path.exists():
        return
    out_path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(src_img, (str, Path)):
        with Image.open(src_img) as im:
            if im.size == (512, 512):
                shutil.copy2(src_img, out_path)
            else:
                padded = resize_and_pad(im)
                padded.save(out_path, "JPEG", quality=95)
    elif isinstance(src_img, bytes):
        with Image.open(io.BytesIO(src_img)) as im:
            if im.size == (512, 512):
                with open(out_path, "wb") as f:
                    f.write(src_img)
            else:
                padded = resize_and_pad(im)
                padded.save(out_path, "JPEG", quality=95)

def main():
    print("=" * 80)
    print("CROP DOCTOR: PREPARING UNIFIED 15-CLASS DATASET")
    print("=" * 80)
    t0 = time.time()
    TARGET_DIR.mkdir(parents=True, exist_ok=True)

    # 1. rice_leaf_blast
    print("\n[1/15] Processing rice_leaf_blast...")
    c_dir = TARGET_DIR / "rice_leaf_blast"
    c_dir.mkdir(parents=True, exist_ok=True)
    # PaddyDoctor (train/val)
    if PADDY_ZIP.exists():
        with zipfile.ZipFile(PADDY_ZIP) as z:
            members = [m for m in z.namelist() if m.startswith("train_images/blast/") and m.lower().endswith((".jpg", ".jpeg", ".png"))][:800]
            for m in members:
                out = c_dir / f"src-PaddyDoctor_{Path(m).name}"
                process_and_save(z.read(m), out)
    # AgML (held-out)
    agml_blast = CROP_512 / "rice_leaf_blast"
    if agml_blast.exists():
        for f in sorted(agml_blast.iterdir())[:800]:
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                out = c_dir / f"src-AgML_{f.name}"
                process_and_save(f, out)

    # 2. rice_healthy
    print("[2/15] Processing rice_healthy...")
    c_dir = TARGET_DIR / "rice_healthy"
    c_dir.mkdir(parents=True, exist_ok=True)
    # PaddyDoctor (train/val)
    if PADDY_ZIP.exists():
        with zipfile.ZipFile(PADDY_ZIP) as z:
            members = [m for m in z.namelist() if m.startswith("train_images/normal/") and m.lower().endswith((".jpg", ".jpeg", ".png"))][:800]
            for m in members:
                out = c_dir / f"src-PaddyDoctor_{Path(m).name}"
                process_and_save(z.read(m), out)
    # AgML (held-out)
    agml_healthy = CROP_512 / "rice_healthy"
    if agml_healthy.exists():
        for f in sorted(agml_healthy.iterdir())[:800]:
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                out = c_dir / f"src-AgML_{f.name}"
                process_and_save(f, out)

    # 3. cotton_bacterial_blight
    print("[3/15] Processing cotton_bacterial_blight...")
    c_dir = TARGET_DIR / "cotton_bacterial_blight"
    c_dir.mkdir(parents=True, exist_ok=True)
    p74j = Path(r"C:\crop-data\candidates\raw\74jsdxtmx2\extracted\Cotton-Original\bacterial blight")
    pt9h = Path(r"C:\crop-data\candidates\raw\t9hgvk2h9p\extracted\Cotton_Original_Dataset\Bacterial Blight")
    pb3j = Path(r"C:\crop-data\candidates\raw\b3jy2p6k8w\extracted\Original Dataset\Bacterial Blight")
    if p74j.exists():
        for f in p74j.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-Mendeley74j_{f.name}")
    if pb3j.exists():
        for f in pb3j.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-MendeleyB3j_{f.name}")
    if pt9h.exists(): # held-out
        for f in pt9h.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-MendeleyT9h_{f.name}")

    # 4. cotton_healthy
    print("[4/15] Processing cotton_healthy...")
    c_dir = TARGET_DIR / "cotton_healthy"
    c_dir.mkdir(parents=True, exist_ok=True)
    p74j = Path(r"C:\crop-data\candidates\raw\74jsdxtmx2\extracted\Cotton-Original\healthy")
    pt9h = Path(r"C:\crop-data\candidates\raw\t9hgvk2h9p\extracted\Cotton_Original_Dataset\Healthy Leaf")
    pb3j = Path(r"C:\crop-data\candidates\raw\b3jy2p6k8w\extracted\Original Dataset\Healthy Leaf")
    if p74j.exists():
        for f in list(p74j.iterdir())[:800]:
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-Mendeley74j_{f.name}")
    if pt9h.exists():
        for f in pt9h.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-MendeleyT9h_{f.name}")
    if pb3j.exists(): # held-out
        for f in pb3j.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-MendeleyB3j_{f.name}")

    # 5. chilli_leaf_curl_virus
    print("[5/15] Processing chilli_leaf_curl_virus...")
    c_dir = TARGET_DIR / "chilli_leaf_curl_virus"
    c_dir.mkdir(parents=True, exist_ok=True)
    src_chilli = CROP_512 / "chilli_leaf_curl_virus"
    if src_chilli.exists():
        for f in src_chilli.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-Krishna_{f.name}")

    # 6. chilli_healthy
    print("[6/15] Processing chilli_healthy...")
    c_dir = TARGET_DIR / "chilli_healthy"
    c_dir.mkdir(parents=True, exist_ok=True)
    src_chilli_h = CROP_512 / "chilli_healthy"
    if src_chilli_h.exists():
        for f in src_chilli_h.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-Krishna_{f.name}")

    # 7. maize_northern_leaf_blight
    print("[7/15] Processing maize_northern_leaf_blight...")
    c_dir = TARGET_DIR / "maize_northern_leaf_blight"
    c_dir.mkdir(parents=True, exist_ok=True)
    pmld = Path(r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Leaf_Blight")
    pcmt = Path(r"C:\Users\<USERNAME>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\leaf blight")
    ppd = Path(r"C:\crop-data\candidates\raw\plantdoc\train\Corn leaf blight")
    if pmld.exists():
        for f in pmld.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-MLD_{f.name}")
    if pcmt.exists():
        for f in list(pcmt.iterdir())[:800]:
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-CCMT_{f.name}")
    if ppd.exists(): # held-out
        for f in ppd.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-PlantDoc_{f.name}")

    # 8. maize_common_rust
    print("[8/15] Processing maize_common_rust...")
    c_dir = TARGET_DIR / "maize_common_rust"
    c_dir.mkdir(parents=True, exist_ok=True)
    src_rust = CROP_512 / "maize_common_rust"
    if src_rust.exists():
        for f in src_rust.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-Rust_{f.name}")

    # 9. maize_healthy
    print("[9/15] Processing maize_healthy...")
    c_dir = TARGET_DIR / "maize_healthy"
    c_dir.mkdir(parents=True, exist_ok=True)
    pmld_h = Path(r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Healthy")
    pcmt_h = Path(r"C:\Users\<USERNAME>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\healthy")
    if pmld_h.exists():
        for f in pmld_h.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-MLD_{f.name}")
    if pcmt_h.exists(): # held-out
        for f in pcmt_h.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-CCMT_{f.name}")

    # 10. maize_fall_armyworm
    print("[10/15] Processing maize_fall_armyworm...")
    c_dir = TARGET_DIR / "maize_fall_armyworm"
    c_dir.mkdir(parents=True, exist_ok=True)
    pfaw_mld = Path(r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Fall_Armyworm")
    pfaw_ccmt = Path(r"C:\Users\<USERNAME>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\fall armyworm")
    if pfaw_mld.exists():
        for f in pfaw_mld.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-MLD_{f.name}")
    if pfaw_ccmt.exists(): # held-out
        for f in pfaw_ccmt.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-CCMT_{f.name}")

    # 11. rice_stem_borer
    print("[11/15] Processing rice_stem_borer...")
    c_dir = TARGET_DIR / "rice_stem_borer"
    c_dir.mkdir(parents=True, exist_ok=True)
    if PADDY_ZIP.exists():
        with zipfile.ZipFile(PADDY_ZIP) as z:
            members = [m for m in z.namelist() if m.startswith("train_images/dead_heart/") and m.lower().endswith((".jpg", ".jpeg", ".png"))][:800]
            for m in members:
                out = c_dir / f"src-PaddyDoctor_{Path(m).name}"
                process_and_save(z.read(m), out)

    # 12. rice_hispa
    print("[12/15] Processing rice_hispa...")
    c_dir = TARGET_DIR / "rice_hispa"
    c_dir.mkdir(parents=True, exist_ok=True)
    if PADDY_ZIP.exists():
        with zipfile.ZipFile(PADDY_ZIP) as z:
            members = [m for m in z.namelist() if m.startswith("train_images/hispa/") and m.lower().endswith((".jpg", ".jpeg", ".png"))][:800]
            for m in members:
                out = c_dir / f"src-PaddyDoctor_{Path(m).name}"
                process_and_save(z.read(m), out)

    # 13. tomato_healthy
    print("[13/15] Processing tomato_healthy...")
    c_dir = TARGET_DIR / "tomato_healthy"
    c_dir.mkdir(parents=True, exist_ok=True)
    src_tom_h = CROP_512 / "tomato_healthy"
    if src_tom_h.exists():
        for f in src_tom_h.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-Archive2_{f.name}")

    # 14. tomato_septoria_leaf_spot
    print("[14/15] Processing tomato_septoria_leaf_spot...")
    c_dir = TARGET_DIR / "tomato_septoria_leaf_spot"
    c_dir.mkdir(parents=True, exist_ok=True)
    src_tom_s = CROP_512 / "tomato_septoria_leaf_spot"
    if src_tom_s.exists():
        for f in src_tom_s.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-Archive2_{f.name}")

    # 15. other_not_a_crop
    print("[15/15] Processing other_not_a_crop...")
    c_dir = TARGET_DIR / "other_not_a_crop"
    c_dir.mkdir(parents=True, exist_ok=True)
    src_weeds = CROP_512 / "weeds_not_a_crop"
    if src_weeds.exists():
        for f in src_weeds.iterdir():
            if f.is_file() and f.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                process_and_save(f, c_dir / f"src-AgriData_{f.name}")

    print("\nDataset assembly complete in {:.1f}s.".format(time.time() - t0))
    print("\nClass Counts in C:\\crop-data\\crop_doctor_15class:")
    total = 0
    for d in sorted(TARGET_DIR.iterdir()):
        if d.is_dir():
            n = len(list(d.iterdir()))
            total += n
            print(f"  - {d.name:28s}: {n:4d} photos")
    print(f"Total photos assembled: {total:,}")

    # Zip packaging
    print(f"\nCompressing to {TARGET_ZIP}...")
    tz0 = time.time()
    with zipfile.ZipFile(TARGET_ZIP, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for root, _, files in os.walk(TARGET_DIR):
            for file in files:
                p = Path(root) / file
                arc = p.relative_to(TARGET_DIR.parent)
                zf.write(p, arcname=arc)
    print(f"✓ Zip complete: {TARGET_ZIP} ({TARGET_ZIP.stat().st_size / (1024*1024):.1f} MB) in {time.time()-tz0:.1f}s")

if __name__ == "__main__":
    main()
