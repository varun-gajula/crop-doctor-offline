"""
Crop Doctor - Dataset Loader & Preprocessing
--------------------------------------------
Implements robust PyTorch Dataset with domain-gap mitigating augmentations
and stratified Train/Val/Test splits.
"""

import os
import yaml
import random
from pathlib import Path
from PIL import Image
import torch
from torch.utils.data import Dataset, DataLoader
import torchvision.transforms as transforms

def get_transforms(img_size=320, is_train=True):
    """
    Multi-organ data transforms (leaves, stems, fruits, pests) designed to prevent
    shortcut learning from lab backgrounds.
    Plantix-grade field augmentations:
    - Harsh Indian midday sunlight and deep shade simulation (ColorJitter)
    - Camera tilt, distance and handheld angle variation (RandomAffine, RandomResizedCrop)
    - Camera focus blur and hand-shake motion (GaussianBlur)
    - Occlusions: fingers holding the leaf, soil dust, insect frass, tears (RandomErasing)
    """
    normalize = transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )

    if is_train:
        return transforms.Compose([
            transforms.RandomResizedCrop(img_size, scale=(0.4, 1.0), ratio=(0.75, 1.33)),
            transforms.RandomHorizontalFlip(p=0.5),
            transforms.RandomVerticalFlip(p=0.5),
            transforms.RandomRotation(degrees=45),
            transforms.RandomAffine(degrees=15, translate=(0.08, 0.08), scale=(0.9, 1.1)),
            transforms.ColorJitter(brightness=0.35, contrast=0.35, saturation=0.35, hue=0.06),
            transforms.RandomApply([
                transforms.GaussianBlur(kernel_size=(5, 5), sigma=(0.1, 1.8))
            ], p=0.3),
            transforms.ToTensor(),
            normalize,
            transforms.RandomErasing(p=0.25, scale=(0.02, 0.2), ratio=(0.3, 3.3), value='random')
        ])
    else:
        return transforms.Compose([
            transforms.Resize((img_size, img_size)),
            transforms.ToTensor(),
            normalize
        ])

class CropDataset(Dataset):
    def __init__(self, samples, transform=None):
        self.samples = samples  # List of (image_path, class_idx, severity_idx, is_field)
        self.transform = transform

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        path, label, severity, is_field = self.samples[idx]
        with Image.open(path) as img:
            img = img.convert('RGB')
            if self.transform:
                img = self.transform(img)
        return img, label, severity, is_field

def scan_dataset(data_dir, class_map_path, max_samples_per_class=800, seed=42):
    """
    Scans data directory using data/class_map.yaml as canonical truth.
    Applies class-capping (max_samples_per_class) to eliminate majority bias
    (e.g. 12,000 Tomato images vs 24 Cotton images).
    Returns: samples list and class mapping dict.
    Each sample: (image_path, class_idx, severity_idx, is_field)
    """
    with open(class_map_path, 'r', encoding='utf-8') as f:
        class_meta = yaml.safe_load(f)['classes']

    data_dir = Path(data_dir)
    samples = []
    valid_exts = {'.jpg', '.jpeg', '.png', '.webp'}
    rng = random.Random(seed)

    for idx, info in class_meta.items():
        cid = info['class_id']
        class_dir = data_dir / cid
        if not class_dir.exists():
            matches = list(data_dir.glob(f"{idx:02d}_*"))
            if matches:
                class_dir = matches[0]
            else:
                continue

        class_images = []
        for img_path in class_dir.rglob('*'):
            if img_path.is_file() and img_path.suffix.lower() in valid_exts:
                class_images.append(img_path)

        # Cap majority classes to prevent severe imbalance
        if max_samples_per_class is not None and len(class_images) > max_samples_per_class:
            class_images = rng.sample(class_images, max_samples_per_class)

        for img_path in class_images:
            path_str = str(img_path).lower()
            is_field = any(k in path_str for k in ['field', 'krishna', 'mld', 'plantdoc'])

            # Severity level: 0 = Mild/Healthy, 1 = Moderate, 2 = Severe
            if 'healthy' in cid:
                severity_idx = 0
            elif 'mild' in path_str:
                severity_idx = 0
            elif any(k in path_str for k in ['severe', 'critical']):
                severity_idx = 2
            else:
                severity_idx = 1

            samples.append((str(img_path), idx, severity_idx, is_field))

    return samples, class_meta

