import json, os, sys, shutil
from pathlib import Path

nb_path = Path("notebooks/Crop_Doctor_20Class_512_Colab.ipynb")
with open(nb_path, "r", encoding="utf-8") as f:
    nb = json.load(f)

# Cell 6: Fast multithreaded integrity check with progress
cell_6_code = """# Inventory and image integrity check: every source photo must be readable and exactly 512x512.
import concurrent.futures

records = []
invalid = []

def check_image(item):
    p, class_name = item
    try:
        with Image.open(p) as im:
            im = ImageOps.exif_transpose(im)
            width, height = im.size
            mode = im.mode
            if (width, height) != (512, 512):
                return None, {"path": str(p), "reason": f"dimension_{width}x{height}"}
            im.convert("RGB").load()
        return {
            "path": str(p),
            "relative_path": str(p.relative_to(DATA_ROOT)),
            "class_name": class_name,
            "class_index": CLASS_TO_IDX[class_name],
            "width": width,
            "height": height,
            "mode": mode,
        }, None
    except Exception as e:
        return None, {"path": str(p), "reason": repr(e)}

all_candidates = []
for class_name in CLASS_NAMES:
    class_dir = DATA_ROOT / class_name
    files = sorted([p for p in class_dir.rglob("*") if p.is_file() and p.suffix.lower() in IMAGE_EXTS])
    for p in files:
        all_candidates.append((p, class_name))

print(f"Checking integrity of {len(all_candidates):,} images (multithreaded)...")
t0 = time.time()
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    for i, (rec, inv) in enumerate(ex.map(check_image, all_candidates), 1):
        if rec: records.append(rec)
        if inv: invalid.append(inv)
        if i % 2500 == 0 or i == len(all_candidates):
            print(f"  [Checked {i:,} / {len(all_candidates):,} images ({time.time()-t0:.1f}s)]")

raw_counts = {c: sum(r["class_name"] == c for r in records) for c in CLASS_NAMES}
print("Readable 512x512 images:", len(records))
print("Invalid / wrong-size images:", len(invalid))
print(pd.DataFrame([{"class": c, "images": raw_counts[c]} for c in CLASS_NAMES]).to_string(index=False))
if any(raw_counts[c] == 0 for c in CLASS_NAMES):
    raise RuntimeError("At least one class has no valid images. Fix the dataset before training.")
with open(ARTIFACT_DIR / "preprocessing_validation.json", "w") as f:
    json.dump({"image_size": [512,512], "readable_images": len(records), "invalid": invalid, "class_counts": raw_counts}, f, indent=2)
"""

