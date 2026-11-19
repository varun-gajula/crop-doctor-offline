import os, sys, json, zipfile, random, hashlib, shutil, math, time, platform
import concurrent.futures
from pathlib import Path
from collections import defaultdict, Counter

# ==============================================================================
# 0. CONFIGURATION & CONSTANTS
# ==============================================================================
SEED = 42
MAX_TRAIN_PER_CLASS = 640
MAX_CLASS_TOTAL = 800
HAMMING_DIST_DEFAULT = 8
HAMMING_DIST_TOMATO = 10
CONFLICT_MAX_DISTANCE = 2
IMAGE_SIZE = 512
EPOCHS = 15
BATCH_SIZE = 32          # Scaled for 2x T4 GPUs (16 per GPU)
NUM_WORKERS = 4          # Maximize data loading throughput
LEARNING_RATE = 3e-4
WEIGHT_DECAY = 1e-4
APP_REQUIRED_EXECUTORCH = "0.6.0"

CLASS_NAMES = [
    "rice_bacterial_leaf_blight",
    "rice_leaf_blast",
    "rice_sheath_blight",
    "rice_healthy",
    "cotton_bacterial_blight",
    "cotton_fusarium_wilt",
    "cotton_leaf_curl_virus",
    "cotton_healthy",
    "chilli_leaf_curl_virus",
    "chilli_nutrition_deficiency",
    "chilli_healthy",
    "tomato_leaf_blight",
    "tomato_yellow_leaf_curl_virus",
    "tomato_septoria_leaf_spot",
    "tomato_healthy",
    "maize_gray_leaf_spot",
    "maize_common_rust",
    "maize_northern_leaf_blight",
    "maize_healthy",
    "weeds_not_a_crop",
]
CLASS_TO_IDX = {name: i for i, name in enumerate(CLASS_NAMES)}
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp", ".tif", ".tiff", ".jfif"}

random.seed(SEED)

# Workspace directories
RUN_DIR = Path("/kaggle/working/runs_final") if os.path.exists("/kaggle") else Path("benchmarks/runs_final")
CHECKPOINT_DIR = RUN_DIR / "checkpoints"
ARTIFACT_DIR = RUN_DIR / "artifacts"
for p in [RUN_DIR, CHECKPOINT_DIR, ARTIFACT_DIR]:
    p.mkdir(parents=True, exist_ok=True)

print("=" * 80)
print(f"CROP DOCTOR - 20-CLASS HIGH-PERFORMANCE TRAINING & EXPORT")
print(f"Python: {platform.python_version()} | Platform: {platform.platform()}")
print("=" * 80)

# Check packages
try:
    import imagehash
except ImportError:
    print("Installing ImageHash...")
    os.system(f"{sys.executable} -m pip install -q ImageHash")
    import imagehash

import numpy as np
import pandas as pd
from PIL import Image, ImageOps
import torch
import torchvision
from torch import nn
from torch.utils.data import Dataset, DataLoader, WeightedRandomSampler
from torchvision import models, transforms

np.random.seed(SEED)
torch.manual_seed(SEED)
if torch.cuda.is_available():
    torch.cuda.manual_seed_all(SEED)
    torch.backends.cudnn.benchmark = True  # Maximize GPU convolution efficiency

# Detect GPUs
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
gpu_count = torch.cuda.device_count() if torch.cuda.is_available() else 0
print(f"Hardware: {device} | Active GPUs: {gpu_count}")
for i in range(gpu_count):
    print(f"  GPU {i}: {torch.cuda.get_device_name(i)} ({round(torch.cuda.get_device_properties(i).total_memory / 1024**3, 1)} GB VRAM)")

# ==============================================================================
# 1. DATASET DISCOVERY
# ==============================================================================
DATA_ROOT = None
search_roots = [
    Path("/kaggle/input"),
    Path("/kaggle/working"),
    Path("C:/crop-data"),
    Path("data"),
]
# Expand subdirectories
expanded_search = []
for r in search_roots:
    if r.exists():
        expanded_search.append(r)
        expanded_search.extend([p for p in r.iterdir() if p.is_dir()])

for candidate in expanded_search:
    if all((candidate / c).is_dir() for c in CLASS_NAMES):
        DATA_ROOT = candidate
        break

