# Scripts (scripts/)

This directory contains utility scripts for dataset preparation, offline knowledge base validation, data leakage audits, and automated hardware benchmarking.

The most important files are `test_real_field_aiagricare.py` (which runs real field photo evaluation against the live phone) and `run_classwise_and_power_test.py` (which measures per-class latency and energy consumption).

Developers use these scripts to verify data hygiene (`validate_kb.py`, `generate_leakage_report.py`), audit single-source datasets (`audit_d6_classes.py`), and reproduce all on-device performance benchmarks reported in the project documentation.
