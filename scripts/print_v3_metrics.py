import json

with open("app/src/main/assets/thresholds.json", encoding="utf-8") as f:
    t = json.load(f)

print("=== NEW THRESHOLDS.JSON VALUES ===")
print(json.dumps(t, indent=2))

with open("benchmarks/final/metrics.json", encoding="utf-8") as f:
    m = json.load(f)

print("\n=== GROUPED PER-CLASS VALIDATION ACCURACY ===")
overall_acc = m.get("overall_accuracy", 0) * 100
val_images = m.get("validation_images", 0)
train_images = m.get("train_images", 0)
macro_acc = m.get("macro_accuracy_balanced_accuracy", 0) * 100

print(f"Overall Top-1 Validation Accuracy: {overall_acc:.2f}% ({val_images} held-out validation images, {train_images} train images)")
print(f"Macro / Balanced Accuracy:         {macro_acc:.2f}%\n")

header = f"{'Class Name':<32} | {'Recall (Acc)':<12} | {'Precision':<10} | {'F1-Score':<10} | {'Support':<9}"
print(header)
print("-" * len(header))

for class_name, metrics in m.get("per_class_grouped_accuracy", {}).items():
    recall = metrics.get("accuracy_recall", 0) * 100
    precision = metrics.get("precision", 0) * 100
    f1 = metrics.get("f1_score", 0) * 100
    support = f"{metrics.get('correct', 0)}/{metrics.get('support', 0)}"
    print(f"{class_name:<32} | {recall:>10.2f}% | {precision:>8.2f}% | {f1:>8.2f}% | {support:>9}")

with open("benchmarks/final/speed_summary_real_leaves.json", encoding="utf-16") as f:
    d = json.load(f)

diagnosed = d.get("diagnosed_runs", [])
confs = [r["confidence"] for r in diagnosed]
print("\n=== PHONE BENCHMARK REAL LEAF RUNS (from benchmarks/final/speed_summary_real_leaves.json) ===")
print(f"Total runs evaluated: {len(diagnosed) + d.get('rejected_runs_count', 0)}")
print(f"Diagnosed count:      {len(diagnosed)}")
print(f"Can't tell (Rejected):{d.get('rejected_runs_count', 0)}")
if confs:
    print(f"Average confidence of diagnosed runs: {sum(confs)/len(confs):.4f} ({sum(confs)/len(confs)*100:.2f}%)")
print("\nDiagnosed Leaves Details (10 Runs):")
for i, r in enumerate(diagnosed):
    print(f"  {i+1:2d}. {r['file_name']:<25} | {r['display_name']:<24} | Conf: {r['confidence']:.4f} ({r['confidence']*100:.2f}%) | Latency: {r['shutter_to_result_ms']:.1f} ms")