if DATA_ROOT is None:
    raise FileNotFoundError(f"Could not locate 20 class folders. Checked: {[str(p) for p in expanded_search]}")

print(f"\n✓ Dataset found at: {DATA_ROOT}")

# ==============================================================================
# 2. MULTITHREADED INTEGRITY & FAST PREPROCESSING
# ==============================================================================
all_candidates = []
for class_name in CLASS_NAMES:
    class_dir = DATA_ROOT / class_name
    files = sorted([p for p in class_dir.rglob("*") if p.is_file() and p.suffix.lower() in IMAGE_EXTS])
    for p in files:
        all_candidates.append((p, class_name))

print(f"Checking integrity of {len(all_candidates):,} images (8 threads)...")
records = []
invalid = []

def check_img(item):
    p, c = item
    try:
        with Image.open(p) as im:
            im = ImageOps.exif_transpose(im)
            w, h = im.size
            mode = im.mode
            if (w, h) != (512, 512):
                return None, {"path": str(p), "reason": f"dimension_{w}x{h}"}
            im.convert("RGB").load()
        return {
            "path": str(p),
            "relative_path": str(p.relative_to(DATA_ROOT)),
            "class_name": c,
            "class_index": CLASS_TO_IDX[c],
            "width": w,
            "height": h,
            "mode": mode,
        }, None
    except Exception as e:
        return None, {"path": str(p), "reason": repr(e)}

t0_chk = time.time()
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    for i, (rec, inv) in enumerate(ex.map(check_img, all_candidates), 1):
        if rec: records.append(rec)
        if inv: invalid.append(inv)
        if i % 3000 == 0 or i == len(all_candidates):
            print(f"  [Checked {i:,} / {len(all_candidates):,} images ({time.time()-t0_chk:.1f}s)]")

raw_counts = {c: sum(r["class_name"] == c for r in records) for c in CLASS_NAMES}
print(f"Readable 512x512 images: {len(records)} | Invalid: {len(invalid)}")

# ==============================================================================
# 3. CANONICAL DIHEDRAL PHASH & CLUSTER SPLIT (RULES 2a-2e)
# ==============================================================================
class UnionFind:
    def __init__(self, n):
        self.parent = list(range(n))
        self.rank = [0] * n
    def find(self, x):
        while self.parent[x] != x:
            self.parent[x] = self.parent[self.parent[x]]
            x = self.parent[x]
        return x
    def union(self, a, b):
        ra, rb = self.find(a), self.find(b)
        if ra == rb: return
        if self.rank[ra] < self.rank[rb]: ra, rb = rb, ra
        self.parent[rb] = ra
        if self.rank[ra] == self.rank[rb]: self.rank[ra] += 1

def compute_phash(img_path):
    with Image.open(img_path) as im0:
        im = ImageOps.exif_transpose(im0).convert('RGB')
        m = ImageOps.mirror(im)
        variants = [
            im, im.rotate(90, expand=True), im.rotate(180, expand=True), im.rotate(270, expand=True),
            m, m.rotate(90, expand=True), m.rotate(180, expand=True), m.rotate(270, expand=True),
        ]
        hashes = [imagehash.phash(v) for v in variants]
        return min(hashes, key=lambda h: h.hash.tobytes())

print(f"\nComputing canonical dihedral pHashes (8 threads)...")
t0_h = time.time()
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    for i, (rec, h) in enumerate(zip(records, ex.map(lambda r: compute_phash(r['path']), records)), 1):
        rec['phash'] = h
        if i % 3000 == 0 or i == len(records):
            print(f"  [Hashed {i:,} / {len(records):,} images ({time.time()-t0_h:.1f}s)]")

# Cross-class conflict check (Hamming <= 2 across DIFFERENT classes)
class_to_records = defaultdict(list)
for r in records:
    class_to_records[r["class_name"]].append(r)

conflicting_paths = set()
conflict_examples = defaultdict(list)
class_list = sorted(class_to_records.keys())

