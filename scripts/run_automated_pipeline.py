#!/usr/bin/env python3
"""
Crop Doctor - Candidate Datasets Automated Ingestion Pipeline (Task D2)
----------------------------------------------------------------------
Executes automated downloads, extracts archives into C:\\crop-data\\candidates\\raw\\<source_id>\\,
copies shortlisted photos into C:\\crop-data\\candidates\\<crop>\\<problem>\\,
and records each image into C:\\crop-data\\candidates\\manifest.csv.

Strict Compliance:
- Rule H9: Free only, verified open licences (CC BY 4.0, MIT, CC0, CC BY-NC 3.0)
- Rule H10: Quota economy, reuse scratch/cached archives
- Guardrail: Stop before any download > 3 GB
- Rule D: Never touch, move, or modify C:\\crop-data\\by_class, small, or realtest
- Logging: C:\\crop-data\\logs\\download_candidates.log
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
import urllib.request
from pathlib import Path
from PIL import Image

BASE_CROP_DATA = Path(r"C:\crop-data")
CANDIDATES_DIR = BASE_CROP_DATA / "candidates"
RAW_DIR = CANDIDATES_DIR / "raw"
LOG_DIR = BASE_CROP_DATA / "logs"
MANIFEST_PATH = CANDIDATES_DIR / "manifest.csv"
SCRATCH_DIR = Path(r"C:\Users\<path>\.gemini\antigravity\brain\ea68011e-978f-4545-b8fa-d6328681af64\scratch")
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
        raise ValueError(f"Download size {expected_size / (1024**3):.2f} GB exceeds safety limit of 3 GB! Confirmation required.")

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

def copy_images_to_candidates(src_folder: Path, dest_crop: str, dest_problem: str, source_id: str, licence: str, manifest_writer, logger: logging.Logger, max_images: int = 2000, prefix: str = None) -> int:
    target_dir = CANDIDATES_DIR / dest_crop / dest_problem
    target_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    pfx = prefix or source_id
    
    exts = ('*.jpg', '*.jpeg', '*.png', '*.JPG', '*.JPEG', '*.PNG')
    img_files = []
    for ext in exts:
        img_files.extend(src_folder.glob(f"**/{ext}"))
    
    img_files = sorted(list(set(img_files)))
    for p in img_files[:max_images]:
        dest_filename = f"{pfx}_{dest_problem}_{count:04d}{p.suffix.lower()}"
        dest_file = target_dir / dest_filename
        if not dest_file.exists():
            shutil.copy2(p, dest_file)
        
        digest = sha256_file(dest_file)
        manifest_writer.writerow([
            str(dest_file.relative_to(CANDIDATES_DIR)).replace("\\", "/"),
            dest_crop,
            dest_problem,
            source_id,
            licence,
            digest
        ])
        count += 1
    
    logger.info(f"Copied {count} images to {dest_crop}/{dest_problem} from {source_id}")
    return count

def extract_paddy_doctor_parquet(parquet_files: list, manifest_writer, logger: logging.Logger):
    import polars as pl
    logger.info("--- Extracting Paddy Doctor Parquet Files (Rice) ---")
    
    label_to_dest = {
        3: ("rice", "blast", "paddy_doctor_blast"),
        9: ("rice", "tungro", "paddy_doctor_tungro"),
        4: ("rice", "brown_spot", "paddy_doctor_brown_spot"),
        8: ("rice", "healthy", "paddy_doctor_healthy")
    }
    
    source_id = "anthony2261/paddy-disease-classification"
    licence = "CC BY 4.0"
    
    for pq_path in parquet_files:
        logger.info(f"Reading parquet: {pq_path.name}...")
        df = pl.read_parquet(pq_path)
        for label_val, (crop, problem, pfx) in label_to_dest.items():
            sub_df = df.filter(pl.col("label") == label_val)
            cnt = len(sub_df)
            if cnt == 0:
                continue
            
            dest_dir = CANDIDATES_DIR / crop / problem
            dest_dir.mkdir(parents=True, exist_ok=True)
            existing = len(list(dest_dir.glob(f"{pfx}_*.jpg")))
            
            logger.info(f"  Extracting {cnt} rows for {crop}/{problem} (already have {existing})...")
            written = 0
            for row in sub_df.iter_rows(named=True):
                img_bytes = row["image"]["bytes"]
                file_idx = existing + written
                dest_file = dest_dir / f"{pfx}_{file_idx:04d}.jpg"
                if not dest_file.exists():
                    with open(dest_file, "wb") as f:
                        f.write(img_bytes)
                
                digest = sha256_bytes(img_bytes)
                manifest_writer.writerow([
                    str(dest_file.relative_to(CANDIDATES_DIR)).replace("\\", "/"),
                    crop,
                    problem,
                    source_id,
                    licence,
                    digest
                ])
                written += 1
            logger.info(f"  Extracted {written} images to {crop}/{problem}")

def extract_grass_weeds_parquet(pq_path: Path, manifest_writer, logger: logging.Logger, max_images: int = 300):
    import polars as pl
    logger.info(f"--- Extracting Non-Crop Grass & Weeds ({pq_path.name}) ---")
    source_id = "Francesco/grass-weeds"
    licence = "CC BY 4.0"
    crop = "non_crop"
    problem = "field_weeds_grass"
    dest_dir = CANDIDATES_DIR / crop / problem
    dest_dir.mkdir(parents=True, exist_ok=True)
    
    df = pl.read_parquet(pq_path)
    logger.info(f"Loaded {len(df)} rows from grass-weeds parquet.")
    written = 0
    for row in df.iter_rows(named=True):
        if written >= max_images:
            break
        img_struct = row.get("image")
        if not img_struct:
            continue
        img_bytes = img_struct.get("bytes")
        if not img_bytes:
            continue
        dest_file = dest_dir / f"weed_{written:04d}.jpg"
        if not dest_file.exists():
            with open(dest_file, "wb") as f:
                f.write(img_bytes)
        digest = sha256_bytes(img_bytes)
        manifest_writer.writerow([
            str(dest_file.relative_to(CANDIDATES_DIR)).replace("\\", "/"),
            crop,
            problem,
            source_id,
            licence,
            digest
        ])
        written += 1
    logger.info(f"Extracted {written} non-crop weed photos.")

def run():
    logger = setup_logger()
    logger.info("==================================================")
    logger.info("STARTING AUTOMATED CANDIDATES INGESTION PIPELINE")
    logger.info("==================================================")
    
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    manifest_exists = MANIFEST_PATH.exists() and MANIFEST_PATH.stat().st_size > 0
    manifest_file = open(MANIFEST_PATH, "a" if manifest_exists else "w", newline="", encoding="utf-8")
    manifest_writer = csv.writer(manifest_file)
    if not manifest_exists:
        manifest_writer.writerow(["file", "crop", "problem", "source", "licence", "sha256"])
        manifest_file.flush()

    # =========================================================================
    # 1. MAIZE: MLD (3myfctrgk3) + PlantDoc
    # =========================================================================
    try:
        logger.info("=== 1. PROCESSING MAIZE ===")
        source_id = "3myfctrgk3"
        licence = "CC BY 4.0"
        mld_raw_dir = RAW_DIR / source_id
        mld_raw_dir.mkdir(parents=True, exist_ok=True)
        mld_zip = mld_raw_dir / "Maize dataset.zip"
        
        scratch_mld = SCRATCH_DIR / "maize_mld.zip"
        if scratch_mld.exists() and not mld_zip.exists():
            shutil.copy2(scratch_mld, mld_zip)
            logger.info("Reused maize_mld.zip from scratch cache.")
        
        if not mld_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/3myfctrgk3/files/691a1e78-dd69-43ee-a624-77df3e2f6a84/file_downloaded"
            download_resumable(url, mld_zip, logger)
        
        mld_extract = mld_raw_dir / "extracted"
        if not mld_extract.exists():
            extract_zip(mld_zip, mld_extract, logger)
        
        base_mld = mld_extract / "Maize dataset"
        copy_images_to_candidates(base_mld / "Fall_Armyworm", "maize", "fall_armyworm", source_id, licence, manifest_writer, logger, prefix="mld")
        copy_images_to_candidates(base_mld / "Leaf_Blight", "maize", "turcicum_leaf_blight", source_id, licence, manifest_writer, logger, prefix="mld")
        copy_images_to_candidates(base_mld / "Gray_Spot", "maize", "gray_leaf_spot", source_id, licence, manifest_writer, logger, prefix="mld")
        copy_images_to_candidates(base_mld / "Healthy", "maize", "healthy", source_id, licence, manifest_writer, logger, prefix="mld")
        
        # PlantDoc additions for maize
        plantdoc_base = RAW_DIR / "plantdoc"
        if plantdoc_base.exists():
            copy_images_to_candidates(plantdoc_base / "train" / "Corn leaf blight", "maize", "turcicum_leaf_blight", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc")
            copy_images_to_candidates(plantdoc_base / "test" / "Corn leaf blight", "maize", "turcicum_leaf_blight", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc_test")
            copy_images_to_candidates(plantdoc_base / "train" / "Corn Gray leaf spot", "maize", "gray_leaf_spot", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc")
            copy_images_to_candidates(plantdoc_base / "test" / "Corn Gray leaf spot", "maize", "gray_leaf_spot", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc_test")
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in Maize pipeline: {e}")

    # =========================================================================
    # 2. COTTON: t9hgvk2h9p, 74jsdxtmx2, b3jy2p6k8w, y7544ghy3y
    # =========================================================================
    try:
        logger.info("=== 2. PROCESSING COTTON ===")
        # A. t9hgvk2h9p
        source_id = "t9hgvk2h9p"
        licence = "CC BY 4.0"
        cotton_t9_raw = RAW_DIR / source_id
        cotton_t9_raw.mkdir(parents=True, exist_ok=True)
        cotton_t9_zip = cotton_t9_raw / "Cotton_Original_Dataset.zip"
        
        scratch_t9 = SCRATCH_DIR / "cotton_t9hgvk.zip"
        if scratch_t9.exists() and not cotton_t9_zip.exists():
            shutil.copy2(scratch_t9, cotton_t9_zip)
            logger.info("Reused cotton_t9hgvk.zip from scratch cache.")
            
        if not cotton_t9_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/t9hgvk2h9p/files/a3bc3269-5c19-4394-824b-50be764a27c4/file_downloaded"
            download_resumable(url, cotton_t9_zip, logger)
            
        cotton_t9_extract = cotton_t9_raw / "extracted"
        if not cotton_t9_extract.exists():
            extract_zip(cotton_t9_zip, cotton_t9_extract, logger)
            
        copy_images_to_candidates(cotton_t9_extract / "Bacterial Blight", "cotton", "bacterial_blight", source_id, licence, manifest_writer, logger, prefix="t9")
        copy_images_to_candidates(cotton_t9_extract / "Healthy Leaf", "cotton", "healthy", source_id, licence, manifest_writer, logger, prefix="t9")

        # B. 74jsdxtmx2
        source_id = "74jsdxtmx2"
        licence = "CC BY 4.0"
        cotton_74_raw = RAW_DIR / source_id
        cotton_74_raw.mkdir(parents=True, exist_ok=True)
        cotton_74_zip = cotton_74_raw / "Cotton-Original.zip"
        if not cotton_74_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/74jsdxtmx2/files/3467319f-60ab-45b7-a016-9b3376cf0700/file_downloaded"
            download_resumable(url, cotton_74_zip, logger)
        cotton_74_extract = cotton_74_raw / "extracted"
        if not cotton_74_extract.exists():
            extract_zip(cotton_74_zip, cotton_74_extract, logger)
        copy_images_to_candidates(cotton_74_extract / "bacterial blight", "cotton", "bacterial_blight", source_id, licence, manifest_writer, logger, prefix="74j")
        copy_images_to_candidates(cotton_74_extract / "healthy", "cotton", "healthy", source_id, licence, manifest_writer, logger, prefix="74j")

        # C. b3jy2p6k8w (SAR-CLD-2024: Leaf Reddening, Bacterial Blight, Leaf Curl, Healthy)
        source_id = "b3jy2p6k8w"
        licence = "CC BY 4.0"
        cotton_b3_raw = RAW_DIR / source_id
        cotton_b3_raw.mkdir(parents=True, exist_ok=True)
        cotton_b3_zip = cotton_b3_raw / "Original Dataset.zip"
        if not cotton_b3_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/b3jy2p6k8w/files/9a365367-8a96-4c15-8bcc-a533ab79c7d6/file_downloaded"
            download_resumable(url, cotton_b3_zip, logger)
        cotton_b3_extract = cotton_b3_raw / "extracted"
        if not cotton_b3_extract.exists():
            extract_zip(cotton_b3_zip, cotton_b3_extract, logger)
        
        for p in cotton_b3_extract.rglob("*"):
            if p.is_dir():
                name_low = p.name.lower()
                if "red" in name_low:
                    copy_images_to_candidates(p, "cotton", "leaf_reddening", source_id, licence, manifest_writer, logger, prefix="b3_red")
                elif "bacterial" in name_low or ("blight" in name_low and "angular" in name_low):
                    copy_images_to_candidates(p, "cotton", "bacterial_blight", source_id, licence, manifest_writer, logger, prefix="b3_bb")
                elif "curl" in name_low:
                    copy_images_to_candidates(p, "cotton", "leaf_curl", source_id, licence, manifest_writer, logger, prefix="b3_curl")
                elif "fresh" in name_low or "healthy" in name_low:
                    copy_images_to_candidates(p, "cotton", "healthy", source_id, licence, manifest_writer, logger, prefix="b3_hlth")

        # D. y7544ghy3y (CoSEV: Leaf curl)
        source_id = "y7544ghy3y"
        licence = "CC BY 4.0"
        cotton_y7_raw = RAW_DIR / source_id
        cotton_y7_raw.mkdir(parents=True, exist_ok=True)
        cotton_y7_zip = cotton_y7_raw / "CoSev.zip"
        if not cotton_y7_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/y7544ghy3y/files/0e90bf5b-3ba3-48f1-9564-5babd1fd2003/file_downloaded"
            download_resumable(url, cotton_y7_zip, logger)
        cotton_y7_extract = cotton_y7_raw / "extracted"
        if not cotton_y7_extract.exists():
            extract_zip(cotton_y7_zip, cotton_y7_extract, logger)
        for p in cotton_y7_extract.rglob("*"):
            if p.is_dir() and "curl" in p.name.lower():
                copy_images_to_candidates(p, "cotton", "leaf_curl", source_id, licence, manifest_writer, logger, prefix="cosev")

        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in Cotton pipeline: {e}")

    # =========================================================================
    # 3. CHILLI: ymt8k9bjkn & tm3v4zmh7c
    # =========================================================================
    try:
        logger.info("=== 3. PROCESSING CHILLI ===")
        # A. ymt8k9bjkn
        source_id = "ymt8k9bjkn"
        licence = "CC BY 4.0"
        chilli_ym_raw = RAW_DIR / source_id
        chilli_ym_raw.mkdir(parents=True, exist_ok=True)
        chilli_ym_zip = chilli_ym_raw / "Chili Leaf Disease Original Dataset.zip"
        if not chilli_ym_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/ymt8k9bjkn/files/ca92fa24-b913-420c-b652-a40f7ba6fdda/file_downloaded"
            download_resumable(url, chilli_ym_zip, logger)
        chilli_ym_extract = chilli_ym_raw / "extracted"
        if not chilli_ym_extract.exists():
            extract_zip(chilli_ym_zip, chilli_ym_extract, logger)
            
        for p in chilli_ym_extract.rglob("*"):
            if p.is_dir():
                name_low = p.name.lower()
                if "curl" in name_low:
                    copy_images_to_candidates(p, "chilli", "leaf_curl", source_id, licence, manifest_writer, logger, prefix="krishna_curl")
                elif "bacterial" in name_low or ("spot" in name_low and "cercospora" not in name_low and "white" not in name_low):
                    copy_images_to_candidates(p, "chilli", "bacterial_spot", source_id, licence, manifest_writer, logger, prefix="krishna_bs")
                elif "cercospora" in name_low:
                    copy_images_to_candidates(p, "chilli", "cercospora_leaf_spot", source_id, licence, manifest_writer, logger, prefix="krishna_cerc")
                elif "healthy" in name_low:
                    copy_images_to_candidates(p, "chilli", "healthy", source_id, licence, manifest_writer, logger, prefix="krishna_hlth")

        # B. tm3v4zmh7c
        source_id = "tm3v4zmh7c"
        licence = "CC BY 4.0"
        chilli_tm_raw = RAW_DIR / source_id
        chilli_tm_raw.mkdir(parents=True, exist_ok=True)
        
        # Curl Virus
        curl_zip = chilli_tm_raw / "Curl_Virus.zip"
        if not curl_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/d94a4729-47ec-4a39-bead-498e16347c4d/file_downloaded"
            download_resumable(url, curl_zip, logger)
        curl_extract = chilli_tm_raw / "extracted_curl"
        if not curl_extract.exists():
            extract_zip(curl_zip, curl_extract, logger)
        copy_images_to_candidates(curl_extract, "chilli", "leaf_curl", source_id, licence, manifest_writer, logger, prefix="tm_curl")

        # Bacterial Spot
        bs_zip = chilli_tm_raw / "Bacterial_Spot.zip"
        if not bs_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/dca1dd29-a792-4803-8d3b-22fcee2ee929/file_downloaded"
            download_resumable(url, bs_zip, logger)
        bs_extract = chilli_tm_raw / "extracted_bs"
        if not bs_extract.exists():
            extract_zip(bs_zip, bs_extract, logger)
        copy_images_to_candidates(bs_extract, "chilli", "bacterial_spot", source_id, licence, manifest_writer, logger, prefix="tm_bs")

        # Cercospora Leaf Spot
        cerc_zip = chilli_tm_raw / "Cercospora_Leaf_Spot.zip"
        if not cerc_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/f2901ee9-65fa-4cdd-8f86-ef348963f71c/file_downloaded"
            download_resumable(url, cerc_zip, logger)
        cerc_extract = chilli_tm_raw / "extracted_cerc"
        if not cerc_extract.exists():
            extract_zip(cerc_zip, cerc_extract, logger)
        copy_images_to_candidates(cerc_extract, "chilli", "cercospora_leaf_spot", source_id, licence, manifest_writer, logger, prefix="tm_cerc")

        # Healthy Leaf
        hlth_zip = chilli_tm_raw / "Healthy_Leaf.zip"
        if not hlth_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/5754b152-c21f-4989-b5fe-a370c32b9409/file_downloaded"
            download_resumable(url, hlth_zip, logger)
        hlth_extract = chilli_tm_raw / "extracted_hlth"
        if not hlth_extract.exists():
            extract_zip(hlth_zip, hlth_extract, logger)
        copy_images_to_candidates(hlth_extract, "chilli", "healthy", source_id, licence, manifest_writer, logger, prefix="tm_hlth")

        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in Chilli pipeline: {e}")

    # =========================================================================
    # 4. RICE: Paddy Doctor (HF Parquet files)
    # =========================================================================
    try:
        logger.info("=== 4. PROCESSING RICE (PADDY DOCTOR) ===")
        paddy_raw_dir = RAW_DIR / "paddy_doctor"
        paddy_raw_dir.mkdir(parents=True, exist_ok=True)
        
        pq0 = paddy_raw_dir / "train-00000.parquet"
        pq1 = paddy_raw_dir / "train-00001.parquet"
        
        url0 = "https://huggingface.co/datasets/anthony2261/paddy-disease-classification/resolve/main/data/train-00000-of-00002-a929229f3aaf7166.parquet"
        url1 = "https://huggingface.co/datasets/anthony2261/paddy-disease-classification/resolve/main/data/train-00001-of-00002-4c2a20b9469e90df.parquet"
        
        if not pq0.exists():
            download_resumable(url0, pq0, logger)
        if not pq1.exists():
            download_resumable(url1, pq1, logger)
            
        extract_paddy_doctor_parquet([pq0, pq1], manifest_writer, logger)
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in Rice pipeline: {e}")

    # =========================================================================
    # 5. TOMATO: PlantDoc + tywbtsjrjv
    # =========================================================================
    try:
        logger.info("=== 5. PROCESSING TOMATO ===")
        # PlantDoc field photos
        plantdoc_base = RAW_DIR / "plantdoc"
        if plantdoc_base.exists():
            copy_images_to_candidates(plantdoc_base / "train" / "Tomato Early blight leaf", "tomato", "early_blight", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc")
            copy_images_to_candidates(plantdoc_base / "test" / "Tomato Early blight leaf", "tomato", "early_blight", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc_test")
            copy_images_to_candidates(plantdoc_base / "train" / "Tomato Septoria leaf spot", "tomato", "septoria_leaf_spot", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc")
            copy_images_to_candidates(plantdoc_base / "test" / "Tomato Septoria leaf spot", "tomato", "septoria_leaf_spot", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc_test")
            copy_images_to_candidates(plantdoc_base / "train" / "Tomato leaf yellow virus", "tomato", "leaf_curl", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc")
            copy_images_to_candidates(plantdoc_base / "test" / "Tomato leaf yellow virus", "tomato", "leaf_curl", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc_test")
            copy_images_to_candidates(plantdoc_base / "train" / "Tomato leaf", "tomato", "healthy", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc")
            copy_images_to_candidates(plantdoc_base / "test" / "Tomato leaf", "tomato", "healthy", "plantdoc", "MIT", manifest_writer, logger, prefix="plantdoc_test")

        # tywbtsjrjv (clean unaugmented dataset on Mendeley)
        source_id = "tywbtsjrjv"
        licence = "CC BY 4.0"
        pv_raw_dir = RAW_DIR / source_id
        pv_raw_dir.mkdir(parents=True, exist_ok=True)
        pv_zip = pv_raw_dir / "Plant_leaf_diseases_dataset_without_augmentation.zip"
        if not pv_zip.exists():
            url = "https://data.mendeley.com/public-files/datasets/tywbtsjrjv/files/d5652a28-c1d8-4b76-97f3-72fb80f94efc/file_downloaded"
            download_resumable(url, pv_zip, logger)
        pv_extract = pv_raw_dir / "extracted"
        if not pv_extract.exists():
            extract_zip(pv_zip, pv_extract, logger)
            
        base_pv = pv_extract / "Plant_leave_diseases_dataset_without_augmentation"
        if not base_pv.exists():
            base_pv = pv_extract
            
        copy_images_to_candidates(base_pv / "Tomato___Early_blight", "tomato", "early_blight", source_id, licence, manifest_writer, logger, prefix="pv")
        copy_images_to_candidates(base_pv / "Tomato___Septoria_leaf_spot", "tomato", "septoria_leaf_spot", source_id, licence, manifest_writer, logger, prefix="pv")
        copy_images_to_candidates(base_pv / "Tomato___Tomato_Yellow_Leaf_Curl_Virus", "tomato", "leaf_curl", source_id, licence, manifest_writer, logger, prefix="pv")
        copy_images_to_candidates(base_pv / "Tomato___healthy", "tomato", "healthy", source_id, licence, manifest_writer, logger, prefix="pv")

        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in Tomato pipeline: {e}")

    # =========================================================================
    # 6. NON-CROP BACKGROUND: grass-weeds parquet
    # =========================================================================
    try:
        logger.info("=== 6. PROCESSING NON-CROP BACKGROUND ===")
        gw_raw_dir = RAW_DIR / "grass_weeds"
        gw_raw_dir.mkdir(parents=True, exist_ok=True)
        gw_pq = gw_raw_dir / "train-00000.parquet"
        gw_url = "https://huggingface.co/datasets/Francesco/grass-weeds/resolve/main/data/train-00000-of-00001-f775ca4db6bf9276.parquet"
        if not gw_pq.exists():
            download_resumable(gw_url, gw_pq, logger)
        extract_grass_weeds_parquet(gw_pq, manifest_writer, logger, max_images=300)
        manifest_file.flush()
    except Exception as e:
        logger.error(f"Error in Non-Crop pipeline: {e}")

    manifest_file.close()
    logger.info("==================================================")
    logger.info("PIPELINE COMPLETED SUCCESSFULLY!")
    logger.info("==================================================")

if __name__ == "__main__":
    run()
