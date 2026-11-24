#!/usr/bin/env python3
"""
Crop Doctor - Offline Voice Model Downloader (Task SETUP-SCRIPTS)
-----------------------------------------------------------------
Downloads, verifies, extracts, and installs official offline speech recognition
models (Vosk) for English, Hindi, and Telugu.

Official URLs:
  - Indian English (en): https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip
  - Hindi (hi):          https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip
  - Telugu (te):         https://alphacephei.com/vosk/models/vosk-model-small-te-0.42.zip

Target Phone Paths Read by Agri Care AI (VoiceModelManager.kt):
  1. Primary External App Storage (No root required via adb):
     /sdcard/Android/data/com.cropdoctor/files/vosk/<lang>/
  2. Internal Private App Storage (Via in-app picker or run-as):
     /data/data/com.cropdoctor/files/vosk/<lang>/
  3. Developer Fallback:
     /data/local/tmp/vosk/<lang>/
"""

import os
import sys
import shutil
import hashlib
import zipfile
import argparse
import subprocess
import urllib.request
from pathlib import Path

VOSK_MODELS = {
    "en": {
        "lang_code": "en",
        "lang_name": "Indian English",
        "model_dir_name": "vosk-model-small-en-in-0.4",
        "archive_name": "vosk-model-small-en-in-0.4.zip",
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip",
        "sha256": "20663dcac4d5cb783a579c54d98339344a688e4ec6e1b4a4b059fd1235454cc7",
        "size_bytes": 37573330,  # ~35.8 MB
    },
    "hi": {
        "lang_code": "hi",
        "lang_name": "Hindi (हिन्दी)",
        "model_dir_name": "vosk-model-small-hi-0.22",
        "archive_name": "vosk-model-small-hi-0.22.zip",
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip",
        "sha256": "7c50a10866889f0ac21d912c20537a055a597ed09fc1d3e5bcd798f9f0017e48",
        "size_bytes": 44458845,  # ~42.4 MB
    },
    "te": {
        "lang_code": "te",
        "lang_name": "Telugu (తెలుగు)",
        "model_dir_name": "vosk-model-small-te-0.42",
        "archive_name": "vosk-model-small-te-0.42.zip",
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-te-0.42.zip",
        "sha256": "3e77b05becc7599a596ccebd8b22cb95eb4eca0cfca524827cdfbfd66af09497",
        "size_bytes": 60544249,  # ~57.7 MB
    },
}

DEFAULT_PHONE_BASE = "/sdcard/Android/data/com.cropdoctor/files/vosk"


def find_adb(custom_adb: str = None) -> str:
    """Finds adb executable in custom path, PATH, or standard Android SDK directories."""
    if custom_adb and Path(custom_adb).is_file():
        return custom_adb

    which_adb = shutil.which("adb")
    if which_adb:
        return which_adb

    sdk_candidates = [
        Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk" / "platform-tools" / "adb.exe",
        Path(os.environ.get("ANDROID_HOME", "")) / "platform-tools" / "adb.exe",
        Path(os.environ.get("ANDROID_HOME", "")) / "platform-tools" / "adb",
        Path(os.environ.get("HOME", "")) / "Android" / "Sdk" / "platform-tools" / "adb",
    ]
    for candidate in sdk_candidates:
        if candidate.is_file():
            return str(candidate)

    return "adb"


def sha256_file(filepath: Path) -> str:
    """Computes SHA-256 hash of a file in 4 MB chunks."""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(4 * 1024 * 1024):
            h.update(chunk)
    return h.hexdigest()


def download_with_progress(url: str, dest_path: Path):
    """Downloads a file showing progress."""
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = dest_path.with_suffix(dest_path.suffix + ".part")

    print(f"  Downloading from: {url}")
    print(f"  Saving to:        {dest_path}")

    req = urllib.request.Request(url, headers={"User-Agent": "CropDoctorOffline-Setup/1.0"})
    with urllib.request.urlopen(req, timeout=60) as response, open(temp_path, "wb") as out_file:
        total_size = int(response.headers.get("Content-Length", 0))
        downloaded = 0
        chunk_size = 1024 * 256

        while chunk := response.read(chunk_size):
            out_file.write(chunk)
            downloaded += len(chunk)
            if total_size > 0:
                percent = downloaded * 100.0 / total_size
                mb_down = downloaded / (1024 * 1024)
                mb_tot = total_size / (1024 * 1024)
                sys.stdout.write(f"\r  Progress: {mb_down:.1f}/{mb_tot:.1f} MB ({percent:.1f}%)")
                sys.stdout.flush()

    print()
    temp_path.replace(dest_path)


