# Technical Report: Crop Doctor Offline

**Challenge:** Arm Bharat AI-SoC Challenge 2026-27  
**Track:** Problem Statement 1 – Offline Crop Disease/Pest Detection and Advisory for Farmers  
**Team Repository:** [https://github.com/varun-gajula/crop-doctor-offline](https://github.com/varun-gajula/crop-doctor-offline)  
**Target Hardware:** Arm Cortex-based mobile processors (tested on MediaTek Dimensity 7400, Armv9-A)  

---

## 1. Executive Summary

Smallholder farmers across rural India face catastrophic crop yield losses due to untimely or inaccurate diagnosis of foliar diseases and pests. Existing mobile solutions fail in rural fields where cellular connectivity is intermittent or absent, or they rely on remote cloud APIs that introduce severe latency, privacy concerns, and operational subscription costs.

**Crop Doctor Offline** solves this crisis by deploying a complete, dual-lane on-device AI system running 100% offline on consumer Arm processors:
1. **Vision Inference Lane:** A lightweight, calibrated convolutional neural network (MobileNetV3-Large) running via PyTorch ExecuTorch XNNPACK on the Arm CPU, achieving sub-150ms classification across 25 target classes spanning 5 core Indian crops (Rice, Cotton, Chilli, Tomato, Maize).
2. **Advisory Reasoning Lane:** Meta Llama 3.2 1B-Instruct running locally via ExecuTorch (INT4 SpinQuant), generating grounded agronomic explanations without cloud dependency.
3. **Agronomic Grounding & Safety Guardrails:** A deterministic, trilingual Knowledge Base strictly citing ICAR, NIPHM, State Agricultural Universities (ANGRAU, TNAU), and CIB&RC regulations. An in-process regex guardrail completely prevents LLM dosage hallucinations.

---

## 2. System Architecture

The architecture enforces a strict decoupling between **instant perceptual diagnosis** and **asynchronous contextual reasoning**:

```
[ CameraX Image Capture ]
           │
           ▼
┌──────────────────────────────────────────────┐
│  Vision Inference Lane (Arm CPU / XNNPACK)   │
│  - Model: MobileNetV3-Large (224x224 RGB)    │
│  - Runtime: ExecuTorch (vision.pte)          │
│  - Latency: ~120 ms                          │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
       Class ID (0-24) + Calibrated Softmax
                       │
                       ▼
┌──────────────────────────────────────────────┐
│   Deterministic Agronomic Rules Engine       │
│   - Severity: MILD / MODERATE / SEVERE       │
│   - Certainty: SURE / PROBABLY / NOT_SURE    │
│   - Treatment Tier Selector (Natural/Chem)   │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│   Trilingual Knowledge Base (Offline JSON)   │
│   - Instant lookup: en / te / hi (< 5ms)     │
│   - Official ICAR / CIB&RC citations         │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
[ Result Screen Displayed in < 1.5 seconds ]
                       │
                       ▼ (Background Stream)
┌──────────────────────────────────────────────┐
│  Advisory Lane: Llama 3.2 1B (Arm CPU)       │
│  - Runtime: ExecuTorch XNNPACK INT4          │
│  - Throughput: ~15-20 tokens/sec             │
│  - Post-filter: GuardrailFilter (Scrub dose) │
└──────────────────────────────────────────────┘
```

---

## 3. Scope: 25 Target Agronomic Classes

The model covers the 5 major crops of South India, including fungal blights, bacterial wilts, viruses, destructive pests, and healthy baselines:

| Crop | Class ID | Scientific Pathogen / Pest | Type |
|---|---|---|---|
| **Rice (వరి)** | `rice_bacterial_leaf_blight` | *Xanthomonas oryzae pv. oryzae* | Disease |
| | `rice_brown_spot` | *Bipolaris oryzae* | Disease |
| | `rice_leaf_blast` | *Magnaporthe oryzae* | Disease |
| | `rice_sheath_blight` | *Rhizoctonia solani* | Disease |
| | `rice_healthy` | None | Healthy |
| **Cotton (పత్తి)** | `cotton_bacterial_blight` | *Xanthomonas citri pv. malvacearum* | Disease |
| | `cotton_fusarium_wilt` | *Fusarium oxysporum f. sp. vasinfectum* | Disease |
| | `cotton_leaf_curl_virus` | Cotton Leaf Curl Virus (CLCuV) | Disease |
| | `cotton_alternaria_leaf_spot` | *Alternaria macrospora* | Disease |
| | `cotton_healthy` | None | Healthy |
| **Chilli (మిరప)** | `chilli_bacterial_spot` | *Xanthomonas campestris pv. vesicatoria* | Disease |
| | `chilli_leaf_curl_virus` | Chilli Leaf Curl Virus (ChiLCV) | Disease |
| | `chilli_cercospora_leaf_spot` | *Cercospora capsici* | Disease |
| | `chilli_white_spot` | *Cercospora capsici / Choanephora* | Disease |
| | `chilli_healthy` | None | Healthy |
| **Tomato (టమాటా)** | `tomato_early_blight` | *Alternaria solani* | Disease |
| | `tomato_late_blight` | *Phytophthora infestans* | Disease |
| | `tomato_yellow_leaf_curl_virus` | Tomato Yellow Leaf Curl Virus (TYLCV) | Disease |
| | `tomato_spider_mites` | *Tetranychus urticae* | Pest |
| | `tomato_healthy` | None | Healthy |
| **Maize (మొక్కజొన్న)** | `maize_gray_leaf_spot` | *Cercospora zeae-maydis* | Disease |
| | `maize_common_rust` | *Puccinia sorghi* | Disease |
| | `maize_northern_leaf_blight` | *Exserohilum turcicum* | Disease |
| | `maize_fall_armyworm` | *Spodoptera frugiperda* | Pest |
| | `maize_healthy` | None | Healthy |

---

## 4. Vision Pipeline & ExecuTorch Export

1. **Architecture:** MobileNetV3-Large with hard-swish activation, offering optimal FLOPs-to-accuracy ratio for Arm CPUs.
2. **Calibration:** Temperature scaling parameter ($T$) is learned on validation data to prevent overconfidence, ensuring softmax values map reliably to certainty bands (`SURE >= 0.80`, `PROBABLY 0.60-0.79`, `NOT_SURE < 0.60`).
3. **ExecuTorch Compilation:**
   - Graph capture via `torch.export.export()`.
   - Lowering to Edge dialect via `to_edge()`.
   - Partitioning with `XnnpackPartitioner()` targeting NEON SIMD vector units on Arm Cortex-A cores.
   - Resulting artifact (`vision.pte`) occupies ~21 MB in FP32 (or ~5.4 MB in INT8).

---

## 5. Agronomic Safety & LLM Guardrails

LLMs present safety hazards when deployed in agriculture due to potential hallucination of toxic or unregistered chemical mixtures. Crop Doctor implements a multi-layered safety barrier:

1. **Deterministic Dosage Protection:** In `knowledge_base.json`, chemical dosages are only displayed if `dose_verified == true`. Otherwise, the app explicitly directs farmers to *"follow the product label"*.
2. **In-Process Regex Guardrail (`GuardrailFilter.kt`):** A pattern scanner intercepts all token outputs from Llama 3.2 before UI rendering. Any numeric dosage pattern (e.g. `\d+\s*(ml|g|kg)\s*(per|/)\s*(L|acre)`) is scrubbed in real-time.
3. **Trilingual Strategy:**
   - **Telugu:** Llama 3.2 does not officially support Telugu. To guarantee zero mistranslation or hallucination, all Telugu text is served directly from the verified Knowledge Base.
   - **Hindi & English:** Supported natively by Llama 3.2 1B-Instruct, grounded with prompt constraints.

---

## 6. Offline Compliance & Accessibility

- **Zero-Network Audit:** Verified via Android Manifest inspection; no `android.permission.INTERNET` is declared. The app operates seamlessly in Airplane Mode.
- **Farmer UX Budget:**
  - Touch targets $\ge 56\,\text{dp}$ (shutter button $76\,\text{dp}$, language buttons $84\,\text{dp}$).
  - Body typography $\ge 18\,\text{sp}$ with high-contrast color palette suitable for direct sunlight readability.
  - Initial diagnosis latency $\le 1.5\,\text{seconds}$.
  - Cold startup $\le 2.0\,\text{seconds}$ through non-blocking background initialization in `CropDoctorApplication`.

---

## 7. Phase 2 Roadmap

- Integration of on-device segmentation head to compute exact diseased leaf surface area percentage rather than heuristic proxy.
- Real-world field evaluation in Andhra Pradesh and Telangana agricultural mandals.
- Fine-tuning of Llama 3.2 1B via LoRA on ICAR/KVK advisory question-answer pairs.

