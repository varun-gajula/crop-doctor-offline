#!/usr/bin/env python3
"""
Crop Doctor - Task DIAG1: Held-Out Failure Diagnosis & Contact Sheet Generator
-----------------------------------------------------------------------------
1. Evaluates all held-out photos from D7 using best_model.pth.
2. Computes the predicted-class distribution for each held-out class.
3. Generates benchmarks/diag1_heldout_vs_train.jpg (24 held-out vs 24 train photos per class).
4. Analyzes visual domain differences (background, framing, camera, resolution, lighting, concepts).
5. Outputs benchmarks/diag1_analysis.json and logs for the DIAG1 report.
"""

import os
import sys
import json
import random
from pathlib import Path
from collections import Counter
import numpy as np
from PIL import Image, ImageOps, ImageDraw, ImageFont
import torch
from torchvision import models, transforms

# Set seed for reproducible 24-photo selection
random.seed(42)

BASE_15CLASS = Path(r"C:\crop-data\crop_doctor_15class")
RAW_BASE = Path(r"C:\crop-data\candidates\raw")
CCMT_RAW_BASE = Path(r"C:\Users\<path>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize")
CKPT_PATH = Path(r"C:\Users\<path>\Downloads\crop_doctor_final_15class_20ep\best_model.pth")
OUTPUT_SHEET = Path(r"C:\crop-doctor-offline\benchmarks\diag1_heldout_vs_train.jpg")
OUTPUT_JSON = Path(r"C:\crop-doctor-offline\benchmarks\diag1_analysis.json")

DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# Load model
print(f"Loading checkpoint from {CKPT_PATH}...")
ckpt = torch.load(CKPT_PATH, map_location=DEVICE, weights_only=False)
CLASS_NAMES = ckpt["class_names"]

model = models.mobilenet_v3_large(weights=None)
model.classifier[3] = torch.nn.Linear(model.classifier[3].in_features, len(CLASS_NAMES))
model.load_state_dict(ckpt["model_state"])
model = model.to(DEVICE).eval()

val_transform = transforms.Compose([
    transforms.Resize((512, 512)),
    transforms.ToTensor(),
    transforms.Normalize((0.485, 0.456, 0.406), (0.229, 0.224, 0.225)),
])

# Define the classes to analyze
# (5 failed held-out classes from Kaggle run + 2 cotton classes from D7 table)
CLASSES_CONFIG = [
    {
        "class_name": "rice_leaf_blast",
        "held_out_prefix": "src-AgML",
        "train_prefix": "src-PaddyDoctor",
        "held_out_desc": "AgML (Synthetic/Augmented Leaf Scans)",
        "train_desc": "PaddyDoctor (Tamil Nadu Field Smartphone)",
        "category": "5_FAILED",
    },
    {
        "class_name": "rice_healthy",
        "held_out_prefix": "src-AgML",
        "train_prefix": "src-PaddyDoctor",
        "held_out_desc": "AgML (Synthetic/Augmented Healthy Scans)",
        "train_desc": "PaddyDoctor (Tamil Nadu Field Smartphone)",
        "category": "5_FAILED",
    },
    {
        "class_name": "maize_northern_leaf_blight",
        "held_out_prefix": "src-PlantDoc",
        "train_prefix": "src-MLD",
        "held_out_desc": "PlantDoc (Wild Web In-Field Whole Plants)",
        "train_desc": "MLD (Controlled 4:3 Leaf Cuts)",
        "category": "5_FAILED",
    },
    {
        "class_name": "maize_healthy",
        "held_out_prefix": "src-CCMT",
        "train_prefix": "src-MLD",
        "held_out_desc": "CCMT Ghana (Farm Canopy 1:1 Crops)",
        "train_desc": "MLD (Controlled 4:3 Leaf Cuts)",
        "category": "5_FAILED",
    },
    {
        "class_name": "maize_fall_armyworm",
        "held_out_prefix": "src-CCMT",
        "train_prefix": "src-MLD",
        "held_out_desc": "CCMT Ghana (Farm Canopy 1:1 Crops)",
        "train_desc": "MLD (Controlled 4:3 Leaf Cuts)",
        "category": "5_FAILED",
    },
    {
        "class_name": "cotton_bacterial_blight",
        "held_out_prefix": "src-MendeleyT9h",
        "train_prefix": "src-MendeleyB3j",
        "held_out_desc": "Mendeley T9h (Field Leaf Close-ups)",
        "train_desc": "Mendeley B3j (Field Leaf Close-ups)",
        "category": "D7_COTTON",
    },
    {
        "class_name": "cotton_healthy",
        "held_out_prefix": "src-MendeleyB3j",
        "train_prefix": "src-Mendeley74j",
        "held_out_desc": "Mendeley B3j (Field Leaf Close-ups)",
        "train_desc": "Mendeley 74j (Field Leaf Close-ups)",
        "category": "D7_COTTON",
    },
]

