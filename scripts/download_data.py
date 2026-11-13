#!/usr/bin/env python3
"""
Crop Doctor - Dataset Ingestion & Download Helper
------------------------------------------------
Handles scripted downloads (Git, Hugging Face, PlantDoc) with resumable
chunking, logs file sizes and checksums to C:\\crop-data\\logs\\, and provides
guided instructions for Mendeley CC BY 4.0 repositories.
"""

import os
import sys
import hashlib
import logging
import argparse
import subprocess
import urllib.request
from pathlib import Path

MENDELEY_DATASETS = [
    {
        "crop": "Chilli (మిరప)",
        "id": "ymt8k9bjkn",
        "version": "3",
        "doi": "10.17632/ymt8k9bjkn.3",
        "name": "Image Dataset on Chili Leaf Diseases in the Krishna River Basin",
        "url": "https://data.mendeley.com/datasets/ymt8k9bjkn/3",
        "licence": "CC BY 4.0",
        "target_folder": "chilli_krishna_basin"
    },
    {
        "crop": "Cotton (పత్తి)",
        "id": "74jsdxtmx2",
        "version": "1",
        "doi": "10.17632/74jsdxtmx2.1",
        "name": "Cotton Leaf Disease Dataset with Severity Levels",
        "url": "https://data.mendeley.com/datasets/74jsdxtmx2/1",
        "licence": "CC BY 4.0",
        "target_folder": "cotton_severity"
    },
    {
        "crop": "Cotton (పత్తి)",
        "id": "t9hgvk2h9p",
        "version": "1",
        "doi": "10.17632/t9hgvk2h9p.1",
        "name": "Cotton Leaf Image Dataset for Disease Classification",
        "url": "https://data.mendeley.com/datasets/t9hgvk2h9p/1",
        "licence": "CC BY 4.0",
        "target_folder": "cotton_alternaria"
    },
    {
        "crop": "Maize (మొక్కజొన్న)",
        "id": "3myfctrgk3",
        "version": "1",
        "doi": "10.17632/3myfctrgk3.1",
        "name": "MLD: Multi-Class Maize Leaf Disease Image Dataset",
        "url": "https://data.mendeley.com/datasets/3myfctrgk3/1",
        "licence": "CC BY 4.0",
        "target_folder": "maize_mld"
    }
]

def setup_logger(log_dir: Path):
    log_dir.mkdir(parents=True, exist_ok=True)
    log_path = log_dir / "download.log"
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        handlers=[
            logging.FileHandler(log_path, encoding="utf-8"),
            logging.StreamHandler(sys.stdout)
        ]
    )
    return logging.getLogger("download_data")

def sha256_file(filepath: Path) -> str:
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(4 * 1024 * 1024):
            h.update(chunk)
    return h.hexdigest()

def download_file_resumable(url: str, dest_path: Path, logger: logging.Logger):
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = dest_path.with_suffix(dest_path.suffix + ".part")
    downloaded_bytes = 0

    if temp_path.exists():
        downloaded_bytes = temp_path.stat().st_size

    req = urllib.request.Request(url, headers={"User-Agent": "CropDoctor/1.0"})
    if downloaded_bytes > 0:
        req.add_header("Range", f"bytes={downloaded_bytes}-")

    logger.info(f"Downloading {url} -> {dest_path.name} (starting at byte {downloaded_bytes})")
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            mode = "ab" if downloaded_bytes > 0 and response.status == 206 else "wb"
            if mode == "wb":
                downloaded_bytes = 0
            with open(temp_path, mode) as f:
                while chunk := response.read(1024 * 1024):
                    f.write(chunk)
                    downloaded_bytes += len(chunk)

        temp_path.replace(dest_path)
        digest = sha256_file(dest_path)
        size_mb = dest_path.stat().st_size / (1024 * 1024)
        logger.info(f"Downloaded {dest_path.name}: {size_mb:.2f} MB, SHA256: {digest}")
        return digest
    except Exception as e:
        logger.error(f"Failed to download {url}: {e}")
        raise

def clone_plantdoc(dest_dir: Path, logger: logging.Logger):
    target = dest_dir / "plantdoc"
    if target.exists():
        logger.info(f"[PlantDoc] Already exists at {target}")
        return
    logger.info("[PlantDoc] Cloning from https://github.com/pratikkayal/PlantDoc-Dataset.git...")
    cmd = ["git", "clone", "--depth", "1", "https://github.com/pratikkayal/PlantDoc-Dataset.git", str(target)]
    subprocess.run(cmd, check=True)
    logger.info("[PlantDoc] Cloned successfully.")

def print_mendeley_instructions(logger: logging.Logger):
    logger.info("=" * 70)
    logger.info("MENDELEY DATASETS (CC BY 4.0) MANUAL DOWNLOAD GUIDANCE")
    logger.info("Mendeley protects bulk zips behind direct browser verification.")
    logger.info("=" * 70)
    for idx, d in enumerate(MENDELEY_DATASETS, 1):
        logger.info(f"{idx}. {d['crop']} - {d['name']}")
        logger.info(f"   URL: {d['url']}")
        logger.info(f"   Licence: {d['licence']} | DOI: {d['doi']}")
        logger.info(f"   Action: Open link in browser -> Click 'Download All' (Zip).")
        logger.info(f"   Save zip to: C:\\crop-data\\raw\\{d['target_folder']}.zip")
    logger.info("=" * 70)

def main():
    parser = argparse.ArgumentParser(description="Crop Doctor - Dataset Ingestion & Downloader")
    parser.add_argument("--dest", type=str, default=r"C:\crop-data\raw", help="Target raw directory")
    parser.add_argument("--log-dir", type=str, default=r"C:\crop-data\logs", help="Log directory")
    parser.add_argument("--download-plantdoc", action="store_true", help="Clone PlantDoc dataset (after approval)")
    args = parser.parse_args()

    dest_dir = Path(args.dest)
    log_dir = Path(args.log_dir)
    logger = setup_logger(log_dir)

    logger.info(f"Crop Doctor Downloader started. Target raw: {dest_dir}, Log dir: {log_dir}")
    print_mendeley_instructions(logger)

    if args.download_plantdoc:
        clone_plantdoc(dest_dir, logger)

if __name__ == "__main__":
    main()