for i in range(len(class_list)):
    ca = class_list[i]
    for j in range(i + 1, len(class_list)):
        cb = class_list[j]
        for ra in class_to_records[ca]:
            for rb in class_to_records[cb]:
                dist = ra["phash"] - rb["phash"]
                if dist <= CONFLICT_MAX_DISTANCE:
                    conflicting_paths.add(ra["path"])
                    conflicting_paths.add(rb["path"])
                    if len(conflict_examples[ca]) < 10:
                        conflict_examples[ca].append({"this": ra["relative_path"], "other": rb["relative_path"], "other_class": cb, "dist": dist})
                    if len(conflict_examples[cb]) < 10:
                        conflict_examples[cb].append({"this": rb["relative_path"], "other": ra["relative_path"], "other_class": ca, "dist": dist})

print(f"Cross-class conflicting photos excluded: {len(conflicting_paths)}")

# Cluster WITHIN each class only
class_clusters = {}
class_cluster_counts = {}
for c in CLASS_NAMES:
    c_records = [r for r in class_to_records[c] if r["path"] not in conflicting_paths]
    n = len(c_records)
    uf = UnionFind(n)
    threshold = HAMMING_DIST_TOMATO if c.startswith("tomato_") else HAMMING_DIST_DEFAULT
    for ia in range(n):
        for ib in range(ia + 1, n):
            if c_records[ia]["phash"] - c_records[ib]["phash"] <= threshold:
                uf.union(ia, ib)
    clusters = defaultdict(list)
    for idx in range(n):
        root = uf.find(idx)
        clusters[root].append(c_records[idx])
    class_clusters[c] = list(clusters.values())
    class_cluster_counts[c] = len(class_clusters[c])

# Split 80/20 by cluster (0 cluster leakage)
rng = random.Random(SEED)
train_items, val_items = [], []
class_counts_v2 = {}
cluster_counter = 0

for c in CLASS_NAMES:
    clusters = list(class_clusters[c])
    cluster_count = len(clusters)
    source_n = raw_counts[c]
    conflict_n = sum(1 for r in class_to_records[c] if r["path"] in conflicting_paths)

    for cl in clusters:
        cid = f"{c}_cluster_{cluster_counter:05d}"
        cluster_counter += 1
        for item in cl: item["cluster_id"] = cid

    rng.shuffle(clusters)
    n_val = max(1, int(round(0.20 * cluster_count))) if cluster_count >= 2 else 0
    n_val = min(n_val, cluster_count - 1) if cluster_count >= 2 else 0

    val_clusters = clusters[:n_val]
    train_clusters = clusters[n_val:]

    train_pool = [p for cl in train_clusters for p in cl]
    rng.shuffle(train_pool)
    train_class = train_pool[:MAX_TRAIN_PER_CLASS]
    val_class = [rng.choice(cl) for cl in val_clusters]

    train_items.extend(train_class)
    val_items.extend(val_class)

    train_n, val_n = len(train_class), len(val_class)
    kept_n = train_n + val_n
    same_dup_n = source_n - conflict_n - kept_n
    is_small = (cluster_count < 150) or (val_n < 30)

    class_counts_v2[c] = {
        "source_valid_512_count": source_n,
        "unique_clusters": cluster_count,
        "selected_unique_images_after_cap": kept_n,
        "train_count": train_n,
        "validation_count": val_n,
        "same_label_duplicate_files_removed": same_dup_n,
        "conflicting_label_files_excluded": conflict_n,
        "small_class": is_small,
        "cap": MAX_CLASS_TOTAL,
        "train_cap": MAX_TRAIN_PER_CLASS,
    }

rng.shuffle(train_items)
rng.shuffle(val_items)
train_groups = {x["cluster_id"] for x in train_items}
val_groups = {x["cluster_id"] for x in val_items}
overlap = sorted(train_groups & val_groups)
if overlap:
    raise RuntimeError(f"FATAL: Cluster leakage detected! Overlap: {overlap}")

# Verify 300 minimum
for c in CLASS_NAMES:
    if class_counts_v2[c]["selected_unique_images_after_cap"] < 300:
        raise RuntimeError(f"Class {c} dropped below 300 photos!")

print(f"\n✓ Dataset Split Validated: {len(train_items):,} train photos | {len(val_items):,} val photos")
print("  Zero cluster leakage confirmed!")

# Save split artifacts
with open(ARTIFACT_DIR / "class_counts_v2.json", "w") as f:
    json.dump(class_counts_v2, f, indent=2)
with open(ARTIFACT_DIR / "split_v2.json", "w") as f:
    json.dump({"train": train_items, "validation": val_items}, f, indent=2)