def main():
    parser = argparse.ArgumentParser(
        description="Download, verify, extract, and push Vosk speech models for Crop Doctor Offline."
    )
    parser.add_argument(
        "--dest",
        type=str,
        default=r"C:\crop-data\vosk_models",
        help="Local staging directory to store zips and extracted models (default: C:\\crop-data\\vosk_models)",
    )
    parser.add_argument(
        "--lang",
        choices=["all", "en", "hi", "te"],
        default="all",
        help="Specific language to download (default: all)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Simulate the workflow: print URLs, expected hashes, target phone paths, and adb commands without executing.",
    )
    parser.add_argument(
        "--skip-push",
        action="store_true",
        help="Download, verify, and extract locally, but skip adb push to phone.",
    )
    parser.add_argument(
        "--device",
        type=str,
        default=None,
        help="Specific adb device serial (optional when multiple devices are connected).",
    )
    parser.add_argument(
        "--adb",
        type=str,
        default=None,
        help="Path to adb executable (optional, auto-detected by default).",
    )
    args = parser.parse_args()

    dest_dir = Path(args.dest)
    adb_cmd = find_adb(args.adb)
    selected_langs = ["en", "hi", "te"] if args.lang == "all" else [args.lang]

    print("=" * 76)
    print("  CROP DOCTOR: OFFLINE SPEECH RECOGNITION MODELS (VOSK)")
    print("=" * 76)
    print(f"  Mode:              {'DRY RUN (simulation only)' if args.dry_run else 'ACTIVE EXECUTION'}")
    print(f"  Local Staging Dir: {dest_dir}")
    print(f"  Selected Models:   {', '.join(selected_langs).upper()}")
    print(f"  ADB Executable:    {adb_cmd}")
    print(f"  Phone Target Base: {DEFAULT_PHONE_BASE}/<lang>/")
    print("=" * 76)

    for lang in selected_langs:
        info = VOSK_MODELS[lang]
        zip_path = dest_dir / info["archive_name"]
        extracted_dir = dest_dir / info["model_dir_name"]
        phone_lang_dir = f"{DEFAULT_PHONE_BASE}/{lang}"

        print(f"\n[{info['lang_code'].upper()}] {info['lang_name']} ({info['model_dir_name']}):")
        print(f"  Official Source:   {info['url']}")
        print(f"  Expected Size:     {info['size_bytes'] / (1024 * 1024):.2f} MB ({info['size_bytes']:,} bytes)")
        print(f"  Recorded SHA-256:  {info['sha256']}")
        print(f"  Local Zip Path:    {zip_path}")
        print(f"  Local Extract Dir: {extracted_dir}")
        print(f"  Target Phone Path: {phone_lang_dir}/")

        if args.dry_run:
            print(f"  [DRY-RUN] Would download: {info['url']} -> {zip_path.name}")
            print(f"  [DRY-RUN] Would verify SHA-256 against: {info['sha256']}")
            print(f"  [DRY-RUN] Would extract to: {extracted_dir}")
            device_flag = f"-s {args.device} " if args.device else ""
            print(f"  [DRY-RUN] Would execute ADB push:")
            print(f"      1. {adb_cmd} {device_flag}shell mkdir -p {phone_lang_dir}")
            print(f"      2. {adb_cmd} {device_flag}push \"{extracted_dir}/*\" {phone_lang_dir}/")
            continue

        # Step 1: Download if not already present
        if zip_path.is_file():
            print(f"  Zip already exists locally: {zip_path.name}")
        else:
            download_with_progress(info["url"], zip_path)

        # Step 2: Verify SHA-256
        print("  Verifying SHA-256 checksum...")
        actual_hash = sha256_file(zip_path)
        if actual_hash.lower() != info["sha256"].lower():
            print(f"  ERROR: Checksum mismatch for {zip_path.name}!")
            print(f"    Expected: {info['sha256']}")
            print(f"    Got:      {actual_hash}")
            sys.exit(1)
        print(f"  ✓ Checksum verified successfully: {actual_hash}")

        # Step 3: Extract archive
        if not extracted_dir.is_dir() or not any(extracted_dir.iterdir()):
            print(f"  Extracting {zip_path.name} to {dest_dir}...")
            with zipfile.ZipFile(zip_path, "r") as zf:
                zf.extractall(dest_dir)
            print(f"  ✓ Extracted to {extracted_dir}")
        else:
            print(f"  Extracted directory already exists: {extracted_dir}")

        # Step 4: ADB Push to device
        if args.skip_push:
            print("  Skipping ADB push (--skip-push passed).")
            continue

        print(f"  Pushing to connected Android device at {phone_lang_dir}/...")
        adb_base = [adb_cmd]
        if args.device:
            adb_base.extend(["-s", args.device])

        try:
            # Create remote target directory
            mkdir_cmd = adb_base + ["shell", f"mkdir -p {phone_lang_dir}"]
            subprocess.run(mkdir_cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

            # Push all files from extracted directory into phone_lang_dir
            for item in extracted_dir.iterdir():
                push_cmd = adb_base + ["push", str(item), f"{phone_lang_dir}/"]
                res = subprocess.run(push_cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
                print(f"    Pushed {item.name}: {res.stdout.strip() or 'OK'}")

            print(f"  ✓ Vosk model for '{lang}' successfully installed to {phone_lang_dir}/")
        except subprocess.CalledProcessError as e:
            print(f"  WARNING: ADB push failed ({e}). Ensure phone is connected via USB and debugging is authorized.")
            print(f"  Manual push command:")
            print(f"    adb shell mkdir -p {phone_lang_dir}")
            print(f"    adb push {extracted_dir}/* {phone_lang_dir}/")

    print("\n" + "=" * 76)
    print("  VOSK SETUP COMPLETED")
    print("=" * 76)
    print("  The Android app (Agri Care AI) will automatically detect installed speech")
    print("  models on next launch or on the 'Install AI Model' screen.")
    print("=" * 76)


if __name__ == "__main__":
    main()
