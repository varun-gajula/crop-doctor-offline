import json

with open("data/kb/knowledge_base.json", "r", encoding="utf-8") as f:
    d = json.load(f)

print("| Index | Class ID | English Name | Telugu Name | Hindi Name | Type | Official Sources Cited |")
print("|:---:|:---|:---|:---|:---|:---:|:---|")

for c in d["classes"]:
    idx = c["index"]
    cid = c["class_id"]
    en = c["names"]["en"]
    te = c["names"]["te"]
    hi = c["names"]["hi"]
    ctype = c["type"].capitalize()
    
    sources = set()
    for n in c.get("treatment", {}).get("natural", []):
        if n.get("source"):
            sources.add(n["source"])
    for ch in c.get("treatment", {}).get("chemical", []):
        if ch.get("source"):
            sources.add(ch["source"])
    for rv in c.get("regional", {}).values():
        if rv.get("source"):
            sources.add(rv["source"])
    
    src_str = ", ".join(sorted(sources))
    print(f"| {idx} | `{cid}` | **{en}** | {te} | {hi} | {ctype} | {src_str} |")
