"""
Crop Doctor - 1,000-Image Robust Evaluation & Benchmark Script
--------------------------------------------------------------
Evaluates the trained 21-class MobileNetV3-Large model on a stratified
sample of 1,000 validation images.
Outputs:
- Top-1 and Top-3 accuracy
- Per-class accuracy breakdown with Telugu labels
- Average inference latency (ms/image)
- Confidence distribution (Sure >=80%, Probably 50-80%, Uncertain <50%)
- Top confusion pairs
- Generates 3x3 sample visualization grid
- Saves metrics to Drive: /content/drive/MyDrive/crop_doctor_runs_v2b/test_1000_metrics.json
"""

import os, sys, json, time, random
from pathlib import Path
import numpy as np
from PIL import Image, ImageFile
ImageFile.LOAD_TRUNCATED_IMAGES = True
import matplotlib.pyplot as plt
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader
import torchvision.models as models
import torchvision.transforms as transforms

# ── 1. Setup Device & Directory ──────────────────────────────────────────────
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"Hardware: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'CPU'}")

RUNS_V2B_DIR = '/content/drive/MyDrive/crop_doctor_runs_v2b'
DATA_DIR = '/content/crop_data'

TARGET_21_CLASSES = [
    'rice_bacterial_leaf_blight', 'rice_brown_spot', 'rice_leaf_blast',
    'rice_sheath_blight', 'rice_healthy', 'cotton_bacterial_blight',
    'cotton_fusarium_wilt', 'cotton_healthy', 'chilli_bacterial_spot',
    'chilli_leaf_curl_virus', 'chilli_healthy', 'tomato_early_blight',
    'tomato_late_blight', 'tomato_yellow_leaf_curl_virus', 'tomato_spider_mites',
    'tomato_healthy', 'maize_gray_leaf_spot', 'maize_common_rust',
    'maize_northern_leaf_blight', 'maize_fall_armyworm', 'maize_healthy'
]

TELUGU_NAMES = {
    'rice_bacterial_leaf_blight': 'వరి బాక్టీరియల్ ఆకు ఎండు తెగులు',
    'rice_brown_spot': 'వరి గోధుమ రంగు మచ్చ తెగులు',
    'rice_leaf_blast': 'వరి అగ్గి తెగులు',
    'rice_sheath_blight': 'వరి కాండం కుళ్లు తెగులు',
    'rice_healthy': 'ఆరోగ్యకరమైన వరి',
    'cotton_bacterial_blight': 'పత్తి బాక్టీరియల్ తెగులు',
    'cotton_fusarium_wilt': 'పత్తి ఫ్యుసేరియం ఎండు తెగులు',
    'cotton_healthy': 'ఆరోగ్యకరమైన పత్తి',
    'chilli_bacterial_spot': 'మిరప బాక్టీరియల్ మచ్చ తెగులు',
    'chilli_leaf_curl_virus': 'మిరప ఆకు ముడత వైరస్',
    'chilli_healthy': 'ఆరోగ్యకరమైన మిరప',
    'tomato_early_blight': 'టమాటా ముందస్తు మచ్చ తెగులు',
    'tomato_late_blight': 'టమాటా ఆకు మాడ తెగులు',
    'tomato_yellow_leaf_curl_virus': 'టమాటా ఆకు ముడత వైరస్',
    'tomato_spider_mites': 'టమాటా ఎర్ర నల్లి పురుగు',
    'tomato_healthy': 'ఆరోగ్యకరమైన టమాటా',
    'maize_gray_leaf_spot': 'మొక్కజొన్న బూడిద రంగు మచ్చ తెగులు',
    'maize_common_rust': 'మొక్కజొన్న తుప్పు తెగులు',
    'maize_northern_leaf_blight': 'మొక్కజొన్న ఆకు ఎండు తెగులు',
    'maize_fall_armyworm': 'మొక్కజొన్న కత్తెర పురుగు',
    'maize_healthy': 'ఆరోగ్యకరమైన మొక్కజొన్న'
}

