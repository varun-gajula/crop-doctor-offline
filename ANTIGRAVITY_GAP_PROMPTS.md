# Crop Doctor - GAP PROMPTS (G1-G5), from the official problem statement

Run ONE per conversation, in this order, after T3 is approved. Paste the task, then the FOOTER. Read FINAL_REQUIREMENTS.md first (the agent must too).

FOOTER:
Follow AGENTS.md (H1-H15). Work only in this folder. Commit after each step, run `./gradlew assembleDebug` and the unit tests after every Kotlin change, push at the end, stop at the CHECKPOINT. Add no features beyond the task. End with: done / next / needed from me.

---

## G1 - Pest vs disease (requirement F3)
1. Add `type` ("disease", "pest" or "healthy") to every class in the knowledge base and the class list; validate it in `scripts/validate_kb.py`.
2. The result screen shows the type as a labelled chip with an icon and word ("Disease" / "Pest" / "Healthy") in all three languages.
3. Pest entries use pest-management steps: how to check the field, when action is needed, biological and cultural control first, chemical last. Use only steps an official source recommends for that crop and pest (H4); anything without a source stays out and the entry stays `needs_expert_review`.
4. Add unit tests: every class has a type; pest results never show a leaf-disease severity sentence that does not apply.
CHECKPOINT G1: screenshots of one disease and one pest result in all three languages.

## G2 - Region-specific advice (requirement F6)
1. Region choices become: Telangana, Andhra Pradesh, Other. Remove Karnataka and Tamil Nadu from the picker (no Kannada or Tamil text exists).
2. Each knowledge-base entry gets a `regional` block per region: `source` (PJTSAU, ANGRAU or ICAR), `differs` (true or false), and text only when `differs` is true. Otherwise the app shows "Same advice for all regions" with the source name. Never invent a regional difference.
3. The result shows "Advice source: ..." for the chosen region.
4. Helplines: keep the Kisan Call Centre 1800-180-1551. Show any state number only if you open the official page and quote the number and URL in `data/HELPLINES.md`; otherwise leave it out and tell me.
CHECKPOINT G2: the table of regional sources, and screenshots for the three regions.

## G3 - Urgency from field spread (requirement F4, F11)
1. After a result with certainty "Sure" or "Probably", show a one-tap question: "How much of the field is affected?" with three large buttons: a few plants, some, most. The question is skipped for "Not sure".
2. In `RulesEngine`, combine leaf-damage severity (mild, moderate, severe) with the answer into urgency: Early warning, Act this week, Act now. Write the full table in `docs/RULES.md` and add a unit test for every combination.
3. Early warning results show "what to watch for in the next days". Act now results show the extension-officer button first.
4. All text in three languages with the same steps.
CHECKPOINT G3: the rules table and screenshots in all three languages.

## G4 - Case report check (requirement F9)
1. Review the "Share Field Report" feature that was added earlier. It may contain crop, finding, type, severity, certainty, region, date, photo and note. It must NOT contain active ingredients, doses or product names unless `dose_verified` is true.
2. Remove anything else not in F9. Saved reports must be listed in an offline "Saved cases" screen.
3. Add tests for the report text with and without verified doses.
CHECKPOINT G4: sample report text and the test results.

## G5 - Grounded follow-up questions with Llama (STRETCH, requirement F8)
Run ONLY after T5 shows Llama streaming within the 2.5 GB budget on the 6 GB phone.
1. After the explanation, show three suggested question chips per case (for example "How long should I wait before harvest?", "Can I use a natural option first?", "What should I watch for next week?").
2. Llama answers using only the structured finding and the approved text for that case; the guardrail applies (no doses, no unknown products); each answer is labelled "AI explanation".
3. If memory is low or the guardrail rejects the answer, show the approved text instead.
4. Replace the keyword "Chat with Agri AI Doctor" feature with this one; remove the old code.
CHECKPOINT G5: memory and speed logs, and 10 sample answers for review.

---

## D2 - Pest data (STRETCH data task, for the person doing downloads)
IP102 (more than 75,000 images, 102 pest classes) is free for academic use only; its page says other uses need the author's permission. Use it only for rice and maize pest classes after the base 25 classes pass the real-photo test. Read its `classes.txt`, list which classes match our crops, check each one for enough photos, credit the dataset in `data/DATASETS.md`, and never put the images in the repo. Report the count table before downloading.
