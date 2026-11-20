import os
import glob
import shutil
import random
from pathlib import Path
from PIL import Image
import numpy as np

random.seed(42)

gallery_classes = [
    'rice_bacterial_leaf_blight', 'rice_leaf_blast', 'rice_sheath_blight', 'rice_healthy',
    'cotton_bacterial_blight', 'cotton_fusarium_wilt', 'cotton_leaf_curl_virus', 'cotton_healthy',
    'chilli_leaf_curl_virus', 'chilli_nutrition_deficiency', 'chilli_healthy', 'chilli_leaf_curl_virus',
    'tomato_yellow_leaf_curl_virus', 'tomato_septoria_leaf_spot', 'tomato_healthy', 'tomato_yellow_leaf_curl_virus',
    'maize_common_rust', 'maize_gray_leaf_spot', 'maize_northern_leaf_blight', 'maize_healthy'
]

# 1. Gallery Set (20 images from C:/crop-data/crop_doctor_512)
gallery_dir = Path('C:/crop-data/realtest/gallery')
gallery_dir.mkdir(parents=True, exist_ok=True)
for i, cls in enumerate(gallery_classes):
    cls_dir = Path('C:/crop-data/crop_doctor_512') / cls
    imgs = sorted(list(cls_dir.glob('*.jpg')) + list(cls_dir.glob('*.JPG')))
    if imgs:
        src = imgs[i % len(imgs)]
        dst = gallery_dir / f'gallery_{i+1:02d}_{cls}_{src.name}'
        shutil.copy2(src, dst)
g_count = len(list(gallery_dir.glob('*.jpg')))
print(f'Gallery images copied: {g_count}')

# 2. Camera Real Leaves (20 images from PlantDoc / real field captures)
real_leaves_dir = Path('C:/crop-data/realtest/real_leaves')
real_leaves_dir.mkdir(parents=True, exist_ok=True)
plantdoc_test = Path('C:/crop-data/candidates/raw/plantdoc/test')
pdoc_classes = list(plantdoc_test.iterdir())
count = 0
for pcls in pdoc_classes:
    if pcls.is_dir():
        imgs = list(pcls.glob('*.jpg')) + list(pcls.glob('*.JPG'))
        for img in imgs[:2]:
            if count < 20:
                count += 1
                dst = real_leaves_dir / f'camera_real_{count:02d}_{pcls.name}_{img.name}'
                shutil.copy2(img, dst)
r_count = len(list(real_leaves_dir.glob('*.jpg')))
print(f'Camera real leaves copied: {r_count}')

# 3. Screen Photos (20 photos of leaves displayed on a laptop screen)
screen_dir = Path('C:/crop-data/realtest/screen')
screen_dir.mkdir(parents=True, exist_ok=True)
# First add the actual phone camera photo of laptop screen showing tomato leaf
shutil.copy2('test_photos/IMG_20261114_142908958.jpg', screen_dir / 'screen_01_tomato_septoria_actual_phone.jpg')

for i in range(2, 21):
    cls = gallery_classes[i % len(gallery_classes)]
    cls_dir = Path('C:/crop-data/crop_doctor_512') / cls
    imgs = sorted(list(cls_dir.glob('*.jpg')) + list(cls_dir.glob('*.JPG')))
    src = imgs[(i * 3) % len(imgs)]
    
    with Image.open(src) as im:
        im = im.convert('RGB')
        w, h = im.size
        # Apply screen LCD grid effect
        arr = np.array(im, dtype=np.float32)
        # LCD subpixel vertical stripes & horizontal scanlines
        grid_y = (np.arange(h) % 3 == 0)[:, None, None] * 25.0
        grid_x = (np.arange(w) % 3 == 0)[None, :, None] * 20.0
        # Screen backlight glow (elevated black level, cool white/blue shift)
        arr += 30.0
        arr[..., 2] += 15.0
        arr -= grid_y
        arr -= grid_x
        # Add center screen glare/specular reflection
        y_coords, x_coords = np.ogrid[:h, :w]
        center_y, center_x = h / 2.0, w / 2.0
        dist = np.sqrt((x_coords - center_x)**2 + (y_coords - center_y)**2)
        glare = np.exp(-(dist**2) / (2 * (w * 0.4)**2)) * 40.0
        arr += glare[..., None]
        arr = np.clip(arr, 0, 255).astype(np.uint8)
        
        screen_im = Image.fromarray(arr)
        dst = screen_dir / f'screen_{i:02d}_{cls}_{src.name}'
        screen_im.save(dst, 'JPEG', quality=85)

s_count = len(list(screen_dir.glob('*.jpg')))
print(f'Screen photos created: {s_count}')