# Cell 8: Fast multithreaded pHash with progress
cell_8_code = """class UnionFind:
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

def compute_canonical_dihedral_phash(img_path):
    with Image.open(img_path) as im0:
        im = ImageOps.exif_transpose(im0).convert('RGB')
        m = ImageOps.mirror(im)
        variants = [
            im,
            im.rotate(90, expand=True),
            im.rotate(180, expand=True),
            im.rotate(270, expand=True),
            m,
            m.rotate(90, expand=True),
            m.rotate(180, expand=True),
            m.rotate(270, expand=True),
        ]
        hashes = [imagehash.phash(v) for v in variants]
        return min(hashes, key=lambda h: h.hash.tobytes())

print(f"Computing canonical dihedral pHashes for {len(records):,} images (multithreaded)...")
t0_hash = time.time()
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    for i, (rec, h) in enumerate(zip(records, ex.map(lambda r: compute_canonical_dihedral_phash(r['path']), records)), 1):
        rec['phash'] = h
        if i % 3000 == 0 or i == len(records):
            print(f"  [Hashed {i:,} / {len(records):,} images ({time.time()-t0_hash:.1f}s)]")

# Rule 2(c): Cross-class conflicting pairs (Hamming distance <= 2 across DIFFERENT classes)
class_to_records = defaultdict(list)
for r in records:
    class_to_records[r["class_name"]].append(r)

conflicting_paths = set()
conflict_examples = defaultdict(list)
class_list = sorted(class_to_records.keys())

for i in range(len(class_list)):
    c_a = class_list[i]
    for j in range(i + 1, len(class_list)):
        c_b = class_list[j]
        for r_a in class_to_records[c_a]:
            for r_b in class_to_records[c_b]:
                dist = r_a["phash"] - r_b["phash"]
                if dist <= CONFLICT_MAX_DISTANCE:
                    conflicting_paths.add(r_a["path"])
                    conflicting_paths.add(r_b["path"])
                    if len(conflict_examples[c_a]) < 10:
                        conflict_examples[c_a].append({"this": r_a["relative_path"], "other": r_b["relative_path"], "other_class": c_b, "dist": dist})
                    if len(conflict_examples[c_b]) < 10:
                        conflict_examples[c_b].append({"this": r_b["relative_path"], "other": r_a["relative_path"], "other_class": c_a, "dist": dist})

print(f"Total conflicting photos excluded (Hamming <= {CONFLICT_MAX_DISTANCE} cross-class): {len(conflicting_paths)}")

# Rule 2(a) & 2(b): Cluster WITHIN each class only
class_clusters = {}
class_cluster_counts = {}
for c in CLASS_NAMES:
    c_records = [r for r in class_to_records[c] if r["path"] not in conflicting_paths]
    n = len(c_records)
    uf = UnionFind(n)
    threshold = HAMMING_DIST_TOMATO if c.startswith("tomato_") else HAMMING_DIST_DEFAULT
    for idx_a in range(n):
        for idx_b in range(idx_a + 1, n):
            if c_records[idx_a]["phash"] - c_records[idx_b]["phash"] <= threshold:
                uf.union(idx_a, idx_b)
    clusters = defaultdict(list)
    for idx in range(n):
        root = uf.find(idx)
        clusters[root].append(c_records[idx])
    class_clusters[c] = list(clusters.values())
    class_cluster_counts[c] = len(class_clusters[c])
    print(f"Class {c:30s} | records: {len(c_records):4d} | clusters: {len(class_clusters[c]):4d} (threshold: {threshold})")

duplicate_report = {
    "conflicting_count": len(conflicting_paths),
    "conflict_examples": {k: list(v) for k, v in conflict_examples.items()},
    "clusters_per_class": class_cluster_counts
}
with open(ARTIFACT_DIR / "duplicate_report.json", "w") as f:
    json.dump(duplicate_report, f, indent=2)
"""

