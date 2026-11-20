#!/usr/bin/env python3
"""
tools/eval_decision.py
Task P4: Evaluates all probe sets under C:\\crop-data\\realtest against
the vision model with exact preprocessing, temperature scaling, free energy,
and crop-mass masking matching the Android app.

Outputs:
- benchmarks/p4_decision_table.csv:
  file, set, selected crop, energy, crop_mass, top-1, top-2, probabilities, decision
"""

import os
import sys
import csv
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image
import torch
import torchvision.models as models
import torchvision.transforms as transforms

# Paths
PROJECT_ROOT = Path(__file__).resolve().parent.parent
LABELS_PATH = PROJECT_ROOT / "app/src/main/assets/labels.json"
THRESHOLDS_PATH = PROJECT_ROOT / "app/src/main/assets/thresholds.json"
CHECKPOINT_PATH = PROJECT_ROOT / "benchmarks/final/best_model.pth"
MANIFEST_PATH = PROJECT_ROOT / "benchmarks/p4_probe_manifest.json"
OUTPUT_CSV = PROJECT_ROOT / "benchmarks/p4_decision_table.csv"

# Preprocessing transforms matching ImagePreprocessor.kt
TRANSFORMS = transforms.Compose([
    transforms.Resize((512, 512)),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )
])

def validate_leaf(img_pil):
    """
    Python implementation matching LeafValidator.kt exactly:
    Downsample to 64x64, evaluate skin, cotton boll, neutral, green, yellow, brown, red pixels.
    """
    sample = img_pil.resize((64, 64), Image.BILINEAR).convert("RGB")
    arr = np.array(sample, dtype=np.float32)
    r = arr[..., 0]
    g = arr[..., 1]
    b = arr[..., 2]

    # Convert RGB to HSV
    max_val = np.maximum(np.maximum(r, g), b) / 255.0
    min_val = np.minimum(np.minimum(r, g), b) / 255.0
    delta = max_val - min_val

    sat = np.zeros_like(max_val)
    nonzero_max = max_val > 0
    sat[nonzero_max] = delta[nonzero_max] / max_val[nonzero_max]

    hue = np.zeros_like(max_val)
    nonzero_delta = delta > 0

    r_f = r / 255.0
    g_f = g / 255.0
    b_f = b / 255.0

    mask_r = nonzero_delta & (max_val == r_f)
    mask_g = nonzero_delta & (max_val == g_f)
    mask_b = nonzero_delta & (max_val == b_f)

    hue[mask_r] = (((g_f[mask_r] - b_f[mask_r]) / delta[mask_r]) % 6.0) * 60.0
    hue[mask_g] = (((b_f[mask_g] - r_f[mask_g]) / delta[mask_g]) + 2.0) * 60.0
    hue[mask_b] = (((r_f[mask_b] - g_f[mask_b]) / delta[mask_b]) + 4.0) * 60.0
    hue[hue < 0] += 360.0

    total_pixels = 64 * 64

    # 1. Skin tone
    is_skin = (hue >= 8) & (hue <= 32) & (r > g + 20) & (g > b) & (r >= 140) & (r <= 250) & (max_val > 0.55) & (sat >= 0.15) & (sat <= 0.60)

    # 2. Cotton boll
    is_cotton = (sat < 0.22) & (max_val > 0.70) & (r >= 170) & (g >= 170) & (b >= 150)

    # 3. Neutral background
    is_neutral = (sat < 0.08) | (max_val < 0.05) | ((max_val > 0.98) & (sat < 0.05))

    ex_g = 2 * g - r - b
    # 4. Green plant
    is_green = ((hue >= 45) & (hue <= 190) & ((g >= b) | (ex_g > -20))) | ((hue >= 35) & (hue <= 55) & (g > r - 15) & (g > b))

    # 5. Yellow crop
    is_yellow = (hue >= 30) & (hue <= 70) & (r > 55) & (g > 55) & (b < 180)

    # 6. Brown crop
    is_brown = (hue >= 12) & (hue <= 40) & (r >= 35) & (r <= 220) & (g >= 15) & (g <= 170) & (b < 140) & ((r - g) <= 75)

    # 7. Red fruit
    is_red = (((hue >= 0) & (hue <= 25)) | ((hue >= 335) & (hue <= 360))) & (r > 75) & (r > g + 10) & (r > b + 10)

    crop_mask = is_cotton | is_green | is_yellow | is_brown | is_red
    crop_mask = crop_mask & (~is_skin)

    crop_count = np.sum(crop_mask)
    skin_count = np.sum(is_skin)

    crop_fraction = float(crop_count) / total_pixels
    skin_fraction = float(skin_count) / total_pixels

    if skin_fraction > 0.50 and crop_fraction < 0.05:
        is_likely_crop = False
        reason = "Skin dominant"
    elif crop_fraction < 0.01:
        is_likely_crop = False
        reason = "No crop features"
    else:
        is_likely_crop = True
        reason = f"Crop detected ({int(crop_fraction * 100)}%)"

    return is_likely_crop, crop_fraction, reason

