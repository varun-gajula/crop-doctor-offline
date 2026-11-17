#!/usr/bin/env python3
"""
Crop Doctor - Candidate Datasets Downloader & Ingestion (Task D3)
----------------------------------------------------------------
Resumable downloader, extractor, and manifest generator for the 6 missing crop classes.
- Downloads into: C:\\crop-data\\candidates_v3\\raw\\<source_id>\\
- Target classes: C:\\crop-data\\candidates_v3\\<crop>_<problem>\\
- Manifest file: C:\\crop-data\\candidates_v3\\manifest.csv (file, class, source, licence, sha256)
- Log file: C:\\crop-data\\logs\\download_candidates_v3.log
- Safety limits: Halts before any single archive > 3 GB
- Rule D: Never touches C:\\crop-data\\by_class, small, or realtest
"""

import os
import sys
import csv
import glob
import shutil
import hashlib
import logging
import zipfile
import urllib.request
from pathlib import Path

BASE_CROP_DATA = Path(r"C:\crop-data")
CANDIDATES_V3_DIR = BASE_CROP_DATA / "candidates_v3"
RAW_DIR = CANDIDATES_V3_DIR / "raw"
LOG_DIR = BASE_CROP_DATA / "logs"
MANIFEST_PATH = CANDIDATES_V3_DIR / "manifest.csv"
MAX_SINGLE_DOWNLOAD = 3 * 1024 * 1024 * 1024  # 3 GB safety limit

def setup_logger() -> logging.Logger:
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    log_file = LOG_DIR / "download_candidates_v3.log"
    logger = logging.getLogger("download_candidates_v3")
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

def download_resumable(url: str, dest_path: Path, logger: logging.Logger) -> str:
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = dest_path.with_suffix(dest_path.suffix + ".part")
    
    if dest_path.exists() and dest_path.stat().st_size > 0:
        logger.info(f"File already exists: {dest_path.name} ({dest_path.stat().st_size / (1024*1024):.2f} MB)")
        return sha256_file(dest_path)

    expected_size = None
    try:
        head_req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 CropDoctor/1.0"}, method="HEAD")
        with urllib.request.urlopen(head_req, timeout=15) as head_res:
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

def copy_images_to_candidates(src_folder: Path, dest_class: str, source_id: str, licence: str, manifest_writer, logger: logging.Logger, max_images: int = 2500, prefix: str = None) -> int:
    target_dir = CANDIDATES_V3_DIR / dest_class
    target_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    pfx = prefix or source_id
    
    exts = ('*.jpg', '*.jpeg', '*.png', '*.JPG', '*.JPEG', '*.PNG')
    img_files = []
    for ext in exts:
        img_files.extend(src_folder.glob(f"**/{ext}"))
    
    img_files = sorted(list(set(img_files)))
    for p in img_files[:max_images]:
        dest_filename = f"{pfx}_{dest_class}_{count:04d}{p.suffix.lower()}"
        dest_file = target_dir / dest_filename
        if not dest_file.exists():
            shutil.copy2(p, dest_file)
        
        digest = sha256_file(dest_file)
        manifest_writer.writerow([
            str(dest_file.relative_to(CANDIDATES_V3_DIR)).replace("\\", "/"),
            dest_class,
            source_id,
            licence,
            digest
        ])
        count += 1
    
    logger.info(f"Copied {count} images to {dest_class} from {source_id}")
    return count

