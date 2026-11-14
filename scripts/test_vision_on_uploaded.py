import json
import torch
import torchvision.transforms as transforms
from PIL import Image

# 1. Load labels and config
with open("app/src/main/assets/labels.json", "r") as f:
    labels = json.load(f)

with open("app/src/main/assets/thresholds.json", "r") as f:
    thresholds_data = json.load(f)

temp = thresholds_data.get("temperature", 1.15)
energy_thresh = thresholds_data.get("energy_threshold", -1.5)
default_thresh = thresholds_data.get("default_threshold", 0.4)
class_threshs = thresholds_data.get("class_thresholds", {})

print(f"Loaded {len(labels)} labels: {labels[:5]}...")
print(f"Temperature: {temp}, Energy threshold: {energy_thresh}, Default threshold: {default_thresh}")

# 2. Check if executorch or torch can load vision.pte
# Or load the PyTorch model / inspect vision.pte
img_path = r"C:/Users/ec026/.gemini/antigravity/brain/d54fec48-e0ec-477f-99a6-3f452cdbfc41/.user_uploaded/media_1794636781668_b4e7933c.jpg"
img = Image.open(img_path).convert("RGB")
print(f"Image opened: {img.size}")

# Preprocessing: resize to 320x320 and normalize
transform = transforms.Compose([
    transforms.Resize((320, 320)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

tensor = transform(img).unsqueeze(0)
print(f"Tensor shape: {tensor.shape}, min: {tensor.min():.2f}, max: {tensor.max():.2f}")

try:
    from executorch.extension.pybindings.portable_lib import _load_for_executorch
    et_module = _load_for_executorch("app/src/main/assets/models/vision.pte")
    outputs = et_module.forward([tensor])
    print("ExecuTorch forward success!")
    logits = outputs[0].squeeze(0).numpy()
    print("Raw logits:", logits)
except Exception as e:
    print(f"ExecuTorch pybinding not directly available: {e}")
    # Try torch.jit or torch
    try:
        import executorch
        print("ExecuTorch module present")
    except Exception as e2:
        print("ExecuTorch import error:", e2)