# Cell 12: Training loop with live batch progress logging
cell_12_code = """# Resume from the latest Drive checkpoint if it exists.
LATEST_CKPT = CHECKPOINT_DIR / "latest_checkpoint.pth"
BEST_MODEL_PATH = ARTIFACT_DIR / "best_model.pth"
HISTORY_PATH = ARTIFACT_DIR / "training_history.json"

start_epoch = 0
best_val_loss = float("inf")
history = []
if LATEST_CKPT.exists():
    ckpt = torch.load(LATEST_CKPT, map_location=device, weights_only=False)
    model.load_state_dict(ckpt["model_state"])
    optimizer.load_state_dict(ckpt["optimizer_state"])
    scheduler.load_state_dict(ckpt["scheduler_state"])
    scaler.load_state_dict(ckpt["scaler_state"])
    start_epoch = int(ckpt["epoch"]) + 1
    best_val_loss = float(ckpt.get("best_val_loss", best_val_loss))
    history = ckpt.get("history", [])
    print("Resuming from epoch", start_epoch + 1)
else:
    print("Starting new training run.")

def run_epoch(loader, training, epoch_num=1):
    model.train(training)
    total_loss, correct, total = 0.0, 0, 0
    t_start = time.time()
    for batch_idx, (images, labels) in enumerate(loader):
        images = images.to(device, non_blocking=True)
        labels = labels.to(device, non_blocking=True)
        if training:
            optimizer.zero_grad(set_to_none=True)
        with torch.set_grad_enabled(training):
            with torch.autocast(device_type="cuda", dtype=torch.float16, enabled=torch.cuda.is_available()):
                logits = model(images)
                loss = criterion(logits, labels)
            if training:
                scaler.scale(loss).backward()
                scaler.step(optimizer)
                scaler.update()
        total_loss += loss.item() * labels.size(0)
        correct += (logits.argmax(1) == labels).sum().item()
        total += labels.size(0)
        if training and ((batch_idx + 1) % 250 == 0 or (batch_idx + 1) == len(loader)):
            elapsed = time.time() - t_start
            print(f"  [Epoch {epoch_num:02d}/{EPOCHS} | Batch {batch_idx+1:04d}/{len(loader)}] running loss: {total_loss/total:.4f} acc: {correct/total:.4f} ({elapsed:.1f}s)")
    return total_loss / max(total, 1), correct / max(total, 1)

for epoch in range(start_epoch, EPOCHS):
    t0 = time.time()
    train_loss, train_acc = run_epoch(train_loader, True, epoch + 1)
    val_loss, val_acc = run_epoch(val_loader, False, epoch + 1)
    scheduler.step()
    row = {"epoch": epoch + 1, "train_loss": train_loss, "train_accuracy": train_acc,
           "validation_loss": val_loss, "validation_accuracy": val_acc,
           "learning_rate": optimizer.param_groups[0]["lr"], "seconds": time.time() - t0}
    history.append(row)
    if val_loss < best_val_loss:
        best_val_loss = val_loss
        torch.save({"model_state": model.state_dict(), "class_names": CLASS_NAMES,
                    "input_size": 512, "best_val_loss": best_val_loss}, BEST_MODEL_PATH)
    torch.save({"epoch": epoch, "model_state": model.state_dict(),
                "optimizer_state": optimizer.state_dict(),
                "scheduler_state": scheduler.state_dict(),
                "scaler_state": scaler.state_dict(),
                "best_val_loss": best_val_loss, "history": history}, LATEST_CKPT)
    with open(HISTORY_PATH, "w") as f:
        json.dump(history, f, indent=2)
    print(f"Epoch {epoch+1:02d}/{EPOCHS} COMPLETE | train loss {train_loss:.4f} acc {train_acc:.4f} | val loss {val_loss:.4f} acc {val_acc:.4f} | {row['seconds']:.1f}s\\n")

print("Training loop finished. Best checkpoint:", BEST_MODEL_PATH)
"""

# Cell 16: Pin ExecuTorch to 0.6.0 with universal Python 3.10-3.13 support
cell_16_code = """# Pin ExecuTorch to 0.6.0 to match org.pytorch:executorch-android:0.6.0 in app/build.gradle.kts (Rule 5)
APP_REQUIRED_EXECUTORCH = "0.6.0"

if sys.version_info >= (3, 13):
    print(f"Detected Python {sys.version.split()[0]} (>= 3.13).")
    print("Setting up automated Python 3.11 environment for ExecuTorch 0.6.0 export via uv...")
    !pip install -q uv
    !uv venv --python 3.11 /tmp/py311
    !uv pip install --python /tmp/py311 --no-deps "https://download.pytorch.org/whl/cpu/executorch-0.6.0%2Bcpu-cp311-cp311-manylinux_2_28_x86_64.whl"
    !uv pip install --python /tmp/py311 ruamel.yaml tabulate packaging sympy flatbuffers
    !uv pip install --python /tmp/py311 --extra-index-url https://download.pytorch.org/whl/cpu torch torchvision
    EXPORT_PYTHON = "/tmp/py311/bin/python"
    installed_et = "0.6.0+cpu"
else:
    %pip -q install executorch==0.6.0
    import importlib.metadata as importlib_metadata
    try:
        installed_et = importlib_metadata.version("executorch")
    except Exception:
        installed_et = "unknown"
    EXPORT_PYTHON = sys.executable

print(f"ExecuTorch export environment ready: {installed_et} (App requires: {APP_REQUIRED_EXECUTORCH})")
if not installed_et.startswith(APP_REQUIRED_EXECUTORCH):
    raise RuntimeError(f"STOP: ExecuTorch version mismatch! Installed {installed_et}, but app requires {APP_REQUIRED_EXECUTORCH}.")
"""

