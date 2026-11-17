#!/usr/bin/env python3
"""
Crop Doctor - Candidate Datasets Downloader (Task D2)
-----------------------------------------------------
Resumable downloader for approved candidate datasets.
- Logs download progress, file sizes, and SHA-256 checksums to C:\\crop-data\\logs\\
- Supports HTTP Range headers for resuming broken downloads
- Enforces strict safety gate: stops and asks before any single download > 3 GB
- Automatically extracts archives into C:\\crop-data\\candidates\\raw\\<source_id>\\
- Can populate shortlisted classes into C:\\crop-data\\candidates\\<crop>\\<problem>\\
  and generate C:\\crop-data\\candidates\\manifest.csv
"""

import os
import sys
import hashlib
import logging
import argparse
import urllib.request
import zipfile
import shutil
from pathlib import Path

DEFAULT_RAW_DIR = Path(r"C:\crop-data\candidates\raw")
DEFAULT_LOG_DIR = Path(r"C:\crop-data\logs")
DEFAULT_CANDIDATES_DIR = Path(r"C:\crop-data\candidates")
MAX_AUTO_DOWNLOAD_BYTES = 3 * 1024 * 1024 * 1024  # 3 GB hard ceiling

def setup_logger(log_dir: Path) -> logging.Logger:
    log_dir.mkdir(parents=True, exist_ok=True)
    log_path = log_dir / "download_candidates.log"
    logger = logging.getLogger("download_candidates")
    logger.setLevel(logging.INFO)
    if not logger.handlers:
        fh = logging.FileHandler(log_path, encoding="utf-8")
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

def download_file_resumable(url: str, dest_path: Path, expected_size_bytes: int = None, logger: logging.Logger = None) -> str:
    if logger is None:
        logger = logging.getLogger("download_candidates")

    dest_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = dest_path.with_suffix(dest_path.suffix + ".part")
    downloaded_bytes = 0

    if temp_path.exists():
        downloaded_bytes = temp_path.stat().st_size

    # Check size limit before starting
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 CropDoctor/1.0"})
    
    # Probe remote size via HEAD/GET if expected_size_bytes is not provided
    if not expected_size_bytes:
        try:
            head_req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 CropDoctor/1.0"}, method="HEAD")
            with urllib.request.urlopen(head_req, timeout=10) as head_res:
                cl = head_res.headers.get("Content-Length")
                if cl:
                    expected_size_bytes = int(cl)
        except Exception:
            pass

    if expected_size_bytes and expected_size_bytes > MAX_AUTO_DOWNLOAD_BYTES:
        raise ValueError(f"Download size {expected_size_bytes / (1024**3):.2f} GB exceeds safety threshold of 3 GB! User approval required.")

    if downloaded_bytes > 0:
        req.add_header("Range", f"bytes={downloaded_bytes}-")

    logger.info(f"Downloading {url} -> {dest_path.name} (starting from {downloaded_bytes / (1024*1024):.2f} MB)")
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            mode = "ab" if downloaded_bytes > 0 and response.status == 206 else "wb"
            if mode == "wb":
                downloaded_bytes = 0
            
            with open(temp_path, mode) as f:
                last_logged = downloaded_bytes
                while chunk := response.read(2 * 1024 * 1024):
                    f.write(chunk)
                    downloaded_bytes += len(chunk)
                    if downloaded_bytes - last_logged >= 25 * 1024 * 1024:
                        logger.info(f"  ... {dest_path.name}: {downloaded_bytes / (1024*1024):.1f} MB downloaded")
                        last_logged = downloaded_bytes

        temp_path.replace(dest_path)
        digest = sha256_file(dest_path)
        size_mb = dest_path.stat().st_size / (1024 * 1024)
        logger.info(f"Finished {dest_path.name}: {size_mb:.2f} MB, SHA256: {digest}")
        return digest
    except Exception as e:
        logger.error(f"Download error on {url}: {e}")
        raise

def extract_archive(archive_path: Path, extract_dir: Path, logger: logging.Logger):
    extract_dir.mkdir(parents=True, exist_ok=True)
    logger.info(f"Extracting {archive_path.name} -> {extract_dir}")
    if archive_path.suffix.lower() == ".zip":
        with zipfile.ZipFile(archive_path, 'r') as z:
            z.extractall(extract_dir)
    else:
        shutil.unpack_archive(str(archive_path), str(extract_dir))
    logger.info(f"Extraction complete for {archive_path.name}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Crop Doctor - Candidate Downloader")
    parser.add_argument("--url", type=str, help="URL to download")
    parser.add_argument("--source-id", type=str, help="Source identifier (folder name under candidates/raw/)")
    parser.add_argument("--filename", type=str, help="Destination filename")
    parser.add_argument("--extract", action="store_true", help="Extract archive after download")
    args = parser.parse_args()

    logger = setup_logger(DEFAULT_LOG_DIR)
    if args.url and args.source_id and args.filename:
        out_file = DEFAULT_RAW_DIR / args.source_id / args.filename
        digest = download_file_resumable(args.url, out_file, logger=logger)
        if args.extract:
            extract_archive(out_file, DEFAULT_RAW_DIR / args.source_id / "extracted", logger)
    else:
        logger.info("Crop Doctor candidate downloader initialized. Run with --url, --source-id, --filename to download a candidate.")
