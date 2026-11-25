# Setup Scripts (scripts/setup/)

This directory contains automated scripts and manifests to download external training datasets and install optional large on-device AI models.

The most important file is `download_datasets.py` (configured via `datasets_manifest.json`), which automates the downloading and extraction of public vision datasets (PlantVillage, PlantDoc, Kaggle).

The companion scripts `install_llama.py` and `download_vosk.py` allow developers to push the optional Llama 3.2 1B Instruct `.pte` binary, tokenizer, and Vosk Indian English/Hindi speech models to the phone's local storage as described in `docs/INSTALL_MODELS.md`.
