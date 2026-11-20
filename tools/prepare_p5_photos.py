"""
Prepare and push the 55 test photos for Task P5 to the phone.
Clean filenames without spaces to avoid adb shell argument splitting.
"""

import os
import shutil
import subprocess
from pathlib import Path

LOCALAPPDATA = os.environ.get("LOCALAPPDATA", r"C:\Users\ec026\AppData\Local")
ADB = os.path.join(LOCALAPPDATA, r"Android\Sdk\platform-tools\adb.exe")
DEVICE = "sseqjnucrkeeprtw"
REALTEST_DIR = Path(r"C:\crop-data\realtest")
STAGING_DIR = Path(r"benchmarks\p5_staging")

def run_adb(args):
    cmd = [ADB, "-s", DEVICE] + args
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return res.stdout.strip()

def main():
    if STAGING_DIR.exists():
        shutil.rmtree(STAGING_DIR)
    STAGING_DIR.mkdir(parents=True, exist_ok=True)

    manifest = []

    # 1. Five crops (5 photos each)
    crops_config = {
        "rice": ("supported", lambda f: "rice" in f.lower()),
        "cotton": ("supported", lambda f: "cotton" in f.lower()),
        "chilli": ("supported", lambda f: "chilli" in f.lower()),
        "tomato": ("supported", lambda f: "tomato" in f.lower() and "early_blight" not in f.lower()),
        "maize": ("supported", lambda f: "maize" in f.lower() or "corn" in f.lower())
    }

    for crop, (folder, filter_fn) in crops_config.items():
        src_folder = REALTEST_DIR / folder
        files = [f for f in sorted(os.listdir(src_folder)) if filter_fn(f)][:5]
        assert len(files) == 5, f"Expected 5 files for {crop}, found {len(files)}"
        for idx, f in enumerate(files):
            staging_name = f"real_{crop}_{idx+1:02d}.jpg"
            shutil.copy2(src_folder / f, STAGING_DIR / staging_name)
            manifest.append({
                "category": f"real_{crop}",
                "crop_hint": crop,
                "orig_file": f,
                "staged_file": staging_name,
                "expected_type": "supported"
            })

    # 2. 10 nocrop photos
    nocrop_folder = REALTEST_DIR / "nocrop"
    nocrop_files = sorted(os.listdir(nocrop_folder))[:10]
    assert len(nocrop_files) == 10, f"Expected 10 nocrop files, found {len(nocrop_files)}"
    for idx, f in enumerate(nocrop_files):
        staging_name = f"nocrop_{idx+1:02d}.jpg"
        shutil.copy2(nocrop_folder / f, STAGING_DIR / staging_name)
        manifest.append({
            "category": "nocrop",
            "crop_hint": "auto",
            "orig_file": f,
            "staged_file": staging_name,
            "expected_type": "nocrop"
        })

    # 3. 10 othercrop photos
    othercrop_folder = REALTEST_DIR / "othercrop"
    othercrop_files = sorted(os.listdir(othercrop_folder))[:10]
    assert len(othercrop_files) == 10, f"Expected 10 othercrop files, found {len(othercrop_files)}"
    test_crops = ["rice", "cotton", "chilli", "tomato", "maize"] * 2
    for idx, f in enumerate(othercrop_files):
        staging_name = f"othercrop_{idx+1:02d}.jpg"
        shutil.copy2(othercrop_folder / f, STAGING_DIR / staging_name)
        manifest.append({
            "category": "othercrop",
            "crop_hint": test_crops[idx],
            "orig_file": f,
            "staged_file": staging_name,
            "expected_type": "othercrop"
        })

    # 4. 10 screen photos
    screen_folder = REALTEST_DIR / "screen"
    screen_files = sorted(os.listdir(screen_folder))[:10]
    assert len(screen_files) == 10, f"Expected 10 screen files, found {len(screen_files)}"
    for idx, f in enumerate(screen_files):
        staging_name = f"screen_{idx+1:02d}.jpg"
        shutil.copy2(screen_folder / f, STAGING_DIR / staging_name)
        manifest.append({
            "category": "screen",
            "crop_hint": "auto",
            "orig_file": f,
            "staged_file": staging_name,
            "expected_type": "screen"
        })

    print(f"Staged {len(manifest)} photos into {STAGING_DIR}")

    # Push to phone
    print("Pushing files to phone /data/local/tmp/p5_staging/...")
    run_adb(["shell", "rm", "-rf", "/data/local/tmp/p5_staging"])
    run_adb(["shell", "mkdir", "-p", "/data/local/tmp/p5_staging"])
    res = subprocess.run([ADB, "-s", DEVICE, "push", str(STAGING_DIR) + "/.", "/data/local/tmp/p5_staging/"],
                         capture_output=True, text=True)
    print(res.stdout.strip())

    # Copy into /data/data/com.cropdoctor/files/p5_photos/ using run-as
    print("Copying to app files directory /data/data/com.cropdoctor/files/p5_photos/...")
    run_adb(["shell", "run-as", "com.cropdoctor", "rm", "-rf", "/data/data/com.cropdoctor/files/p5_photos"])
    run_adb(["shell", "run-as", "com.cropdoctor", "mkdir", "-p", "/data/data/com.cropdoctor/files/p5_photos"])
    run_adb(["shell", "run-as", "com.cropdoctor", "cp", "-r", "/data/local/tmp/p5_staging/.", "/data/data/com.cropdoctor/files/p5_photos/"])
    run_adb(["shell", "run-as", "com.cropdoctor", "chmod", "-R", "777", "/data/data/com.cropdoctor/files/p5_photos"])

    out = run_adb(["shell", "run-as", "com.cropdoctor", "ls", "/data/data/com.cropdoctor/files/p5_photos"])
    count = len([x for x in out.splitlines() if x.strip().endswith(".jpg")])
    print(f"Successfully copied {count} photos into /data/data/com.cropdoctor/files/p5_photos/")
    assert count == 55, f"Expected 55 photos on phone, found {count}"

    import json
    with open("benchmarks/p5_manifest.json", "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
    print("Saved benchmarks/p5_manifest.json")

if __name__ == "__main__":
    main()