# Run full evaluation on all held-out images
eval_results = {}

print("\n" + "=" * 80)
print("1. RUNNING INFERENCE ON ALL HELD-OUT PHOTOS")
print("=" * 80)

with torch.no_grad():
    for cfg in CLASSES_CONFIG:
        c_name = cfg["class_name"]
        ho_pref = cfg["held_out_prefix"]
        tr_pref = cfg["train_prefix"]
        c_dir = BASE_15CLASS / c_name
        
        ho_files = sorted([f for f in c_dir.iterdir() if f.is_file() and f.name.startswith(ho_pref)])
        tr_files = sorted([f for f in c_dir.iterdir() if f.is_file() and f.name.startswith(tr_pref)])
        
        preds_counter = Counter()
        correct = 0
        total = len(ho_files)
        file_preds = {}
        
        for f in ho_files:
            with Image.open(f) as im:
                im_rgb = ImageOps.exif_transpose(im).convert("RGB")
                tensor = val_transform(im_rgb).unsqueeze(0).to(DEVICE)
                logits = model(tensor)
                pred_idx = logits.argmax(dim=1).item()
                pred_cls = CLASS_NAMES[pred_idx]
                preds_counter[pred_cls] += 1
                file_preds[f.name] = pred_cls
                if pred_cls == c_name:
                    correct += 1
        
        accuracy = correct / total if total > 0 else 0.0
        eval_results[c_name] = {
            "held_out_source": cfg["held_out_desc"],
            "train_source": cfg["train_desc"],
            "total_held_out": total,
            "correct_held_out": correct,
            "accuracy": round(accuracy, 4),
            "distribution": dict(preds_counter.most_common()),
            "total_train_available": len(tr_files),
        }
        
        cfg["ho_files"] = ho_files
        cfg["tr_files"] = tr_files
        cfg["file_preds"] = file_preds
        cfg["accuracy"] = accuracy
        cfg["distribution"] = dict(preds_counter.most_common())
        
        print(f"\n--- {c_name} ---")
        print(f"Held-out source: {cfg['held_out_desc']} ({ho_pref})")
        print(f"Total: {total} photos | Correct: {correct} | Accuracy: {accuracy:.2%}")
        print("Predicted distribution:")
        for pred_cls, count in preds_counter.most_common():
            pct = count / total * 100
            print(f"  {pred_cls:28s}: {count:4d} ({pct:5.1f}%)")

# Save quantitative results
OUTPUT_JSON.parent.mkdir(parents=True, exist_ok=True)
with open(OUTPUT_JSON, "w") as fp:
    json.dump(eval_results, fp, indent=2)
print(f"\nSaved evaluation metrics to {OUTPUT_JSON}")

# 2. Build Contact Sheet
print("\n" + "=" * 80)
print("2. BUILDING CONTACT SHEET: diag1_heldout_vs_train.jpg")
print("=" * 80)

THUMB_W = 110
THUMB_H = 110
COLS = 6
ROWS = 4
PER_SIDE = COLS * ROWS # 24

