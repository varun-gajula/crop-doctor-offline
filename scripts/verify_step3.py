import hashlib
import json
import re

src_tr4 = r"C:\Users\<USERNAME>\Downloads\crop_doctor_final_15class_20ep (2)\vision.pte"
app_model = r"app\src\main\assets\models\vision.pte"

h_tr4 = hashlib.sha256(open(src_tr4, "rb").read()).hexdigest()
h_app = hashlib.sha256(open(app_model, "rb").read()).hexdigest()

print(f"TR4 Download SHA-256: {h_tr4}")
print(f"App Asset   SHA-256: {h_app}")
print(f"SHA-256 Match: {h_tr4 == h_app}")

with open("app/src/main/assets/labels.json") as f:
    labels = json.load(f)
print(f"Labels count: {len(labels)}")
print("Labels list:", labels)

with open("app/src/main/assets/thresholds.json") as f:
    th = json.load(f)
print("Thresholds selection_note:", th.get("selection_note"))

for p in ["app/src/main/res/values/strings.xml", "app/src/main/res/values-te/strings.xml", "app/src/main/res/values-hi/strings.xml"]:
    with open(p, encoding="utf-8") as f:
        content = f.read()
    m = re.search(r'<string name="app_name">(.*?)</string>', content)
    val = m.group(1) if m else "NOT FOUND"
    print(f"{p}: app_name = '{val}'")
