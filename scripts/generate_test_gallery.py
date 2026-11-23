import json, os, urllib.parse

labels = json.load(open('app/src/main/assets/labels.json', 'r', encoding='utf-8'))
kb_classes = json.load(open('data/kb/knowledge_base.json', 'r', encoding='utf-8'))['classes']
kb_map = {item.get('class_id'): item for item in kb_classes}
supp = json.load(open('data/supported_classes.json', 'r', encoding='utf-8'))['classes']

sheet_map = {
    'rice_leaf_blast': 'contact_sheets/rice_leaf_blast.jpg',
    'rice_healthy': 'contact_sheets/rice_healthy.jpg',
    'cotton_bacterial_blight': 'contact_sheets/cotton_bacterial_blight.jpg',
    'cotton_healthy': 'contact_sheets/cotton_healthy.jpg',
    'chilli_leaf_curl_virus': 'contact_sheets/chilli_leaf_curl_virus.jpg',
    'chilli_healthy': 'contact_sheets/chilli_healthy.jpg',
    'maize_northern_leaf_blight': 'contact_sheets/maize_northern_leaf_blight.jpg',
    'maize_common_rust': 'contact_sheets/maize_common_rust.jpg',
    'maize_healthy': 'contact_sheets/maize_healthy.jpg',
    'maize_fall_armyworm': 'contact_sheets/maize_fall_armyworm.jpg',
    'rice_stem_borer': 'contact_sheets/rice_stem_borer.jpg',
    'rice_hispa': 'contact_sheets/rice_hispa.jpg',
    'tomato_healthy': 'contact_sheets/tomato_healthy.jpg',
    'tomato_septoria_leaf_spot': 'contact_sheets/tomato_septoria_leaf_spot.jpg',
    'other_not_a_crop': 'contact_sheets/weeds_not_a_crop.jpg'
}

google_queries = {
    'rice_leaf_blast': 'rice leaf blast disease leaf photo',
    'rice_healthy': 'healthy rice leaf photo',
    'cotton_bacterial_blight': 'cotton bacterial blight leaf disease photo',
    'cotton_healthy': 'healthy cotton leaf photo',
    'chilli_leaf_curl_virus': 'chilli leaf curl virus disease photo',
    'chilli_healthy': 'healthy chilli plant leaf photo',
    'maize_northern_leaf_blight': 'northern corn leaf blight maize photo',
    'maize_common_rust': 'maize common rust Puccinia sorghi leaf photo',
    'maize_healthy': 'healthy maize leaf corn photo',
    'maize_fall_armyworm': 'fall armyworm maize leaf damage photo',
    'rice_stem_borer': 'rice yellow stem borer dead heart damage photo',
    'rice_hispa': 'rice hispa leaf damage Dicladispa armigera photo',
    'tomato_healthy': 'healthy tomato leaf photo',
    'tomato_septoria_leaf_spot': 'tomato septoria leaf spot disease photo',
    'other_not_a_crop': 'garden weeds field soil background photo'
}

html = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Crop Doctor - 15 Classes Phone Testing Gallery</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
        .header { text-align: center; margin-bottom: 32px; border-bottom: 1px solid #334155; padding-bottom: 20px; }
        h1 { color: #22c55e; margin: 0 0 8px 0; }
        p.subtitle { color: #94a3b8; font-size: 16px; margin: 0; }
        .instruction { background: #1e293b; border-left: 4px solid #22c55e; padding: 14px 18px; border-radius: 6px; max-width: 900px; margin: 20px auto; color: #cbd5e1; text-align: left; }
        .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(420px, 1fr)); gap: 24px; max-width: 1400px; margin: 0 auto; }
        .card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; overflow: hidden; display: flex; flex-direction: column; transition: transform 0.15s ease; }
        .card:hover { border-color: #22c55e; transform: translateY(-2px); }
        .card-header { padding: 14px 18px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #334155; }
        .class-idx { font-weight: 800; font-size: 18px; color: #38bdf8; }
        .badge { font-size: 12px; font-weight: 700; padding: 4px 10px; border-radius: 9999px; text-transform: uppercase; letter-spacing: 0.5px; }
        .badge-disease { background: #7f1d1d; color: #fca5a5; }
        .badge-pest { background: #78350f; color: #fde68a; }
        .badge-healthy { background: #14532d; color: #86efac; }
        .badge-ood { background: #334155; color: #cbd5e1; }
        .card-body { padding: 16px 18px; flex-grow: 1; }
        .name-en { font-size: 20px; font-weight: 700; color: #ffffff; margin-bottom: 6px; }
        .name-local { font-size: 14px; color: #94a3b8; margin-bottom: 4px; }
        .pathogen { font-size: 13px; font-style: italic; color: #a5b4fc; margin-top: 8px; }
        .card-img { width: 100%; height: 260px; object-fit: contain; background: #000; border-top: 1px solid #334155; border-bottom: 1px solid #334155; }
        .card-footer { padding: 12px 18px; display: flex; justify-content: space-between; align-items: center; background: #0f172a; gap: 8px; }
        .btn-link { font-size: 13px; color: #38bdf8; text-decoration: none; font-weight: 600; padding: 6px 12px; background: #1e293b; border-radius: 6px; border: 1px solid #334155; }
        .btn-link:hover { text-decoration: none; border-color: #38bdf8; background: #0f172a; }
        .btn-gphotos { color: #fde047; border-color: #ca8a04; }
        .btn-gphotos:hover { border-color: #facc15; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🌿 Crop Doctor: 15 Classes Phone Testing Gallery</h1>
        <p class="subtitle">Point your Android phone (POCO M4 5G) at each photo card on this screen to test real-time vision inference & advisory.</p>
        <div class="instruction">
            <strong>How to test with your phone:</strong><br>
            1. Open <strong>Crop Doctor</strong> on your phone (Airplane Mode ON ✈️).<br>
            2. Tap <strong>"Scan Leaf"</strong> on the Home screen.<br>
            3. Point your phone camera directly at the image card on your laptop monitor within the green frame.<br>
            4. Press the large green shutter button.<br>
            5. Watch the app detect the crop, condition, severity, and multi-lingual advisory in under 1.5 seconds!
        </div>
    </div>
    <div class="grid">
"""

for idx, lbl in enumerate(labels):
    kb_entry = kb_map.get(lbl, {})
    s_info = supp.get(str(idx), {})
    names = kb_entry.get('names', {})
    crop = kb_entry.get('crop', '')
    c_type = s_info.get('type', kb_entry.get('type', ''))
    pathogen = kb_entry.get('pathogen', '')
    en = names.get('en', lbl)
    te = names.get('te', '-')
    hi = names.get('hi', '-')
    
    badge_cls = 'badge-disease' if c_type == 'disease' else ('badge-pest' if c_type == 'pest' else ('badge-healthy' if c_type == 'healthy' else 'badge-ood'))
    type_display = '🦠 DISEASE' if c_type == 'disease' else ('🪲 PEST' if c_type == 'pest' else ('✔ HEALTHY' if c_type == 'healthy' else '⚠️ OUT OF CLASS'))
    
    img_rel = sheet_map.get(lbl, '')
    q = urllib.parse.quote_plus(google_queries.get(lbl, lbl))
    g_url = f"https://www.google.com/search?q={q}&udm=2"
    
    html += f"""
        <div class="card" id="class-{idx}">
            <div class="card-header">
                <span class="class-idx">#{idx:02d} • {crop.upper() if crop else 'GUARDRAIL'}</span>
                <span class="badge {badge_cls}">{type_display}</span>
            </div>
            <img class="card-img" src="{img_rel}" alt="{en}" loading="lazy">
            <div class="card-body">
                <div class="name-en">{en}</div>
                <div class="name-local"><strong>తెలుగు:</strong> {te}</div>
                <div class="name-local"><strong>हिन्दी:</strong> {hi}</div>
                <div class="pathogen">Cause: {pathogen if pathogen else 'Non-crop / Clean leaf'}</div>
            </div>
            <div class="card-footer">
                <span style="font-size: 12px; color: #64748b;">ID: <code>{lbl}</code></span>
                <a class="btn-link btn-gphotos" href="{g_url}" target="_blank">🔍 Google Photos / Images ↗</a>
            </div>
        </div>
    """

html += """
    </div>
</body>
</html>
"""

os.makedirs('benchmarks', exist_ok=True)
with open('benchmarks/test_gallery_15classes.html', 'w', encoding='utf-8') as f:
    f.write(html)
print('Successfully generated benchmarks/test_gallery_15classes.html')
