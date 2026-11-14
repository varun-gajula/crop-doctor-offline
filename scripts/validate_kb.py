# -*- coding: utf-8 -*-
"""
Knowledge Base Validator for Crop Doctor.
Verifies structure, language completeness (en, te, hi), agronomic citations,
and safety constraints across all 25 classes.
"""

import json
import os
import re
import sys

REQUIRED_CLASS_IDS = [
    "rice_bacterial_leaf_blight",
    "rice_brown_spot",
    "rice_leaf_blast",
    "rice_sheath_blight",
    "rice_healthy",
    "cotton_bacterial_blight",
    "cotton_fusarium_wilt",
    "cotton_leaf_curl_virus",
    "cotton_alternaria_leaf_spot",
    "cotton_healthy",
    "chilli_bacterial_spot",
    "chilli_leaf_curl_virus",
    "chilli_cercospora_leaf_spot",
    "chilli_white_spot",
    "chilli_healthy",
    "tomato_early_blight",
    "tomato_late_blight",
    "tomato_yellow_leaf_curl_virus",
    "tomato_spider_mites",
    "tomato_healthy",
    "maize_gray_leaf_spot",
    "maize_common_rust",
    "maize_northern_leaf_blight",
    "maize_fall_armyworm",
    "maize_healthy"
]

def check_localized_text(obj, path, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path}: expected dictionary with en, te, hi")
        return
    for lang in ["en", "te", "hi"]:
        val = obj.get(lang)
        if not val or not isinstance(val, str) or not val.strip():
            errors.append(f"{path}.{lang}: missing or empty text")