def run_downloads():
    logger = setup_logger()
    logger.info("==================================================")
    logger.info("STARTING TASK D3 AUTOMATED DOWNLOADS PIPELINE")
    logger.info("==================================================")
    
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    manifest_exists = MANIFEST_PATH.exists() and MANIFEST_PATH.stat().st_size > 0
    manifest_file = open(MANIFEST_PATH, "a" if manifest_exists else "w", newline="", encoding="utf-8")
    manifest_writer = csv.writer(manifest_file)
    if not manifest_exists:
        manifest_writer.writerow(["file", "class", "source", "licence", "sha256"])
        manifest_file.flush()

    # 1. CHILLI LEAF CURL: tm3v4zmh7c + ymt8k9bjkn
    try:
        logger.info("--- Processing Chilli Leaf Curl ---")
        tm_raw = RAW_DIR / "tm3v4zmh7c"
        tm_raw.mkdir(parents=True, exist_ok=True)
        curl_zip = tm_raw / "Curl_Virus.zip"
        if not curl_zip.exists():
            download_resumable("https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/d94a4729-47ec-4a39-bead-498e16347c4d/file_downloaded", curl_zip, logger)
        extract_zip(curl_zip, tm_raw / "extracted_curl", logger)
        copy_images_to_candidates(tm_raw / "extracted_curl", "chilli_leaf_curl", "tm3v4zmh7c", "CC BY 4.0", manifest_writer, logger, prefix="tm")
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in chilli leaf curl: {e}")

    # 2. CHILLI ANTHRACNOSE: wzc6r6w5w5
    try:
        logger.info("--- Processing Chilli Anthracnose ---")
        wzc_raw = RAW_DIR / "wzc6r6w5w5"
        wzc_raw.mkdir(parents=True, exist_ok=True)
        anth_zip = wzc_raw / "Anthracnose.zip"
        if not anth_zip.exists():
            download_resumable("https://data.mendeley.com/public-files/datasets/wzc6r6w5w5/files/cc609830-6f19-450c-83cf-6165fd5c4bef/file_downloaded", anth_zip, logger)
        extract_zip(anth_zip, wzc_raw / "extracted_anthracnose", logger)
        copy_images_to_candidates(wzc_raw / "extracted_anthracnose", "chilli_anthracnose", "wzc6r6w5w5", "CC BY 4.0", manifest_writer, logger, prefix="wzc")
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in chilli anthracnose: {e}")

    # 3. CHILLI BACTERIAL SPOT: tm3v4zmh7c
    try:
        logger.info("--- Processing Chilli Bacterial Spot ---")
        tm_raw = RAW_DIR / "tm3v4zmh7c"
        bs_zip = tm_raw / "Bacterial_Spot.zip"
        if not bs_zip.exists():
            download_resumable("https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/dca1dd29-a792-4803-8d3b-22fcee2ee929/file_downloaded", bs_zip, logger)
        extract_zip(bs_zip, tm_raw / "extracted_bs", logger)
        copy_images_to_candidates(tm_raw / "extracted_bs", "chilli_bacterial_spot", "tm3v4zmh7c", "CC BY 4.0", manifest_writer, logger, prefix="tm")
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in chilli bacterial spot: {e}")

    # 4. CHILLI HEALTHY: tm3v4zmh7c
    try:
        logger.info("--- Processing Chilli Healthy ---")
        tm_raw = RAW_DIR / "tm3v4zmh7c"
        hlth_zip = tm_raw / "Healthy_Leaf.zip"
        if not hlth_zip.exists():
            download_resumable("https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/5754b152-c21f-4989-b5fe-a370c32b9409/file_downloaded", hlth_zip, logger)
        extract_zip(hlth_zip, tm_raw / "extracted_hlth", logger)
        copy_images_to_candidates(tm_raw / "extracted_hlth", "chilli_healthy", "tm3v4zmh7c", "CC BY 4.0", manifest_writer, logger, prefix="tm")
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in chilli healthy: {e}")

    # 5. MAIZE FALL ARMYWORM: 3myfctrgk3
    try:
        logger.info("--- Processing Maize Fall Armyworm ---")
        mld_raw = RAW_DIR / "3myfctrgk3"
        mld_raw.mkdir(parents=True, exist_ok=True)
        mld_zip = mld_raw / "Maize dataset.zip"
        if not mld_zip.exists():
            download_resumable("https://data.mendeley.com/public-files/datasets/3myfctrgk3/files/691a1e78-dd69-43ee-a624-77df3e2f6a84/file_downloaded", mld_zip, logger)
        extract_zip(mld_zip, mld_raw / "extracted", logger)
        copy_images_to_candidates(mld_raw / "extracted" / "Maize dataset" / "Fall_Armyworm", "maize_fall_armyworm", "3myfctrgk3", "CC BY 4.0", manifest_writer, logger, prefix="mld")
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in maize fall armyworm: {e}")

    # 6. COTTON LEAF CURL VIRUS: b3jy2p6k8w + y7544ghy3y + 74jsdxtmx2
    try:
        logger.info("--- Processing Cotton Leaf Curl Virus ---")
        b3_raw = RAW_DIR / "b3jy2p6k8w"
        b3_raw.mkdir(parents=True, exist_ok=True)
        b3_zip = b3_raw / "Original Dataset.zip"
        if not b3_zip.exists():
            download_resumable("https://data.mendeley.com/public-files/datasets/b3jy2p6k8w/files/9a365367-8a96-4c15-8bcc-a533ab79c7d6/file_downloaded", b3_zip, logger)
        extract_zip(b3_zip, b3_raw / "extracted", logger)
        for p in (b3_raw / "extracted").rglob("*"):
            if p.is_dir() and "curl" in p.name.lower():
                copy_images_to_candidates(p, "cotton_leaf_curl", "b3jy2p6k8w", "CC BY 4.0", manifest_writer, logger, prefix="b3")

        y7_raw = RAW_DIR / "y7544ghy3y"
        y7_raw.mkdir(parents=True, exist_ok=True)
        y7_zip = y7_raw / "CoSev.zip"
        if not y7_zip.exists():
            download_resumable("https://data.mendeley.com/public-files/datasets/y7544ghy3y/files/0e90bf5b-3ba3-48f1-9564-5babd1fd2003/file_downloaded", y7_zip, logger)
        extract_zip(y7_zip, y7_raw / "extracted", logger)
        for p in (y7_raw / "extracted").rglob("*"):
            if p.is_dir() and "curl" in p.name.lower():
                copy_images_to_candidates(p, "cotton_leaf_curl", "y7544ghy3y", "CC BY 4.0", manifest_writer, logger, prefix="cosev")
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in cotton leaf curl: {e}")

    manifest_file.close()
    logger.info("Pipeline D3 execution finished.")

if __name__ == "__main__":
    print("download_candidates_v3 module ready. Run with approval.")
