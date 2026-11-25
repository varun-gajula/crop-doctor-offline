# Application Screenshots (docs/screenshots/)

This directory contains real-device screenshots captured on physical Android hardware (POCO M4 5G) demonstrating the offline workflow and class diagnoses.

### Application Workflow Screens:
- `flow_01_language_selection.png`: Home screen language selector supporting English, Telugu, and Hindi.
- `flow_02_crop_selection.png`: Crop selection screen with Auto Detect, Rice, Cotton, Chilli, Maize, and Tomato.
- `flow_03_camera_scan.png`: Camera viewfinder interface with leaf framing guide and shutter trigger.
- `flow_04_result_diagnosis.png`: Top-1 diagnosis card displaying detected condition name and crop category.
- `flow_05_certainty_severity.png`: Confidence certainty badge and severity level indicator.
- `flow_06_treatment_tiers.png`: Sourced immediate cultural, biological, and chemical treatment cards.
- `flow_07_llama_explanation.png`: On-device Llama 3.2 1B plain-language explanation generated offline.
- `flow_08_case_report_share.png`: Offline case summary report ready for local export or messaging.
- `flow_09_extension_referral.png`: Extension referral card showing Kisan Call Centre (1800-180-1551) contact.
- `flow_10_cant_tell_non_crop.png`: Out-of-distribution rejection when input is not a recognized crop leaf.
- `flow_11_install_ai_models.png`: Local model management screen displaying installed ExecuTorch, Llama, and Vosk files.
- `flow_12_result_telugu.png`: Complete Telugu language localized diagnosis and advisory screen.
- `flow_13_result_hindi.png`: Complete Hindi language localized diagnosis and advisory screen.
- `flow_14_airplane_mode_offline.png`: Real-device verification with Airplane Mode ON and Wi-Fi disabled.
- `flow_15_chat_question_answer.png`: Interactive offline chat Q&A grounded in verified local knowledge base guidance.
- `flow_16_voice_to_text.png`: Offline speech-to-text input via Vosk recognizer with real-time transcription.
- `flow_17_read_aloud.png`: Offline read-aloud playback using bundled native Opus audio clips and local TTS.

### Supported Class Diagnoses (`classes/`):
- `classes/01_rice_leaf_blast.png`: Top-1 diagnosis for Rice Leaf Blast (*Magnaporthe oryzae*).
- `classes/02_rice_healthy.png`: Top-1 diagnosis for Healthy Rice.
- `classes/03_cotton_bacterial_blight.png`: Top-1 diagnosis for Cotton Bacterial Blight (*Xanthomonas citri*).
- `classes/04_cotton_healthy.png`: Top-1 diagnosis for Healthy Cotton.
- `classes/05_chilli_leaf_curl_virus.png`: Top-1 diagnosis for Chilli Leaf Curl Virus (begomovirus).
- `classes/06_chilli_healthy.png`: Top-1 diagnosis for Healthy Chilli.
- `classes/07_maize_northern_leaf_blight.png`: Top-1 diagnosis for Northern Leaf Blight (*Exserohilum turcicum*).
- `classes/08_maize_common_rust.png`: Top-1 diagnosis for Maize Common Rust (*Puccinia sorghi*).
- `classes/09_maize_healthy.png`: Top-1 diagnosis for Healthy Maize.
- `classes/10_maize_fall_armyworm.png`: Top-1 diagnosis for Fall Armyworm damage (*Spodoptera frugiperda*).
- `classes/11_rice_stem_borer.png`: Top-1 diagnosis for Rice Yellow Stem Borer damage (*Scirpophaga incertulas*).
- `classes/12_rice_hispa.png`: Top-1 diagnosis for Rice Hispa damage (*Dicladispa armigera*).
- `classes/13_tomato_healthy.png`: Top-1 diagnosis for Healthy Tomato.
- `classes/14_tomato_septoria_leaf_spot.png`: Top-1 diagnosis for Tomato Septoria Leaf Spot (*Septoria lycopersici*).
- `classes/15_other_not_a_crop.png`: Top-1 classification for Non-Crop / Weeds triggering rejection.

### Rejection Examples (`rejections/`):
- `rejections/cant_tell_example.png`: Real-device rejection when input image does not show a clear leaf (CAN'T TELL safety gate).
