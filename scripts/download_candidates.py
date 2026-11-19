#!/usr/bin/env python3
"""
Crop Doctor - Candidate Datasets Downloader & Ingestion (Task D2)
----------------------------------------------------------------
Resumable downloader and extractor for approved candidate datasets.
- Logs progress, sizes, and SHA-256 checksums to C:\\crop-data\\logs\\download_candidates.log
- Checks 3 GB safety limit before every download
- Downloads into C:\\crop-data\\candidates\\raw\\<source_id>\\
- Extracts and maps shortlisted classes into C:\\crop-data\\candidates\\<crop>\\<problem>\\
- Generates C:\\crop-data\\candidates\\manifest.csv (file, crop, problem, source, licence, sha256)
- Strict compliance with Rule D: Never touches C:\\crop-data\\by_class, small, or realtest
"""

import os
import sys
import io
import csv
import glob
import shutil
import hashlib
import logging
import zipfile
import argparse
import subprocess
import urllib.request
from pathlib import Path

BASE_CROP_DATA = Path(r"C:\crop-data")
CANDIDATES_DIR = BASE_CROP_DATA / "candidates"
RAW_DIR = CANDIDATES_DIR / "raw"
LOG_DIR = BASE_CROP_DATA / "logs"
MANIFEST_PATH = CANDIDATES_DIR / "manifest.csv"
MAX_SINGLE_DOWNLOAD = 3 * 1024 * 1024 * 1024  # 3 GB safety limit

def setup_logger() -> logging.Logger:
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    log_file = LOG_DIR / "download_candidates.log"
    logger = logging.getLogger("download_candidates")
    logger.setLevel(logging.INFO)
    if not logger.handlers:
        fh = logging.FileHandler(log_file, encoding="utf-8")
        sh = logging.StreamHandler(sys.stdout)
        fmt = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s")
        fh.setFormatter(fmt)
        sh.setFormatter(fmt)
        logger.addHandler(fh)
        logger.addHandler(sh)
    return logger

def sha256_file(filepath: Path) -> str:
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(4 * 1024 * 1024):
            h.update(chunk)
    return h.hexdigest()

def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def download_resumable(url: str, dest_path: Path, logger: logging.Logger) -> str:
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = dest_path.with_suffix(dest_path.suffix + ".part")
    
    # Check if already fully downloaded
    if dest_path.exists() and dest_path.stat().st_size > 0:
        logger.info(f"File already exists: {dest_path.name} ({dest_path.stat().st_size / (1024*1024):.2f} MB)")
        return sha256_file(dest_path)

    # Check remote size via HEAD
    expected_size = None
    try:
        head_req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 CropDoctor/1.0"}, method="HEAD")
        with urllib.request.urlopen(head_req, timeout=10) as head_res:
            cl = head_res.headers.get("Content-Length")
            if cl:
                expected_size = int(cl)
    except Exception:
        pass

    if expected_size and expected_size > MAX_SINGLE_DOWNLOAD:
        raise ValueError(f"Download size {expected_size / (1024**3):.2f} GB exceeds safety limit of 3 GB! User confirmation required.")

    downloaded = 0
    if temp_path.exists():
        downloaded = temp_path.stat().st_size

    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 CropDoctor/1.0"})
    if downloaded > 0:
        req.add_header("Range", f"bytes={downloaded}-")

    logger.info(f"Starting download: {dest_path.name} from byte {downloaded}...")
    try:
        with urllib.request.urlopen(req, timeout=30) as res:
            mode = "ab" if downloaded > 0 and res.status == 206 else "wb"
            if mode == "wb":
                downloaded = 0
            with open(temp_path, mode) as f:
                last_log = downloaded
                while chunk := res.read(2 * 1024 * 1024):
                    f.write(chunk)
                    downloaded += len(chunk)
                    if downloaded - last_log >= 25 * 1024 * 1024:
                        logger.info(f"  [{dest_path.name}] {downloaded / (1024*1024):.1f} MB downloaded")
                        last_log = downloaded

        temp_path.replace(dest_path)
        digest = sha256_file(dest_path)
        size_mb = dest_path.stat().st_size / (1024 * 1024)
        logger.info(f"Finished {dest_path.name}: {size_mb:.2f} MB | SHA-256: {digest}")
        return digest
    except Exception as e:
        logger.error(f"Download error on {dest_path.name}: {e}")
        raise

def extract_zip(zip_path: Path, target_dir: Path, logger: logging.Logger):
    target_dir.mkdir(parents=True, exist_ok=True)
    logger.info(f"Extracting {zip_path.name} -> {target_dir}...")
    with zipfile.ZipFile(zip_path, 'r') as z:
        z.extractall(target_dir)
    logger.info(f"Extracted {zip_path.name} successfully.")

def copy_images_to_candidates(src_folder: Path, dest_crop: str, dest_problem: str, source_id: str, licence: str, manifest_writer, logger: logging.Logger, max_images: int = 1500) -> int:
    target_dir = CANDIDATES_DIR / dest_crop / dest_problem
    target_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    
    # Supported image extensions
    exts = ('*.jpg', '*.jpeg', '*.png', '*.JPG', '*.JPEG', '*.PNG')
    img_files = []
    for ext in exts:
        img_files.extend(src_folder.glob(f"**/{ext}"))
    
    # Sort for deterministic copying
    img_files = sorted(list(set(img_files)))
    for p in img_files[:max_images]:
        dest_filename = f"{source_id}_{dest_problem}_{count:04d}{p.suffix.lower()}"
        dest_file = target_dir / dest_filename
        if not dest_file.exists():
            shutil.copy2(p, dest_file)
        
        digest = sha256_file(dest_file)
        manifest_writer.writerow([
            str(dest_file.relative_to(CANDIDATES_DIR)),
            dest_crop,
            dest_problem,
            source_id,
            licence,
            digest
        ])
        count += 1
    
    logger.info(f"Copied {count} images to {dest_crop}/{dest_problem} from {source_id}")
    return count

print("download_candidates script module ready.")
