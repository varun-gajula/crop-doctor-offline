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

def get_transforms(img_size=224, is_train=True):
    """
    Data transforms designed to prevent shortcut learning from lab backgrounds.
    Augmentations simulate outdoor sunlight, blur, and orientation variance.
    """
    normalize = transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )

    if is_train:
        return transforms.Compose([
            transforms.RandomResizedCrop(img_size, scale=(0.7, 1.0)),
            transforms.RandomHorizontalFlip(p=0.5),
            transforms.RandomVerticalFlip(p=0.5),
            transforms.RandomRotation(degrees=30),
            transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2, hue=0.05),
            transforms.GaussianBlur(kernel_size=(5, 5), sigma=(0.1, 1.5)),
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
        self.samples = samples  # List of (image_path, class_idx, is_field)
        self.transform = transform

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        path, label, is_field = self.samples[idx]
        with Image.open(path) as img:
            img = img.convert('RGB')
            if self.transform:
                img = self.transform(img)
        return img, label, is_field

def scan_dataset(data_dir, class_map_path):
    """
    Scans data directory using data/class_map.yaml as canonical truth.
    Returns: samples list and class mapping dict.
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
            continue

        for img_path in class_dir.rglob('*'):
            if img_path.is_file() and img_path.suffix.lower() in valid_exts:
                # Mark as real-field if path contains 'field', 'krishna', or 'mld'
                is_field = any(k in str(img_path).lower() for k in ['field', 'krishna', 'mld', 'plantdoc'])
                samples.append((str(img_path), idx, is_field))

    return samples, class_meta

