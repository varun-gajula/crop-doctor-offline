#!/usr/bin/env python3
"""
Diagnostic script to test and verify image paths for all 13 classes.
"""
import os
import zipfile
from pathlib import Path

# Paths to verify
SOURCES = {
    "rice_leaf_blast": [
        ("Paddy Doctor (Zip)", r"C:\crop-data\candidates\raw\paddy-disease-classification.zip", "train_images/blast"),
        ("Project-AgML (512)", r"C:\crop-data\crop_doctor_512\rice_leaf_blast", None),
    ],
    "rice_healthy": [
        ("Paddy Doctor (Zip)", r"C:\crop-data\candidates\raw\paddy-disease-classification.zip", "train_images/normal"),
        ("Project-AgML (512)", r"C:\crop-data\crop_doctor_512\rice_healthy", None),
    ],
    "cotton_bacterial_blight": [
        ("Mendeley 74jsdxtmx2", r"C:\crop-data\candidates\raw\74jsdxtmx2\extracted\Cotton-Original\bacterial blight", None),
        ("Mendeley t9hgvk2h9p", r"C:\crop-data\candidates\raw\t9hgvk2h9p\extracted\Cotton_Original_Dataset\Bacterial Blight", None),
        ("Mendeley b3jy2p6k8w", r"C:\crop-data\candidates\raw\b3jy2p6k8w\extracted\Original Dataset\Bacterial Blight", None),
    ],
    "cotton_healthy": [
        ("Mendeley 74jsdxtmx2", r"C:\crop-data\candidates\raw\74jsdxtmx2\extracted\Cotton-Original\healthy", None),
        ("Mendeley t9hgvk2h9p", r"C:\crop-data\candidates\raw\t9hgvk2h9p\extracted\Cotton_Original_Dataset\Healthy Leaf", None),
        ("Mendeley b3jy2p6k8w", r"C:\crop-data\candidates\raw\b3jy2p6k8w\extracted\Original Dataset\Healthy Leaf", None),
    ],
    "chilli_leaf_curl_virus": [
        ("Mendeley ymt8k9bjkn", r"C:\Users\<path>\Downloads\Chili Leaf Disease Original Dataset\Chili Leaf Disease Original Dataset\Curl Virus", None),
    ],
    "chilli_healthy": [
        ("Mendeley ymt8k9bjkn", r"C:\Users\<path>\Downloads\Chili Leaf Disease Original Dataset\Chili Leaf Disease Original Dataset\Healthy Leaf", None),
    ],
    "maize_northern_leaf_blight": [
        ("Mendeley 3myfctrgk3 (MLD)", r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Leaf_Blight", None),
        ("Mendeley bwh3zbpkpv (CCMT)", r"C:\Users\<path>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\leaf blight", None),
        ("PlantDoc", r"C:\crop-data\candidates\raw\plantdoc\train\Corn leaf blight", None),
    ],
    "maize_healthy": [
        ("Mendeley 3myfctrgk3 (MLD)", r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Healthy", None),
        ("Mendeley bwh3zbpkpv (CCMT)", r"C:\Users\<path>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\healthy", None),
    ],
    "maize_fall_armyworm": [
        ("Mendeley 3myfctrgk3 (MLD)", r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset\Fall_Armyworm", None),
        ("Mendeley bwh3zbpkpv (CCMT)", r"C:\Users\<path>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize\fall armyworm", None),
    ],
    "rice_stem_borer": [
        ("Paddy Doctor (Zip)", r"C:\crop-data\candidates\raw\paddy-disease-classification.zip", "train_images/dead_heart"),
    ],
    "rice_hispa": [
        ("Paddy Doctor (Zip)", r"C:\crop-data\candidates\raw\paddy-disease-classification.zip", "train_images/hispa"),
    ],
    "tomato_spider_mites": [
        ("PlantVillage (D:)", r"D:\main set\Tomato_Spider_mites Two-spotted_spider_mite", None),
        ("PlantDoc (Train)", r"C:\crop-data\candidates\raw\plantdoc\train\Tomato two spotted spider mites leaf", None),
    ],
    "other_not_a_crop": [
        ("Kaggle agri_data (512)", r"C:\crop-data\crop_doctor_512\weeds_not_a_crop", None),
        ("Kaggle agri_data (Full)", r"C:\Users\<path>\Downloads\archive\agri_data\data", None),
    ]
}

def verify():
    print(f"Verifying {len(SOURCES)} classes...")
    for c_idx, (c_name, src_list) in enumerate(SOURCES.items(), 1):
        print(f"\n[{c_idx}/13] {c_name}:")
        for label, path, sub in src_list:
            if not os.path.exists(path):
                print(f"  FAILED: {label} -> {path} (NOT FOUND)")
                continue
            if path.endswith(".zip"):
                with zipfile.ZipFile(path, "r") as z:
                    imgs = [n for n in z.namelist() if n.startswith(sub + "/") and n.lower().endswith((".jpg", ".jpeg", ".png"))]
                    print(f"  OK: {label} (Zip) -> {len(imgs)} photos in {sub}")
            else:
                if os.path.isdir(path):
                    # Check if recursive subdirectories (e.g. Cotton mild/mod/sev)
                    files = []
                    for root, _, fs in os.walk(path):
                        for f in fs:
                            if f.lower().endswith((".jpg", ".jpeg", ".png", ".bmp", ".webp")):
                                files.append(os.path.join(root, f))
                    print(f"  OK: {label} -> {len(files)} photos in {path}")
                else:
                    print(f"  EXISTS: {path}")

if __name__ == "__main__":
    verify()