# ── 2. Load Model & Temperature ──────────────────────────────────────────────
# Check if model is in global scope; if not, reload weights
if 'model' not in globals() or not isinstance(globals()['model'], nn.Module):
    print("Loading MobileNetV3-Large model weights...")
    model = models.mobilenet_v3_large(weights=None)
    in_feat = model.classifier[3].in_features
    model.classifier[3] = nn.Linear(in_feat, 21)
    
    ckpt_paths = [
        os.path.join(RUNS_V2B_DIR, 'mobilenet_v3_large_v2b_21class.pt'),
        '/content/model_v2b.pt'
    ]
    loaded = False
    for cp in ckpt_paths:
        if os.path.exists(cp):
            model.load_state_dict(torch.load(cp, map_location=device))
            print(f"✓ Loaded weights from {cp}")
            loaded = True
            break
    if not loaded:
        raise FileNotFoundError("Could not find trained checkpoint! Please check Drive path.")
else:
    print("Using active trained model in memory.")

model = model.to(device)
model.eval()

# Temperature factor T
T = 1.5778
thresh_file = os.path.join(RUNS_V2B_DIR, 'thresholds.json')
if os.path.exists(thresh_file):
    try:
        with open(thresh_file, 'r') as f:
            t_data = json.load(f)
            T = float(t_data.get('temperature', 1.5778))
    except Exception:
        pass
print(f"Platt Temperature T: {T:.4f}")

