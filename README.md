# Crop Doctor (Offline)

> **Offline Crop Disease & Pest Detection and Advisory for Farmers**  
> *Developed for the Arm "Bharat AI-SoC Challenge 2026–27" (Problem Statement 1: Agriculture & Food Security)*

[![Offline Verification](https://img.shields.io/badge/Network-100%25%20Offline-success.svg)](docs/REQUIREMENTS.md)
[![Hardware Target](https://img.shields.io/badge/Target-Arm%20Cortex--A%20%28Android%29-blue.svg)](AGENTS.md)
[![Advisory Engine](https://img.shields.io/badge/Advisory-Llama%203.2%201B%20%7C%20ExecuTorch-orange.svg)](AGENTS.md)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

---

## 1. Overview
In rural farming communities, cellular coverage frequently drops deep in the fields where crop diseases and pest outbreaks first appear. **Crop Doctor** is an edge-native, zero-cloud Android application engineered to run entirely offline on Arm-based System-on-Chips (SoCs).

Using a two-lane on-device architecture, Crop Doctor captures an image of a leaf or pest, determines the threat and severity in under 1.5 seconds, and provides grounded, actionable treatment recommendations in **Telugu (తెలుగు)**, **Hindi (हिन्दी)**, and **English**, all within a strict 60-second field workflow.

---

## 2. System Architecture

```
                    ┌──────────────────────────────────────────────┐
                    │            ARM-BASED EDGE DEVICE             │
                    │         (Motorola Edge 60 / Android)         │
                    └──────────────────────┬───────────────────────┘
                                           │
                                  [ Camera Shutter ]
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │          LANE 1: VISION INFERENCE            │
                    │   MobileNetV3-Large (ExecuTorch / XNNPACK)   │
                    │      Output: Class, Confidence, Severity     │
                    └──────────────────────┬───────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │        LOCAL GROUNDED KNOWLEDGE BASE         │
                    │  - Verified ICAR / KVK / SAU Packages        │
                    │  - Registered CIB&RC Chemical Actives        │
                    │  - Natural & Traditional Bio-remedies        │
                    └──────────────────────┬───────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │          LANE 2: ADVISORY REASONING          │
                    │   Meta Llama 3.2 1B-Instruct (ExecuTorch)    │
                    │   Arm Neon CPU Execution (Streaming Text)    │
                    └──────────────────────┬───────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │            FARMER-FIRST INTERFACE            │
                    │   - High Contrast Material 3 Cards           │
                    │   - Natural vs Chemical Treatment Switch     │
                    │   - Regional Referral & Offline Audio        │
                    └──────────────────────────────────────────────┘
```

---

## 3. Supported Crops & Target Scope (25 Classes)
Focused on high-priority South Indian crops across Telangana, Andhra Pradesh, Karnataka, Tamil Nadu, and Kerala:
1. **Rice (వరి):** Bacterial leaf blight, Brown spot, Leaf blast, Sheath blight, Healthy.
2. **Cotton (పత్తి):** Bacterial blight, Fusarium wilt, Leaf curl virus, Alternaria leaf spot, Healthy.
3. **Chilli (మిరప):** Bacterial spot, Leaf curl virus, Cercospora leaf spot, White spot, Healthy.
4. **Tomato (టమాటా):** Early blight, Late blight, Yellow leaf curl virus, Spider mites, Healthy.
5. **Maize (మొక్కజొన్న):** Gray leaf spot, Common rust, Northern leaf blight, Fall armyworm, Healthy.

---

## 4. Key Performance Benchmarks (Edge 60 Target)
*All metrics measured under airplane mode with zero connectivity.*

| Metric | Target Budget | Measured Value | Verification Source |
| :--- | :--- | :--- | :--- |
| **Vision Inference Latency** | $< 150\text{ ms}$ | *Pending Phase 8* | `benchmarks/edge60_perf.md` |
| **Shutter to Result Screen** | $\le 1.5\text{ s}$ | *Pending Phase 8* | `benchmarks/latency_log.md` |
| **Llama 3.2 1B Generation** | $8\text{--}15\text{ tokens/s}$ | *Pending Phase 8* | `benchmarks/edge60_perf.md` |
| **Cold App Start Time** | $\le 2.0\text{ s}$ | *Pending Phase 8* | `benchmarks/startup.md` |
| **Network Permission Count**| Exactly 0 | 0 | `benchmarks/permissions_check.txt` |

---

## 5. Traceability & Requirements
For full mapping to Arm Challenge requirements (R1–R9) and engineering hard rules (H1–H10), refer to [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md).

---

## 6. Licenses & Attribution
* **Application Code:** Apache License 2.0.
* **Advisory LLM:** [Meta Llama 3.2 Community License](https://llama.meta.com/llama3/license/). Built with Llama.
* **Fonts:** Noto Sans Telugu & Noto Sans Devanagari (SIL Open Font License).
* **Datasets:** CC BY 4.0 accredited research sets (Mendeley, PlantVillage, PlantDoc). Full details in `data/DATASETS.md`.

