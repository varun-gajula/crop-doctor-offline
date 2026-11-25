#!/usr/bin/env python3
"""
Crop Doctor - Kaggle Output Watcher & Automatic Downloader
---------------------------------------------------------
Polls Kaggle API for your running notebook and automatically downloads
the final deliverables zip (crop_doctor_final_15class_20ep.zip) directly
to your disk as soon as training finishes, with zero user intervention.

Usage:
  python scripts/kaggle_watch_and_download.py <kaggle_username>/<notebook_slug>
Example:
  python scripts/kaggle_watch_and_download.py varun-gajula/crop-doctor-15class-retrain
"""

import os
import sys
import time
import shutil
from pathlib import Path

def main():
    if len(sys.argv) < 2:
        print("Usage: python scripts/kaggle_watch_and_download.py <username>/<kernel_slug>")
        print("Example: python scripts/kaggle_watch_and_download.py varun-gajula/crop-doctor-15class-retrain")
        sys.exit(1)

    kernel_id = sys.argv[1]
    downloads_dir = Path(r"C:\Users\<path>\Downloads")
    benchmarks_dir = Path("benchmarks")

    kaggle_json = Path.home() / ".kaggle" / "kaggle.json"
    if not kaggle_json.exists():
        print("!" * 80)
        print("KAGGLE API TOKEN REQUIRED FOR HEADLESS AUTO-DOWNLOAD:")
        print("1. In Kaggle, go to Account Settings -> API -> Create New Token.")
        print(f"2. Place the downloaded 'kaggle.json' file into: {kaggle_json.parent}")
        print("!" * 80)
        sys.exit(1)

    print("=" * 80)
    print(f"WATCHING KAGGLE KERNEL: {kernel_id}")
    print(f"Target Download Directory: {downloads_dir}")
    print("=" * 80)

    try:
        import kaggle
    except ImportError:
        print("Installing kaggle CLI...")
        os.system("pip install -q kaggle")
        import kaggle

    while True:
        status_res = kaggle.api.kernel_status(kernel_id)
        status = status_res.get("status", "unknown")
        print(f"[{time.strftime('%H:%M:%S')}] Kernel Status: {status}")

        if status == "complete":
            print("\n🎉 Kernel completed successfully! Downloading final deliverables...")
            kaggle.api.kernel_output(kernel_id, path=str(downloads_dir))
            
            # Check for zip
            zips = list(downloads_dir.glob("crop_doctor_final_15class_*.zip"))
            if zips:
                latest_zip = max(zips, key=lambda p: p.stat().st_mtime)
                print(f"✓ Successfully downloaded: {latest_zip} ({latest_zip.stat().st_size / (1024*1024):.2f} MB)")
                
                # Copy to benchmarks for tracking
                dest_bm = benchmarks_dir / latest_zip.name
                shutil.copy2(latest_zip, dest_bm)
                print(f"✓ Copied to project benchmarks: {dest_bm}")
            else:
                print(f"✓ Outputs downloaded to {downloads_dir}")
            break
        elif status in ["error", "cancelAck", "canceled"]:
            print(f"\n❌ Kernel ended with status '{status}'!")
            break
        else:
            time.sleep(60)

if __name__ == "__main__":
    main()
