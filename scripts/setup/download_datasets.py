#!/usr/bin/env python3
"""
Crop Doctor - Dataset Ingestion & Downloader (Task SETUP-SCRIPTS)
----------------------------------------------------------------
Automates and guides the acquisition of training and evaluation datasets
defined in `datasets_manifest.json`.

Features:
  1. Automated Kaggle Ingestion: Uses user's own Kaggle API credentials
     from environment (`KAGGLE_USERNAME`, `KAGGLE_KEY`) or `~/.kaggle/kaggle.json`.
  2. Automated Git Ingestion: Clones open-access repositories (PlantDoc, DeepWeeds).
  3. Guided Mendeley Ingestion: Provides step-by-step browser instructions for
     Mendeley CC BY 4.0 datasets protected by Cloudflare bot-checks.
  4. Non-Commercial License Auditing: Explicitly warns about CC BY-NC 3.0 (PlantVillage).
  5. Preparation Pipeline Guidance: Explains and coordinates:
     - Provenance source tagging (`src-<Source>_...`)
     - Uniform 512x512 resizing & letterbox trimming
     - Dihedral perceptual hashing (pHash/dHash)
     - Single-linkage clustering (Hamming <= 8 default, <= 10 tomato)
     - Cross-class collision filtering (Hamming <= 2)
     - Capping at 800 photos per class source pool
"""

import os
import sys
import json
import shutil
import argparse
import subprocess
from pathlib import Path

MANIFEST_DEFAULT_PATH = Path(__file__).parent / "datasets_manifest.json"
DEFAULT_RAW_DIR = Path(r"C:\crop-data\candidates\raw")
DEFAULT_TARGET_15 = Path(r"C:\crop-data\crop_doctor_15class")


def has_kaggle_credentials() -> bool:
    """Checks if Kaggle API credentials exist in environment or ~/.kaggle/kaggle.json."""
    if os.environ.get("KAGGLE_USERNAME") and os.environ.get("KAGGLE_KEY"):
        return True
    kaggle_json = Path.home() / ".kaggle" / "kaggle.json"
    if kaggle_json.is_file() and kaggle_json.stat().st_size > 10:
        return True
    return False


def check_dataset_present(item: dict, base_dir: Path) -> bool:
    """Checks if a dataset's archive file or extracted directory already exists on disk."""
    candidates = []
    if item.get("target_file"):
        candidates.append(base_dir / item["target_file"])
        candidates.append(base_dir.parent / item["target_file"])
        candidates.append(Path(r"C:\crop-data\candidates\raw") / item["target_file"])
        candidates.append(Path.home() / "Downloads" / item["target_file"])

    if item.get("target_dir"):
        candidates.append(base_dir / item["target_dir"])
        candidates.append(base_dir / item["id"])
        candidates.append(Path(r"C:\crop-data\candidates\raw") / item["id"])

    for c in candidates:
        if c.exists() and (c.is_file() or (c.is_dir() and any(c.iterdir()))):
            return True
    return False


