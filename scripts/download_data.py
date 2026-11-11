#!/usr/bin/env python3
"""
Crop Doctor - Dataset Ingestion & Download Helper
------------------------------------------------
Handles scripted downloads (Git, Hugging Face, Kaggle) and provides
guided manual download instructions for Mendeley CC BY 4.0 repositories.
"""

import os
import sys
import subprocess
import argparse
from pathlib import Path

MENDELEY_DATASETS = [
    {
        "crop": "Chilli (మిరప)",
        "id": "ymt8k9bjkn",
        "version": "3",
        "name": "Image Dataset on Chili Leaf Diseases in the Krishna River Basin",
        "url": "https://data.mendeley.com/datasets/ymt8k9bjkn/3",
        "target_folder": "chilli_krishna_basin"
    },
    {
        "crop": "Cotton (పత్తి)",
        "id": "74jsdxtmx2",
        "version": "3",
        "name": "Cotton Leaf Disease Dataset with Severity Levels",
        "url": "https://data.mendeley.com/datasets/74jsdxtmx2/3",
        "target_folder": "cotton_severity"
    },
    {
        "crop": "Cotton (పత్తి)",
        "id": "t9hgvk2h9p",
        "version": "1",
        "name": "Cotton Leaf Image Dataset for Disease Classification",
        "url": "https://data.mendeley.com/datasets/t9hgvk2h9p/1",
        "target_folder": "cotton_alternaria"
    },
    {
        "crop": "Maize (మొక్కజొన్న)",
        "id": "3myfctrgk3",
        "version": "1",
        "name": "MLD: Multi-Class Maize Leaf Disease Image Dataset",
        "url": "https://data.mendeley.com/datasets/3myfctrgk3/1",
        "target_folder": "maize_mld"
    }
]

def clone_plantdoc(target_dir):
    plantdoc_path = Path(target_dir) / "plantdoc"
    if plantdoc_path.exists():
        print(f"[PlantDoc] Already present at {plantdoc_path}")
        return
    print("\n[PlantDoc] Cloning real-field dataset from GitHub...")
    cmd = ["git", "clone", "--depth", "1", "https://github.com/pratikkayal/PlantDoc-Dataset.git", str(plantdoc_path)]
    subprocess.run(cmd, check=True)
    print("[PlantDoc] Cloned successfully!")

def print_mendeley_instructions(dest_raw):
    print("\n" + "="*70)
    print("MANUAL DOWNLOAD REQUIRED FOR MENDELEY DATASETS (CC BY 4.0)")
    print("Mendeley protects bulk downloads behind direct browser verification.")
    print("Follow these 3 simple steps on your laptop:")
    print("="*70)
    for idx, d in enumerate(MENDELEY_DATASETS, 1):
        print(f"\n{idx}. {d['crop']} - {d['name']}")
        print(f"   Link: {d['url']}")
        print(f"   Action: Open link in browser -> Click 'Download All' (Zip file).")
        print(f"   Save zip to: C:\\crop-data\\raw\\{d['target_folder']}.zip")
        print(f"   Extract to:  {dest_raw}/{d['target_folder']}/")
    print("\n" + "="*70)
    print(f"Once extracted, run the resize script before uploading to Google Drive:")
    print(f"python scripts/resize_images.py --src {dest_raw} --dst C:\\crop-data\\drive_upload")
    print("="*70 + "\n")

def main():
    parser = argparse.ArgumentParser(description="Download and organize Crop Doctor datasets.")
    parser.add_argument("--dest", type=str, default="C:\\crop-data\\raw", help="Destination raw folder")
    parser.add_argument("--scripted-only", action="store_true", help="Download only scripted datasets")
    args = parser.parse_args()

    dest_path = Path(args.dest)
    dest_path.mkdir(parents=True, exist_ok=True)

    print(f"Target raw directory: {dest_path.resolve()}")

    # 1. Download PlantDoc (real field photos for tomato & maize)
    try:
        clone_plantdoc(dest_path)
    except Exception as e:
        print(f"Warning: Failed to clone PlantDoc: {e}", file=sys.stderr)

    # 2. Print Mendeley download guidance
    print_mendeley_instructions(dest_path)

if __name__ == '__main__':
    main()

