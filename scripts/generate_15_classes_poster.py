import os
import json
from PIL import Image, ImageDraw, ImageFont

def get_font(path_or_name, size, fallback_name=None):
    try:
        if os.path.exists(path_or_name):
            return ImageFont.truetype(path_or_name, size)
    except Exception:
        pass
    try:
        return ImageFont.truetype(fallback_name or "arial.ttf", size)
    except Exception:
        return ImageFont.load_default()

def draw_rounded_rect(draw, xy, radius, fill=None, outline=None, width=1):
    x0, y0, x1, y1 = xy
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill, outline=outline, width=width)

def main():
    classes_data = [
        {
            "idx": "00", "crop": "RICE", "type": "DISEASE",
            "en": "Rice Leaf Blast",
            "te": "వరి అగ్గి తెగులు", "hi": "धान का झोंका रोग (ब्लास्ट)",
            "cause": "Magnaporthe oryzae", "id": "rice_leaf_blast",
            "img": "benchmarks/contact_sheets/rice_leaf_blast.jpg"
        },
        {
            "idx": "01", "crop": "RICE", "type": "HEALTHY",
            "en": "Healthy Rice",
            "te": "ఆరోగ్యకరమైన వరి", "hi": "स्वस्थ धान",
            "cause": "None (Healthy plant)", "id": "rice_healthy",
            "img": "benchmarks/contact_sheets/rice_healthy.jpg"
        },
        {
            "idx": "02", "crop": "COTTON", "type": "DISEASE",
            "en": "Cotton Bacterial Blight",
            "te": "పత్తి బ్యాక్టీరియా ఆకుమచ్చ తెగులు", "hi": "कपास का जीवाणु झुलसा",
            "cause": "Xanthomonas citri pv. malvacearum", "id": "cotton_bacterial_blight",
            "img": "benchmarks/contact_sheets/cotton_bacterial_blight.jpg"
        },
        {
            "idx": "03", "crop": "COTTON", "type": "HEALTHY",
            "en": "Healthy Cotton",
            "te": "ఆరోగ్యకరమైన పత్తి", "hi": "स्वस्थ कपास",
            "cause": "None (Healthy plant)", "id": "cotton_healthy",
            "img": "benchmarks/contact_sheets/cotton_healthy.jpg"
        },
        {
            "idx": "04", "crop": "CHILLI", "type": "DISEASE",
            "en": "Chilli Leaf Curl Virus",
            "te": "మిరప ఆకు ముడత తెగులు", "hi": "मिर्च का पत्ती मरोड़ रोग",
            "cause": "Chilli leaf curl virus (ChiLCV)", "id": "chilli_leaf_curl_virus",
            "img": "benchmarks/contact_sheets/chilli_leaf_curl_virus.jpg"
        },
        {
            "idx": "05", "crop": "CHILLI", "type": "HEALTHY",
            "en": "Healthy Chilli",
            "te": "ఆరోగ్యకరమైన మిరప", "hi": "स्वस्थ मिर्च",
            "cause": "None (Healthy plant)", "id": "chilli_healthy",
            "img": "benchmarks/contact_sheets/chilli_healthy.jpg"
        },
        {
            "idx": "06", "crop": "MAIZE", "type": "DISEASE",
            "en": "Maize Northern Leaf Blight",
            "te": "మొక్కజొన్న ఆకు మాడు తెగులు", "hi": "मक्का का उत्तरी पत्ती झुलसा",
            "cause": "Exserohilum turcicum", "id": "maize_northern_leaf_blight",
            "img": "benchmarks/contact_sheets/maize_northern_leaf_blight.jpg"
        },
        {
            "idx": "07", "crop": "MAIZE", "type": "DISEASE",
            "en": "Maize Common Rust",
            "te": "మొక్కజొన్న తుప్పు తెగులు", "hi": "मक्का का रतुआ रोग",
            "cause": "Puccinia sorghi", "id": "maize_common_rust",
            "img": "benchmarks/contact_sheets/maize_common_rust.jpg"
        },
        {
            "idx": "08", "crop": "MAIZE", "type": "HEALTHY",
            "en": "Healthy Maize",
            "te": "ఆరోగ్యకరమైన మొక్కజొన్న", "hi": "स्वस्थ मक्का",
            "cause": "None (Healthy plant)", "id": "maize_healthy",
            "img": "benchmarks/contact_sheets/maize_healthy.jpg"
        },
        {
            "idx": "09", "crop": "MAIZE", "type": "PEST",
            "en": "Maize Fall Armyworm",
            "te": "మొక్కజొన్న కత్తెర పురుగు", "hi": "मक्का का फॉल आर्मीवर्म",
            "cause": "Spodoptera frugiperda", "id": "maize_fall_armyworm",
            "img": "benchmarks/contact_sheets/maize_fall_armyworm.jpg"
        },
        {
            "idx": "10", "crop": "RICE", "type": "PEST",
            "en": "Rice Yellow Stem Borer",
            "te": "వరి కాండం తొలుచు పురుగు", "hi": "धान का पीला तना छेदक",
            "cause": "Scirpophaga incertulas", "id": "rice_stem_borer",
            "img": "benchmarks/contact_sheets/rice_stem_borer.jpg"
        },
        {
            "idx": "11", "crop": "RICE", "type": "PEST",
            "en": "Rice Hispa",
            "te": "వరి హిస్పా (ముళ్ల పురుగు)", "hi": "धान का हिस्पा कीट",
            "cause": "Dicladispa armigera", "id": "rice_hispa",
            "img": "benchmarks/contact_sheets/rice_hispa.jpg"
        },
        {
            "idx": "12", "crop": "TOMATO", "type": "HEALTHY",
            "en": "Healthy Tomato",
            "te": "ఆరోగ్యకరమైన టమాటా", "hi": "स्वस्थ टमाटर",
            "cause": "None (Healthy plant)", "id": "tomato_healthy",
            "img": "benchmarks/contact_sheets/tomato_healthy.jpg"
        },
        {
            "idx": "13", "crop": "TOMATO", "type": "DISEASE",
            "en": "Tomato Septoria Leaf Spot",
            "te": "టమాటా సెప్టోరియా ఆకుమచ్చ తెగులు", "hi": "टमाटर का सेप्टोरिया पत्ती धब्बा",
            "cause": "Septoria lycopersici", "id": "tomato_septoria_leaf_spot",
            "img": "benchmarks/contact_sheets/tomato_septoria_leaf_spot.jpg"
        },
        {
            "idx": "14", "crop": "GUARDRAIL", "type": "GUARDRAIL",
            "en": "Out of Class / Non-Crop",
            "te": "పంట కాని చిత్రం / ఇతర", "hi": "गैर-फसल / अन्य वस्तु",
            "cause": "Non-crop background / weeds", "id": "other_not_a_crop",
            "img": "benchmarks/contact_sheets/weeds_not_a_crop.jpg"
        }
    ]

    # Fonts
    font_bold_path = "C:/Windows/Fonts/segoeuib.ttf"
    font_reg_path = "C:/Windows/Fonts/segoeui.ttf"
    font_te_path = "app/src/main/assets/fonts/NotoSansTelugu-Regular.ttf"
    font_hi_path = "app/src/main/assets/fonts/NotoSansDevanagari-Regular.ttf"

    title_font = get_font(font_bold_path, 42, "arialbd.ttf")
    sub_font = get_font(font_reg_path, 22, "arial.ttf")
    card_idx_font = get_font(font_bold_path, 20, "arialbd.ttf")
    badge_font = get_font(font_bold_path, 16, "arialbd.ttf")
    en_name_font = get_font(font_bold_path, 24, "arialbd.ttf")
    cause_font = get_font(font_reg_path, 18, "arial.ttf")
    id_font = get_font(font_reg_path, 16, "arial.ttf")
    te_font = get_font(font_te_path, 18, "arial.ttf")
    hi_font = get_font(font_hi_path, 18, "arial.ttf")

    # Dimensions
    cols = 5
    rows = 3
    card_w = 560
    card_h = 630
    gap_x = 24
    gap_y = 24
    pad_x = 48
    header_h = 160
    footer_h = 50
    total_w = pad_x * 2 + cols * card_w + (cols - 1) * gap_x
    total_h = header_h + rows * card_h + (rows - 1) * gap_y + footer_h

    # Create master image
    img = Image.new("RGB", (total_w, total_h), (15, 23, 42))  # Slate 900
    draw = ImageDraw.Draw(img)

    # Header
    # Title
    draw.text((pad_x, 40), "Crop Doctor — 15 Supported Classes (TR4-15 Model)", font=title_font, fill=(34, 197, 94))
    draw.text((pad_x, 96), "Arm Bharat AI-SoC Challenge PS1 • MobileNetV3-Large (vision.pte) • 100% Offline Multi-Lingual Advisory", font=sub_font, fill=(148, 163, 184))

    # Badge color mappings
    badge_colors = {
        "DISEASE": {"bg": (127, 29, 29), "fg": (252, 165, 165), "icon": "DISEASE"},
        "PEST": {"bg": (120, 53, 15), "fg": (253, 230, 138), "icon": "PEST"},
        "HEALTHY": {"bg": (20, 83, 45), "fg": (134, 239, 172), "icon": "HEALTHY"},
        "GUARDRAIL": {"bg": (51, 65, 85), "fg": (203, 213, 225), "icon": "GUARDRAIL"}
    }

    for i, item in enumerate(classes_data):
        c = i % cols
        r = i // cols
        x0 = pad_x + c * (card_w + gap_x)
        y0 = header_h + r * (card_h + gap_y)
        x1 = x0 + card_w
        y1 = y0 + card_h

        # Card container
        draw_rounded_rect(draw, (x0, y0, x1, y1), radius=16, fill=(30, 41, 59), outline=(51, 65, 85), width=2)

        # Card Header
        pad = 20
        idx_text = f"#{item['idx']} • {item['crop']}"
        draw.text((x0 + pad, y0 + 16), idx_text, font=card_idx_font, fill=(56, 189, 248))

        # Badge
        btype = item['type']
        bcfg = badge_colors.get(btype, badge_colors["GUARDRAIL"])
        btext = bcfg["icon"]
        # Measure badge width approx
        bw = len(btext) * 11 + 24
        bx1 = x1 - pad
        bx0 = bx1 - bw
        by0 = y0 + 14
        by1 = by0 + 28
        draw_rounded_rect(draw, (bx0, by0, bx1, by1), radius=14, fill=bcfg["bg"])
        draw.text((bx0 + 12, by0 + 4), btext, font=badge_font, fill=bcfg["fg"])

        # Photo area
        img_x0 = x0 + pad
        img_y0 = y0 + 54
        img_w = card_w - 2 * pad
        img_h = 320

        # Load & crop image
        if os.path.exists(item["img"]):
            with Image.open(item["img"]) as leaf_img:
                leaf_img = leaf_img.convert("RGB")
                # Fit into img_w, img_h
                src_w, src_h = leaf_img.size
                src_ratio = src_w / src_h
                tgt_ratio = img_w / img_h
                if src_ratio > tgt_ratio:
                    new_h = img_h
                    new_w = int(img_h * src_ratio)
                else:
                    new_w = img_w
                    new_h = int(img_w / src_ratio)
                leaf_resized = leaf_img.resize((new_w, new_h), Image.Resampling.LANCZOS)
                # Center crop
                crop_x = (new_w - img_w) // 2
                crop_y = (new_h - img_h) // 2
                leaf_cropped = leaf_resized.crop((crop_x, crop_y, crop_x + img_w, crop_y + img_h))
                img.paste(leaf_cropped, (img_x0, img_y0))
                # Border around image
                draw.rectangle([img_x0, img_y0, img_x0 + img_w, img_y0 + img_h], outline=(51, 65, 85), width=1)
        else:
            draw.rectangle([img_x0, img_y0, img_x0 + img_w, img_y0 + img_h], fill=(15, 23, 42), outline=(51, 65, 85))

        # Typography area
        text_y = img_y0 + img_h + 14
        # English Name
        draw.text((x0 + pad, text_y), item["en"], font=en_name_font, fill=(255, 255, 255))

        # Telugu Name
        text_y += 34
        draw.text((x0 + pad, text_y), "తెలుగు: ", font=card_idx_font, fill=(148, 163, 184))
        draw.text((x0 + pad + 70, text_y + 1), item["te"], font=te_font, fill=(241, 245, 249))

        # Hindi Name
        text_y += 28
        draw.text((x0 + pad, text_y), "हिन्दी: ", font=card_idx_font, fill=(148, 163, 184))
        draw.text((x0 + pad + 65, text_y + 1), item["hi"], font=hi_font, fill=(241, 245, 249))

        # Cause
        text_y += 28
        cause_str = f"Cause: {item['cause']}"
        if len(cause_str) > 42:
            cause_str = cause_str[:40] + "..."
        draw.text((x0 + pad, text_y), cause_str, font=cause_font, fill=(165, 180, 252))

        # ID
        text_y += 28
        draw.text((x0 + pad, text_y), f"ID: {item['id']}", font=id_font, fill=(100, 116, 139))

    # Bottom footer note
    draw.text((pad_x, total_h - 36), "15 Classes Total (14 Agricultural Crop Classes + 1 Negative Guardrail) • All Models & Inferencing 100% Offline On-Device", font=sub_font, fill=(100, 116, 139))

    # Save outputs
    out_png = "benchmarks/crop_doctor_15_classes.png"
    out_jpg = "benchmarks/crop_doctor_15_classes.jpg"
    img.save(out_png, "PNG")
    img.save(out_jpg, "JPEG", quality=92)
    print(f"Saved poster to {out_png} and {out_jpg} ({total_w}x{total_h})")

if __name__ == "__main__":
    main()
