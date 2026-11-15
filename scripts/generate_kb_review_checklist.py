# -*- coding: utf-8 -*-
"""
Generates docs/KB_REVIEW_CHECKLIST.md from data/kb/knowledge_base.json.
Provides human domain reviewers (Telugu native speaker, Hindi native speaker,
and KVK / Agriculture Extension Officer) with a complete sign-off sheet.
"""

import json
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KB_PATH = os.path.join(BASE_DIR, "data", "kb", "knowledge_base.json")
OUTPUT_PATH = os.path.join(BASE_DIR, "docs", "KB_REVIEW_CHECKLIST.md")

def main():
    with open(KB_PATH, "r", encoding="utf-8") as f:
        kb_data = json.load(f)

    classes = kb_data.get("classes", [])

    lines = []
    lines.append("# Knowledge Base Expert Review Checklist (21 Shipped Classes)")
    lines.append("")
    lines.append("> [!IMPORTANT]")
    lines.append("> **Review Instructions for Domain Experts:**")
    lines.append("> 1. **Target Reviewers:** (a) Native Telugu speaker with farming vocabulary, (b) Native Hindi speaker with farming vocabulary, (c) Agriculture Officer / Krishi Vigyan Kendra (KVK) Agronomist.")
    lines.append("> 2. **Review Deadline:** **19 November 2026**.")
    lines.append("> 3. **Verification Scope:** Verify language naturalness, farmer comprehension (Grade-6 reading level), agronomic accuracy of irrigation/nutrients, IPM steps for pests, and safety thresholds.")
    lines.append("> 4. **Review Policy (Rule H4):** Any class not signed off by 19 Nov 2026 continues to display `needs_expert_review: true` in the application.")
    lines.append("")
    lines.append("---")
    lines.append("")
    lines.append("## 1. Summary Count Table of Shipped Classes")
    lines.append("")
    lines.append("| Index | Class ID | English Name | Telugu Name | Hindi Name | Type | Pathogen / Cause | Official Sources Cited |")
    lines.append("|:---:|:---|:---|:---|:---|:---:|:---|:---|")

    for c in classes:
        idx = c.get("index")
        cid = c.get("class_id")
        en_name = c.get("names", {}).get("en", "")
        te_name = c.get("names", {}).get("te", "")
        hi_name = c.get("names", {}).get("hi", "")
        ctype = c.get("type", "").capitalize()
        pathogen = c.get("pathogen", "")

        # Collect sources
        sources = set()
        for n in c.get("treatment", {}).get("natural", []):
            if n.get("source"):
                sources.add(n["source"])
        for ch in c.get("treatment", {}).get("chemical", []):
            if ch.get("source"):
                sources.add(ch["source"])
        for rk, rv in c.get("regional", {}).items():
            if rv.get("source"):
                sources.add(rv["source"])
        src_str = ", ".join(sorted(sources)) if sources else "Official Guidelines"

        lines.append(f"| {idx} | `{cid}` | **{en_name}** | {te_name} | {hi_name} | {ctype} | *{pathogen}* | {src_str} |")

    lines.append("")
    lines.append("---")
    lines.append("")
    lines.append("## 2. Detailed Class-by-Class Review Sheets")
    lines.append("")

    for c in classes:
        idx = c.get("index")
        cid = c.get("class_id")
        en_name = c.get("names", {}).get("en", "")
        te_name = c.get("names", {}).get("te", "")
        hi_name = c.get("names", {}).get("hi", "")
        crop = c.get("crop", "").capitalize()
        ctype = c.get("type", "").capitalize()
        pathogen = c.get("pathogen", "")
        threshold = c.get("referral_threshold", "SEVERE")

        lines.append(f"### Class {idx}: `{cid}`")
        lines.append(f"- **Crop:** {crop} | **Type:** {ctype} | **Scientific Cause:** *{pathogen}* | **Referral Threshold:** `{threshold}`")
        lines.append(f"- **Names:**")
        lines.append(f"  - **English:** {en_name}")
        lines.append(f"  - **Telugu:** {te_name}")
        lines.append(f"  - **Hindi:** {hi_name}")
        lines.append("")
        
        # Immediate Step
        imm = c.get("immediate_step", {})
        lines.append("#### Immediate Cultural Step")
        lines.append(f"- **English:** {imm.get('en', '')}")
        lines.append(f"- **Telugu:** {imm.get('te', '')}")
        lines.append(f"- **Hindi:** {imm.get('hi', '')}")
        lines.append("")

        # Prevention & Field Practice
        prev = c.get("prevention_and_field_practice", {})
        lines.append("#### (a) Prevention & Field Practice (Irrigation & Nutrients)")
        lines.append(f"- **English:** {prev.get('en', '')}")
        lines.append(f"- **Telugu:** {prev.get('te', '')}")
        lines.append(f"- **Hindi:** {prev.get('hi', '')}")
        lines.append("")

        # Early Warning Signs
        ew = c.get("early_warning_signs", {})
        lines.append("#### (b) Early-Warning Signs (What to Watch)")
        lines.append(f"- **English:** {ew.get('en', '')}")
        lines.append(f"- **Telugu:** {ew.get('te', '')}")
        lines.append(f"- **Hindi:** {ew.get('hi', '')}")
        lines.append("")

        # When to Call Extension Officer
        call_off = c.get("when_to_call_extension_officer", {})
        lines.append("#### (c) When to Call Extension Officer")
        lines.append(f"- **English:** {call_off.get('en', '')}")
        lines.append(f"- **Telugu:** {call_off.get('te', '')}")
        lines.append(f"- **Hindi:** {call_off.get('hi', '')}")
        lines.append("")

        # Treatment steps
        trt = c.get("treatment", {})
        nat_steps = trt.get("natural", [])
        if nat_steps:
            lines.append("#### Natural & Cultural Treatments")
            for ns in nat_steps:
                lines.append(f"- **Step {ns.get('step')}:** {ns.get('action', {}).get('en', '')}")
                lines.append(f"  - *Telugu:* {ns.get('action', {}).get('te', '')}")
                lines.append(f"  - *Hindi:* {ns.get('action', {}).get('hi', '')}")
                lines.append(f"  - *Citation:* {ns.get('source')} ([Source Link]({ns.get('source_url')})) — *\"{ns.get('source_quote')}\"*")
            lines.append("")

        chem_steps = trt.get("chemical", [])
        if chem_steps:
            lines.append("#### Chemical Treatments (CIB&RC Registered)")
            for cs in chem_steps:
                lines.append(f"- **Step {cs.get('step')}:** Active Ingredient: `{cs.get('active_ingredient')}` (Example: `{cs.get('product_example')}`)")
                lines.append(f"  - *Dose Policy:* `{cs.get('dose')}` (dose_verified: `{cs.get('dose_verified')}`)")
                lines.append(f"  - *Safety Line (EN):* {cs.get('safety_line', {}).get('en', '')}")
                lines.append(f"  - *Safety Line (TE):* {cs.get('safety_line', {}).get('te', '')}")
                lines.append(f"  - *Safety Line (HI):* {cs.get('safety_line', {}).get('hi', '')}")
                lines.append(f"  - *Citation:* {cs.get('source')} ([Source Link]({cs.get('source_url')})) — *\"{cs.get('source_quote')}\"*")
            lines.append("")

        # Sign-off box
        lines.append("##### Review Sign-off Box:")
        lines.append("- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.")
        lines.append("- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.")
        lines.append("- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.")
        lines.append("- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.")
        lines.append("- **Reviewer Name & Designation:** ________________________________________")
        lines.append("- **Signature / Date:** __________________")
        lines.append("- **Corrections / Notes:** __________________________________________________")
        lines.append("")
        lines.append("---")
        lines.append("")

    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"Successfully generated {OUTPUT_PATH} ({len(lines)} lines)")

if __name__ == "__main__":
    main()