def validate_kb(kb_path):
    errors = []
    
    if not os.path.exists(kb_path):
        return [f"File not found: {kb_path}"]
    
    try:
        with open(kb_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        return [f"Invalid JSON: {e}"]
    
    classes = data.get("classes", [])
    if len(classes) != 25:
        errors.append(f"Expected exactly 25 classes, found {len(classes)}")
    
    found_ids = []
    for idx, item in enumerate(classes):
        cid = item.get("class_id", f"entry_{idx}")
        found_ids.append(cid)
        
        # Index check
        if item.get("index") != idx:
            errors.append(f"{cid}: index {item.get('index')} does not match position {idx}")
        
        # Basic fields
        for field in ["crop", "type", "pathogen", "referral_threshold"]:
            if not item.get(field):
                errors.append(f"{cid}: missing field '{field}'")

        # Type validation: must be strictly disease, pest, or healthy
        item_type = item.get("type")
        if item_type not in ["disease", "pest", "healthy"]:
            errors.append(f"{cid}: invalid type '{item_type}', must be 'disease', 'pest', or 'healthy'")

        # Pest specific validations: pest management steps (scouting, ETL, biological/cultural control first)
        if item_type == "pest":
            trt = item.get("treatment", {})
            nat_steps = trt.get("natural", [])
            if len(nat_steps) < 2:
                errors.append(f"{cid}: pest entry must have at least 2 biological/cultural pest-management steps")
            # Verify official pest source citations
            for n_idx, n_step in enumerate(nat_steps):
                src = n_step.get("source", "")
                if not any(auth in src for auth in ["NBAIR", "NIPHM", "ICAR", "PJTSAU", "ANGRAU", "IIHR"]):
                    errors.append(f"{cid}.treatment.natural[{n_idx}]: pest step requires recognized authority (NBAIR/NIPHM/ICAR/PJTSAU/ANGRAU/IIHR), got '{src}'")
        
        if item.get("needs_expert_review") is not True:
            errors.append(f"{cid}: 'needs_expert_review' must be True")
            
        # Names
        check_localized_text(item.get("names"), f"{cid}.names", errors)
        
        # Severity advice
        sev = item.get("severity_advice", {})
        for lvl in ["MILD", "MODERATE", "SEVERE"]:
            check_localized_text(sev.get(lvl), f"{cid}.severity_advice.{lvl}", errors)
            
        # Immediate step & explain text
        check_localized_text(item.get("immediate_step"), f"{cid}.immediate_step", errors)
        check_localized_text(item.get("explain_text"), f"{cid}.explain_text", errors)
        
        # Treatment
        trt = item.get("treatment", {})
        nat = trt.get("natural", [])
        if not nat and item.get("type") != "healthy":
            errors.append(f"{cid}: missing natural treatment steps")
        for s_idx, step in enumerate(nat):
            check_localized_text(step.get("action"), f"{cid}.treatment.natural[{s_idx}].action", errors)
            if not step.get("source") or not step.get("source_url") or not step.get("source_quote"):
                errors.append(f"{cid}.treatment.natural[{s_idx}]: missing source, source_url, or source_quote")
                
        chem = trt.get("chemical", [])
        if item.get("type") == "healthy":
            if len(chem) != 0:
                errors.append(f"{cid}: healthy class must not have chemical treatments")
        else:
            if not chem:
                errors.append(f"{cid}: non-healthy class must have chemical treatment options")
            for c_idx, c_step in enumerate(chem):
                if not c_step.get("active_ingredient"):
                    errors.append(f"{cid}.treatment.chemical[{c_idx}]: missing active_ingredient")
                
                check_localized_text(c_step.get("safety_line"), f"{cid}.treatment.chemical[{c_idx}].safety_line", errors)
                
                # Dose verification rule
                if c_step.get("dose_verified") is True:
                    if not c_step.get("source_url") or not c_step.get("source_quote"):
                        errors.append(f"{cid}.treatment.chemical[{c_idx}]: dose_verified=True requires source_url and source_quote")
                else:
                    if c_step.get("dose") != "follow the product label":
                        errors.append(f"{cid}.treatment.chemical[{c_idx}]: unverified dose must be 'follow the product label', got '{c_step.get('dose')}'")
        
        # Virus classes check
        if "virus" in cid:
            if item.get("referral_threshold") != "MODERATE":
                errors.append(f"{cid}: virus class referral_threshold must be MODERATE")
    
    # Check all required IDs
    missing = set(REQUIRED_CLASS_IDS) - set(found_ids)
    if missing:
        errors.append(f"Missing required class IDs: {missing}")
        
    return errors

def validate_class_lists(base_dir, kb_data):
    errors = []
    kb_classes = {c["class_id"]: c.get("type") for c in kb_data.get("classes", [])}

    # 1. Validate data/class_map.yaml
    yaml_path = os.path.join(base_dir, "data", "class_map.yaml")
    if not os.path.exists(yaml_path):
        errors.append(f"Missing {yaml_path}")
    else:
        try:
            import yaml
            with open(yaml_path, "r", encoding="utf-8") as f:
                cmap = yaml.safe_load(f).get("classes", {})
            if len(cmap) != 25:
                errors.append(f"class_map.yaml expected 25 classes, got {len(cmap)}")
            for idx, cinfo in cmap.items():
                cid = cinfo.get("class_id")
                ctype = cinfo.get("type")
                if ctype not in ["disease", "pest", "healthy"]:
                    errors.append(f"class_map.yaml class {cid}: invalid type '{ctype}'")
                if cid in kb_classes and ctype != kb_classes[cid]:
                    errors.append(f"class_map.yaml class {cid} type '{ctype}' does not match KB type '{kb_classes[cid]}'")
        except Exception as e:
            errors.append(f"Error parsing class_map.yaml: {e}")

    # 2. Validate data/supported_classes.json and app asset copy
    for sp_rel in ["data/supported_classes.json", "app/src/main/assets/supported_classes.json"]:
        sp_path = os.path.join(base_dir, sp_rel.replace("/", os.sep))
        if not os.path.exists(sp_path):
            errors.append(f"Missing {sp_path}")
            continue
        try:
            with open(sp_path, "r", encoding="utf-8") as f:
                sdata = json.load(f).get("classes", {})
            if len(sdata) != 25:
                errors.append(f"{sp_rel} expected 25 classes, got {len(sdata)}")
            for idx_str, sinfo in sdata.items():
                cid = sinfo.get("class_id")
                stype = sinfo.get("type")
                if stype not in ["disease", "pest", "healthy"]:
                    errors.append(f"{sp_rel} class {cid}: invalid or missing type '{stype}'")
                if cid in kb_classes and stype != kb_classes[cid]:
                    errors.append(f"{sp_rel} class {cid} type '{stype}' does not match KB type '{kb_classes[cid]}'")
        except Exception as e:
            errors.append(f"Error parsing {sp_rel}: {e}")

    # 3. Validate app assets KB copy parity
    asset_kb_path = os.path.join(base_dir, "app", "src", "main", "assets", "kb", "knowledge_base.json")
    if not os.path.exists(asset_kb_path):
        errors.append(f"Missing assets copy: {asset_kb_path}")
    else:
        try:
            with open(asset_kb_path, "r", encoding="utf-8") as f:
                asset_data = json.load(f)
            asset_classes = {c["class_id"]: c.get("type") for c in asset_data.get("classes", [])}
            if asset_classes != kb_classes:
                errors.append("app/src/main/assets/kb/knowledge_base.json class types do not match data/kb/knowledge_base.json")
        except Exception as e:
            errors.append(f"Error reading asset KB: {e}")

    return errors

def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    kb_path = os.path.join(base_dir, "data", "kb", "knowledge_base.json")
    print(f"Validating Knowledge Base: {kb_path} ...")
    errors = validate_kb(kb_path)
    
    try:
        with open(kb_path, "r", encoding="utf-8") as f:
            kb_data = json.load(f)
        print("Validating class lists (class_map.yaml, supported_classes.json, assets) ...")
        cl_errors = validate_class_lists(base_dir, kb_data)
        errors.extend(cl_errors)
    except Exception as e:
        errors.append(f"Failed to load KB data for class list validation: {e}")

    if errors:
        print(f"FAILED with {len(errors)} errors:")
        for err in errors[:20]:
            print(f"  - {err}")
        if len(errors) > 20:
            print(f"  ... and {len(errors) - 20} more errors.")
        sys.exit(1)
    else:
        print("PASS: Knowledge Base and class lists verified! All 25 classes have valid 'type' (disease/pest/healthy).")
        sys.exit(0)

if __name__ == "__main__":
    main()