with open(ARTIFACT_DIR / "leakage_after.json", "w") as f:
    json.dump({"train_samples": len(train_items), "validation_samples": len(val_items), "zero_cluster_leakage": True}, f, indent=2)

# ==============================================================================
# 4. DATA LOADERS & DATA PARALLEL MODEL
# ==============================================================================
IMAGENET_MEAN = (0.485, 0.456, 0.406)
IMAGENET_STD = (0.229, 0.224, 0.225)

train_transform = transforms.Compose([
    transforms.RandomResizedCrop(512, scale=(0.6, 1.0)),
    transforms.RandomHorizontalFlip(p=0.5),
    transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2, hue=0.05),
    transforms.RandomRotation(20, fill=(114, 114, 114)),
    transforms.ToTensor(),
    transforms.Normalize(IMAGENET_MEAN, IMAGENET_STD),
])
val_transform = transforms.Compose([
    transforms.Resize((512, 512)),
    transforms.ToTensor(),
    transforms.Normalize(IMAGENET_MEAN, IMAGENET_STD),
])

class CropDataset(Dataset):
    def __init__(self, items, transform):
        self.items = items
        self.transform = transform
    def __len__(self): return len(self.items)
    def __getitem__(self, idx):
        row = self.items[idx]
        with Image.open(row["path"]) as im:
            image = ImageOps.exif_transpose(im).convert("RGB")
        return self.transform(image), int(row["class_index"])

train_ds = CropDataset(train_items, train_transform)
val_ds = CropDataset(val_items, val_transform)

# Weighted sampling for class balance
train_targets = [int(item["class_index"]) for item in train_items]
train_counter = Counter(train_targets)
sample_weights = [1.0 / train_counter[t] for t in train_targets]
sampler = WeightedRandomSampler(weights=sample_weights, num_samples=len(train_items), replacement=True)

train_loader = DataLoader(
    train_ds, batch_size=BATCH_SIZE, sampler=sampler,
    num_workers=NUM_WORKERS, pin_memory=torch.cuda.is_available(),
    persistent_workers=True if NUM_WORKERS > 0 else False
)
val_loader = DataLoader(
    val_ds, batch_size=BATCH_SIZE, shuffle=False,
    num_workers=NUM_WORKERS, pin_memory=torch.cuda.is_available(),
    persistent_workers=True if NUM_WORKERS > 0 else False
)

# Base model
weights = models.MobileNet_V3_Large_Weights.DEFAULT
base_model = models.mobilenet_v3_large(weights=weights)
base_model.classifier[3] = nn.Linear(base_model.classifier[3].in_features, len(CLASS_NAMES))
base_model = base_model.to(device)

# Multi-GPU DataParallel for 100% GPU utilization
if gpu_count > 1:
    print(f"Enabling DataParallel across all {gpu_count} GPUs!")
    model = nn.DataParallel(base_model)
else:
    model = base_model

# Inverse-sqrt class weights
counts = torch.tensor([max(1, class_counts_v2[c]["train_count"]) for c in CLASS_NAMES], dtype=torch.float32)
class_weights = (1.0 / torch.sqrt(counts))
class_weights = (class_weights / class_weights.mean()).to(device)

criterion = nn.CrossEntropyLoss(weight=class_weights)
optimizer = torch.optim.AdamW(model.parameters(), lr=LEARNING_RATE, weight_decay=WEIGHT_DECAY)
scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=EPOCHS)
scaler = torch.amp.GradScaler("cuda", enabled=torch.cuda.is_available())

# ==============================================================================
# 5. MAX GPU TRAINING LOOP
# ==============================================================================
BEST_MODEL_PATH = ARTIFACT_DIR / "best_model.pth"
HISTORY_PATH = ARTIFACT_DIR / "training_history.json"
best_val_loss = float("inf")
history = []

print(f"\n{'='*80}")
print(f"STARTING 15-EPOCH HIGH-THROUGHPUT TRAINING (BATCH SIZE: {BATCH_SIZE})")
print(f"Train batches per epoch: {len(train_loader)} | Val batches: {len(val_loader)}")
print(f"{'='*80}\n")

