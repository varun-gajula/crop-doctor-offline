# On-Device Memory Summary Benchmark (Rule H15 & Task M1)

**Date of Measurement**: 2026-11-14T21:33:00+05:30  
**Device**: Xiaomi Redmi Note 11S / POCO M4 Pro (`22041219PI`)  
**Target Hardware**: 64-bit Arm CPU (`arm64-v8a`), 8 physical cores (MediaTek Helio G96)  
**Total Device RAM**: 5,731,384 kB (~6.0 GB physical RAM)  
**OS Version**: Android 14 (API 34)  
**Build Variant**: `debug` (Rule: "Release-like build, debug is acceptable for now, say which" · signed with Android debug keystore)  
**Measurement Method**: Direct native `adb shell dumpsys meminfo com.cropdoctor` recorded live on connected phone; zero estimations. Raw dumps stored in `benchmarks/mem_*.txt`.

---

## 1. Scenario Memory Breakdown Table

| Scenario | Total PSS (KB) | Total PSS (MB) | Java Heap (MB) | Native Heap (MB) | Code (MB) | Graphics (MB) | Rule H15 Limit | Status | Raw Dump File |
|---|---|---|---|---|---|---|---|---|---|
| **(a) App Idle After Start** | 151,224 | **147.68 MB** | 13.57 MB | 17.43 MB | 84.70 MB | 7.78 MB | $\le 500\text{ MB}$ | **PASS** | [`mem_a_idle_start.txt`](file:///c:/crop-doctor-offline/benchmarks/mem_a_idle_start.txt) |
| **(b) After One Vision Result** | 191,291 | **186.81 MB** | 12.64 MB | 49.32 MB | 85.71 MB | 8.72 MB | $\le 500\text{ MB}$ | **PASS** | [`mem_b_after_vision.txt`](file:///c:/crop-doctor-offline/benchmarks/mem_b_after_vision.txt) |
| **(c) With Llama Loaded & Idle** | 1,618,115 | **1,580.19 MB** | 12.57 MB | 1,445.10 MB | 84.95 MB | 8.72 MB | $\le 2500\text{ MB}$ | **PASS** | [`mem_c_llama_loaded_idle.txt`](file:///c:/crop-doctor-offline/benchmarks/mem_c_llama_loaded_idle.txt) |
| **(d) Peak While Streaming Answer** | 1,747,506 | **1,706.55 MB** | 14.79 MB | 1,565.74 MB | 87.11 MB | 9.57 MB | $\le 2500\text{ MB}$ | **PASS** | [`mem_d_llama_streaming_peak.txt`](file:///c:/crop-doctor-offline/benchmarks/mem_d_llama_streaming_peak.txt) |
| **(e) 60s After Llama Idle Release** | 249,641 | **243.79 MB** | 12.96 MB | 107.29 MB | 84.85 MB | 8.73 MB | $\le 500\text{ MB}$ | **PASS** | [`mem_e_60s_after_release.txt`](file:///c:/crop-doctor-offline/benchmarks/mem_e_60s_after_release.txt) |

---

## 2. Rule H15 Compliance Analysis (6 GB RAM Phone Budget)

1. **Peak RAM Budget With Llama ($\le 2.5\text{ GB} = 2,500\text{ MB}$)**:
   - **Measured Peak**: **1,706.55 MB** (during token decode streaming on Arm64 CPU).
   - **Margin Available**: $2,500\text{ MB} - 1,706.55\text{ MB} = \mathbf{793.45\text{ MB}}$ headroom.
   - **Status**: **PASS**.

2. **RAM Budget Without Llama ($\le 500\text{ MB}$)**:
   - **Cold launch on Home screen**: **147.68 MB** (352.32 MB headroom).
   - **After Vision Inference (`vision.pte` loaded + ResultScreen)**: **186.81 MB** (313.19 MB headroom).
   - **60 seconds after Llama is released**: **243.79 MB** (256.21 MB headroom).
   - **Status**: **PASS**.

3. **Reclamation & Component Analysis**:
   - The largest memory consumer in the application is the native weights allocation of `llama-3.2-1b-spinquant.pte` (1.05 GB INT4 weights + runtime KV cache), accounting for $1,565.74\text{ MB}$ of the Native Heap at peak.
   - Upon release, the ExecuTorch native module is closed via `resetNative()`, freeing $\sim 1.45\text{ GB}$ of native allocations.
   - The 60-second idle auto-release mechanism in `ExecuTorchLlamaAdvisor` cleanly reclaims Native Heap, dropping total process PSS from $1,706.55\text{ MB}$ down to $243.79\text{ MB}$.
