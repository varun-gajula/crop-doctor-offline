"""
Crop Doctor - Dataset Loader & Preprocessing
--------------------------------------------
Implements robust PyTorch Dataset with domain-gap mitigating augmentations
and stratified Train/Val/Test splits.
"""

import os
import yaml
from pathlib import Path
from PIL import Image
import torch
from torch.utils.data import Dataset, DataLoader
import torchvision.transforms as transforms

def get_transforms(img_size=320, is_train=True):
    """
    Multi-organ data transforms (leaves, stems, fruits, pests) designed to prevent
    shortcut learning from lab backgrounds.
    Augmentations simulate outdoor sunlight, variable angles, scale variance, and blur.
    """
    normalize = transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )

    if is_train:
        return transforms.Compose([
            transforms.RandomResizedCrop(img_size, scale=(0.4, 1.0)),
            transforms.RandomHorizontalFlip(p=0.5),
            transforms.RandomVerticalFlip(p=0.5),
            transforms.RandomRotation(degrees=45),
            transforms.ColorJitter(brightness=0.3, contrast=0.3, saturation=0.3, hue=0.08),
            transforms.GaussianBlur(kernel_size=(5, 5), sigma=(0.1, 2.0)),
            transforms.ToTensor(),
            normalize
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

def scan_dataset(data_dir, class_map_path):
    """
    Scans data directory using data/class_map.yaml as canonical truth.
    Returns: samples list and class mapping dict.
    Each sample: (image_path, class_idx, severity_idx, is_field)
    """
    with open(class_map_path, 'r', encoding='utf-8') as f:
        class_meta = yaml.safe_load(f)['classes']

    data_dir = Path(data_dir)
    samples = []
    valid_exts = {'.jpg', '.jpeg', '.png', '.webp'}

    for idx, info in class_meta.items():
        cid = info['class_id']
        class_dir = data_dir / cid
        if not class_dir.exists():
            matches = list(data_dir.glob(f"{idx:02d}_*"))
            if matches:
                class_dir = matches[0]
            else:
                continue

        for img_path in class_dir.rglob('*'):
            if img_path.is_file() and img_path.suffix.lower() in valid_exts:
                # Mark as real-field if path contains 'field', 'krishna', or 'mld'
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