GRID_W = COLS * THUMB_W
GRID_H = ROWS * THUMB_H

MARGIN_X = 40
SPACER_MID = 60
TOTAL_W = MARGIN_X + GRID_W + SPACER_MID + GRID_W + MARGIN_X # 40 + 660 + 60 + 660 + 40 = 1460

HEADER_TOP = 110
CLASS_BLOCK_H = 36 + 28 + GRID_H + 32 # Title + Subheader + Grid + Padding = ~646
TOTAL_H = HEADER_TOP + len(CLASSES_CONFIG) * CLASS_BLOCK_H + 40

sheet = Image.new("RGB", (TOTAL_W, TOTAL_H), (245, 246, 248))
draw = ImageDraw.Draw(sheet)

# Try loading standard truetype font
try:
    font_title = ImageFont.truetype("arial.ttf", 26)
    font_class = ImageFont.truetype("arial.ttf", 20)
    font_sub = ImageFont.truetype("arial.ttf", 15)
    font_small = ImageFont.truetype("arial.ttf", 11)
    font_tiny = ImageFont.truetype("arial.ttf", 10)
except:
    font_title = ImageFont.load_default()
    font_class = ImageFont.load_default()
    font_sub = ImageFont.load_default()
    font_small = ImageFont.load_default()
    font_tiny = ImageFont.load_default()

# Draw Main Sheet Header
draw.rectangle([(0, 0), (TOTAL_W, HEADER_TOP - 15)], fill=(20, 30, 45))
draw.text((MARGIN_X, 18), "Crop Doctor - Task DIAG1: Held-Out Source vs Training Distribution Audit", fill=(255, 255, 255), font=font_title)
draw.text((MARGIN_X, 54), "Model: MobileNetV3-Large 15-Class (20 Epochs) | Checkpoint: best_model.pth", fill=(180, 205, 235), font=font_sub)
draw.text((MARGIN_X, 76), "Visual Comparison: 24 Held-Out Photos (Left) vs 24 Training Photos (Right) | Red border = Incorrect prediction", fill=(220, 220, 220), font=font_small)

curr_y = HEADER_TOP

