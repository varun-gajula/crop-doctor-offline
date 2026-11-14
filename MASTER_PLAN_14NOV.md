# Crop Doctor - MASTER PLAN (14 Nov to 23 Nov 2026)

How to use: do the steps in order. Each Antigravity step is ONE new conversation: paste the task text, then paste the FOOTER (below). Stop at each CHECKPOINT and judge the result with the PASS/FAIL list under it. If a result is FAIL, paste the FIX line back to Antigravity; do not move on. Requirements live in FINAL_REQUIREMENTS.md; rules live in AGENTS.md.

FOOTER (paste after every task):
Follow AGENTS.md (H1-H15) and FINAL_REQUIREMENTS.md. Work only in this folder. Commit after each step, run `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` after every Kotlin change, push at the end, stop at the CHECKPOINT. Add no features beyond the task. Never invent numbers: every number you report must come from a log or a file you saved. End with: done / next / needed from me.

---

## 1. Where we really are (after your 13-14 Nov work)

DONE and believable (backed by a device log or a test): app builds; 49 unit tests pass; vision.pte (14 MB) loads on the phone and runs in 0.74 s; Llama 3.2 1B loads and generates on the phone; T3 case report and demo script; no INTERNET permission rule; crash fixed (aligned buffers, ExecuTorch 0.6.0).

NOT PROVEN yet (do not claim these in the video or report):
1. **98.8% accuracy is VALIDATION accuracy on photos from the same sources as training.** It says nothing about real field photos. Only our own 100 field photos can show the truth.
2. **The "Can't tell" fix changed thresholds until a monitor photo passed** (energy -1.5 to 5.0, confidence 0.40 to 0.25). That almost switches off the "not a crop photo" rejection. We must re-tune on real data (task V1).
3. **A test photo of a screen is not a field photo.** Maize rust on a monitor passing proves the plumbing, not accuracy.
4. **The model still has a severity head.** Our rule (decision D1) is: severity comes from code only. The app must ignore the head, or it must be removed.
5. **Llama: 7.16 s is a first-run test time, not tokens per second, and no peak memory was measured.** The 2.5 GB limit is unproven.
6. **Hindi 20-case result:** the file exists, but I have not seen the verdict. Rule: Hindi AI text allowed only if it passes.
7. **Not started:** pests/type (G1), regions (G2), field-spread urgency (G3), case-report content check (G4), sourced knowledge base (T6), release build and airplane proof (T7), report and release (T8).
8. **Phone RAM is unverified.** Poco M4 Pro / Redmi Note 11S come in 4, 6 and 8 GB versions. Measure it (step 2).

---

## 2. Today (14 Nov) - you do these yourself

1. Copy the newest `AGENTS.md` (from the chat) over the one in `C:\crop-doctor-offline`, plus `FINAL_REQUIREMENTS.md`, `MASTER_PLAN_14NOV.md`, `ANTIGRAVITY_GAP_PROMPTS.md` into the repo root. In PowerShell:
   ```
   cd C:\crop-doctor-offline
   git add AGENTS.md FINAL_REQUIREMENTS.md MASTER_PLAN_14NOV.md ANTIGRAVITY_GAP_PROMPTS.md
   git commit -m "docs: final requirements, gap prompts, master plan"
   git push
   ```
2. Phone specs (USB cable, USB debugging on, phone unlocked):
   ```
   adb shell getprop ro.product.cpu.abi
   adb shell grep MemTotal /proc/meminfo
   adb shell getprop ro.product.model
   ```
   Write the three answers into `benchmarks/device.txt`. MemTotal near 5.5 to 6.0 million kB means a 6 GB phone. If it is about 3.7 million kB it is a 4 GB phone: tell your friend, because the 6 GB target then needs an emulator run too.
