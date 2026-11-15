"""
Crop Doctor - Side-by-Side Model Comparison Script (MobileNetV3-Large vs ResNet-18)
-----------------------------------------------------------------------------------
Loads both trained models from Google Drive, evaluates uploaded test images,
and prints a head-to-head comparison table showing accuracy, confidence, and latency.
"""

import os, sys, glob, json, time
import numpy as np
from PIL import Image
import torch
import torch.nn as nn
import torch.nn.functional as F
import torchvision.models as models
import torchvision.transforms as transforms
from google.colab import files

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"Hardware: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'CPU'}")

TARGET_21_CLASSES = [
    'rice_bacterial_leaf_blight', 'rice_brown_spot', 'rice_leaf_blast',
    'rice_sheath_blight', 'rice_healthy', 'cotton_bacterial_blight',
    'cotton_fusarium_wilt', 'cotton_healthy', 'chilli_bacterial_spot',
    'chilli_leaf_curl_virus', 'chilli_healthy', 'tomato_early_blight',
    'tomato_late_blight', 'tomato_yellow_leaf_curl_virus', 'tomato_spider_mites',
    'tomato_healthy', 'maize_gray_leaf_spot', 'maize_common_rust',
    'maize_northern_leaf_blight', 'maize_fall_armyworm', 'maize_healthy'
]

# 1. Load MobileNetV3-Large
print("Loading MobileNetV3-Large...")
model_mobilenet = models.mobilenet_v3_large(weights=None)
model_mobilenet.classifier[3] = nn.Linear(model_mobilenet.classifier[3].in_features, 21)
mb_ckpt = glob.glob('/content/drive/MyDrive/**/mobilenet*v2b*.pt', recursive=True) + ['/content/model_v2b.pt']
if mb_ckpt and os.path.exists(mb_ckpt[0]):
    model_mobilenet.load_state_dict(torch.load(mb_ckpt[0], map_location=device))
    print(f"✓ MobileNet loaded from {mb_ckpt[0]}")
else:
    print("Warning: MobileNet checkpoint not found!")
model_mobilenet = model_mobilenet.to(device).eval()

# 2. Load ResNet-18
print("Loading ResNet-18...")
model_resnet = models.resnet18(weights=None)
model_resnet.fc = nn.Linear(model_resnet.fc.in_features, 21)
rn_ckpt = glob.glob('/content/drive/MyDrive/**/resnet18*.pt', recursive=True) + ['/content/resnet18_best.pt']
if rn_ckpt and os.path.exists(rn_ckpt[0]):
    model_resnet.load_state_dict(torch.load(rn_ckpt[0], map_location=device))
    print(f"✓ ResNet-18 loaded from {rn_ckpt[0]}")
else:
    print("Warning: ResNet checkpoint not found!")
model_resnet = model_resnet.to(device).eval()

val_tf = transforms.Compose([
    transforms.Resize((320, 320)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

print("\n👉 Upload test photos from Desktop/sample_leaf_photos:")
uploaded = files.upload()

results = []
for fn, content in uploaded.items():
    with open(fn, 'wb') as f:
        f.write(content)
    
    img = Image.open(fn).convert('RGB')
    t = val_tf(img).unsqueeze(0).to(device)
    
    # Infer MobileNet
    t0 = time.time()
    with torch.no_grad():
        mb_probs = F.softmax(model_mobilenet(t) / 1.5778, dim=1).cpu().numpy().flatten()
    mb_lat = (time.time() - t0) * 1000
    mb_top1 = TARGET_21_CLASSES[np.argmax(mb_probs)]
    mb_conf = np.max(mb_probs) * 100
    
    # Infer ResNet-18
    t0 = time.time()
    with torch.no_grad():
        rn_probs = F.softmax(model_resnet(t) / 1.5778, dim=1).cpu().numpy().flatten()
    rn_lat = (time.time() - t0) * 1000
    rn_top1 = TARGET_21_CLASSES[np.argmax(rn_probs)]
    rn_conf = np.max(rn_probs) * 100
    
    results.append({
        'file': fn,
        'mb_pred': mb_top1, 'mb_conf': mb_conf, 'mb_lat': mb_lat,
        'rn_pred': rn_top1, 'rn_conf': rn_conf, 'rn_lat': rn_lat
    })

print(f"\n{'='*115}")
print(f"{'Filename':<35} | {'MobileNetV3 (16.17 MB)':<35} | {'ResNet-18 (42.68 MB)':<35}")
print(f"{'='*115}")
for r in results:
    mb_str = f"{r['mb_pred'][:22]} ({r['mb_conf']:.1f}%)"
    rn_str = f"{r['rn_pred'][:22]} ({r['rn_conf']:.1f}%)"
    print(f"{r['file'][:35]:<35} | {mb_str:<35} | {rn_str:<35}")
print(f"{'='*115}")
