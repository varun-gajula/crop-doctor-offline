# Notebooks (notebooks/)

This directory contains the production training notebook used to train and calibrate the final 15-class MobileNetV3-Large vision model.

The most important file is `Crop_Doctor_15Class_Kaggle_20ep.ipynb`, which contains the end-to-end training pipeline: grouped train/val/test split generation, cosine classifier head training over 20 epochs, temperature scaling calibration, and ExecuTorch `.pte` model export.

To reproduce the model, upload this notebook to Kaggle or run it locally in an environment with GPU acceleration (P100 or T4 recommended) and the dataset prepared by `scripts/setup/download_datasets.py`.
