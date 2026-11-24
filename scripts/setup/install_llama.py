#!/usr/bin/env python3
"""
Crop Doctor - Llama 3.2 1B Advisory Model Installer (Task SETUP-SCRIPTS)
------------------------------------------------------------------------
Verifies local Llama 3.2 1B-Instruct SpinQuant INT4 weights and tokenizer,
checks the Llama 3.2 Community License acknowledgement, and pushes them
to the target Android phone directory via ADB.

Reference Files & Official Checksums:
  1. Model .pte:
     Filename:   llama-3.2-1b-spinquant.pte (or Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte)
     Size:       1,135,951,488 bytes (~1.13 GB)
     SHA-256:    8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b
  2. Tokenizer .model:
     Filename:   tokenizer.model
     Size:       2,183,982 bytes (~2.18 MB)
     SHA-256:    82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55

Target Phone Paths Read by Agri Care AI (ExecuTorchLlamaAdvisor.kt):
  - Primary: /sdcard/Android/data/com.cropdoctor/files/llama-3.2-1b-spinquant.pte
  - Primary: /sdcard/Android/data/com.cropdoctor/files/tokenizer.model
  - Fallback: /data/local/tmp/llama-3.2-1b-spinquant.pte
  - Fallback: /data/local/tmp/tokenizer.model
"""

import os
import sys
import shutil
import hashlib
import argparse
import subprocess
from pathlib import Path

REF_MODEL_SHA256 = "8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b"
REF_MODEL_SIZE = 1135951488  # ~1.13 GB
REF_TOKENIZER_SHA256 = "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55"
REF_TOKENIZER_SIZE = 2183982  # ~2.18 MB

DEFAULT_REMOTE_DIR = "/sdcard/Android/data/com.cropdoctor/files"
REMOTE_MODEL_NAME = "llama-3.2-1b-spinquant.pte"
REMOTE_TOKENIZER_NAME = "tokenizer.model"


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
    """Computes SHA-256 hash in 4 MB chunks."""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(4 * 1024 * 1024):
            h.update(chunk)
    return h.hexdigest()


def discover_candidate_files():
    """Searches common local directories for model .pte and tokenizer.model."""
    search_dirs = [
        Path.cwd(),
        Path(r"C:\crop-data\llama"),
        Path(r"C:\crop-data"),
        Path.home() / "Downloads",
    ]
    model_found = None
    tok_found = None

    for d in search_dirs:
        if not d.is_dir():
            continue
        if not model_found:
            for f in d.glob("*.pte"):
                if "llama" in f.name.lower() or "1b" in f.name.lower() or "spinquant" in f.name.lower():
                    if f.stat().st_size > 100_000_000:
                        model_found = f
                        break
        if not tok_found:
            candidate_tok = d / "tokenizer.model"
            if candidate_tok.is_file() and candidate_tok.stat().st_size > 50_000:
                tok_found = candidate_tok

    return model_found, tok_found


