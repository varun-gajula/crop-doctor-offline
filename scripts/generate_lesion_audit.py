import os
import glob
import csv
import colorsys
import numpy as np
from PIL import Image, ImageDraw

def rgb_to_hsv(r, g, b):
    h_norm, s, v = colorsys.rgb_to_hsv(r/255.0, g/255.0, b/255.0)
    return h_norm * 360.0, s, v

def old_estimator(pixels):
    dark_spot = 0
    rust_orange = 0
    white_mold = 0
    yellow = 0
    green = 0
    total_foliage = 0
    
    for r, g, b in pixels:
        h, s, v = rgb_to_hsv(r, g, b)
        if s < 0.10 or v < 0.08 or (v > 0.95 and s < 0.15):
            continue
        exG = 2 * g - r - b
        is_plant = (55 <= h <= 180 and (g >= b or exG > -15)) or \
                   (35 <= h <= 55 and g > r - 15) or \
                   (14 <= h <= 35 and 45 <= r <= 210 and 25 <= g <= 140 and (r - g) <= 120)
        if is_plant:
            total_foliage += 1
            if 65 <= h <= 175 and g >= r - 10:
                green += 1
            elif 35 <= h <= 65:
                yellow += 1
            elif 16 <= h <= 34 and r > 125 and 45 <= g <= 115 and b < 55:
                rust_orange += 1
            elif 14 <= h <= 35 and 45 <= r <= 190 and (r - g) <= 50:
                dark_spot += 1
            elif v > 0.75 and s < 0.20:
                white_mold += 1
                
    if total_foliage == 0:
        return 0.0
    foliage_total = float(total_foliage)
    frac = (yellow/foliage_total * 0.7 + dark_spot/foliage_total * 1.2 + rust_orange/foliage_total * 1.0 + white_mold/foliage_total * 0.8)
    return min(max(frac, 0.0), 0.95)

