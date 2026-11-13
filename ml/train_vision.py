"""
Crop Doctor - Vision Model Training, Calibration & Export
---------------------------------------------------------
Trains MobileNetV3-Large on the 25 Crop Doctor target classes.
Features:
- Outdoor domain-gap mitigating augmentations
- AdamW + CosineAnnealingLR + Label Smoothing (0.1)
- Temperature calibration for probabilistic certainty
- Export to ExecuTorch .pte (reference Arm runtime) and TFLite (LiteRT fallback)
"""

import os
import sys
import json
import time
import argparse
from pathlib import Path

import torch
import torch.nn as nn
from torch.utils.data import DataLoader
from sklearn.model_selection import train_test_split

# Add project root to sys.path
SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from ml.dataset import CropDataset, get_transforms, scan_dataset
from ml.model import create_model, TemperatureScaler
from ml.evaluate import evaluate_model

def train_epoch(model, loader, criterion_class, criterion_sev, optimizer, scaler, device, is_multi_task=False):
    model.train()
    total_loss = 0.0
    correct = 0
    total = 0

    for batch in loader:
        imgs = batch[0].to(device)
        labels = batch[1].to(device)
        severities = batch[2].to(device)

        optimizer.zero_grad()
        if device.type == 'cuda':
            with torch.amp.autocast(device_type='cuda'):
                if is_multi_task:
                    logits_class, logits_sev = model(imgs)
                    loss_class = criterion_class(logits_class, labels)
                    loss_sev = criterion_sev(logits_sev, severities)
                    loss = loss_class + 0.5 * loss_sev
                    logits = logits_class
                else:
                    logits = model(imgs)
                    loss = criterion_class(logits, labels)
            scaler.scale(loss).backward()
            scaler.step(optimizer)
            scaler.update()
        else:
            if is_multi_task:
                logits_class, logits_sev = model(imgs)
                loss_class = criterion_class(logits_class, labels)
                loss_sev = criterion_sev(logits_sev, severities)
                loss = loss_class + 0.5 * loss_sev
                logits = logits_class
            else:
                logits = model(imgs)
                loss = criterion_class(logits, labels)
            loss.backward()
            optimizer.step()

        total_loss += loss.item() * labels.size(0)
        preds = logits.argmax(dim=1)
        correct += (preds == labels).sum().item()
        total += labels.size(0)

    return total_loss / total, (correct / total) * 100.0

def validate_epoch(model, loader, criterion_class, criterion_sev, device, is_multi_task=False):
    model.eval()
    total_loss = 0.0
    correct = 0
    total = 0

    with torch.no_grad():
        for batch in loader:
            imgs = batch[0].to(device)
            labels = batch[1].to(device)
            severities = batch[2].to(device)

            if is_multi_task:
                logits_class, logits_sev = model(imgs)
                loss_class = criterion_class(logits_class, labels)
                loss_sev = criterion_sev(logits_sev, severities)
                loss = loss_class + 0.5 * loss_sev
                logits = logits_class
            else:
                logits = model(imgs)
                loss = criterion_class(logits, labels)

            total_loss += loss.item() * labels.size(0)
            preds = logits.argmax(dim=1)
            correct += (preds == labels).sum().item()
            total += labels.size(0)

    return total_loss / total, (correct / total) * 100.0

def export_to_executorch(model, output_path, img_size=224):
    """
    Exports PyTorch model to ExecuTorch .pte format using XNNPACK partitioner.
    """
    print(f"\n--- Exporting to ExecuTorch (.pte) ---")
    try:
        from executorch.exir import to_edge
        from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner

        model.eval().cpu()
        example_input = (torch.randn(1, 3, img_size, img_size),)

        print("1. Capturing torch.export graph...")
        exported_program = torch.export.export(model, example_input)

        print("2. Lowering to edge dialect...")
        edge_program = to_edge(exported_program)

        print("3. Partitioning with XNNPACK...")
        edge_program = edge_program.to_backend(XnnpackPartitioner())

        print("4. Emitting ExecuTorch binary...")
        executorch_program = edge_program.to_executorch()

        with open(output_path, "wb") as f:
            executorch_program.write_to_file(f)

        pte_size_mb = os.path.getsize(output_path) / (1024 * 1024)
        print(f"[SUCCESS] Exported vision.pte to: {output_path} ({pte_size_mb:.2f} MB)")
        return True
    except Exception as e:
        print(f"[WARNING] ExecuTorch export failed or executorch is not installed: {e}")
        print("Note: ExecuTorch export is best run in Linux/Google Colab where prebuilt wheels are standard.")
        return False

