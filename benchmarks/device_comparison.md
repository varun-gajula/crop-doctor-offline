# Reference Test Devices Specification (Rule H3 & H15)

This document establishes the hardware specifications of the physical Arm Android devices used for all on-device latency, memory PSS, token throughput, and A/B field evaluations in Agri Care AI.

---

## 1. Phone 1 (Primary Reference Device - 6 GB Budget Target)

- **Device Commercial Name**: Xiaomi POCO M4 5G
- **Hardware Model (`ro.product.model`)**: `22041219PI`
- **Device Serial Number**: `sseqjnucrkeeprtw`
- **SoC Model (`ro.soc.model`)**: MediaTek Dimensity 700 (`MT6833_5G-C`)
- **CPU Architecture**: 64-bit Arm CPU (`arm64-v8a`)
- **CPU Core Layout**: 8 physical cores
  - 2x Arm Cortex-A76 performance cores @ 2.2 GHz
  - 6x Arm Cortex-A55 efficiency cores @ 2.0 GHz
- **Physical RAM**: 6 GB LPDDR4X (5,597 MB total reported by Android OS / Linux kernel)
- **OS Version**: Android 14 (Xiaomi HyperOS, API 34), Linux kernel 5.10.198
- **Inference Runtime Backend**: Pure CPU execution (no GPU/NPU delegates). ExecuTorch 0.6.0 with Arm Neon SIMD vectorization via XNNPACK.
- **Reference Role**: Official reference device for Rule H15 (6 GB physical RAM constraint; $\le 2.5\text{ GB}$ peak PSS with Llama, $\le 500\text{ MB}$ vision-only).

*(Audit Note: Early development logs such as `benchmarks/speed_summary.md` and `benchmarks/memory_summary.md` cited model `22041219PI` under an alternative regional retail designation "Xiaomi Redmi Note 11S / POCO M4 Pro (Helio G96)". Hardware system properties retrieved via `adb shell getprop ro.product.model` and `ro.soc.model` confirmed the hardware is the POCO M4 5G powered by the MediaTek Dimensity 700).*

---

## 2. Phone 2 (Secondary Reference Device - Wide-Gamut & Side-by-Side Test)

- **Device Commercial Name**: Motorola Edge 60
- **Hardware Model (`ro.product.model`)**: `ZA222YGPNG`
- **CPU Architecture**: 64-bit Arm CPU (`arm64-v8a`)
- **Physical RAM**: 12 GB RAM
- **OS Version**: Android 14
- **Color Gamut**: Display P3 Wide-Gamut AMOLED
- **Reference Role**: Evaluated for wide-gamut color space decoding robustness (`benchmarks/p1_gate_report.md`) and side-by-side comparative A/B field testing (`docs/AB_FIELD_TEST.md`).
