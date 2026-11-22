import json
from pathlib import Path

nb_path = Path("notebooks/Crop_Doctor_15Class_Kaggle_MaxGPU.ipynb")
with open(nb_path, "r", encoding="utf-8") as f:
    nb = json.load(f)

# Update markdown cells
for cell in nb["cells"]:
    if cell["cell_type"] == "markdown":
        new_source = []
        for line in cell["source"]:
            new_source.append(line.replace("30-Epoch", "20-Epoch").replace("30 epochs", "20 epochs").replace("30ep", "20ep"))
        cell["source"] = new_source
    elif cell["cell_type"] == "code":
        new_source = []
        for line in cell["source"]:
            new_source.append(line.replace("STARTING 30-EPOCH", "STARTING 20-EPOCH")
                                  .replace("30-EPOCH TRAINING", "20-EPOCH TRAINING")
                                  .replace("30-epoch run", "20-epoch run")
                                  .replace("final_15class_30ep.zip", "final_15class_20ep.zip"))
        cell["source"] = new_source

with open(nb_path, "w", encoding="utf-8") as f:
    json.dump(nb, f, indent=1)

print("✓ Updated all 30-Epoch mentions to 20-Epoch in Crop_Doctor_15Class_Kaggle_MaxGPU.ipynb")
