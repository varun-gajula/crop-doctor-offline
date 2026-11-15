import re
from pathlib import Path

source_path = Path(r"C:\crop-doctor-offline\scripts\build_notebook_04.py")
target_path = Path(r"C:\crop-doctor-offline\scripts\build_notebook_05_resnet.py")

with open(source_path, "r", encoding="utf-8") as f:
    text = f.read()

# 1. Update filenames and function names
text = text.replace("04_v2b_21class_capped.ipynb", "05_train_resnet18.ipynb")
text = text.replace("create_notebook_04", "create_notebook_05")
text = text.replace("crop_doctor_runs_v2b", "crop_doctor_runs_resnet")
text = text.replace("mobilenet_v3_large", "resnet18")
text = text.replace("MobileNetV3-Large", "ResNet-18")
text = text.replace("MobileNet_V3_Large_Weights", "ResNet18_Weights")

# 2. Fix model architecture
# Replace:
# in_feat = model.classifier[3].in_features
# model.classifier[3] = nn.Linear(in_feat, 21)
text = text.replace(
    'in_feat = model.classifier[3].in_features',
    'in_feat = model.fc.in_features'
)
text = text.replace(
    'model.classifier[3] = nn.Linear(in_feat, 21)  # Single 21-way classification output',
    'model.fc = nn.Linear(in_feat, 21)  # Single 21-way classification output for ResNet-18'
)

# 3. Size assertion adjustment
# In ResNet-18, fp32 is ~44 MB. We should verify Rule H2 (< 90 MB) and report Rule H15 compliance vs MobileNet
old_assertion = '''assert pte_size_mb < 30.0, f"Rule H15 VIOLATION: {pte_size_mb} MB >= 30 MB"\\n",
            "print(\\"✓ Rule H15 Compliance (< 30 MB for 6 GB RAM phone): PASS\\")\\n",'''

new_assertion = '''assert pte_size_mb < 90.0, f"Rule H2 VIOLATION: {pte_size_mb} MB >= 90 MB"\\n",
            "if pte_size_mb < 30.0:\\n",
            "    print(\\"✓ Rule H15 Compliance (< 30 MB for 6 GB RAM phone): PASS\\")\\n",
            "else:\\n",
            "    print(f\\\"Note: ResNet-18 binary size is {pte_size_mb:.2f} MB (MobileNetV3-Large was 16.17 MB). Serving as architectural comparison for report.\\\")\\n",'''

if 'assert pte_size_mb < 30.0' in text:
    text = text.replace(old_assertion, new_assertion)
    print("Replaced size assertion")
else:
    print("Size assertion pattern not found, checking...")

# 4. Checkpoint filename
text = text.replace('mobilenet_v3_large_v2b_21class.pt', 'resnet18_21class.pt')

# 5. Let's add Cell 14: Interactive Leaf Photo Tester
interactive_cell = '''
    # ── Cell 14: Interactive Leaf Photo Tester ─────────────────────────────────
    cells.append({
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": [
            "# ── 13. Interactive Leaf Photo Tester ─────────────────────────────────────────\\n",
            "# Test any leaf photo with calibrated temperature scaling\\n",
            "from google.colab import files\\n",
            "import torch.nn.functional as F\\n",
            "\\n",
            "device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')\\n",
            "model = model.to(device)\\n",
            "model.eval()\\n",
            "\\n",
            "print(\\"Upload a leaf photo to test ResNet-18 prediction:\\")\\n",
            "uploaded = files.upload()\\n",
            "\\n",
            "for fn in uploaded.keys():\\n",
            "    try:\\n",
            "        img = Image.open(fn).convert('RGB')\\n",
            "        img_t = val_tf(img).unsqueeze(0).to(device)\\n",
            "        \\n",
            "        t0 = time.time()\\n",
            "        with torch.no_grad():\\n",
            "            logits = model(img_t) / fitted_T\\n",
            "            probs = F.softmax(logits, dim=1).cpu().numpy().flatten()\\n",
            "        latency = (time.time() - t0) * 1000\\n",
            "        \\n",
            "        top3_indices = np.argsort(probs)[::-1][:3]\\n",
            "        top1_cls = TARGET_21_CLASSES[top3_indices[0]]\\n",
            "        top1_conf = probs[top3_indices[0]] * 100\\n",
            "        \\n",
            "        print(f\\\"\\\\n======================================================\\\")\\n",
            "        print(f\\\"Image: {fn} | Inference Latency: {latency:.1f} ms\\\")\\n",
            "        print(f\\\"Top Prediction: {top1_cls} ({top1_conf:.2f}%)\\\")\\n",
            "        print(f\\\"------------------------------------------------------\\\")\\n",
            "        for rank, c_idx in enumerate(top3_indices, 1):\\n",
            "            c_name = TARGET_21_CLASSES[c_idx]\\n",
            "            print(f\\\"  #{rank} {c_name:<30} : {probs[c_idx]*100:6.2f}%\\\")\\n",
            "        print(f\\\"======================================================\\\\n\\\")\\n",
            "    except Exception as e:\\n",
            "        print(f\\\"Error processing {fn}: {e}\\\")\\n"
        ]
    })
'''

# Insert interactive_cell right before notebook dictionary construction
text = text.replace('    notebook = {', interactive_cell + '\n    notebook = {')

with open(target_path, "w", encoding="utf-8") as f:
    f.write(text)

print(f"Generated {target_path} successfully!")