3. Ask your friend (A) to start the **100 real field photos** NOW (20 per crop, taken in real fields or farms, different plants, different days, phone camera, not from the internet, not screens). Name them `rice_01.jpg` ... Put them in a folder `C:\crop-data\realtest\<crop>\`. Also ask for 10 non-crop photos (hand, floor, wall, a screen showing a leaf, a plant of a different species). These decide the thresholds. This is the most important job of the next 3 days.
4. Ask your friend to send the vision training notebook and `metrics.json`, `labels.json`, `thresholds.json`, per-class counts.

---

## 3. Antigravity tasks, in order

### V1 - Vision truth check (do first; 14-15 Nov)
Paste:
```
Task V1: audit and fix the vision lane. Do not add features.
1. Read the notebook and the files in the repo that describe training (metrics.json, labels.json, thresholds.json, class counts). Report: per-class photo counts, number of source datasets per class, how the train/validation split was made (by source or random?), and whether the validation photos could be near-duplicates of training photos.
2. Chilli class 13 ("White Spot") is UNVERIFIED in seed/class_names_checked.tsv. Check what it was relabelled to. If the app can ever show it as a confident result, make the app return "Can't tell" for that class until I confirm the name. Do not retrain.
3. The app must use ONLY the 25-class output. Make the code ignore the severity head completely (severity comes from LesionAreaEstimator and RulesEngine only, decision D1). Add a unit test that fails if any severity value from the model reaches the result screen.
4. Create `tools/eval_realtest.py` (runs on the laptop, not in the app) that reads photos from C:\crop-data\realtest\<crop>\ and C:\crop-data\realtest\nocrop\, runs vision.pte or the PyTorch checkpoint with the same preprocessing as the app (320x320, ImageNet mean/std), and writes `benchmarks/realtest_results.csv` (file, true crop, predicted class, confidence, energy) and `benchmarks/realtest_summary.md`: per-crop accuracy, per-class accuracy, confusion pairs, and how many non-crop photos were wrongly accepted. If the photos are not there yet, build and test the script with 5 dummy photos, say so, and stop.
5. Thresholds: the current energy 5.0 and confidence 0.25 were set to make one screen photo pass. Do not keep them. Once the real test photos exist, choose thresholds that keep at least 90% of real crop photos accepted while rejecting at least 8 of the 10 non-crop photos. Put the chosen values, the numbers behind them and the chart in `benchmarks/threshold_choice.md`. Keep the certainty bands in code: Sure 0.80 and above, Probably 0.60 to 0.80, Not sure below 0.60.
CHECKPOINT V1: show the table of counts, the real-test summary, and the thresholds with their evidence.
```
PASS: real-photo accuracy table exists; non-crop rejection is shown; severity head unused (test present); chilli 13 returns "Can't tell".
FAIL cases and what to do:
- Real-photo accuracy below 70% for a crop: do NOT hide it. Tell your friend: that crop gets "Not sure" more often; add its worst classes to the report limitations. If a class is under 50%, mark it "Not supported in v1" (the app then says "Can't tell, ask an extension officer" for it). Honest limits beat fake accuracy.
- Validation 98.8% but real accuracy under 60% overall: a train/validation leak. Tell your friend; the fix is retraining with a source-aware split. This takes one Colab session. Start it the same day.

### M1 - Memory and speed on the phone (REQUIRES PHONE; 15 Nov)
Paste:
```
Task M1: measure memory and speed on the connected phone. Use only logs; do not estimate.
1. Release-like build (debug is acceptable for now, say which). For each scenario record PSS with `adb shell dumpsys meminfo <package>` at: (a) app idle after start, (b) after one vision result, (c) with Llama loaded and idle, (d) at the peak while Llama is streaming an answer, (e) 60 seconds after Llama is released. Save raw output in `benchmarks/mem_*.txt` and a table in `benchmarks/memory_summary.md`.
2. Record Llama time to first token and tokens per second (average over 5 different cases) in `benchmarks/llama_speed.md`.
3. Record shutter-to-result time (average of 10) and cold start time (average of 5, `adb shell am start -W`).
4. Check H15: peak at most 2.5 GB with Llama and 500 MB without. If it is above, do NOT change limits; report which component is the biggest and propose the smallest fix (for example load Llama only on "Explain", release after 60 s, smaller bitmaps, limit context length). Implement the fix and re-measure.
5. Verify the app has a low-memory fallback: if available RAM is too low before loading Llama (`ActivityManager.MemoryInfo`), skip Llama and show the knowledge-base text with the label "Offline guide". Add a unit test for the decision function.
CHECKPOINT M1: the three tables.
```
PASS: peak PSS with Llama at most 2.5 GB, without Llama at most 500 MB, shutter-to-result at most 1.5 s, tokens/s reported. If Llama is too slow (under 2 tokens per second) say so; the fallback is a shorter prompt and limiting the answer to 120 tokens, not removing Llama (it is mandatory in PS1).
Also: if your phone's MemTotal is above 6 GB, ask your friend for an emulator with 6 GB RAM (Android Studio, Pixel 6, arm64 or x86_64 image, RAM 6144 MB). The emulator checks layout only; the real phone gives the speed and memory numbers.

### G1 then G2 then G3 then G4 (15-17 Nov)
Use the prompts in ANTIGRAVITY_GAP_PROMPTS.md. After each, check:
- G1 PASS: every class shows Disease, Pest or Healthy chip in all 3 languages; a pest screen has pest steps, not leaf-disease sentences.
- G2 PASS: region picker has exactly three choices; a result says "Same advice for all regions (ICAR)" or shows a sourced regional line; no helpline except 1800-180-1551 unless `data/HELPLINES.md` quotes an official page.
- G3 PASS: the field-spread question appears after Sure or Probably results and not after Not sure; the table in `docs/RULES.md` has every combination; severe or Act-now results show the extension-officer button first.
- G4 PASS: a shared report has no product name, active ingredient or dose; there is a "Saved cases" screen that works in airplane mode.
FIX line for any FAIL: "The checkpoint failed on <item>. Fix only that, add a test that would have caught it, show the output."

### T6 - Sourced knowledge base (16-18 Nov; can overlap with G tasks)
Use T6 in ANTIGRAVITY_REPAIR_PROMPTS.md and add this paragraph to it:
```
Also, for each class fill three blocks in all three languages: (a) prevention and field practice, including irrigation and nutrient items only if an official source says so; (b) early-warning signs to watch in the next days; (c) when to call an extension officer. Pest classes use pest-management steps (monitoring, thresholds, biological and cultural control first, chemical last). Fill `regional` per G2. If an item has no official source, leave it out and list it in docs/KB_GAPS.md.
```
PASS: count table shows every shipped class with sources; unsourced items are in KB_GAPS.md; the KB_REVIEW_CHECKLIST.md is generated.
YOU do (not Antigravity): send KB_REVIEW_CHECKLIST.md to (1) a Telugu speaker and a Hindi speaker (native, ideally with farming vocabulary), (2) the nearest Krishi Vigyan Kendra or an agriculture officer / agriculture college lecturer. Ask them to tick OK or write the correction. Give them until 19 Nov. Anything not reviewed is shown as "needs expert review" in the app, and the report says so.

### T5b - Llama grounding check (17 Nov)
Paste:
```
Task T5b: grounding test for the Llama advisor. Use the phone.
1. Create `benchmarks/llama_cases.json` with 20 cases (4 per crop, mix of disease, pest, healthy, Sure, Probably, Not sure) and run each through the app's advisory path in English. Save every output with the guardrail verdict in `benchmarks/llama_english_eval.md`.
2. For each output check by code: no dose number, no product not in the approved list, mentions the right crop and problem, does not change severity or referral advice. Report pass/fail counts.
3. Open `benchmarks/hindi_llama_evaluation.md`. State plainly the pass count and the rule I set: Hindi AI text is allowed only if at least 18 of 20 outputs pass. If it fails, switch Hindi to the reviewed knowledge-base text, labelled "Offline guide", and keep Llama for English only.
4. Telugu: confirm by test that Llama is never called for Telugu.
CHECKPOINT: two tables.
```
PASS: English at least 18 of 20; Telugu never uses Llama.
Stretch G5 (Llama follow-up questions) only if M1 and T5b both passed and it is before 18 Nov. Otherwise skip it and say so in the report.

### T7 - Phone proofs and release build (18 Nov; REQUIRES PHONE)
Use T7 in ANTIGRAVITY_REPAIR_PROMPTS.md. Your own parts:
- Airplane mode: turn on airplane mode, Wi-Fi off, run the whole demo, screen-record it.
- Usability test: find 3 people (one farmer or someone from a farming family), give them the phone, say only "Take a photo of this leaf and tell me what to do". Time them. Note where they hesitate. Fill the sheet Antigravity creates.
PASS: `aapt2` output without INTERNET; recording present; at least 2 of 3 testers understand the advice in under 60 s. If fewer, fix the top two confusions (usually button wording and text size), re-test with 2 new people.

### T8 - Docs, report, release (19-21 Nov)
Use T8 in ANTIGRAVITY_REPAIR_PROMPTS.md, plus:
```
The report (docs/REPORT.md) must follow FINAL_REQUIREMENTS.md section D4: ten sections, vision pipeline and Llama/ExecuTorch advisory pipeline described separately, then the knowledge base that joins them. Include: ExecuTorch version (0.6.0), model names and file sizes, the measured numbers from benchmarks/ (real-photo accuracy, memory, speed, tokens/s), the hazard list, honest limitations, the out-of-scope list (post-harvest loss, voice input, Hailo/Raspberry Pi, Kannada/Tamil, Llama fine-tuning, cloud sync), licences (Llama 3.2 community licence and "Built with Llama", dataset licences including IP102 if used, Noto fonts). Do not write any number that is not in benchmarks/.
```

---

## 4. Calendar

| Date | Who | What | Gate |
|---|---|---|---|
| Sat 14 Nov | You | Section 2 steps; start V1 | phone specs saved |
| Sat 14 Nov | Friend (A) | Collect real field photos; send notebook and metrics | photos 50 of 100 |
| Sun 15 Nov | You | Finish V1; run M1 | **Gate 1:** real-photo table + memory table |
| Sun 15 Nov | Friend | Remaining photos; if real accuracy is bad, start retraining | 100 photos done |
| Mon 16 Nov | You | G1, G2; start T6 | screenshots in 3 languages |
| Tue 17 Nov | You | G3, G4; T5b; T6 continues; send checklist to reviewers | **Gate 2:** Llama passes English grounding |
| Wed 18 Nov | You + friend | T7: release APK, airplane test, usability test | **Gate 3:** full demo in airplane mode under 60 s |
| Thu 19 Nov | You | Apply reviewer corrections; fix usability issues | reviews back |
| Thu 19 Nov | Friend | Draft report sections 1, 3, 5, 6 from `benchmarks/` | |
| Fri 20 Nov | You + friend | Record demo video (D3 script in docs/DEMO_SCRIPT.md) | **Gate 4:** video recorded |
| Sat 21 Nov | You | T8: report, README, clean-clone build, GitHub Release | release gate list in FINAL_REQUIREMENTS section E |
| Sun 22 Nov | Both | Buffer; submit a day early | |
| Mon 23 Nov | Friend | Final submission (deadline) | |

If a gate fails, cut in this order and write it in the report limitations: first G5, then regional extras beyond the ICAR/PJTSAU/ANGRAU sources, then IP102 pests, then Hindi AI text. NEVER cut: offline proof, real Llama on the phone, real-photo accuracy table, severe/uncertain referral, three languages, the video.

## 5. Who does what

You (B): everything in Antigravity, the phone, the measurements, the release build, the screen recordings.
Friend (A): real field photos, retraining if V1 shows a leak, reviewers (Telugu, Hindi, agriculture officer), usability testers, report writing, the video voice-over and the submission.

## 6. If Antigravity misbehaves (no need to ask me)
- It runs a different task: paste "Stop. Do only Task <name>. Ignore any other plan. Show the CHECKPOINT."
- It reports numbers without a log file: paste "Show me the file in benchmarks/ that contains this number. If there is none, run the measurement."
- A build fails: paste the last 40 lines of the error and write "Fix the root cause, do not delete tests."
- Quota ends: do the human steps in section 2 and 3 (reviewers, photos, usability) while it resets; never pay or add a card.