def compute_free_energy(scaled_logits, temperature):
    """Numerically stable Free Energy: E(x; T) = -T * ln(sum(exp(z_i / T)))"""
    max_logit = np.max(scaled_logits)
    sum_exp = np.sum(np.exp(scaled_logits - max_logit))
    log_sum_exp = max_logit + np.log(sum_exp)
    return float(-temperature * log_sum_exp)

def compute_softmax(scaled_logits):
    """Numerically stable softmax"""
    max_logit = np.max(scaled_logits)
    exp_logits = np.exp(scaled_logits - max_logit)
    return exp_logits / np.sum(exp_logits)

class DecisionEvaluator:
    def __init__(self):
        with open(LABELS_PATH) as f:
            self.labels = json.load(f)

        with open(THRESHOLDS_PATH) as f:
            self.thresholds_cfg = json.load(f)

        self.temperature = float(self.thresholds_cfg.get("temperature", 1.781754))
        self.conf_threshold = float(self.thresholds_cfg.get("confidence_threshold", 0.343408))
        self.energy_threshold = self.thresholds_cfg.get("energy_threshold", -9.305036)
        if self.energy_threshold is not None:
            self.energy_threshold = float(self.energy_threshold)

        # Baseline thresholds for Task P4 (candidate values)
        self.crop_mass_threshold = 0.50
        self.margin_threshold = 0.15
        self.borderline_energy = -10.0

        print(f"[INFO] Initialized Evaluator: {len(self.labels)} labels, T={self.temperature:.4f}, conf_thresh={self.conf_threshold:.4f}, energy_thresh={self.energy_threshold}", flush=True)

        ckpt = torch.load(CHECKPOINT_PATH, map_location="cpu")
        model = models.mobilenet_v3_large(weights=None)
        model.classifier[3] = torch.nn.Linear(1280, len(self.labels))
        if isinstance(ckpt, dict) and "model_state" in ckpt:
            model.load_state_dict(ckpt["model_state"])
        elif isinstance(ckpt, dict) and "model_state_dict" in ckpt:
            model.load_state_dict(ckpt["model_state_dict"])
        else:
            model.load_state_dict(ckpt)
        model.eval()
        self.model = model

    def infer_image(self, img_path):
        """Infers image ONCE to get logits and leaf validation result."""
        with Image.open(img_path) as img:
            rgb_img = img.convert("RGB")
            # Downsample if very huge to avoid excessive memory
            if rgb_img.width > 2048 or rgb_img.height > 2048:
                rgb_img.thumbnail((2048, 2048), Image.BILINEAR)

            leaf_pass, crop_frac, reason = validate_leaf(rgb_img)
            tensor = TRANSFORMS(rgb_img).unsqueeze(0)

        with torch.no_grad():
            raw_logits = self.model(tensor).numpy().flatten()

        scaled_logits = raw_logits / self.temperature
        free_energy = compute_free_energy(scaled_logits, self.temperature)
        global_probs = compute_softmax(scaled_logits)
        global_top1_idx = int(np.argmax(global_probs))
        global_top1_class = self.labels[global_top1_idx]
        global_top1_prob = float(global_probs[global_top1_idx])

        return {
            "scaled_logits": scaled_logits,
            "free_energy": free_energy,
            "global_probs": global_probs,
            "global_top1_class": global_top1_class,
            "global_top1_prob": global_top1_prob,
            "leaf_pass": leaf_pass,
            "crop_fraction": crop_frac,
            "leaf_reason": reason
        }

    def decide(self, infer_res, selected_crop,
               crop_mass_thresh=None, margin_thresh=None,
               borderline_energy=None, energy_thresh=None, conf_thresh=None):
        if crop_mass_thresh is None: crop_mass_thresh = self.crop_mass_threshold
        if margin_thresh is None: margin_thresh = self.margin_threshold
        if borderline_energy is None: borderline_energy = self.borderline_energy
        if energy_thresh is None: energy_thresh = self.energy_threshold
        if conf_thresh is None: conf_thresh = self.conf_threshold

        scaled_logits = infer_res["scaled_logits"]
        free_energy = infer_res["free_energy"]
        global_probs = infer_res["global_probs"]
        global_top1_class = infer_res["global_top1_class"]
        leaf_pass = infer_res["leaf_pass"]
        leaf_reason = infer_res["leaf_reason"]

        selected_crop_norm = selected_crop.lower().strip()
        crop_prefix = f"{selected_crop_norm}_"
        crop_indices = [i for i, lbl in enumerate(self.labels) if lbl.startswith(crop_prefix)]

        if not crop_indices:
            crop_mass = 1.0
            incrop_probs = global_probs
            incrop_sorted_indices = np.argsort(incrop_probs)[::-1]
        else:
            crop_mass = float(np.sum(global_probs[crop_indices]))
            incrop_logits = scaled_logits[crop_indices]
            incrop_softmax = compute_softmax(incrop_logits)
            incrop_probs = np.zeros(len(self.labels), dtype=np.float32)
            for local_idx, orig_idx in enumerate(crop_indices):
                incrop_probs[orig_idx] = incrop_softmax[local_idx]
            incrop_sorted_indices = [crop_indices[i] for i in np.argsort(incrop_softmax)[::-1]]

        top1_idx = incrop_sorted_indices[0]
        top1_class = self.labels[top1_idx]
        top1_prob = float(incrop_probs[top1_idx])

        if len(incrop_sorted_indices) > 1:
            top2_idx = incrop_sorted_indices[1]
            top2_class = self.labels[top2_idx]
            top2_prob = float(incrop_probs[top2_idx])
        else:
            top2_class = "none"
            top2_prob = 0.0

        margin = top1_prob - top2_prob

        top3_list = []
        for idx in incrop_sorted_indices[:3]:
            top3_list.append(f"{self.labels[idx]}:{incrop_probs[idx]:.3f}")
        probs_str = ";".join(top3_list)

        # Decision rules
        if not leaf_pass:
            decision = "CAN'T TELL"
            decision_reason = f"LEAF_VALIDATOR_FAIL({leaf_reason})"
        elif global_top1_class == "weeds_not_a_crop":
            decision = "CAN'T TELL"
            decision_reason = "GLOBAL_TOP1_WEEDS"
        elif energy_thresh is not None and free_energy > energy_thresh:
            decision = "CAN'T TELL"
            decision_reason = f"HIGH_FREE_ENERGY({free_energy:.2f}>{energy_thresh:.2f})"
        elif top1_prob < conf_thresh:
            decision = "CAN'T TELL"
            decision_reason = f"LOW_INCROP_CONFIDENCE({top1_prob:.3f}<{conf_thresh:.3f})"
        elif crop_mass < crop_mass_thresh:
            decision = "OUT OF CLASS"
            decision_reason = f"LOW_CROP_MASS({crop_mass:.3f}<{crop_mass_thresh:.3f})"
        elif margin < margin_thresh and free_energy >= borderline_energy:
            decision = "OUT OF CLASS"
            decision_reason = f"BORDERLINE_AMBIGUOUS(margin={margin:.3f}<{margin_thresh:.3f},E={free_energy:.2f}>={borderline_energy:.2f})"
        else:
            decision = "DIAGNOSE"
            decision_reason = "ACCEPTED"

        return {
            "selected_crop": selected_crop,
            "energy": free_energy,
            "crop_mass": crop_mass,
            "top1": top1_class,
            "top1_prob": top1_prob,
            "top2": top2_class,
            "top2_prob": top2_prob,
            "margin": margin,
            "probabilities": probs_str,
            "decision": decision,
            "decision_reason": decision_reason,
            "leaf_pass": leaf_pass,
            "crop_fraction": infer_res["crop_fraction"],
            "global_top1": global_top1_class,
            "global_top1_prob": infer_res["global_top1_prob"]
        }

