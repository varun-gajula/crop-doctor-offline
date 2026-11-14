import os
from PIL import Image, ImageDraw, ImageFont

screens = [
    ("1. Home Screen", "benchmarks/screen_1_home.png"),
    ("2. Camera & Crop Chips", "benchmarks/screen_2_camera.png"),
    ("3. Result (English) & Demo Banner", "benchmarks/screen_3_result_en.png"),
    ("4. Judge Benchmark Panel", "benchmarks/screen_4_judge_panel.png"),
    ("5. Result (Telugu)", "benchmarks/screen_5_result_telugu.png"),
    ("6. Result (Hindi)", "benchmarks/screen_6_result_hindi.png"),
    ("7. Save Case Report Screen", "benchmarks/screen_7_save_report.png"),
    ("8. Saved Reports & Share", "benchmarks/screen_8_saved_cases.png"),
]

thumb_w = 360
sample = Image.open(screens[0][1])
thumb_h = int(sample.height * (thumb_w / sample.width))
header_h = 36

cols = 4
rows = 2
pad = 16

total_w = cols * thumb_w + (cols + 1) * pad
total_h = rows * (thumb_h + header_h) + (rows + 1) * pad

collage = Image.new("RGB", (total_w, total_h), color=(240, 244, 248))
draw = ImageDraw.Draw(collage)

try:
    font = ImageFont.truetype("arial.ttf", 20)
except Exception:
    font = ImageFont.load_default()

for i, (title, path) in enumerate(screens):
    r = i // cols
    c = i % cols
    x = pad + c * (thumb_w + pad)
    y = pad + r * (thumb_h + header_h + pad)

    # Title banner
    draw.rectangle([x, y, x + thumb_w, y + header_h], fill=(46, 125, 50))
    draw.text((x + 10, y + 8), title, fill=(255, 255, 255), font=font)

    # Image
    img = Image.open(path).convert("RGB")
    thumb = img.resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
    collage.paste(thumb, (x, y + header_h))

out_path = "benchmarks/laptop_emulator_test_collage.png"
collage.save(out_path, quality=92)
print(f"Successfully saved collage to {out_path} ({total_w}x{total_h})")