def print_preparation_pipeline_steps():
    """Prints the rigorous preparation pipeline steps enforced across training."""
    print("\n" + "=" * 78)
    print("  CROP DOCTOR: 15-CLASS TRAINING PREPARATION PIPELINE SPECIFICATION")
    print("=" * 78)
    print("  When all raw archives are placed under C:\\crop-data\\candidates\\raw\\,")
    print("  the training dataset is assembled using the following audited steps:")
    print()
    print("  1. Provenance Source Tagging (Rule H11 & Data Leakage Prevention):")
    print("     Every photo is renamed with its origin tag prefix to prevent cross-split leakage:")
    print("     - PaddyDoctor:  'src-PaddyDoctor_<filename>.jpg' (Rice blast, healthy, stem borer, hispa)")
    print("     - AgML Rice:    'src-AgML_<filename>.jpg' (Rice blast, healthy held-out)")
    print("     - Mendeley 74j: 'src-Mendeley74j_<filename>.jpg' (Cotton bacterial blight, healthy)")
    print("     - Mendeley T9h: 'src-MendeleyT9h_<filename>.jpg' (Cotton bacterial blight, healthy)")
    print("     - Mendeley B3j: 'src-MendeleyB3j_<filename>.jpg' (Cotton bacterial blight, healthy)")
    print("     - MLD Maize:    'src-MLD_<filename>.jpg' (Maize leaf blight, healthy, fall armyworm)")
    print("     - CCMT Maize:   'src-CCMT_<filename>.jpg' (Maize fall armyworm, blight, healthy)")
    print("     - PlantDoc:     'src-PlantDoc_<filename>.jpg' (Maize leaf blight held-out)")
    print("     - Krishna:      'src-Krishna_<filename>.jpg' (Chilli leaf curl virus, healthy)")
    print("     - Archive2:     'src-Archive2_<filename>.jpg' (Tomato septoria leaf spot, healthy)")
    print("     - AgriData:     'src-AgriData_<filename>.jpg' (other_not_a_crop / weeds)")
    print("     - Rust:         'src-Rust_<filename>.jpg' (Maize common rust)")
    print()
    print("  2. Resizing & Letterbox Bar Trimming:")
    print("     - Raw camera images are inspected for uniform single-color letterbox bars.")
    print("     - Bars are cropped before lanczos resizing to uniform 512x512 with centered padding.")
    print()
    print("  3. Perceptual Hashing & Single-Linkage Clustering:")
    print("     - Dihedral 64-bit dHash computed across 8 rotations and reflections.")
    print("     - Within-class clustering eliminates duplicate shots of the same leaf:")
    print("         * Default classes: Hamming distance <= 8")
    print("         * Tomato classes:  Hamming distance <= 10")
    print("     - Cross-class collision check eliminates label contamination:")
    print("         * Conflicting pairs across different classes: Hamming distance <= 2")
    print()
    print("  4. Capping & Class Balancing:")
    print("     - Capped at maximum 800 photos per class source pool (or balanced 50/50 splits).")
    print()
    print("  5. Execution Script:")
    print("     Run: python scripts/prepare_dataset_v2.py")
    print("     Output: C:\\crop-data\\crop_doctor_15_v2.zip (or C:\\crop-data\\crop_doctor_15class.zip)")
    print("=" * 78)


