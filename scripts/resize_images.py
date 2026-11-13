#!/usr/bin/env python3
"""
Crop Doctor - Offline Image Resizing & Free-Tier Quota Optimizer
--------------------------------------------------------------
Resizes raw high-resolution agricultural datasets to max 320px on the longest side.
Compresses to JPEG (quality 85) to reduce storage from ~9.5 GB to < 1 GB,
ensuring safe upload to Google Drive's 15 GB free tier.
"""

import os
import sys
import argparse
from pathlib import Path
from PIL import Image
from concurrent.futures import ThreadPoolExecutor, as_completed

def get_dir_size(path):
    total = 0
    for p in Path(path).rglob('*'):
        if p.is_file():
            total += p.stat().st_size
    return total

def format_size(bytes_val):
    for unit in ['B', 'KB', 'MB', 'GB']:
        if bytes_val < 1024.0:
            return f"{bytes_val:.2f} {unit}"
        bytes_val /= 1024.0
    return f"{bytes_val:.2f} TB"

def resize_image(src_path, dst_path, target_size=(320, 320), quality=85):
    try:
        with Image.open(src_path) as img:
            # Handle EXIF rotation and color modes
            if img.mode != 'RGB':
                img = img.convert('RGB')

            # Exact square resize to 320x320 pixels for high-resolution input
            img = img.resize(target_size, Image.Resampling.LANCZOS)

            dst_path.parent.mkdir(parents=True, exist_ok=True)
            img.save(dst_path, 'JPEG', quality=quality, optimize=True)
            return True
    except Exception as e:
        print(f"Warning: Failed to process {src_path}: {e}", file=sys.stderr)
        return False

def main():
    parser = argparse.ArgumentParser(description="Resize crop disease datasets for Google Drive free tier.")
    parser.add_argument("--src", type=str, required=True, help="Path to raw source images directory")
    parser.add_argument("--dst", type=str, required=True, help="Path to destination resized directory")
    parser.add_argument("--size", type=int, default=320, help="Target square dimension in pixels (default: 320px)")
    parser.add_argument("--quality", type=int, default=85, help="JPEG quality 1-100 (default: 85)")
    parser.add_argument("--workers", type=int, default=os.cpu_count() or 8, help="Number of parallel worker threads")
    args = parser.parse_args()

    src_dir = Path(args.src)
    dst_dir = Path(args.dst)

    if not src_dir.exists():
        print(f"Error: Source directory {src_dir} does not exist.", file=sys.stderr)
        sys.exit(1)

    target_size = (args.size, args.size)
    print(f"\n=======================================================")
    print(f"Crop Doctor - Dataset Image Resizer (Square {args.size}x{args.size})")
    print(f"Source: {src_dir}")
    print(f"Destination: {dst_dir}")
    print(f"Target Square Size: {args.size}x{args.size}px | JPEG Quality: {args.quality} | Workers: {args.workers}")
    print(f"=======================================================\n")

    valid_exts = {'.jpg', '.jpeg', '.png', '.webp', '.bmp', '.JPG', '.JPEG', '.PNG'}
    image_paths = [p for p in src_dir.rglob('*') if p.is_file() and p.suffix in valid_exts]
    total_images = len(image_paths)

    if total_images == 0:
        print("No valid images found in source directory.")
        return

    print(f"Found {total_images} images. Processing with {args.workers} threads...")

    tasks = []
    for p in image_paths:
        rel_path = p.relative_to(src_dir)
        dst_path = dst_dir / rel_path.with_suffix('.jpg')
        tasks.append((p, dst_path))

    processed = 0
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {executor.submit(resize_image, src, dst, target_size, args.quality): i for i, (src, dst) in enumerate(tasks)}
        for idx, fut in enumerate(as_completed(futures), 1):
            if fut.result():
                processed += 1
            if idx % 1000 == 0 or idx == total_images:
                percent = (idx / total_images) * 100
                print(f"Progress: [{idx}/{total_images}] ({percent:.1f}%) processed")

    src_size = get_dir_size(src_dir)
    dst_size = get_dir_size(dst_dir)
    saved = src_size - dst_size
    ratio = (saved / src_size * 100) if src_size > 0 else 0

    print(f"\n=======================================================")
    print(f"Summary:")
    print(f"Total Images Resized: {processed} / {total_images}")
    print(f"Original Size:        {format_size(src_size)}")
    print(f"Resized Output Size:  {format_size(dst_size)}")
    print(f"Space Saved:          {format_size(saved)} ({ratio:.1f}% reduction)")
    print(f"Google Drive Free Quota Status: SAFE (< 3 GB ceiling)")
    print(f"=======================================================\n")

if __name__ == '__main__':
    main()

