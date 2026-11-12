import os
from PIL import Image, ImageDraw, ImageFont

def make_collage():
    benchmarks_dir = "benchmarks"
    output_path = os.path.join(benchmarks_dir, "t2_screens_collage.png")

    items = [
        # Row 1: Onboarding
        ("screen_01_first_launch.png", "1. First Launch (Language)"),
        ("screen_02_region_te.png", "2. Region Selection (Telugu)"),
        ("screen_06_lang_chip_dialog.png", "3. Dynamic Language Switcher"),
        # Row 2: Telugu Pipeline
        ("screen_03_home_te.png", "4. Telugu - Home Screen"),
        ("screen_04_camera_te.png", "5. Telugu - Camera Screen"),
        ("screen_05_result_te.png", "6. Telugu - Result Screen"),
        # Row 3: Hindi Pipeline
        ("screen_11_home_hi.png", "7. Hindi - Home Screen"),
        ("screen_12_camera_hi.png", "8. Hindi - Camera Screen"),
        ("screen_07_result_hi.png", "9. Hindi - Result Screen"),
        # Row 4: English Pipeline
        ("screen_10_home_en.png", "10. English - Home Screen"),
        ("screen_13_camera_en.png", "11. English - Camera Screen"),
        ("screen_08_result_en.png", "12. English - Result Screen"),
        # Row 5: Details & Kisan Call Centre
        ("screen_09_result_details.png", "13. Measurement Details (Expanded)"),
        ("screen_05_result_te_bottom.png", "14. Telugu - Kisan Call Centre"),
        ("screen_07_result_hi_bottom.png", "15. Hindi - Kisan Call Centre"),
    ]

    cols = 3
    rows = 5
    card_w = 400
    card_h = int(card_w * (2400 / 1080))  # Maintain 9:20 aspect ratio -> ~888 px
    padding_x = 30
    padding_y = 30
    title_h = 42
    header_h = 160

    total_w = padding_x * (cols + 1) + card_w * cols
    total_h = header_h + padding_y * (rows + 1) + (card_h + title_h) * rows

    # Background canvas: Soft dark green/slate tone
    collage = Image.new("RGB", (total_w, total_h), color=(244, 247, 244))
    draw = ImageDraw.Draw(collage)

    # Top Header banner
    draw.rectangle([0, 0, total_w, header_h], fill=(46, 125, 50))  # FarmGreenPrimary
    
    # Try loading default font or truetype
    try:
        font_header = ImageFont.truetype("arial.ttf", 46)
        font_sub = ImageFont.truetype("arial.ttf", 24)
        font_label = ImageFont.truetype("arialbd.ttf", 20)
    except Exception:
        font_header = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        font_label = ImageFont.load_default()

    draw.text((padding_x, 30), "Crop Doctor - T2 Verification Screen Matrix", fill=(255, 255, 255), font=font_header)
    draw.text((padding_x, 90), "Arm Bharat AI-SoC Challenge 2026-27 | 100% Offline | Telugu, Hindi & English Complete Coverage", fill=(220, 245, 220), font=font_sub)

    for idx, (filename, label) in enumerate(items):
        r = idx // cols
        c = idx % cols

        x = padding_x + c * (card_w + padding_x)
        y = header_h + padding_y + r * (card_h + title_h + padding_y)

        # Card label
        draw.text((x, y), label, fill=(33, 50, 33), font=font_label)

        # Load and resize screenshot
        img_path = os.path.join(benchmarks_dir, filename)
        if os.path.exists(img_path):
            with Image.open(img_path) as img:
                resized_img = img.resize((card_w, card_h), Image.Resampling.LANCZOS)
                collage.paste(resized_img, (x, y + title_h))

                # Thin rounded-like border around screenshot
                draw.rectangle(
                    [x - 1, y + title_h - 1, x + card_w + 1, y + title_h + card_h + 1],
                    outline=(180, 200, 180),
                    width=2
                )

    collage.save(output_path, quality=92, optimize=True)
    print(f"Collage successfully created at: {os.path.abspath(output_path)}")
    print(f"Collage Dimensions: {total_w} x {total_h} pixels")

if __name__ == "__main__":
    make_collage()