def main():
    parser = argparse.ArgumentParser(
        description="Verify and install Llama 3.2 1B-Instruct model & tokenizer on Android for Agri Care AI."
    )
    parser.add_argument(
        "--model",
        type=str,
        default=None,
        help="Path to Llama-3.2-1B-Instruct SpinQuant INT4 .pte model file.",
    )
    parser.add_argument(
        "--tokenizer",
        type=str,
        default=None,
        help="Path to tokenizer.model file.",
    )
    parser.add_argument(
        "--accept-license",
        action="store_true",
        help="Confirm acceptance of the Meta Llama 3.2 Community License Agreement.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Simulate the workflow: verify files and print adb commands without executing phone file transfers.",
    )
    parser.add_argument(
        "--skip-push",
        action="store_true",
        help="Verify local file integrity and hashes, but skip pushing to Android device.",
    )
    parser.add_argument(
        "--device",
        type=str,
        default=None,
        help="Target specific adb device serial when multiple devices are connected.",
    )
    parser.add_argument(
        "--adb",
        type=str,
        default=None,
        help="Path to adb executable (auto-detected if omitted).",
    )
    args = parser.parse_args()

    print("=" * 76)
    print("  CROP DOCTOR: LLAMA 3.2 1B OFFLINE ADVISORY INSTALLER")
    print("=" * 76)
    print(f"  Execution Mode: {'DRY RUN (simulation only)' if args.dry_run else 'ACTIVE INSTALLATION'}")

    # Step 1: License Acknowledgement Check
    print("\n[Step 1/4] Checking Meta Llama 3.2 Community License...")
    if not args.accept_license:
        print("\n  IMPORTANT: Meta Llama 3.2 models are subject to the Llama 3.2 Community License.")
        print("  You must have accepted the license terms at https://llama.meta.com/")
        print("  or https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct.")
        print()
        if sys.stdin.isatty():
            choice = input("  Have you reviewed and accepted the Llama 3.2 Community License? [y/N]: ").strip().lower()
            if choice not in ["y", "yes"]:
                print("  Aborting: License acceptance is required to use Llama 3.2 weights.")
                sys.exit(1)
        else:
            print("  ERROR: License not confirmed. Pass --accept-license to proceed.")
            sys.exit(1)
    print("  ✓ License acknowledgement confirmed.")

    # Step 2: Locate Files
    print("\n[Step 2/4] Locating model .pte and tokenizer.model...")
    disc_model, disc_tok = discover_candidate_files()

    model_path = Path(args.model) if args.model else disc_model
    tokenizer_path = Path(args.tokenizer) if args.tokenizer else disc_tok

    if not model_path or not model_path.is_file():
        print(f"  ERROR: Model file not found! ({model_path})")
        print("  Provide path with --model <path/to/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte>")
        print("  See scripts/setup/install_llama.md for download instructions.")
        sys.exit(1)

    if not tokenizer_path or not tokenizer_path.is_file():
        print(f"  ERROR: Tokenizer file not found! ({tokenizer_path})")
        print("  Provide path with --tokenizer <path/to/tokenizer.model>")
        print("  See scripts/setup/install_llama.md for download instructions.")
        sys.exit(1)

    print(f"  Model File:     {model_path} ({model_path.stat().st_size / (1024*1024):.1f} MB)")
    print(f"  Tokenizer File: {tokenizer_path} ({tokenizer_path.stat().st_size / 1024:.1f} KB)")

    # Step 3: Checksum & Size Verification
    print("\n[Step 3/4] Verifying File Checksums (SHA-256)...")

    # Verify model
    print(f"  Computing SHA-256 for model: {model_path.name}...")
    actual_model_hash = sha256_file(model_path)
    model_size = model_path.stat().st_size
    print(f"    Computed SHA-256: {actual_model_hash}")
    print(f"    Reference SHA-256: {REF_MODEL_SHA256}")
    if actual_model_hash.lower() == REF_MODEL_SHA256.lower():
        print("    ✓ Checksum EXACT MATCH with official ExecuTorch SpinQuant INT4 reference build.")
    else:
        if model_size < 100_000_000:
            print("    ERROR: File size is under 100 MB. This is not a valid 1B model.")
            sys.exit(1)
        print("    NOTICE: Checksum differs from default reference build, but file size is valid.")
        print("    (This is expected if you exported custom SpinQuant or fp16 weights.)")

    # Verify tokenizer
    print(f"  Computing SHA-256 for tokenizer: {tokenizer_path.name}...")
    actual_tok_hash = sha256_file(tokenizer_path)
    tok_size = tokenizer_path.stat().st_size
    print(f"    Computed SHA-256: {actual_tok_hash}")
    print(f"    Reference SHA-256: {REF_TOKENIZER_SHA256}")
    if actual_tok_hash.lower() == REF_TOKENIZER_SHA256.lower():
        print("    ✓ Checksum EXACT MATCH with official Meta Llama tokenizer.")
    else:
        if tok_size < 50_000:
            print("    ERROR: Tokenizer file size is under 50 KB. File appears corrupted.")
            sys.exit(1)
        print("    NOTICE: Checksum differs from default reference tokenizer, but size is valid.")

    # Step 4: ADB Push to Target Phone
    print("\n[Step 4/4] Installation to Target Phone...")
    remote_model_path = f"{DEFAULT_REMOTE_DIR}/{REMOTE_MODEL_NAME}"
    remote_tok_path = f"{DEFAULT_REMOTE_DIR}/{REMOTE_TOKENIZER_NAME}"
    adb_cmd = find_adb(args.adb)

    print(f"  Target Model Destination:     {remote_model_path}")
    print(f"  Target Tokenizer Destination: {remote_tok_path}")
    print(f"  ADB Executable:               {adb_cmd}")

    if args.dry_run:
        device_flag = f"-s {args.device} " if args.device else ""
        print("\n  [DRY-RUN] Simulated ADB commands:")
        print(f"    1. {adb_cmd} {device_flag}shell mkdir -p {DEFAULT_REMOTE_DIR}")
        print(f"    2. {adb_cmd} {device_flag}push \"{model_path}\" {remote_model_path}")
        print(f"    3. {adb_cmd} {device_flag}push \"{tokenizer_path}\" {remote_tok_path}")
        print("\n  [DRY-RUN] Verification step:")
        print(f"    {adb_cmd} {device_flag}shell ls -lh {DEFAULT_REMOTE_DIR}")
        print("\n  ✓ Dry-run completed successfully.")
        return

    if args.skip_push:
        print("  Skipping ADB push (--skip-push passed).")
        return

    adb_base = [adb_cmd]
    if args.device:
        adb_base.extend(["-s", args.device])

    try:
        print("  Creating target directory on device...")
        subprocess.run(adb_base + ["shell", f"mkdir -p {DEFAULT_REMOTE_DIR}"], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        print(f"  Pushing model weights ({model_size / (1024*1024):.1f} MB)... (this takes ~15-30s over USB)")
        res1 = subprocess.run(adb_base + ["push", str(model_path), remote_model_path], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        print(f"    ✓ Model pushed: {res1.stdout.strip()}")

        print(f"  Pushing tokenizer ({tok_size / 1024:.1f} KB)...")
        res2 = subprocess.run(adb_base + ["push", str(tokenizer_path), remote_tok_path], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        print(f"    ✓ Tokenizer pushed: {res2.stdout.strip()}")

        print("\n" + "=" * 76)
        print("  ✓ INSTALLATION SUCCESSFUL!")
        print("=" * 76)
        print("  Next Steps:")
        print("  1. Launch 'Agri Care AI' on the phone.")
        print("  2. Open More Options (☰) -> 'Install AI Model (.pte)' to confirm green status.")
        print("  3. Take any crop photo: Llama 3.2 1B will stream on-device AI explanations.")
        print("=" * 76)
    except subprocess.CalledProcessError as e:
        print(f"  WARNING: ADB push failed ({e}). Check USB connection and debugging authorizations.")
        print("  Manual push commands:")
        print(f"    adb push {model_path} {remote_model_path}")
        print(f"    adb push {tokenizer_path} {remote_tok_path}")


if __name__ == "__main__":
    main()