for idx, cfg in enumerate(CLASSES_CONFIG, 1):
    c_name = cfg["class_name"]
    ho_pref = cfg["held_out_prefix"]
    tr_pref = cfg["train_prefix"]
    ho_desc = cfg["held_out_desc"]
    tr_desc = cfg["train_desc"]
    acc = cfg["accuracy"]
    file_preds = cfg["file_preds"]
    
    # Select 24 sample photos for held-out and train
    ho_all = cfg["ho_files"]
    tr_all = cfg["tr_files"]
    
    # Deterministic stride selection to span the full set
    if len(ho_all) >= PER_SIDE:
        indices = np.linspace(0, len(ho_all) - 1, PER_SIDE, dtype=int)
        ho_samples = [ho_all[i] for i in indices]
    else:
        ho_samples = ho_all[:PER_SIDE]
        
    if len(tr_all) >= PER_SIDE:
        indices = np.linspace(0, len(tr_all) - 1, PER_SIDE, dtype=int)
        tr_samples = [tr_all[i] for i in indices]
    else:
        tr_samples = tr_all[:PER_SIDE]
    
    # Class Header Banner
    banner_color = (230, 235, 242) if cfg["category"] == "5_FAILED" else (240, 240, 230)
    draw.rectangle([(MARGIN_X, curr_y), (TOTAL_W - MARGIN_X, curr_y + 32)], fill=banner_color, outline=(180, 190, 205))
    status_str = f"FAILED ON HELD-OUT (Recall: {acc:.1%})" if cfg["category"] == "5_FAILED" else f"D7 COTTON SOURCE (Recall: {acc:.1%})"
    draw.text((MARGIN_X + 12, curr_y + 6), f"Class {idx}/7: {c_name.upper()}  —  {status_str}", fill=(10, 25, 50), font=font_class)
    curr_y += 38
    
    # Subheaders for Left and Right
    left_x = MARGIN_X
    right_x = MARGIN_X + GRID_W + SPACER_MID
    
    ho_top_mis = cfg["distribution"].copy()
    ho_top_mis.pop(c_name, None)
    top_mis_str = f" | Top Error: {list(ho_top_mis.keys())[0]} ({list(ho_top_mis.values())[0]})" if ho_top_mis else ""
    
    draw.text((left_x, curr_y), f"HELD-OUT SOURCE (24 Photos): {ho_desc}{top_mis_str}", fill=(160, 20, 20), font=font_sub)
    draw.text((right_x, curr_y), f"TRAINING POOL (24 Photos): {tr_desc}", fill=(20, 100, 30), font=font_sub)
    curr_y += 24
    
    grid_top_y = curr_y
    
    # Draw Left: Held-Out Grid
    for i, f in enumerate(ho_samples):
        r = i // COLS
        c = i % COLS
        px = left_x + c * THUMB_W
        py = grid_top_y + r * THUMB_H
        
        try:
            with Image.open(f) as im:
                thumb = im.convert("RGB").resize((THUMB_W - 4, THUMB_H - 4), Image.Resampling.LANCZOS)
                sheet.paste(thumb, (px + 2, py + 2))
                
                # Check prediction
                pred = file_preds.get(f.name, "")
                is_correct = (pred == c_name)
                border_color = (40, 180, 40) if is_correct else (220, 30, 30)
                border_w = 2 if is_correct else 3
                
                # Draw border
                draw.rectangle([(px + 1, py + 1), (px + THUMB_W - 3, py + THUMB_H - 3)], outline=border_color, width=border_w)
                
                # Draw mini prediction tag
                pred_short = pred.replace("maize_", "m_").replace("rice_", "r_").replace("cotton_", "c_").replace("tomato_", "t_")
                tag_bg = (0, 0, 0, 180)
                draw.rectangle([(px + 3, py + THUMB_H - 18), (px + THUMB_W - 5, py + THUMB_H - 3)], fill=(20, 20, 20))
                tag_col = (120, 255, 120) if is_correct else (255, 130, 130)
                draw.text((px + 5, py + THUMB_H - 17), pred_short[:15], fill=tag_col, font=font_tiny)
        except Exception as e:
            draw.rectangle([(px + 2, py + 2), (px + THUMB_W - 2, py + THUMB_H - 2)], fill=(200, 200, 200))
    
    # Draw Right: Training Grid
    for i, f in enumerate(tr_samples):
        r = i // COLS
        c = i % COLS
        px = right_x + c * THUMB_W
        py = grid_top_y + r * THUMB_H
        
        try:
            with Image.open(f) as im:
                thumb = im.convert("RGB").resize((THUMB_W - 4, THUMB_H - 4), Image.Resampling.LANCZOS)
                sheet.paste(thumb, (px + 2, py + 2))
                draw.rectangle([(px + 1, py + 1), (px + THUMB_W - 3, py + THUMB_H - 3)], outline=(70, 130, 80), width=1)
        except Exception as e:
            draw.rectangle([(px + 2, py + 2), (px + THUMB_W - 2, py + THUMB_H - 2)], fill=(200, 200, 200))
            
    # Draw vertical divider between held-out and train
    div_x = MARGIN_X + GRID_W + SPACER_MID // 2
    draw.line([(div_x, grid_top_y - 10), (div_x, grid_top_y + GRID_H + 10)], fill=(200, 210, 220), width=2)
    
    curr_y += GRID_H + 28
    
    # Separator line
    draw.line([(MARGIN_X, curr_y - 14), (TOTAL_W - MARGIN_X, curr_y - 14)], fill=(210, 220, 230), width=1)

# Save final sheet
sheet.save(OUTPUT_SHEET, "JPEG", quality=92)
print(f"✓ Contact sheet saved: {OUTPUT_SHEET} ({OUTPUT_SHEET.stat().st_size / (1024*1024):.2f} MB)")