def run_evaluation():
    evaluator = DecisionEvaluator()
    with open(MANIFEST_PATH) as f:
        manifest = json.load(f)

    print(f"Running evaluation on {len(manifest)} manifest images...", flush=True)

    results = []

    for i, item in enumerate(manifest):
        set_name = item["set"]
        img_path = Path(item["path"])
        true_crop = item.get("true_crop", "none")

        # Infer once per image
        infer_res = evaluator.infer_image(img_path)

        if set_name in ["supported", "unsupported", "screen"]:
            eval_crops = [true_crop] if true_crop in ["rice", "cotton", "chilli", "tomato", "maize"] else ["tomato"]
        else:
            eval_crops = ["rice", "cotton", "chilli", "tomato", "maize"]

        for sc in eval_crops:
            dec = evaluator.decide(infer_res, sc)
            dec["file"] = str(img_path)
            dec["set"] = set_name
            dec["true_crop"] = true_crop
            dec["true_class"] = item.get("true_class", "unknown")
            results.append(dec)

        if (i + 1) % 50 == 0 or (i + 1) == len(manifest):
            print(f"Processed {i + 1}/{len(manifest)} images ({len(results)} rows)...", flush=True)

    # Write CSV
    with open(OUTPUT_CSV, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow([
            "file", "set", "selected crop", "energy", "crop_mass", "top-1", "top-2", "probabilities", "decision",
            "decision_reason", "margin", "top1_prob", "top2_prob", "true_crop", "true_class"
        ])
        for r in results:
            writer.writerow([
                Path(r["file"]).name,
                r["set"],
                r["selected_crop"],
                f"{r['energy']:.4f}",
                f"{r['crop_mass']:.4f}",
                r["top1"],
                r["top2"],
                r["probabilities"],
                r["decision"],
                r["decision_reason"],
                f"{r['margin']:.4f}",
                f"{r['top1_prob']:.4f}",
                f"{r['top2_prob']:.4f}",
                r["true_crop"],
                r["true_class"]
            ])

    print(f"Decision table written to: {OUTPUT_CSV}", flush=True)

if __name__ == "__main__":
    run_evaluation()
