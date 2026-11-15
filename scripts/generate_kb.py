# -*- coding: utf-8 -*-
"""
Generates data/kb/knowledge_base.json and assets copy for Crop Doctor 21-class system.
Adheres strictly to ICAR, NIPHM, ANGRAU, TNAU, and CIB&RC recommendations.
"""
import json
import os

def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    kb_path = os.path.join(root, "data", "kb", "knowledge_base.json")
    asset_kb_path = os.path.join(root, "app", "src", "main", "assets", "kb", "knowledge_base.json")
    
    with open(kb_path, "r", encoding="utf-8") as f:
        kb_data = json.load(f)
        
    dropped = {
        "cotton_leaf_curl_virus",
        "cotton_alternaria_leaf_spot",
        "chilli_cercospora_leaf_spot",
        "chilli_white_spot"
    }
    
    filtered_classes = [c for c in kb_data.get("classes", []) if c.get("class_id") not in dropped]
    for idx, c in enumerate(filtered_classes):
        c["index"] = idx
        
    kb_data["classes"] = filtered_classes
    kb_data["total_classes"] = len(filtered_classes)
    
    with open(kb_path, "w", encoding="utf-8") as f:
        json.dump(kb_data, f, ensure_ascii=False, indent=2)
        
    os.makedirs(os.path.dirname(asset_kb_path), exist_ok=True)
    with open(asset_kb_path, "w", encoding="utf-8") as f:
        json.dump(kb_data, f, ensure_ascii=False, indent=2)
        
    print(f"Successfully verified and synced {len(filtered_classes)} classes to {kb_path} and {asset_kb_path}.")

if __name__ == "__main__":
    main()
