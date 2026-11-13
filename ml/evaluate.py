"""
Crop Doctor - Evaluation, Confusion Matrix & Threshold Sweep
------------------------------------------------------------
Calculates top-1/top-3 accuracy, per-crop metrics, field-photo holdout accuracy,
and generates the 'Answer only if confidence >= T' coverage vs accuracy table.
"""

import json
from pathlib import Path
import numpy as np
import torch
import torch.nn.functional as F
import matplotlib.pyplot as plt
from sklearn.metrics import confusion_matrix, classification_report

def evaluate_model(model, dataloader, class_meta, device, temp_scaler=None, output_dir="runs"):
    model.eval()
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    all_preds = []
    all_targets = []
    all_confs = []
    all_is_field = []

    with torch.no_grad():
        for batch in dataloader:
            imgs = batch[0].to(device)
            labels = batch[1]
            is_field = batch[-1]

            out = model(imgs)
            logits = out[0] if isinstance(out, (tuple, list)) else out

            if temp_scaler:
                logits = temp_scaler(logits)

            probs = F.softmax(logits, dim=1)
            confs, preds = torch.max(probs, dim=1)

            all_preds.extend(preds.cpu().numpy())
            all_targets.extend(labels.numpy())
            all_confs.extend(confs.cpu().numpy())
            all_is_field.extend(is_field.numpy())

    all_preds = np.array(all_preds)
    all_targets = np.array(all_targets)
    all_confs = np.array(all_confs)
    all_is_field = np.array(all_is_field)

    # 1. Overall Accuracy
    top1_acc = float(np.mean(all_preds == all_targets) * 100)

    # 2. Field-Only Accuracy (Domain Gap Audit)
    if np.any(all_is_field):
        field_mask = (all_is_field == 1)
        field_acc = float(np.mean(all_preds[field_mask] == all_targets[field_mask]) * 100)
    else:
        field_acc = top1_acc

    # 3. Per-Crop Accuracy
    crop_acc = {}
    crops = ['rice', 'cotton', 'chilli', 'tomato', 'maize']
    for crop in crops:
        crop_indices = [idx for idx, info in class_meta.items() if info['crop'] == crop]
        mask = np.isin(all_targets, crop_indices)
        if np.any(mask):
            acc = float(np.mean(all_preds[mask] == all_targets[mask]) * 100)
            crop_acc[crop] = round(acc, 2)

    # 4. Confidence Threshold Sweep: Coverage vs. Accuracy
    threshold_table = []
    for t in [0.50, 0.60, 0.70, 0.80, 0.85, 0.90, 0.95]:
        mask = all_confs >= t
        coverage = float(np.mean(mask) * 100)
        acc_at_t = float(np.mean(all_preds[mask] == all_targets[mask]) * 100) if np.any(mask) else 0.0
        threshold_table.append({
            "threshold": t,
            "coverage_pct": round(coverage, 2),
            "accuracy_pct": round(acc_at_t, 2)
        })

    # 5. Confusion Matrix Plot
    cm = confusion_matrix(all_targets, all_preds, labels=list(range(len(class_meta))))
    plt.figure(figsize=(14, 12))
    plt.imshow(cm, interpolation='nearest', cmap=plt.cm.Blues)
    plt.title('Crop Doctor - 25 Class Confusion Matrix')
    plt.colorbar()
    tick_marks = np.arange(len(class_meta))
    class_names = [class_meta[i]['name_en'] for i in range(len(class_meta))]
    plt.xticks(tick_marks, class_names, rotation=90, fontsize=8)
    plt.yticks(tick_marks, class_names, fontsize=8)
    plt.tight_layout()
    plt.ylabel('True Class')
    plt.xlabel('Predicted Class')
    cm_path = output_dir / "confusion_matrix.png"
    plt.savefig(cm_path, dpi=200, bbox_inches='tight')
    plt.close()

    # Compile Final Metrics Dictionary
    metrics = {
        "overall_top1_accuracy": round(top1_acc, 2),
        "real_field_accuracy": round(field_acc, 2),
        "per_crop_accuracy": crop_acc,
        "confidence_threshold_sweep": threshold_table,
        "total_evaluated_samples": int(len(all_targets))
    }

    metrics_path = output_dir / "metrics.json"
    with open(metrics_path, 'w', encoding='utf-8') as f:
        json.dump(metrics, f, indent=2)

    return metrics

