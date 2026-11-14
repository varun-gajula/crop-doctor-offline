"""
Crop Doctor - Generate Parity Samples
------------------------------------
Generates reference parity samples using PyTorch model or benchmark logits
for the Android parity test suite (Rule H3 & T4).
"""

import json
from pathlib import Path
import torch
import torch.nn.functional as F

REPO_ROOT = Path(__file__).resolve().parent.parent
LABELS_PATH = REPO_ROOT / "app" / "src" / "main" / "assets" / "labels.json"
PREPROCESS_PATH = REPO_ROOT / "app" / "src" / "main" / "assets" / "preprocess.json"
OUTPUT_PATH = REPO_ROOT / "ml" / "parity_samples.json"

def mask_and_renormalize_py(labels, logits, crop_hint, temperature):
    t_logits = torch.tensor(logits, dtype=torch.float32) / temperature
    hint = crop_hint.lower().strip() if crop_hint else "auto"
    if hint not in ["all", "auto", ""]:
        prefix = f"{hint}_"
        indices = [i for i, l in enumerate(labels) if l.startswith(prefix)]
        if not indices:
            indices = list(range(len(labels)))
    else:
        indices = list(range(len(labels)))

    sub_logits = t_logits[indices]
    sub_probs = F.softmax(sub_logits, dim=-1)

    top_sub_idx = int(torch.argmax(sub_probs))
    global_top_idx = indices[top_sub_idx]
    top_prob = float(sub_probs[top_sub_idx])

    return labels[global_top_idx], top_prob

def main():
    with open(LABELS_PATH, "r", encoding="utf-8") as f:
        labels = json.load(f)

    with open(PREPROCESS_PATH, "r", encoding="utf-8") as f:
        preprocess = json.load(f)

    temperature = float(preprocess.get("temperature", 1.15))

    samples = []

    # Sample 1: Rice BLB with Rice filter
    logits_1 = [-2.0] * len(labels)
    logits_1[labels.index("rice_bacterial_leaf_blight")] = 4.5
    logits_1[labels.index("rice_brown_spot")] = 1.0
    logits_1[labels.index("rice_healthy")] = -1.0
    top_label_1, top_prob_1 = mask_and_renormalize_py(labels, logits_1, "rice", temperature)
    samples.append({
        "sample_id": "sample_1_rice_blb",
        "description": "High confidence Rice Bacterial Leaf Blight with Rice crop chip",
        "crop_hint": "rice",
        "logits": logits_1,
        "temperature": temperature,
        "expected_top1_label": top_label_1,
        "expected_top1_prob": round(top_prob_1, 4)
    })

    # Sample 2: Cotton CLCV with Cotton filter
    logits_2 = [-2.0] * len(labels)
    logits_2[labels.index("cotton_leaf_curl_virus")] = 5.0
    logits_2[labels.index("cotton_bacterial_blight")] = 0.5
    top_label_2, top_prob_2 = mask_and_renormalize_py(labels, logits_2, "cotton", temperature)
    samples.append({
        "sample_id": "sample_2_cotton_clcv",
        "description": "High confidence Cotton Leaf Curl Virus with Cotton crop chip",
        "crop_hint": "cotton",
        "logits": logits_2,
        "temperature": temperature,
        "expected_top1_label": top_label_2,
        "expected_top1_prob": round(top_prob_2, 4)
    })

    # Sample 3: Chilli Bacterial Spot (Auto / all crops)
    logits_3 = [-1.5] * len(labels)
    logits_3[labels.index("chilli_bacterial_spot")] = 4.2
    logits_3[labels.index("tomato_early_blight")] = 1.2
    top_label_3, top_prob_3 = mask_and_renormalize_py(labels, logits_3, "all", temperature)
    samples.append({
        "sample_id": "sample_3_chilli_spot",
        "description": "Chilli Bacterial Spot on Auto mode",
        "crop_hint": "all",
        "logits": logits_3,
        "temperature": temperature,
        "expected_top1_label": top_label_3,
        "expected_top1_prob": round(top_prob_3, 4)
    })

    # Sample 4: Tomato Early Blight with Tomato chip
    logits_4 = [-1.0] * len(labels)
    logits_4[labels.index("tomato_early_blight")] = 3.8
    logits_4[labels.index("tomato_late_blight")] = 2.0
    top_label_4, top_prob_4 = mask_and_renormalize_py(labels, logits_4, "tomato", temperature)
    samples.append({
        "sample_id": "sample_4_tomato_early_blight",
        "description": "Tomato Early Blight with Tomato crop chip masking",
        "crop_hint": "tomato",
        "logits": logits_4,
        "temperature": temperature,
        "expected_top1_label": top_label_4,
        "expected_top1_prob": round(top_prob_4, 4)
    })

    # Sample 5: Maize Common Rust (Auto filter)
    logits_5 = [-2.0] * len(labels)
    logits_5[labels.index("maize_common_rust")] = 4.6
    logits_5[labels.index("maize_gray_leaf_spot")] = 0.8
    top_label_5, top_prob_5 = mask_and_renormalize_py(labels, logits_5, "auto", temperature)
    samples.append({
        "sample_id": "sample_5_maize_common_rust",
        "description": "Maize Common Rust under Auto filter",
        "crop_hint": "auto",
        "logits": logits_5,
        "temperature": temperature,
        "expected_top1_label": top_label_5,
        "expected_top1_prob": round(top_prob_5, 4)
    })

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(samples, f, indent=2)

    # Also update app/src/test/resources/parity_samples.json
    res_path = REPO_ROOT / "app" / "src" / "test" / "resources" / "parity_samples.json"
    res_path.parent.mkdir(parents=True, exist_ok=True)
    with open(res_path, "w", encoding="utf-8") as f:
        json.dump(samples, f, indent=2)

    print(f"✓ Generated {len(samples)} parity samples at: {OUTPUT_PATH}")

if __name__ == "__main__":
    main()