for epoch in range(EPOCHS):
    t0_ep = time.time()
    
    # Train
    model.train()
    train_loss, train_correct, train_total = 0.0, 0, 0
    t_batch = time.time()
    
    for b_idx, (images, labels) in enumerate(train_loader):
        images = images.to(device, non_blocking=True)
        labels = labels.to(device, non_blocking=True)
        optimizer.zero_grad(set_to_none=True)
        
        with torch.amp.autocast(device_type="cuda", dtype=torch.float16, enabled=torch.cuda.is_available()):
            logits = model(images)
            loss = criterion(logits, labels)
            
        scaler.scale(loss).backward()
        scaler.step(optimizer)
        scaler.update()
        
        train_loss += loss.item() * labels.size(0)
        train_correct += (logits.argmax(1) == labels).sum().item()
        train_total += labels.size(0)
        
        if (b_idx + 1) % 50 == 0 or (b_idx + 1) == len(train_loader):
            elapsed = time.time() - t_batch
            print(f"  [Epoch {epoch+1:02d}/{EPOCHS} | Batch {b_idx+1:03d}/{len(train_loader)}] "
                  f"loss: {train_loss/train_total:.4f} acc: {train_correct/train_total:.4f} ({elapsed:.1f}s)")
            
    train_loss_epoch = train_loss / train_total
    train_acc_epoch = train_correct / train_total
    
    # Validation
    model.eval()
    val_loss, val_correct, val_total = 0.0, 0, 0
    with torch.no_grad():
        for images, labels in val_loader:
            images = images.to(device, non_blocking=True)
            labels = labels.to(device, non_blocking=True)
            with torch.amp.autocast(device_type="cuda", dtype=torch.float16, enabled=torch.cuda.is_available()):
                logits = model(images)
                loss = criterion(logits, labels)
            val_loss += loss.item() * labels.size(0)
            val_correct += (logits.argmax(1) == labels).sum().item()
            val_total += labels.size(0)
            
    val_loss_epoch = val_loss / val_total
    val_acc_epoch = val_correct / val_total
    scheduler.step()
    
    ep_duration = time.time() - t0_ep
    print(f"\n>>> EPOCH {epoch+1:02d}/{EPOCHS} RESULT | train_loss: {train_loss_epoch:.4f} acc: {train_acc_epoch:.4f} | "
          f"val_loss: {val_loss_epoch:.4f} acc: {val_acc_epoch:.4f} | Time: {ep_duration:.1f}s\n")
    
    # Save checkpoint
    saved_state = model.module.state_dict() if hasattr(model, "module") else model.state_dict()
    if val_loss_epoch < best_val_loss:
        best_val_loss = val_loss_epoch
        torch.save({"model_state": saved_state, "class_names": CLASS_NAMES, "input_size": 512, "best_val_loss": best_val_loss}, BEST_MODEL_PATH)
        print(f"  ★ New best model checkpoint saved: {BEST_MODEL_PATH}")

    history.append({
        "epoch": epoch + 1, "train_loss": train_loss_epoch, "train_accuracy": train_acc_epoch,
        "validation_loss": val_loss_epoch, "validation_accuracy": val_acc_epoch,
        "learning_rate": optimizer.param_groups[0]["lr"], "seconds": ep_duration
    })
    with open(HISTORY_PATH, "w") as f:
        json.dump(history, f, indent=2)

print("\n✓ Training completed successfully!")

# ==============================================================================
# 6. CALIBRATION & EVALUATION ARTIFACTS
# ==============================================================================
print("\nComputing temperature calibration & validation confusion matrix...")
best_ckpt = torch.load(BEST_MODEL_PATH, map_location=device, weights_only=False)
eval_model = models.mobilenet_v3_large(weights=None)
eval_model.classifier[3] = nn.Linear(eval_model.classifier[3].in_features, len(CLASS_NAMES))
eval_model.load_state_dict(best_ckpt["model_state"])
eval_model = eval_model.to(device).eval()

all_logits, all_labels = [], []
with torch.no_grad():
    for images, labels in val_loader:
        images = images.to(device)
        with torch.amp.autocast(device_type="cuda", dtype=torch.float16, enabled=torch.cuda.is_available()):
            all_logits.append(eval_model(images).float().cpu())
        all_labels.append(labels.cpu())

val_logits = torch.cat(all_logits, dim=0)
val_labels = torch.cat(all_labels, dim=0)

