#!/usr/bin/env python3
"""
Crop Doctor - 4-Image per Class Dataset Collage Generator & Downloader
---------------------------------------------------------------------
For each of the 15 dataset classes, selects 4 diverse high-resolution photos
from C:\\crop-data\\crop_doctor_15_v2, creates a high-res 2x2 collage (1024x1134 px)
with full multilingual agronomic metadata (English, Telugu, Hindi, Pathogen),
and exports:
  1. benchmarks/collages_4images/<class_id>_4images.jpg (and .png)
  2. benchmarks/contact_sheets/<class_id>.jpg (for the interactive HTML phone tester)
  3. C:\\crop-data\\collages_4images/<class_id>_4images.jpg
  4. C:\\Users\\<USERNAME>\\Downloads\\crop_doctor_4image_collages/<class_id>_4images.jpg
  5. C:\\Users\\<USERNAME>\\Downloads\\crop_doctor_4image_collages.zip (Single-click complete download)
  6. benchmarks/crop_doctor_4image_collages.zip
  7. Updates master 15-class poster (benchmarks/crop_doctor_15_classes.png)
"""

import os
import sys
import shutil
import zipfile
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw, ImageFont

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = Path(r"C:\crop-data\crop_doctor_15_v2")

BENCHMARKS_DIR = PROJECT_ROOT / "benchmarks"
OUT_BENCHMARKS = BENCHMARKS_DIR / "collages_4images"
CONTACT_SHEETS_DIR = BENCHMARKS_DIR / "contact_sheets"
OUT_CROP_DATA = Path(r"C:\crop-data\collages_4images")
OUT_DOWNLOADS = Path(os.path.expanduser("~")) / "Downloads" / "crop_doctor_4image_collages"
ZIP_DOWNLOADS = Path(os.path.expanduser("~")) / "Downloads" / "crop_doctor_4image_collages.zip"
ZIP_BENCHMARKS = BENCHMARKS_DIR / "crop_doctor_4image_collages.zip"

for d in [OUT_BENCHMARKS, CONTACT_SHEETS_DIR, OUT_CROP_DATA, OUT_DOWNLOADS]:
    d.mkdir(parents=True, exist_ok=True)