def main():
    parser = argparse.ArgumentParser(description="Train Crop Doctor Vision Model")
    parser.add_argument("--data_dir", type=str, default="C:/crop-data/small", help="Path to resized dataset directory")
    parser.add_argument("--class_map", type=str, default="data/class_map.yaml", help="Path to class_map.yaml")
    parser.add_argument("--output_dir", type=str, default="runs/vision_mobilenet", help="Output directory for runs and checkpoints")
    parser.add_argument("--model_name", type=str, default="mobilenet_v3_large", choices=["mobilenet_v3_large", "efficientnet_b0", "convnext_tiny"])
    parser.add_argument("--batch_size", type=int, default=64)
    parser.add_argument("--epochs", type=int, default=15)
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--img_size", type=int, default=320)
    parser.add_argument("--device", type=str, default="auto", choices=["auto", "cuda", "cpu"])
    parser.add_argument("--multi_task", action="store_true", help="Enable multi-task dual heads for disease and severity")
    parser.add_argument("--export", action="store_true", help="Attempt export to ExecuTorch .pte")
    args = parser.parse_args()

    # Determine device
    if args.device == "auto":
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    else:
        device = torch.device(args.device)

    print("=" * 60)
    print("Crop Doctor - Vision Model Training Pipeline")
    print(f"Device:      {device}")
    print(f"Data Dir:    {args.data_dir}")
    print(f"Model:       {args.model_name} (Image Size: {args.img_size}x{args.img_size})")
    print(f"Multi-Task:  {args.multi_task} (Dual Heads: Disease + Severity)")
    print(f"Batch Size:  {args.batch_size} | Epochs: {args.epochs} | LR: {args.lr}")
    print("=" * 60)

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # Scan dataset
    print("\nScanning dataset...")
    samples, class_meta = scan_dataset(args.data_dir, args.class_map)
    print(f"Found {len(samples)} valid images across classes.")
    if len(samples) == 0:
        print("[ERROR] No samples found in data_dir! Please check the path.", file=sys.stderr)
        sys.exit(1)

    # Stratified Split (80% train, 20% val)
    labels = [s[1] for s in samples]
    train_samples, val_samples = train_test_split(
        samples, test_size=0.20, stratify=labels, random_state=42
    )
    print(f"Split sizes: Train={len(train_samples)}, Val={len(val_samples)}")

    # Transforms & DataLoaders
    train_tf = get_transforms(img_size=args.img_size, is_train=True)
    val_tf = get_transforms(img_size=args.img_size, is_train=False)

    num_workers = min(4, os.cpu_count() or 2) if device.type == 'cuda' else 0
    train_loader = DataLoader(
        CropDataset(train_samples, train_tf),
        batch_size=args.batch_size,
        shuffle=True,
        num_workers=num_workers,
        pin_memory=(device.type == 'cuda')
    )
    val_loader = DataLoader(
        CropDataset(val_samples, val_tf),
        batch_size=args.batch_size,
        shuffle=False,
        num_workers=num_workers,
        pin_memory=(device.type == 'cuda')
    )

    # Initialize Model
    num_classes = len(class_meta)
    print(f"\nInitializing {args.model_name} with {num_classes} classes (multi_task={args.multi_task})...")
    model = create_model(
        args.model_name,
        num_classes=num_classes,
        num_severity=3,
        multi_task=args.multi_task,
        pretrained=True
    ).to(device)

    # Class-frequency loss weighting to ensure minority classes are not ignored
    class_sample_counts = torch.zeros(num_classes)
    for s in train_samples:
        class_sample_counts[s[1]] += 1

    weights = torch.ones(num_classes)
    active_mask = class_sample_counts > 0
    weights[active_mask] = 1.0 / torch.sqrt(class_sample_counts[active_mask])
    weights[active_mask] = weights[active_mask] / weights[active_mask].mean()
    weights = weights.to(device)
    print(f"Applied class-frequency loss weights (min={weights[active_mask].min():.2f}, max={weights[active_mask].max():.2f})")

    criterion = nn.CrossEntropyLoss(weight=weights, label_smoothing=0.1)
    criterion_sev = nn.CrossEntropyLoss()
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=args.epochs, eta_min=1e-6)
    scaler = torch.amp.GradScaler('cuda', enabled=(device.type == 'cuda'))

    best_val_acc = 0.0
    best_model_path = output_dir / "best_model.pth"
    history = []

    print("\n--- Starting Training ---")
    start_time = time.time()

    for epoch in range(1, args.epochs + 1):
        ep_start = time.time()
        train_loss, train_acc = train_epoch(
            model, train_loader, criterion, criterion_sev, optimizer, scaler, device, is_multi_task=args.multi_task
        )
        val_loss, val_acc = validate_epoch(
            model, val_loader, criterion, criterion_sev, device, is_multi_task=args.multi_task
        )
        scheduler.step()
        ep_duration = time.time() - ep_start

        is_best = val_acc > best_val_acc
        if is_best:
            best_val_acc = val_acc
            torch.save({
                'epoch': epoch,
                'model_state_dict': model.state_dict(),
                'val_acc': val_acc,
                'model_name': args.model_name,
                'num_classes': num_classes
            }, best_model_path)

        star = " *" if is_best else ""
        print(f"Epoch {epoch:02d}/{args.epochs:02d} | Train Acc: {train_acc:6.2f}% (Loss: {train_loss:.4f}) | "
              f"Val Acc: {val_acc:6.2f}% (Loss: {val_loss:.4f}) | Time: {ep_duration:4.1f}s{star}")

        history.append({
            'epoch': epoch,
            'train_acc': round(train_acc, 2),
            'train_loss': round(train_loss, 4),
            'val_acc': round(val_acc, 2),
            'val_loss': round(val_loss, 4)
        })

    total_time = time.time() - start_time
    print(f"\nTraining completed in {total_time / 60:.1f} minutes. Best Val Accuracy: {best_val_acc:.2f}%")

    # Load best checkpoint for calibration & evaluation
    checkpoint = torch.load(best_model_path, map_location=device)
    model.load_state_dict(checkpoint['model_state_dict'])

    print("\n--- Temperature Calibration ---")
    temp_scaler = TemperatureScaler()
    calibrated_T = temp_scaler.fit(val_loader, model, device)
    print(f"Learned Temperature Scalar T = {calibrated_T:.4f}")

    # Full Evaluation
    print("\n--- Comprehensive Evaluation ---")
    metrics = evaluate_model(model, val_loader, class_meta, device, temp_scaler, output_dir=output_dir)
    metrics["best_val_accuracy"] = round(best_val_acc, 2)
    metrics["temperature_T"] = round(calibrated_T, 4)
    metrics["training_time_seconds"] = round(total_time, 1)

    with open(output_dir / "metrics.json", "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    with open(output_dir / "train_log.json", "w", encoding="utf-8") as f:
        json.dump(history, f, indent=2)

    # Save canonical labels.json for Android
    labels_list = [class_meta[i]['class_id'] for i in range(num_classes)]
    with open(output_dir / "labels.json", "w", encoding="utf-8") as f:
        json.dump(labels_list, f, indent=2)

    # Save preprocess.json
    preprocess_spec = {
        "input_size": args.img_size,
        "mean": [0.485, 0.456, 0.406],
        "std": [0.229, 0.224, 0.225],
        "temperature": round(calibrated_T, 4)
    }
    with open(output_dir / "preprocess.json", "w", encoding="utf-8") as f:
        json.dump(preprocess_spec, f, indent=2)

    print(f"Artifacts successfully written to: {output_dir}")

    # Export to ExecuTorch if requested
    if args.export:
        pte_path = output_dir / "vision.pte"
        export_to_executorch(model, pte_path, img_size=args.img_size)

if __name__ == "__main__":
    main()