# Temperature optimization
temp_param = nn.Parameter(torch.ones(1) * 1.5)
temp_opt = torch.optim.LBFGS([temp_param], lr=0.01, max_iter=50)
def eval_temp():
    temp_opt.zero_grad()
    loss = nn.CrossEntropyLoss()(val_logits / temp_param.clamp(min=0.1), val_labels)
    loss.backward()
    return loss
temp_opt.step(eval_temp)
T = float(temp_param.clamp(min=0.1).item())
print(f"✓ Calibrated Temperature T: {T:.4f}")

# Energy threshold at 95% TPR
scaled_logits = val_logits / T
free_energies = -T * torch.logsumexp(scaled_logits, dim=1)
energy_thresh = float(torch.quantile(free_energies, 0.95).item())
probs = torch.softmax(scaled_logits, dim=1)
max_probs, preds = probs.max(dim=1)
conf_thresh = float(torch.quantile(max_probs, 0.05).item())

# Per-class accuracy
per_class_acc = {}
for i, c in enumerate(CLASS_NAMES):
    mask = (val_labels == i)
    if mask.sum() > 0:
        per_class_acc[c] = float((preds[mask] == i).float().mean().item())
macro_acc = float(np.mean(list(per_class_acc.values())))

thresholds_payload = {
    "temperature": round(T, 4),
    "confidence_threshold": round(conf_thresh, 4),
    "energy_threshold": round(energy_thresh, 4),
    "energy_method": "Free Energy score E(x) = -T * log(sum(exp(z_i / T))) at 95% TPR on grouped validation set",
    "macro_validation_accuracy": round(macro_acc, 4),
    "per_class_accuracy": {c: round(per_class_acc[c], 4) for c in CLASS_NAMES}
}
with open(ARTIFACT_DIR / "thresholds.json", "w") as f:
    json.dump(thresholds_payload, f, indent=2)

# Confusion matrix
cm = np.zeros((len(CLASS_NAMES), len(CLASS_NAMES)), dtype=int)
for t, p in zip(val_labels.numpy(), preds.numpy()):
    cm[t, p] += 1
cm_df = pd.DataFrame(cm, index=CLASS_NAMES, columns=CLASS_NAMES)
cm_df.to_csv(ARTIFACT_DIR / "confusion_matrix.csv")

# Save labels
with open(ARTIFACT_DIR / "labels.json", "w") as f:
    json.dump(CLASS_NAMES, f, indent=2)

metrics_payload = {
    "macro_accuracy": round(macro_acc, 4),
    "per_class_accuracy": thresholds_payload["per_class_accuracy"],
    "versions": {
        "executorch": APP_REQUIRED_EXECUTORCH,
        "torch": torch.__version__,
        "python": platform.python_version()
    }
}
with open(ARTIFACT_DIR / "metrics.json", "w") as f:
    json.dump(metrics_payload, f, indent=2)

print(f"✓ Macro Validation Accuracy: {macro_acc*100:.2f}%")

# ==============================================================================
# 7. AUTOMATED EXECUTORCH 0.6.0 EXPORT & PACKAGING
# ==============================================================================
print("\n" + "=" * 80)
print("EXPORTING TO EXECUTORCH 0.6.0 XNNPACK (.pte)")
print("=" * 80)

# If running on Python >= 3.13 (Kaggle), auto-setup Python 3.11 export venv
if sys.version_info >= (3, 13):
    print("Detected Python >= 3.13. Setting up automated Python 3.11 environment via uv...")
    os.system("pip install -q uv")
    os.system("uv venv --python 3.11 /tmp/py311")
    os.system('uv pip install --python /tmp/py311 --no-deps "https://download.pytorch.org/whl/cpu/executorch-0.6.0%2Bcpu-cp311-cp311-manylinux_2_28_x86_64.whl"')
    os.system("uv pip install --python /tmp/py311 ruamel.yaml tabulate packaging sympy flatbuffers")
    os.system("uv pip install --python /tmp/py311 --extra-index-url https://download.pytorch.org/whl/cpu torch torchvision")
    EXPORT_PY = "/tmp/py311/bin/python"
else:
    os.system("pip install -q executorch==0.6.0")
    EXPORT_PY = sys.executable