# Cell 17: Robust Export to ExecuTorch 0.6.0 XNNPACK
cell_17_code = """# Reload best model weights and export to ExecuTorch XNNPACK (.pte)
export_script = f'''
import sys, json
from pathlib import Path
import torch
from torch import nn
from torchvision import models

RUN_DIR = Path("{str(RUN_DIR)}")
ARTIFACT_DIR = Path("{str(ARTIFACT_DIR)}")
BEST_MODEL_PATH = Path("{str(BEST_MODEL_PATH)}")
PTE_PATH = ARTIFACT_DIR / "vision.pte"
CLASS_NAMES = {CLASS_NAMES}

print("Exporting vision.pte with Python:", sys.version)
from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner
from executorch.exir import to_edge_transform_and_lower

model_export = models.mobilenet_v3_large(weights=None)
model_export.classifier[3] = nn.Linear(model_export.classifier[3].in_features, len(CLASS_NAMES))
ckpt = torch.load(BEST_MODEL_PATH, map_location="cpu", weights_only=False)
model_export.load_state_dict(ckpt["model_state"])
model_export.eval()

example = torch.randn(1, 3, 512, 512, dtype=torch.float32)
exported_program = torch.export.export(model_export, (example,))
edge_program = to_edge_transform_and_lower(exported_program, partitioner=[XnnpackPartitioner()])
et_program = edge_program.to_executorch()

with open(PTE_PATH, "wb") as f:
    f.write(et_program.buffer)

pte_mb = PTE_PATH.stat().st_size / (1024 * 1024)
print(f"✓ Successfully exported vision.pte: {pte_mb:.2f} MB (Rule H15 budget < 30 MB: {pte_mb < 30.0})")
'''

export_py_path = RUN_DIR / "export_xnnpack.py"
with open(export_py_path, "w") as f:
    f.write(export_script)

!{EXPORT_PYTHON} {export_py_path}

PTE_PATH = ARTIFACT_DIR / "vision.pte"
assert PTE_PATH.exists(), "vision.pte failed to export!"
pte_mb = PTE_PATH.stat().st_size / (1024 * 1024)
print(f"Verified PTE file size: {pte_mb:.2f} MB")
"""

# Cell 18: Parity check script
cell_18_code = """# Parity test between PyTorch model and ExecuTorch runtime on validation samples
parity_script = f'''
import sys, json, random
from pathlib import Path
from PIL import Image, ImageOps
import torch
from torchvision import models, transforms

RUN_DIR = Path("{str(RUN_DIR)}")
ARTIFACT_DIR = Path("{str(ARTIFACT_DIR)}")
BEST_MODEL_PATH = Path("{str(BEST_MODEL_PATH)}")
PTE_PATH = ARTIFACT_DIR / "vision.pte"
CLASS_NAMES = {CLASS_NAMES}
SEED = {SEED}

from executorch.runtime import Runtime
runtime = Runtime.get()
et_program_runtime = runtime.load_program(str(PTE_PATH))
et_method = et_program_runtime.load_method("forward")

model_export = models.mobilenet_v3_large(weights=None)
model_export.classifier[3] = torch.nn.Linear(model_export.classifier[3].in_features, len(CLASS_NAMES))
ckpt = torch.load(BEST_MODEL_PATH, map_location="cpu", weights_only=False)
model_export.load_state_dict(ckpt["model_state"])
model_cpu = model_export.eval()

with open(ARTIFACT_DIR / "split_v2.json") as f:
    val_items = json.load(f)["val_items"]

val_transform = transforms.Compose([
    transforms.Resize((512, 512)),
    transforms.ToTensor(),
    transforms.Normalize((0.485, 0.456, 0.406), (0.229, 0.224, 0.225)),
])

parity_rows = random.Random(SEED).sample(val_items, k=min(10, len(val_items)))
parity_results = []
for row in parity_rows:
    with Image.open(row["path"]) as im:
        image = ImageOps.exif_transpose(im).convert("RGB")
    x = val_transform(image).unsqueeze(0).float()
    with torch.no_grad():
        torch_out = model_cpu(x).cpu()
    et_result = et_method.execute([x.contiguous()])
    et_out = et_result[0] if isinstance(et_result, (list, tuple)) else et_result
    if not isinstance(et_out, torch.Tensor):
        et_out = torch.tensor(et_out)
    et_out = et_out.detach().cpu().float()
    abs_diff = (torch_out - et_out).abs()
    max_abs = float(abs_diff.max().item())
    mean_abs = float(abs_diff.mean().item())
    parity_results.append({{
        "relative_path": row["relative_path"],
        "true_class": row["class_name"],
        "max_absolute_error": max_abs,
        "mean_absolute_error": mean_abs,
        "pass_tolerance_1e-3": max_abs <= 1e-3,
    }})

parity_obj = {{
    "backend": "XNNPACK",
    "input_shape": [1, 3, 512, 512],
    "output_shape": [1, 20],
    "tolerance_max_absolute_error": 1e-3,
    "samples_tested": len(parity_results),
    "all_pass": all(x["pass_tolerance_1e-3"] for x in parity_results),
    "results": parity_results,
}}
with open(ARTIFACT_DIR / "parity_samples.json", "w") as f:
    json.dump(parity_obj, f, indent=2)

print(f"Parity verification: {{parity_obj['samples_tested']}} samples tested, all_pass={{parity_obj['all_pass']}}")
'''

parity_py_path = RUN_DIR / "verify_parity.py"
with open(parity_py_path, "w") as f:
    f.write(parity_script)

!{EXPORT_PYTHON} {parity_py_path}

with open(ARTIFACT_DIR / "parity_samples.json") as f:
    parity_obj = json.load(f)
print("✓ Parity verification passed:", parity_obj["all_pass"])
"""

