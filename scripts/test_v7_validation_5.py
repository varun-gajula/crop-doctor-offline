"""
Test 5 images from benchmarks/final/split_v2.json validation split
using exact app preprocessing and PyTorch model (benchmarks/final/best_model.pth).
"""

import json
from pathlib import Path
from PIL import Image
import torch
import torch.nn as nn
import torch.nn.functional as F
from torchvision import models, transforms

CROP_DATA_DIR = Path(r"C:\crop-data\crop_doctor_512")
REPO_ROOT = Path(__file__).resolve().parent.parent
SPLIT_PATH = REPO_ROOT / "benchmarks" / "final" / "split_v2.json"
CKPT_PATH = REPO_ROOT / "benchmarks" / "final" / "best_model.pth"
THRESHOLDS_PATH = REPO_ROOT / "app" / "src" / "main" / "assets" / "thresholds.json"
LABELS_PATH = REPO_ROOT / "app" / "src" / "main" / "assets" / "labels.json"

with open(SPLIT_PATH, "r", encoding="utf-8") as f:
    split_data = json.load(f)

val_items = split_data.get("validation", [])
print(f"Total validation items: {len(val_items)}")

with open(THRESHOLDS_PATH, "r", encoding="utf-8") as f:
    thresholds_data = json.load(f)
temperature = float(thresholds_data["temperature"])
confidence_threshold = float(thresholds_data["confidence_threshold"])
print(f"Temperature: {temperature}, Confidence Threshold: {confidence_threshold}")

with open(LABELS_PATH, "r", encoding="utf-8") as f:
    labels = json.load(f)

# Pick 5 diverse validation images from 5 different crops:
# 1. rice
# 2. cotton
# 3. chilli
# 4. tomato
# 5. maize

target_classes = [
    "rice_leaf_blast",
    "cotton_bacterial_blight",
    "chilli_leaf_curl_virus",
    "tomato_yellow_leaf_curl_virus",
    "maize_northern_leaf_blight"
]

selected_samples = []
for target_cls in target_classes:
    for item in val_items:
        if item["class_name"] == target_cls:
            img_rel_path = item["relative_path"]
            img_full_path = CROP_DATA_DIR / img_rel_path
            if img_full_path.exists():
                selected_samples.append({
                    "class_name": target_cls,
                    "relative_path": img_rel_path,
                    "full_path": str(img_full_path)
                })
                break

print(f"Selected {len(selected_samples)} samples:")
for s in selected_samples:
    print(f"  {s['class_name']}: {s['relative_path']}")

# Load PyTorch model
print("\nLoading PyTorch model...")
ckpt = torch.load(CKPT_PATH, map_location="cpu")
model = models.mobilenet_v3_large(weights=None)
model.classifier[3] = nn.Linear(model.classifier[3].in_features, len(labels))
model.load_state_dict(ckpt["model_state"])
model.eval()

# Exact app preprocessing:
# Android:
# 1. Bitmap.createScaledBitmap(bitmap, 512, 512, true) (bilinear resize)
# 2. RGB, float values [0..1]
# 3. Normalized by mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]
transform = transforms.Compose([
    transforms.Resize((512, 512), interpolation=transforms.InterpolationMode.BILINEAR),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

print("\n--- Running Laptop PyTorch Inference ---")
results = []
with torch.no_grad():
    for s in selected_samples:
        img = Image.open(s["full_path"]).convert("RGB")
        tensor = transform(img).unsqueeze(0) # [1, 3, 512, 512]
        
        logits = model(tensor)[0] # [20]
        scaled_logits = logits / temperature
        probs = F.softmax(scaled_logits, dim=-1)
        
        top1_prob, top1_idx = torch.max(probs, dim=-1)
        top1_label = labels[top1_idx.item()]
        
        results.append({
            "sample_image": s["relative_path"],
            "true_label": s["class_name"],
            "top1_label": top1_label,
            "probability": float(top1_prob.item()),
            "raw_logits": logits.tolist(),
            "probs": probs.tolist()
        })
        print(f"Image: {s['relative_path']}")
        print(f"  True Label: {s['class_name']}")
        print(f"  Top-1 Label: {top1_label}")
        print(f"  Probability: {top1_prob.item():.4f} ({top1_prob.item()*100:.2f}%)")
        print()

out_json = REPO_ROOT / "benchmarks" / "v7_sanity_laptop.json"
with open(out_json, "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2)
print(f"Saved laptop results to: {out_json}")