CLASSES = [
    {
        "idx": "00", "crop": "RICE", "type": "DISEASE",
        "en": "Rice Leaf Blast",
        "te": "వరి అగ్గి తెగులు", "hi": "धान का झोंका रोग (ब्लास्ट)",
        "cause": "Magnaporthe oryzae", "id": "rice_leaf_blast",
    },
    {
        "idx": "01", "crop": "RICE", "type": "HEALTHY",
        "en": "Healthy Rice",
        "te": "ఆరోగ్యకరమైన వరి", "hi": "स्वस्थ धान",
        "cause": "None (Healthy plant leaf)", "id": "rice_healthy",
    },
    {
        "idx": "02", "crop": "COTTON", "type": "DISEASE",
        "en": "Cotton Bacterial Blight",
        "te": "పత్తి బ్యాక్టీరియా ఆకుమచ్చ తెగులు", "hi": "कपास का जीवाणु झुलसा",
        "cause": "Xanthomonas citri pv. malvacearum", "id": "cotton_bacterial_blight",
    },
    {
        "idx": "03", "crop": "COTTON", "type": "HEALTHY",
        "en": "Healthy Cotton",
        "te": "ఆరోగ్యకరమైన పత్తి", "hi": "स्वस्थ कपास",
        "cause": "None (Healthy plant leaf)", "id": "cotton_healthy",
    },
    {
        "idx": "04", "crop": "CHILLI", "type": "DISEASE",
        "en": "Chilli Leaf Curl Virus",
        "te": "మిరప ఆకు ముడత తెగులు", "hi": "मिर्च का पत्ती मरोड़ रोग",
        "cause": "Chilli leaf curl virus (ChiLCV)", "id": "chilli_leaf_curl_virus",
    },
    {
        "idx": "05", "crop": "CHILLI", "type": "HEALTHY",
        "en": "Healthy Chilli",
        "te": "ఆరోగ్యకరమైన మిరప", "hi": "स्वस्थ मिर्च",
        "cause": "None (Healthy plant leaf)", "id": "chilli_healthy",
    },
    {
        "idx": "06", "crop": "MAIZE", "type": "DISEASE",
        "en": "Maize Northern Leaf Blight",
        "te": "మొక్కజొన్న ఆకు మాడు తెగులు", "hi": "मक्का का उत्तरी पत्ती झुलसा",
        "cause": "Exserohilum turcicum", "id": "maize_northern_leaf_blight",
    },
    {
        "idx": "07", "crop": "MAIZE", "type": "DISEASE",
        "en": "Maize Common Rust",
        "te": "మొక్కజొన్న తుప్పు తెగులు", "hi": "मक्का का रतुआ रोग",
        "cause": "Puccinia sorghi", "id": "maize_common_rust",
    },
    {
        "idx": "08", "crop": "MAIZE", "type": "HEALTHY",
        "en": "Healthy Maize",
        "te": "ఆరోగ్యకరమైన మొక్కజొన్న", "hi": "स्वस्थ मक्का",
        "cause": "None (Healthy plant leaf)", "id": "maize_healthy",
    },
    {
        "idx": "09", "crop": "MAIZE", "type": "PEST",
        "en": "Maize Fall Armyworm",
        "te": "మొక్కజొన్న కత్తెర పురుగు", "hi": "मक्का का फॉल आर्मीवर्म",
        "cause": "Spodoptera frugiperda", "id": "maize_fall_armyworm",
    },
    {
        "idx": "10", "crop": "RICE", "type": "PEST",
        "en": "Rice Yellow Stem Borer",
        "te": "వరి కాండం తొలుచు పురుగు", "hi": "धान का पीला तना छेदक",
        "cause": "Scirpophaga incertulas", "id": "rice_stem_borer",
    },
    {
        "idx": "11", "crop": "RICE", "type": "PEST",
        "en": "Rice Hispa",
        "te": "వరి హిస్పా (ముళ్ల పురుగు)", "hi": "धान का हिस्पा कीट",
        "cause": "Dicladispa armigera", "id": "rice_hispa",
    },
    {
        "idx": "12", "crop": "TOMATO", "type": "HEALTHY",
        "en": "Healthy Tomato",
        "te": "ఆరోగ్యకరమైన టమాటా", "hi": "स्वस्थ टमाटर",
        "cause": "None (Healthy plant leaf)", "id": "tomato_healthy",
    },
    {
        "idx": "13", "crop": "TOMATO", "type": "DISEASE",
        "en": "Tomato Septoria Leaf Spot",
        "te": "టమాటా సెప్టోరియా ఆకుమచ్చ తెగులు", "hi": "टमाटर का सेप्टोरिया पत्ती धब्बा",
        "cause": "Septoria lycopersici", "id": "tomato_septoria_leaf_spot",
    },
    {
        "idx": "14", "crop": "GUARDRAIL", "type": "GUARDRAIL",
        "en": "Out of Class / Non-Crop",
        "te": "పంట కాని చిత్రం / ఇతర", "hi": "गैर-फसल / अन्य वस्तु",
        "cause": "Non-crop background / weeds", "id": "other_not_a_crop",
    }
]

def get_font(path_or_name, size, fallback="arial.ttf"):
    try:
        if os.path.exists(path_or_name):
            return ImageFont.truetype(path_or_name, size)
    except Exception:
        pass
    try:
        return ImageFont.truetype(fallback, size)
    except Exception:
        return ImageFont.load_default()

def draw_rounded_rect(draw, xy, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)

def pick_4_diverse_images(class_id: str):
    class_dir = DATA_DIR / class_id
    if not class_dir.exists():
        print(f"Warning: Class directory missing: {class_dir}")
        return []
    files = sorted([f for f in class_dir.glob("*.*") if f.suffix.lower() in (".jpg", ".jpeg", ".png")])
    n = len(files)
    if n == 0:
        return []
    if n <= 4:
        return files
    # Pick 4 diverse percentiles: 10%, 35%, 65%, 88%
    indices = [
        int(n * 0.10),
        int(n * 0.35),
        int(n * 0.65),
        min(int(n * 0.88), n - 1),
    ]
    return [files[i] for i in indices]

