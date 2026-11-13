import os
from PIL import Image, ImageDraw, ImageFont

def make_t2b_collage():
    benchmarks_dir = "benchmarks"
    output_path = os.path.join(benchmarks_dir, "t2b_result_screens_collage.png")

    items = [
        ("result_screen_te.png", "1. Telugu: Result (Top)"),
        ("result_screen_hi.png", "2. Hindi: Result (Top)"),
        ("result_screen_en.png", "3. English: Result (Top)"),
        ("result_screen_te_bottom.png", "4. Telugu: Result (Bottom)"),
        ("result_screen_hi_bottom.png", "5. Hindi: Result (Bottom)"),
        ("result_screen_en_bottom.png", "6. English: Result (Bottom)")
    ]

    cols = 3
    rows = 2
    card_w = 400
    card_h = int(card_w * (2400 / 1080))  # ~888 px
    padding_x = 30
    padding_y = 30
    title_h = 42
    header_h = 160

    total_w = padding_x * (cols + 1) + card_w * cols
    total_h = header_h + padding_y * (rows + 1) + (card_h + title_h) * rows

    collage = Image.new("RGB", (total_w, total_h), color=(244, 247, 244))
    draw = ImageDraw.Draw(collage)

    # Top Header banner
    draw.rectangle([0, 0, total_w, header_h], fill=(46, 125, 50))
    
    try:
        font_header = ImageFont.truetype("arial.ttf", 46)
        font_sub = ImageFont.truetype("arial.ttf", 22)
        font_label = ImageFont.truetype("arialbd.ttf", 20)
    except Exception:
        font_header = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        font_label = ImageFont.load_default()

    draw.text((padding_x, 30), "Crop Doctor - T2b Verification Result Screens", fill=(255, 255, 255), font=font_header)
    draw.text((padding_x, 90), "Fix 1 (Uncertainty Status) | Fix 2 (No Chatbot/Tips on Home/Result) | Fix 3 (Demo Text - Not AI) | Fix 4 (No Latency/RAM in Details)", fill=(220, 245, 220), font=font_sub)

    for idx, (filename, label) in enumerate(items):
        r = idx // cols
        c = idx % cols

        x = padding_x + c * (card_w + padding_x)
        y = header_h + padding_y + r * (card_h + title_h + padding_y)

        draw.text((x, y), label, fill=(33, 50, 33), font=font_label)

        img_path = os.path.join(benchmarks_dir, filename)
        if os.path.exists(img_path):
            with Image.open(img_path) as img:
                resized_img = img.resize((card_w, card_h), Image.Resampling.LANCZOS)
                collage.paste(resized_img, (x, y + title_h))

                draw.rectangle(
                    [x - 1, y + title_h - 1, x + card_w + 1, y + title_h + card_h + 1],
                    outline=(180, 200, 180),
                    width=2
                )

    collage.save(output_path, quality=92, optimize=True)
    print(f"Collage created at: {output_path}")

if __name__ == "__main__":
    make_t2b_collage()
