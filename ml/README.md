# Machine Learning (ml/)

This directory contains the core model architecture, export pipeline, evaluation scripts, and decision thresholds for the 15-class vision classifier.

The most important file is `thresholds.json` (alongside `model.py`), which defines the exact calibrated operating parameters: confidence threshold 0.32, energy threshold -3.30, crop mass threshold 0.40, margin threshold 0.15, and temperature 0.9433.

These scripts are used to train the MobileNetV3-Large backbone, validate PyTorch-to-ExecuTorch parity via `parity_samples.json`, evaluate grouped test metrics (`evaluate.py`), and export the final optimized `vision.pte` binary for Android deployment.