def main():
    parser = argparse.ArgumentParser(
        description="Download and guide ingestion of training datasets for Crop Doctor Offline."
    )
    parser.add_argument(
        "--manifest",
        type=str,
        default=str(MANIFEST_DEFAULT_PATH),
        help="Path to datasets_manifest.json",
    )
    parser.add_argument(
        "--raw-dir",
        type=str,
        default=str(DEFAULT_RAW_DIR),
        help="Target base directory for raw downloaded datasets (default: C:\\crop-data\\candidates\\raw)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Audit manifest, display download plans and instructions, without writing files or calling APIs.",
    )
    parser.add_argument(
        "--prepare-info",
        action="store_true",
        help="Display the exact 15-class training dataset preparation specification and clustering rules.",
    )
    parser.add_argument(
        "--download-all-available",
        action="store_true",
        help="Execute automated downloads for permissible sources (Kaggle if credentials exist, Git clones).",
    )
    args = parser.parse_args()

    manifest_path = Path(args.manifest)
    if not manifest_path.is_file():
        print(f"ERROR: Datasets manifest not found at {manifest_path}!")
        sys.exit(1)

    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)

    raw_dir = Path(args.raw_dir)
    has_kaggle = has_kaggle_credentials()

    print("=" * 78)
    print("  CROP DOCTOR: DATASET INGESTION & AUDIT SPECIFICATION")
    print("=" * 78)
    print(f"  Execution Mode:         {'DRY RUN (simulation only)' if args.dry_run else 'ACTIVE INGESTION'}")
    print(f"  Manifest File:          {manifest_path}")
    print(f"  Raw Datasets Directory: {raw_dir}")
    print(f"  Kaggle API Credentials: {'DETECTED (Environment / ~/.kaggle/kaggle.json)' if has_kaggle else 'NOT DETECTED (Manual instructions will be provided)'}")
    print(f"  Total Manifest Sources: {len(manifest['datasets'])}")
    print("=" * 78)

    kaggle_sources = []
    mendeley_sources = []
    git_sources = []
    hf_sources = []

    for d in manifest["datasets"]:
        method = d.get("download_method")
        is_present = check_dataset_present(d, raw_dir)
        d["_is_present"] = is_present

        if "kaggle" in method:
            kaggle_sources.append(d)
        elif "mendeley" in method:
            mendeley_sources.append(d)
        elif "git" in method:
            git_sources.append(d)
        elif "huggingface" in method:
            hf_sources.append(d)

    # 1. Summary table
    print("\n--- DATASETS AUDIT & AVAILABILITY STATUS ---")
    for idx, d in enumerate(manifest["datasets"], 1):
        status_symbol = "✓ PRESENT" if d["_is_present"] else "MISSING / PENDING"
        licence_str = d["licence"]
        if not d.get("is_commercial_allowed", True):
            licence_str += " [CC BY-NC RESTRICTED]"
        print(f"{idx:2d}. [{status_symbol:17s}] {d['name'][:42]:42s} | {licence_str}")
        print(f"    Classes Fed: {', '.join(d['classes_it_feeds'])}")
        print(f"    Expected Photos: ~{d['expected_photo_count']:,} | Tag: {d.get('source_tag', 'N/A')}")

    # 2. Kaggle Guidance
    print("\n" + "=" * 78)
    print("  KAGGLE SOURCES INGESTION")
    print("=" * 78)
    if has_kaggle:
        print("  ✓ Kaggle API credentials detected.")
        for d in kaggle_sources:
            print(f"\n  Dataset: {d['name']}")
            print(f"  Licence: {d['licence']}")
            print(f"  Command: {d.get('download_command')}")
            if d["_is_present"]:
                print(f"  Status:  Already present on disk ({d.get('target_file')})")
            else:
                if args.dry_run or not args.download_all_available:
                    print(f"  [DRY-RUN] Would run: {d.get('download_command')}")
                else:
                    print(f"  Executing: {d.get('download_command')}...")
                    raw_dir.mkdir(parents=True, exist_ok=True)
                    subprocess.run(d.get("download_command").split(), cwd=raw_dir, check=False)
    else:
        print("  Kaggle API credentials not configured in environment or ~/.kaggle/kaggle.json.")
        print("  To download Kaggle sources automatically:")
        print("    1. Create API token at https://www.kaggle.com/settings -> 'Create New Token'")
        print("    2. Save kaggle.json to ~/.kaggle/kaggle.json (or set KAGGLE_USERNAME & KAGGLE_KEY)")
        print("  Or run manual download commands:")
        for d in kaggle_sources:
            print(f"    - {d.get('download_command')}")
            print(f"      Save archive to: {raw_dir / d.get('target_file')}")

    # 3. Mendeley Guidance
    print("\n" + "=" * 78)
    print("  MENDELEY CC BY 4.0 SOURCES (MANUAL BROWSER INGESTION)")
    print("=" * 78)
    print("  Mendeley Data bulk archives are protected by Cloudflare bot-verification.")
    print("  Reviewers must perform one-click browser downloads for each repository below:")
    for idx, d in enumerate(mendeley_sources, 1):
        status_text = "✓ ALREADY DOWNLOADED" if d["_is_present"] else "ACTION REQUIRED"
        print(f"\n  {idx}. {d['name']} [{status_text}]")
        print(f"     Licence:         {d['licence']} (Attribution 4.0 International)")
        print(f"     DOI / Web URL:   {d.get('web_url') or d.get('source_url_or_doi')}")
        print(f"     Classes Fed:     {', '.join(d['classes_it_feeds'])}")
        print(f"     Browser Action:  Open URL -> Click 'Download All' (Zip)")
        print(f"     Target Location: {raw_dir / d.get('target_file')}")

    # 4. Git / Open-Access Clones
    print("\n" + "=" * 78)
    print("  OPEN REPOSITORIES (GIT CLONE / HUGGING FACE)")
    print("=" * 78)
    for d in git_sources + hf_sources:
        status_text = "✓ PRESENT" if d["_is_present"] else "AVAILABLE FOR CLONE"
        lic_notice = f" [WARNING: {d['non_commercial_notice']}]" if not d.get("is_commercial_allowed", True) else ""
        print(f"\n  Dataset:        {d['name']} [{status_text}]")
        print(f"  Licence:        {d['licence']}{lic_notice}")
        print(f"  Source URL:     {d.get('source_url_or_doi')}")
        if d.get("git_url"):
            print(f"  Git Clone:      git clone --depth 1 {d['git_url']} {raw_dir / d.get('target_dir')}")
            if not d["_is_present"] and args.download_all_available and not args.dry_run:
                target_dest = raw_dir / d.get("target_dir")
                print(f"  Cloning {d['name']} into {target_dest}...")
                subprocess.run(["git", "clone", "--depth", "1", d["git_url"], str(target_dest)], check=False)

    # 5. Training Folder Assembly Steps
    print_preparation_pipeline_steps()


if __name__ == "__main__":
    main()
