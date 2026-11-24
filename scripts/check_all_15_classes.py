import json
import csv
import os

print("=" * 80)
print("CROP DOCTOR - COMPREHENSIVE AUDIT OF ALL 15 CLASSES")
print("=" * 80)

# 1. Labels & Metadata
with open("app/src/main/assets/labels.json", "r", encoding="utf-8") as f:
    labels = json.load(f)

with open("app/src/main/assets/supported_classes.json", "r", encoding="utf-8") as f:
    supported = json.load(f)["classes"]

with open("app/src/main/assets/kb/knowledge_base.json", "r", encoding="utf-8") as f:
    kb = json.load(f)

with open("app/src/main/assets/thresholds.json", "r", encoding="utf-8") as f:
    thresholds = json.load(f)

kb_map = {c["class_id"]: c for c in kb["classes"]}

print(f"\n1. CLASSIFICATION & KNOWLEDGE BASE COVERAGE (Total: {len(labels)} classes)\n")
fmt = "{:<3} {:<28} {:<12} {:<24} {:<28} {:<26}"
print(fmt.format("Idx", "Class ID", "Type", "English Name", "Hindi Name", "Telugu Name"))
print("-" * 125)

for idx, cid in enumerate(labels):
    sup = supported.get(str(idx), {})
    ctype = sup.get("type", "unknown").upper()
    kb_entry = kb_map.get(cid)
    if kb_entry:
        en = kb_entry["names"]["en"]
        hi = kb_entry["names"]["hi"]
        te = kb_entry["names"]["te"]
        print(fmt.format(idx, cid, ctype, en[:23], hi[:26], te[:25]))
    else:
        print(fmt.format(idx, cid, ctype, "(OOD / Non-crop)", "(गैर-फसल / बाहर)", "(పంట కాదు)"))

# 2. Knowledge Base Depth & Treatment Details
print("\n" + "=" * 80)
print("2. KNOWLEDGE BASE INTEGRITY & AGRONOMY CHECK (Rules H4 & R11)")
print("=" * 80)
kb_fmt = "{:<28} {:<12} {:<10} {:<10} {:<10} {:<15} {:<15}"
print(kb_fmt.format("Class ID", "Immediate", "Natural", "Chemical", "Regional", "Sources", "Verified"))
print("-" * 105)

for cid in labels:
    kb_entry = kb_map.get(cid)
    if not kb_entry:
        msg = "N/A (OOD Class - Filtered to Can't Tell / Out of Class)"
        print(f"{cid:<28} {msg:<60}")
        continue
    
    imm = "YES" if kb_entry.get("immediate_step", {}).get("en") else "NO"
    nat_count = len(kb_entry.get("treatment", {}).get("natural", []))
    chem_count = len(kb_entry.get("treatment", {}).get("chemical", []))
    reg = "YES" if kb_entry.get("regional_advice") else "NO"
    src_cnt = len(kb_entry.get("sources", []))
    verified = "YES" if not kb_entry.get("needs_expert_review", False) else "PENDING_REV"
    print(kb_fmt.format(cid, imm, f"{nat_count} steps", f"{chem_count} actives", reg, f"{src_cnt} official", verified))

# 3. Model Decision Thresholds
print("\n" + "=" * 80)
print("3. DECISION THRESHOLDS (thresholds.json)")
print("=" * 80)
print(f"Temperature:            {thresholds.get('temperature')}")
print(f"Confidence Threshold:   {thresholds.get('confidence_threshold')}")
print(f"Energy Threshold:       {thresholds.get('energy_threshold')}")
print(f"Borderline Energy:      {thresholds.get('borderline_energy')}")
print(f"Crop Mass Threshold:    {thresholds.get('crop_mass_threshold')}")
print(f"Margin Threshold:       {thresholds.get('margin_threshold')}")
print(f"Energy Method:          {thresholds.get('energy_method')}")
print(f"Selection Note:         {thresholds.get('selection_note')}")

# 4. Field Photo Performance on real test set
if os.path.exists("benchmarks/compare_field_15class.csv"):
    print("\n" + "=" * 80)
    print("4. FIELD PHOTO BENCHMARK PERFORMANCE (compare_field_15class.csv)")
    print("=" * 80)
    with open("benchmarks/compare_field_15class.csv", "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        field_rows = list(reader)
    
    by_class = {}
    for r in field_rows:
        tc = r.get("true_label", "unknown")
        pred = r.get("auto_top1", "unknown")
        dec = r.get("auto_decision", "unknown")
        if tc not in by_class:
            by_class[tc] = {"total": 0, "correct": 0, "wrong": 0, "cant_tell": 0, "ood": 0}
        by_class[tc]["total"] += 1
        if dec == "DIAGNOSE":
            if pred == tc:
                by_class[tc]["correct"] += 1
            else:
                by_class[tc]["wrong"] += 1
        elif "CAN'T TELL" in dec:
            by_class[tc]["cant_tell"] += 1
        elif "OUT_OF_CLASS" in dec:
            by_class[tc]["ood"] += 1
            
    p_fmt = "{:<30} {:<6} {:<8} {:<8} {:<10} {:<6}"
    print(p_fmt.format("Class", "Total", "Correct", "Wrong", "Can't Tell", "OOD"))
    print("-" * 75)
    for k in sorted(by_class.keys()):
        v = by_class[k]
        print(p_fmt.format(k, v["total"], v["correct"], v["wrong"], v["cant_tell"], v["ood"]))

print("\n" + "=" * 80)
print("AUDIT COMPLETE")
print("=" * 80)