def create_class_collage(info: dict):
    class_id = info["id"]
    crop = info["crop"]
    c_type = info["type"]
    idx_str = info["idx"]
    en = info["en"]
    te = info["te"]
    hi = info["hi"]
    cause = info["cause"]

    selected_files = pick_4_diverse_images(class_id)
    if not selected_files:
        print(f"Skipping {class_id} (no images)")
        return

    # Geometry: 2 cols x 2 rows
    cols, rows = 2, 2
    cell_w, cell_h = 480, 480
    gap = 16
    pad = 24
    header_h = 130
    footer_h = 24

    total_w = pad * 2 + cols * cell_w + (cols - 1) * gap  # 24*2 + 960 + 16 = 1024
    total_h = header_h + rows * cell_h + (rows - 1) * gap + footer_h  # 130 + 960 + 16 + 24 = 1130

    # Fonts
    font_title = get_font("C:/Windows/Fonts/segoeuib.ttf", 26, "arialbd.ttf")
    font_badge = get_font("C:/Windows/Fonts/segoeuib.ttf", 15, "arialbd.ttf")
    font_sub = get_font("C:/Windows/Fonts/segoeui.ttf", 17, "arial.ttf")
    font_te = get_font(str(PROJECT_ROOT / "app/src/main/assets/fonts/NotoSansTelugu-Regular.ttf"), 17, "arial.ttf")
    font_hi = get_font(str(PROJECT_ROOT / "app/src/main/assets/fonts/NotoSansDevanagari-Regular.ttf"), 17, "arial.ttf")
    font_tag = get_font("C:/Windows/Fonts/segoeuib.ttf", 14, "arialbd.ttf")

    # Dark background theme
    canvas = Image.new("RGB", (total_w, total_h), color=(15, 23, 42))  # Slate 900
    draw = ImageDraw.Draw(canvas)

    # Top header bar
    badge_colors = {
        "DISEASE": {"bg": (127, 29, 29), "fg": (254, 202, 202), "label": "DISEASE"},
        "PEST": {"bg": (120, 53, 15), "fg": (254, 240, 138), "label": "PEST"},
        "HEALTHY": {"bg": (20, 83, 45), "fg": (187, 247, 208), "label": "HEALTHY"},
        "GUARDRAIL": {"bg": (51, 65, 85), "fg": (226, 232, 240), "label": "GUARDRAIL"}
    }
    bcfg = badge_colors.get(c_type, badge_colors["GUARDRAIL"])

    # Header Card Background
    draw_rounded_rect(draw, (pad, 16, total_w - pad, header_h), radius=12, fill=(30, 41, 59), outline=(51, 65, 85), width=1)

    # Line 1: Index tag + Badge
    draw.text((pad + 16, 26), f"#{idx_str} • {crop.upper()}", font=font_title, fill=(56, 189, 248))
    
    # Badge pill on top right
    b_text = f" {bcfg['label']} "
    b_w = len(b_text) * 10 + 20
    b_x1 = total_w - pad - 16
    b_x0 = b_x1 - b_w
    draw_rounded_rect(draw, (b_x0, 26, b_x1, 52), radius=13, fill=bcfg["bg"])
    draw.text((b_x0 + 10, 30), bcfg["label"], font=font_badge, fill=bcfg["fg"])

    # Line 2: English name
    draw.text((pad + 16, 58), en, font=font_title, fill=(255, 255, 255))

    # Line 3: Multilingual names + Cause
    local_text = f"తెలుగు: {te}   |   हिन्दी: {hi}   |   Cause: {cause}"
    draw.text((pad + 16, 92), local_text, font=font_sub, fill=(203, 213, 225))

    # Place the 4 images in 2x2 grid
    for idx, img_path in enumerate(selected_files[:4]):
        r = idx // cols
        c = idx % cols
        x0 = pad + c * (cell_w + gap)
        y0 = header_h + 12 + r * (cell_h + gap)
        x1 = x0 + cell_w
        y1 = y0 + cell_h

        # Load and center-crop to square
        try:
            with Image.open(img_path) as src:
                src = ImageOps.exif_transpose(src).convert("RGB")
                w, h = src.size
                min_dim = min(w, h)
                crop_box = ((w - min_dim) // 2, (h - min_dim) // 2, (w + min_dim) // 2, (h + min_dim) // 2)
                cropped = src.crop(crop_box).resize((cell_w, cell_h), Image.Resampling.LANCZOS)
                canvas.paste(cropped, (x0, y0))

                # Thin rounded border
                draw.rectangle([x0, y0, x1, y1], outline=(51, 65, 85), width=2)

                # Sample label pill at bottom-left of each photo
                tag_str = f" Photo {idx + 1} "
                draw_rounded_rect(draw, (x0 + 10, y1 - 32, x0 + 82, y1 - 8), radius=6, fill=(15, 23, 42, 220))
                draw.text((x0 + 16, y1 - 30), f"Sample {idx + 1}", font=font_tag, fill=(255, 255, 255))
        except Exception as e:
            print(f"Error loading {img_path}: {e}")
            draw.rectangle([x0, y0, x1, y1], fill=(30, 41, 59), outline=(51, 65, 85), width=2)

    # Save outputs across target locations
    filename_base = f"{class_id}_4images"
    
    # 1. Benchmarks 4images dir
    p_jpg = OUT_BENCHMARKS / f"{filename_base}.jpg"
    p_png = OUT_BENCHMARKS / f"{filename_base}.png"
    canvas.save(p_jpg, "JPEG", quality=92)
    canvas.save(p_png, "PNG")

    # 2. Benchmarks contact_sheets (for the HTML gallery)
    cs_jpg = CONTACT_SHEETS_DIR / f"{class_id}.jpg"
    canvas.save(cs_jpg, "JPEG", quality=90)
    if class_id == "other_not_a_crop":
        canvas.save(CONTACT_SHEETS_DIR / "weeds_not_a_crop.jpg", "JPEG", quality=90)

    # 3. C:\crop-data\collages_4images
    shutil.copy2(p_jpg, OUT_CROP_DATA / f"{filename_base}.jpg")

    # 4. Downloads folder
    shutil.copy2(p_jpg, OUT_DOWNLOADS / f"{filename_base}.jpg")

    print(f"✓ Generated 4-image collage for {class_id} -> {p_jpg.name}")
    return p_jpg

def build_zip_package():
    print("\nPackaging all 4-image collages into zip archives...")
    with zipfile.ZipFile(ZIP_DOWNLOADS, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(OUT_DOWNLOADS.glob("*.jpg")):
            z.write(f, arcname=f.name)
    shutil.copy2(ZIP_DOWNLOADS, ZIP_BENCHMARKS)
    print(f"✓ Created download zip: {ZIP_DOWNLOADS} ({ZIP_DOWNLOADS.stat().st_size / (1024*1024):.2f} MB)")
    print(f"✓ Mirrored to benchmarks: {ZIP_BENCHMARKS}")

def update_master_poster_and_gallery():
    poster_script = PROJECT_ROOT / "scripts" / "generate_15_classes_poster.py"
    if poster_script.exists():
        print("\nUpdating master 15-class poster...")
        import subprocess
        subprocess.run(["python", str(poster_script)], check=True)
        shutil.copy2(BENCHMARKS_DIR / "crop_doctor_15_classes.png", OUT_DOWNLOADS / "crop_doctor_15_classes.png")
        shutil.copy2(BENCHMARKS_DIR / "crop_doctor_15_classes.jpg", OUT_DOWNLOADS / "crop_doctor_15_classes.jpg")

    gallery_script = PROJECT_ROOT / "scripts" / "generate_test_gallery.py"
    if gallery_script.exists():
        print("\nUpdating HTML test gallery...")
        import subprocess
        subprocess.run(["python", str(gallery_script)], check=True)

def main():
    print("=" * 80)
    print("Crop Doctor - Generating 4-Image Collages for All 15 Classes")
    print("=" * 80)
    for c in CLASSES:
        create_class_collage(c)
    
    build_zip_package()
    update_master_poster_and_gallery()
    print("\nDone! All 4-image collages downloaded to disk.")

if __name__ == "__main__":
    main()