def new_estimator_and_overlay(img_path, overlay_path):
    orig = Image.open(img_path).convert('RGB')
    
    # We evaluate at standard 64x64 grid (matching mobile sampleGrid)
    sample_grid = 64
    scaled = orig.resize((sample_grid, sample_grid), Image.Resampling.BILINEAR)
    grid_arr = np.array(scaled)
    
    green_count = 0
    chlorotic_count = 0
    necrotic_count = 0
    rust_count = 0
    white_mold_count = 0
    
    grid_pixels = grid_arr.reshape(-1, 3)
    for p in grid_pixels:
        r, g, b = int(p[0]), int(p[1]), int(p[2])
        hue, s, v = rgb_to_hsv(r, g, b)
        
        # 1. Background exclusion
        if v < 0.15 or (v > 0.92 and s < 0.18):
            continue
        if s < 0.12 or (abs(r - g) + abs(g - b) + abs(b - r) < 25):
            continue
        if 180 <= hue <= 330:
            continue
            
        exG = 2 * g - r - b
        
        # 2. Leaf identification
        if (50 <= hue <= 175) and (g >= b) and (exG > -15) and (s >= 0.15) and (v >= 0.15):
            green_count += 1
        elif (35 <= hue < 50) and (g >= b + 10) and (r >= g - 25) and (s >= 0.18) and (v >= 0.18):
            chlorotic_count += 1
        elif (14 <= hue <= 34) and (r >= 85) and (g >= 35) and (b <= 80) and (r > g) and (g > b) and (s >= 0.25) and (r - b >= 25):
            rust_count += 1
        elif (12 <= hue <= 40) and (r >= 45) and (g >= 25) and (r > b + 18) and (g >= b - 5) and (s >= 0.16) and (v <= 0.65):
            necrotic_count += 1
        elif (v >= 0.65 and v <= 0.92) and (0.08 <= s <= 0.22) and (g >= b) and (r >= b):
            white_mold_count += 1
            
    plant_foliage = green_count + chlorotic_count
    total_leaf = plant_foliage + necrotic_count + rust_count + white_mold_count
    
    if plant_foliage < 5 or total_leaf < 15:
        is_leaf = False
        new_frac = 0.0
        lesion_count = 0
        status = "no leaf found"
    else:
        is_leaf = True
        lesion_count = necrotic_count + rust_count + white_mold_count + chlorotic_count
        new_frac = lesion_count / float(total_leaf)
        status = "leaf detected"
        
    # Generate visual overlay image (480x480 for clear side-by-side inspection)
    vis_size = (480, 480)
    vis_img = orig.resize(vis_size, Image.Resampling.BILINEAR)
    vis_arr = np.array(vis_img)
    overlay_arr = vis_arr.copy()
    
    h_vis, w_vis, _ = vis_arr.shape
    for y in range(h_vis):
        for x in range(w_vis):
            r, g, b = int(vis_arr[y, x, 0]), int(vis_arr[y, x, 1]), int(vis_arr[y, x, 2])
            hue, s, v = rgb_to_hsv(r, g, b)
            
            # Background check
            if v < 0.15 or (v > 0.92 and s < 0.18) or s < 0.12 or (abs(r - g) + abs(g - b) + abs(b - r) < 25) or (180 <= hue <= 330):
                # Dim background
                overlay_arr[y, x] = [int(r * 0.35), int(g * 0.35), int(b * 0.35)]
                continue
                
            exG = 2 * g - r - b
            is_grn = (50 <= hue <= 175) and (g >= b) and (exG > -15) and (s >= 0.15) and (v >= 0.15)
            is_chl = (35 <= hue < 50) and (g >= b + 10) and (r >= g - 25) and (s >= 0.18) and (v >= 0.18)
            is_rst = (14 <= hue <= 34) and (r >= 85) and (g >= 35) and (b <= 80) and (r > g) and (g > b) and (s >= 0.25) and (r - b >= 25)
            is_nec = (12 <= hue <= 40) and (r >= 45) and (g >= 25) and (r > b + 18) and (g >= b - 5) and (s >= 0.16) and (v <= 0.65)
            is_mld = (v >= 0.65 and v <= 0.92) and (0.08 <= s <= 0.22) and (g >= b) and (r >= b)
            
            if not is_leaf:
                # Dim because no leaf body was found in frame
                overlay_arr[y, x] = [int(r * 0.4), int(g * 0.4), int(b * 0.4)]
            elif is_grn:
                # Healthy foliage: preserve color with subtle green clarity
                overlay_arr[y, x] = [int(r * 0.8), min(255, int(g * 1.1 + 8)), int(b * 0.8)]
            elif is_rst or is_nec or is_chl or is_mld:
                # Highlight lesions in bright RED/MAGENTA
                overlay_arr[y, x] = [255, 30, 80]
            else:
                overlay_arr[y, x] = [int(r * 0.35), int(g * 0.35), int(b * 0.35)]
                
    # Side-by-side composite: [ORIGINAL | OVERLAY]
    comp = Image.new('RGB', (vis_size[0] * 2, vis_size[1]))
    comp.paste(vis_img, (0, 0))
    comp.paste(Image.fromarray(overlay_arr), (vis_size[0], 0))
    
    draw = ImageDraw.Draw(comp)
    # Left tag
    draw.rectangle([10, 10, 180, 36], fill=(0, 0, 0))
    draw.text((16, 15), "ORIGINAL PHOTO", fill=(255, 255, 255))
    
    # Right tag
    right_text = f"LESION OVERLAY: {new_frac*100:.1f}% ({status.upper()})" if is_leaf else "OVERLAY: NO LEAF FOUND"
    draw.rectangle([vis_size[0] + 10, 10, vis_size[0] + 330, 36], fill=(0, 0, 0))
    draw.text((vis_size[0] + 16, 15), right_text, fill=(255, 60, 100) if (is_leaf and new_frac > 0) else (255, 255, 255))
    
    comp.save(overlay_path)
    
    # Also compute old fraction
    old_pixels = [tuple(p) for p in grid_pixels]
    old_frac = old_estimator(old_pixels)
    
    return {
        'file_name': os.path.basename(img_path),
        'old_fraction': old_frac,
        'new_fraction': new_frac,
        'status': status,
        'leaf_pixels': total_leaf,
        'lesion_pixels': lesion_count,
        'overlay_file': os.path.basename(overlay_path)
    }

def main():
    os.makedirs('benchmarks/lesion_masks', exist_ok=True)
    photo_files = sorted(glob.glob('test_photos/*.jpg'))
    
    results = []
    for p in photo_files:
        base = os.path.splitext(os.path.basename(p))[0]
        out_overlay = f"benchmarks/lesion_masks/{base}_overlay.png"
        res = new_estimator_and_overlay(p, out_overlay)
        results.append(res)
        
    csv_path = "benchmarks/lesion_masks/summary.csv"
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["photo_index", "file_name", "old_lesion_fraction", "new_lesion_fraction", "status", "leaf_pixel_count", "lesion_pixel_count", "overlay_image"])
        for idx, r in enumerate(results, 1):
            writer.writerow([
                idx,
                r['file_name'],
                f"{r['old_fraction']:.4f}",
                f"{r['new_fraction']:.4f}",
                r['status'],
                r['leaf_pixels'],
                r['lesion_pixels'],
                r['overlay_file']
            ])
            
    print(f"Saved {len(results)} overlays in benchmarks/lesion_masks/ and {csv_path}!")
    print("\n=== BEFORE vs AFTER COMPARISON ===")
    print(f"{'#':<3} {'Image File':<28} {'Old Fraction':<14} {'New Fraction':<14} {'Status':<14} {'Leaf Pixels':<12}")
    print("-" * 88)
    for idx, r in enumerate(results, 1):
        old_pct = f"{r['old_fraction']*100:.1f}%"
        new_pct = f"{r['new_fraction']*100:.1f}%"
        print(f"{idx:<3} {r['file_name']:<28} {old_pct:<14} {new_pct:<14} {r['status']:<14} {r['leaf_pixels']:<12}")

if __name__ == "__main__":
    main()