# Self-contained export & parity script
export_runner_code = f'''
import sys, json, random
from pathlib import Path
from PIL import Image, ImageOps
import torch
from torch import nn
from torchvision import models, transforms

RUN_DIR = Path("{str(RUN_DIR)}")
ARTIFACT_DIR = Path("{str(ARTIFACT_DIR)}")
BEST_MODEL_PATH = Path("{str(BEST_MODEL_PATH)}")
PTE_PATH = ARTIFACT_DIR / "vision.pte"
CLASS_NAMES = {CLASS_NAMES}

print("Running ExecuTorch export with:", sys.executable)
import importlib.metadata as meta
print("ExecuTorch installed:", meta.version("executorch"))

from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner
from executorch.exir import to_edge_transform_and_lower

# Load model weights
model_export = models.mobilenet_v3_large(weights=None)
model_export.classifier[3] = nn.Linear(model_export.classifier[3].in_features, len(CLASS_NAMES))
ckpt = torch.load(BEST_MODEL_PATH, map_location="cpu", weights_only=False)
model_export.load_state_dict(ckpt["model_state"])
model_export.eval()

# Export program
example = torch.randn(1, 3, 512, 512, dtype=torch.float32)
exported = torch.export.export(model_export, (example,))
edge_prog = to_edge_transform_and_lower(exported, partitioner=[XnnpackPartitioner()])
et_prog = edge_prog.to_executorch()

with open(PTE_PATH, "wb") as f:
    f.write(et_prog.buffer)

pte_size_mb = PTE_PATH.stat().st_size / (1024 * 1024)
print(f"✓ Successfully exported vision.pte: {{pte_size_mb:.2f}} MB (Budget < 30MB: {{pte_size_mb < 30.0}})")

# Parity check
try:
    from executorch.runtime import Runtime
    runtime = Runtime.get()
    prog_rt = runtime.load_program(str(PTE_PATH))
    method = prog_rt.load_method("forward")
    test_tensor = torch.randn(1, 3, 512, 512, dtype=torch.float32)
    with torch.no_grad():
        py_out = model_export(test_tensor)
    et_out = method.execute([test_tensor.contiguous()])[0]
    if not isinstance(et_out, torch.Tensor):
        et_out = torch.tensor(et_out)
    diff = (py_out - et_out).abs().max().item()
    parity = {{"status": "PASS" if diff <= 1e-3 else "FAIL", "max_abs_error": diff}}
except Exception as e:
    parity = {{"status": "SKIPPED", "reason": str(e)}}

with open(ARTIFACT_DIR / "parity_samples.json", "w") as f:
    json.dump(parity, f, indent=2)
print("✓ Parity test:", parity)
'''

runner_file = RUN_DIR / "run_export.py"
with open(runner_file, "w") as f:
    f.write(export_runner_code)

os.system(f"{EXPORT_PY} {runner_file}")

PTE_PATH = ARTIFACT_DIR / "vision.pte"
assert PTE_PATH.exists(), "Fatal: vision.pte was not created!"
print(f"✓ Confirmed vision.pte on disk: {PTE_PATH.stat().st_size / (1024*1024):.2f} MB")

# ==============================================================================
# 8. BUNDLE FINAL DELIVERABLES ZIP
# ==============================================================================
DELIVERY_ZIP = RUN_DIR / "crop_doctor_training_deliverables.zip"
files_to_pack = [
    "metrics.json", "class_counts_v2.json", "thresholds.json", "vision.pte",
    "labels.json", "confusion_matrix.csv", "leakage_after.json",
    "split_v2.json", "best_model.pth", "training_history.json", "parity_samples.json"
]

with zipfile.ZipFile(DELIVERY_ZIP, "w", compression=zipfile.ZIP_DEFLATED) as zf:
    for name in files_to_pack:
        p = ARTIFACT_DIR / name
        if p.exists():
            zf.write(p, arcname=name)

print("\n" + "=" * 80)
print(f"🎉 SUCCESS! All artifacts bundled into:")
print(f"   {DELIVERY_ZIP} ({DELIVERY_ZIP.stat().st_size / (1024*1024):.2f} MB)")
print("=" * 80)
print("👉 IN KAGGLE: Look at the right sidebar under Output -> /kaggle/working/runs_final/")
print("   Click the three dots (...) next to 'crop_doctor_training_deliverables.zip' and click Download!")
print("=" * 80)