# ── 3. Stratified 1,000-Image Selection ───────────────────────────────────────
val_tf = transforms.Compose([
    transforms.Resize((320, 320)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

pool = []
if 'val_samples_v2b' in globals() and len(val_samples_v2b) > 0:
    pool = val_samples_v2b
else:
    split_file = os.path.join(RUNS_V2B_DIR, 'split_v2b.json')
    if os.path.exists(split_file):
        with open(split_file, 'r') as f:
            pool = json.load(f).get('val', [])

if len(pool) < 1000:
    # If val pool is smaller or not available, discover from DATA_DIR
    print("Scanning /content/crop_data for test images...")
    valid_exts = {'.jpg', '.jpeg', '.png', '.webp'}
    for root, _, files in os.walk(DATA_DIR):
        for f in files:
            if Path(f).suffix.lower() in valid_exts:
                p_str = os.path.join(root, f)
                for c_idx, c_name in enumerate(TARGET_21_CLASSES):
                    if c_name in p_str.lower():
                        pool.append({'path': p_str, 'class_id': c_idx, 'class_name': c_name})
                        break

print(f"Total available evaluation pool: {len(pool)} images")
assert len(pool) >= 1000, f"Need at least 1,000 images, but found {len(pool)}"

# Stratified sampling: ~47-48 images per class
rng = random.Random(42)
class_buckets = {}
for s in pool:
    class_buckets.setdefault(s['class_id'], []).append(s)

selected_1000 = []
samples_per_class = 1000 // len(TARGET_21_CLASSES)
for c_idx in range(len(TARGET_21_CLASSES)):
    bucket = class_buckets.get(c_idx, [])
    k = min(len(bucket), samples_per_class)
    selected_1000.extend(rng.sample(bucket, k))

rem_pool = [s for s in pool if s['path'] not in {x['path'] for x in selected_1000}]
if len(selected_1000) < 1000 and rem_pool:
    selected_1000.extend(rng.sample(rem_pool, 1000 - len(selected_1000)))

selected_1000 = selected_1000[:1000]
print(f"✓ Stratified 1,000 images selected across {len(TARGET_21_CLASSES)} classes!")

# ── 4. Batched Inference on 1,000 Images ──────────────────────────────────────
class Test1000Dataset(Dataset):
    def __init__(self, records, transform):
        self.records = records
        self.transform = transform
    def __len__(self):
        return len(self.records)
    def __getitem__(self, idx):
        rec = self.records[idx]
        with Image.open(rec['path']) as img:
            t = self.transform(img.convert('RGB'))
        return t, int(rec['class_id']), str(rec['path'])

test_loader = DataLoader(Test1000Dataset(selected_1000, val_tf), batch_size=32, shuffle=False, num_workers=2)

all_preds, all_probs, all_targets, all_paths = [], [], [], []
total_infer_time = 0.0

print("\nRunning inference across 1,000 images...")
with torch.no_grad():
    for imgs, targets, paths in test_loader:
        imgs = imgs.to(device)
        t0 = time.time()
        logits = model(imgs) / T
        probs = F.softmax(logits, dim=1)
        total_infer_time += (time.time() - t0)
        
        preds = probs.argmax(dim=1).cpu().numpy()
        probs_np = probs.cpu().numpy()
        
        all_preds.extend(preds)
        all_probs.extend(probs_np)
        if isinstance(targets, torch.Tensor):
            all_targets.extend(targets.cpu().numpy())
        else:
            all_targets.extend(np.array(targets, dtype=np.int64))
        all_paths.extend(paths)

all_preds = np.array(all_preds, dtype=np.int64)
all_probs = np.array(all_probs, dtype=np.float32)
all_targets = np.array(all_targets, dtype=np.int64)

# ── 5. Accuracy & Performance Metrics ─────────────────────────────────────────
top1_correct = (all_preds == all_targets).sum()
top1_acc = (top1_correct / 1000.0) * 100.0

# Top-3 Accuracy
top3_correct = 0
for i in range(1000):
    top3_idx = np.argsort(all_probs[i])[::-1][:3]
    if all_targets[i] in top3_idx:
        top3_correct += 1
top3_acc = (top3_correct / 1000.0) * 100.0

avg_latency_ms = (total_infer_time / 1000.0) * 1000.0
throughput = 1000.0 / total_infer_time

print(f"\n{'='*70}")
print(f"CROP DOCTOR - 1,000 IMAGE TEST RESULTS")
print(f"{'='*70}")
print(f"Overall Top-1 Accuracy: {top1_correct}/1000 ({top1_acc:.2f}%)")
print(f"Overall Top-3 Accuracy: {top3_correct}/1000 ({top3_acc:.2f}%)")
print(f"Avg GPU Latency:        {avg_latency_ms:.2f} ms per image ({throughput:.1f} imgs/sec)")
print(f"{'='*70}")

# Confidence Bands (U2 & Rule H11)
confidences = np.max(all_probs, axis=1)
sure_mask = confidences >= 0.80
probably_mask = (confidences >= 0.50) & (confidences < 0.80)
uncertain_mask = confidences < 0.50

print(f"\nConfidence Breakdown (Farmer Advisory Certainty):")
print(f"  ● Sure     (>= 80%): {sure_mask.sum():4d}/1000 ({sure_mask.mean()*100:5.1f}%) | Accuracy: {(all_preds[sure_mask] == all_targets[sure_mask]).mean()*100:5.2f}%")
print(f"  ● Probably (50-80%): {probably_mask.sum():4d}/1000 ({probably_mask.mean()*100:5.1f}%) | Accuracy: {(all_preds[probably_mask] == all_targets[probably_mask]).mean()*100:5.2f}%")
print(f"  ● Not Sure (<  50%): {uncertain_mask.sum():4d}/1000 ({uncertain_mask.mean()*100:5.1f}%) -> Routes to Extension Officer (Rule H11)")

# ── 6. Per-Class Accuracy Table ───────────────────────────────────────────────
print(f"\n{'Class Name':<30} {'Telugu Name':<32} {'Correct/Total':<14} {'Accuracy':<8}")
print("-" * 88)
per_class_results = {}
for c_idx, c_name in enumerate(TARGET_21_CLASSES):
    mask = (all_targets == c_idx)
    tot = mask.sum()
    corr = (all_preds[mask] == c_idx).sum() if tot > 0 else 0
    acc = (corr / tot * 100.0) if tot > 0 else 0.0
    te_name = TELUGU_NAMES.get(c_name, '')
    per_class_results[c_name] = {'total': int(tot), 'correct': int(corr), 'accuracy': round(acc, 2)}
    print(f"{c_name:<30} {te_name:<32} {corr:3d}/{tot:3d}        {acc:6.2f}%")

# ── 7. Top Confusion Pairs ───────────────────────────────────────────────────
from sklearn.metrics import confusion_matrix
cm = confusion_matrix(all_targets, all_preds, labels=list(range(21)))
top_confusions = []
for i in range(21):
    for j in range(21):
        if i != j and cm[i, j] > 0:
            top_confusions.append({
                'true_class': TARGET_21_CLASSES[i],
                'pred_class': TARGET_21_CLASSES[j],
                'count': int(cm[i, j])
            })
top_confusions.sort(key=lambda x: x['count'], reverse=True)

print(f"\nTop 5 Confusion Pairs:")
for rank, conf in enumerate(top_confusions[:5], 1):
    print(f"  #{rank}: True: {conf['true_class']:<28} -> Predicted: {conf['pred_class']:<28} (Count: {conf['count']})")

# ── 8. 3x3 Visual Sample Grid ────────────────────────────────────────────────
plt.figure(figsize=(14, 14))
sample_indices = rng.sample(range(1000), 9)

for idx, s_i in enumerate(sample_indices, 1):
    img_path = all_paths[s_i]
    t_cls = TARGET_21_CLASSES[all_targets[s_i]]
    p_cls = TARGET_21_CLASSES[all_preds[s_i]]
    conf = all_probs[s_i][all_preds[s_i]] * 100.0
    is_correct = (all_preds[s_i] == all_targets[s_i])
    
    plt.subplot(3, 3, idx)
    with Image.open(img_path) as im:
        plt.imshow(im)
    plt.axis('off')
    color = 'green' if is_correct else 'red'
    title = f"True: {t_cls}\nPred: {p_cls} ({conf:.1f}%)\n{'✓ CORRECT' if is_correct else '✗ WRONG'}"
    plt.title(title, fontsize=10, color=color, fontweight='bold')

plt.tight_layout()
grid_path = os.path.join(RUNS_V2B_DIR, 'test_1000_sample_grid.png')
plt.savefig(grid_path, dpi=180, bbox_inches='tight')
plt.show()
print(f"\n✓ Saved 3x3 sample visualization grid to: {grid_path}")

# ── 9. Save JSON Summary to Drive ─────────────────────────────────────────────
metrics_1000 = {
    'total_test_images': 1000,
    'top1_accuracy_pct': round(top1_acc, 2),
    'top3_accuracy_pct': round(top3_acc, 2),
    'avg_gpu_latency_ms': round(avg_latency_ms, 2),
    'throughput_imgs_sec': round(throughput, 1),
    'confidence_breakdown': {
        'sure_count': int(sure_mask.sum()),
        'sure_acc_pct': round(float((all_preds[sure_mask] == all_targets[sure_mask]).mean() * 100), 2) if sure_mask.sum() > 0 else 0,
        'probably_count': int(probably_mask.sum()),
        'probably_acc_pct': round(float((all_preds[probably_mask] == all_targets[probably_mask]).mean() * 100), 2) if probably_mask.sum() > 0 else 0,
        'uncertain_count': int(uncertain_mask.sum())
    },
    'per_class_results': per_class_results,
    'top_confusions': top_confusions[:10]
}

metrics_path = os.path.join(RUNS_V2B_DIR, 'test_1000_metrics.json')
with open(metrics_path, 'w', encoding='utf-8') as f:
    json.dump(metrics_1000, f, indent=2, ensure_ascii=False)

print(f"✓ Saved 1,000-image benchmark report to: {metrics_path}")
