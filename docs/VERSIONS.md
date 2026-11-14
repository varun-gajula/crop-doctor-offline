# Model & Runtime Version Specifications (Rule H3 & T5)

| Component | Specification / Version | File / Artifact | Source & Verification |
| :--- | :--- | :--- | :--- |
| **ExecuTorch Android SDK** | `0.6.0` (`org.pytorch:executorch-android:0.6.0`) | `app/build.gradle.kts` | Maven Central official release (supports EO8 LlmModule) |
| **ExecuTorch Python Runtime** | `0.5.1` (`torch 2.5.1+cpu` / `torch 2.5.1+cu124`) | Colab export environment | PyTorch ExecuTorch release 0.5.1 |
| **Advisory Model** | `meta-llama/Llama-3.2-1B-Instruct` | `llama-3.2-1b-spinquant.pte` (1.05 GB) | `executorch-community` Hugging Face |
| **Quantization Scheme** | SpinQuant INT4 (4-bit weight, group-wise 128) | INT4-EO8 Arm optimized | Hardware-accelerated for Arm Cortex-A |
| **Tokenizer** | SentencePiece Tokenizer | `tokenizer.model` (2.08 MB) | Meta Llama 3.2 official vocabulary |
| **Vision Backbone** | MobileNetV3-Large Multi-Task (25 classes + 3 severity) | `vision.pte` (14.25 MB) | Trained & calibrated on 46k crop images |
| **Vision Runtime** | ExecuTorch XNNPACK Partitioner (Arm NEON) | `app/src/main/assets/models/vision.pte` | Sub-15 MB on-device inference |
| **Target Hardware** | Arm64-v8a (6 GB RAM budget - Rule H15) | Samsung Galaxy S24 / Pixel 7a | Real on-device validation |

## Compatibility Guarantee
- The Android ExecuTorch runtime (`0.5.1`) matches the PyTorch ExecuTorch export version (`0.5.1`) exactly.
- Models exported with ExecuTorch 0.5.1 contain operator schema hash version 8 (`EO8`), which is natively supported by `LlamaModule` in `executorch-android:0.5.1`.
- The SentencePiece `tokenizer.model` is loaded directly by the native runtime tokenizer (avoiding legacy `tokenizer.bin` incompatibility).

