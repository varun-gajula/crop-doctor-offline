import zipfile
from pathlib import Path

PADDY_ZIP = Path(r"C:\crop-data\candidates\raw\paddy-disease-classification.zip")
CROP_512 = Path(r"C:\crop-data\crop_doctor_512")
MLD_ROOT = Path(r"C:\crop-data\candidates\raw\3myfctrgk3\extracted\Maize dataset")
CCMT_ROOT = Path(r"C:\Users\<path>\Downloads\Dataset for Crop Pest and Disease Detection\Dataset for Crop Pest and Disease Detection\Raw Data\CCMT Dataset\Maize")
PLANTDOC = Path(r"C:\crop-data\candidates\raw\plantdoc\train\Corn leaf blight")
M74 = Path(r"C:\crop-data\candidates\raw\74jsdxtmx2\extracted\Cotton-Original")
MT9 = Path(r"C:\crop-data\candidates\raw\t9hgvk2h9p\extracted\Cotton_Original_Dataset")
MB3 = Path(r"C:\crop-data\candidates\raw\b3jy2p6k8w\extracted\Original Dataset")

print("--- PADDY ZIP ---")
if PADDY_ZIP.exists():
    with zipfile.ZipFile(PADDY_ZIP) as z:
        for prefix in ["train_images/blast/", "train_images/normal/", "train_images/dead_heart/", "train_images/hispa/"]:
            files = [m for m in z.namelist() if m.startswith(prefix) and m.lower().endswith((".jpg", ".jpeg", ".png"))]
            print(f"{prefix}: {len(files)}")

print("--- MLD MAIZE ---")
if MLD_ROOT.exists():
    for p in MLD_ROOT.iterdir():
        if p.is_dir():
            print(f"{p.name}: {len(list(p.glob('*')))}")

print("--- CCMT MAIZE ---")
if CCMT_ROOT.exists():
    for p in CCMT_ROOT.iterdir():
        if p.is_dir():
            print(f"{p.name}: {len(list(p.glob('*')))}")

print("--- PLANTDOC ---")
if PLANTDOC.exists():
    print(f"Corn leaf blight: {len(list(PLANTDOC.glob('*')))}")

print("--- MENDELEY COTTON ---")
if M74.exists():
    print("M74 bacterial blight:", len(list((M74 / "bacterial blight").glob("*"))))
    print("M74 healthy:", len(list((M74 / "healthy").glob("*"))))
if MT9.exists():
    print("MT9 bacterial blight:", len(list((MT9 / "Bacterial Blight").glob("*"))))
    print("MT9 healthy:", len(list((MT9 / "Healthy Leaf").glob("*"))))
if MB3.exists():
    print("MB3 bacterial blight:", len(list((MB3 / "Bacterial Blight").glob("*"))))
    print("MB3 healthy:", len(list((MB3 / "Healthy Leaf").glob("*"))))

print("--- CROP_512 ---")
if CROP_512.exists():
    for p in sorted(CROP_512.iterdir()):
        if p.is_dir():
            print(f"{p.name}: {len(list(p.glob('*')))}")
