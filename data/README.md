# Data and Knowledge Base (data/)

This directory contains the structured agricultural knowledge base, class taxonomy maps, and dataset documentation for AI Agri Care.

The most important file is `kb/knowledge_base.json` (validated by `kb/kb.schema.json`), containing 576 sourced treatment and management passages covering all 15 supported classes.

These files are used by the Android app (bundled into assets) to provide deterministic, offline cultural, biological, and chemical recommendations, regional helpline directories (`HELPLINES.md`), and multi-language agricultural advice across English, Telugu, and Hindi.