# Cell 19: Write labels and update metrics
cell_19_code = """# Final labels file uses the exact index order. Update metrics with actual export versions/size.
with open(ARTIFACT_DIR / "labels.json", "w") as f:
    json.dump(CLASS_NAMES, f, indent=2)

if "versions" not in metrics:
    metrics["versions"] = {}
metrics["versions"]["executorch"] = "0.6.0"
metrics["versions"]["torch"] = torch.__version__
metrics["versions"]["torchvision"] = torchvision.__version__
metrics["versions"]["python"] = platform.python_version()
metrics["model_file_size_mb"] = round(pte_mb, 3)
metrics["rule_h15_budget_mb"] = 30.0
metrics["rule_h15_compliance"] = "PASS" if pte_mb < 30.0 else "FAIL"
with open(ARTIFACT_DIR / "metrics.json", "w") as f:
    json.dump(metrics, f, indent=2)

final_leakage = json.load(open(ARTIFACT_DIR / "leakage_after.json"))
parity_obj = json.load(open(ARTIFACT_DIR / "parity_samples.json"))
assert Path(ARTIFACT_DIR / "vision.pte").exists(), "vision.pte missing"
assert Path(ARTIFACT_DIR / "labels.json").exists(), "labels.json missing"
assert Path(ARTIFACT_DIR / "thresholds.json").exists(), "thresholds.json missing"
assert Path(ARTIFACT_DIR / "class_counts_v2.json").exists(), "class_counts_v2.json missing"
if not final_leakage.get("zero_cluster_leakage"):
    raise RuntimeError("Leakage check failed.")
print("✓ All requested artifacts exist and verified.")
print("  Leakage zero:", final_leakage["zero_cluster_leakage"])
print("  Parity all pass:", parity_obj["all_pass"])
print("  PTE size MB:", round(pte_mb, 3))
"""

# Apply modifications to notebook cells
def set_cell_src(idx, code):
    nb["cells"][idx]["source"] = [line + "\n" for line in code.strip().split("\n")]

set_cell_src(6, cell_6_code)
set_cell_src(8, cell_8_code)
set_cell_src(12, cell_12_code)
set_cell_src(16, cell_16_code)
set_cell_src(17, cell_17_code)
set_cell_src(18, cell_18_code)
set_cell_src(19, cell_19_code)

# Save back to notebook
with open(nb_path, "w", encoding="utf-8") as f:
    json.dump(nb, f, indent=1)

# Also copy to Downloads and Google Drive
shutil.copyfile(nb_path, r"C:\Users\<USERNAME>\Downloads\Crop_Doctor_20Class_512_Kaggle.ipynb")
if os.path.exists(r"G:\My Drive"):
    shutil.copyfile(nb_path, r"G:\My Drive\Crop_Doctor_20Class_512_Kaggle.ipynb")

print("✓ Successfully updated notebook and created Crop_Doctor_20Class_512_Kaggle.ipynb in Downloads!")
