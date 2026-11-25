import json
import tiktoken
from tiktoken.load import load_tiktoken_bpe

mergeable_ranks = load_tiktoken_bpe(r'C:\Users\<path>\Downloads\tokenizer.model')
special_tokens = {
    '<|begin_of_text|>': 128000,
    '<|end_of_text|>': 128001,
    '<|reserved_special_token_0|>': 128002,
    '<|reserved_special_token_1|>': 128003,
    '<|reserved_special_token_2|>': 128004,
    '<|reserved_special_token_3|>': 128005,
    '<|start_header_id|>': 128006,
    '<|end_header_id|>': 128007,
    '<|reserved_special_token_4|>': 128008,
    '<|eot_id|>': 128009,
}
enc = tiktoken.Encoding('llama_3', pat_str=r"(?i:'s|'t|'re|'ve|'m|'ll|'d)|[^\r\n\p{L}\p{N}]?\p{L}+|\p{N}{1,3}| ?[^\s\p{L}\p{N}]+[\r\n]*|\s*[\r\n]+|\s+(?!\S)|\s+", mergeable_ranks=mergeable_ranks, special_tokens=special_tokens)

with open('app/src/main/assets/kb/knowledge_base.json', 'r', encoding='utf-8') as f:
    kb_data = json.load(f)

# Old Advice Prompt breakdown for tomato_septoria_leaf_spot
c = next(x for x in kb_data['classes'] if x['class_id'] == 'tomato_septoria_leaf_spot')
old_sys = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
You are Crop Doctor, an offline agricultural advisory assistant for smallholder farmers.
Explain the practical next steps clearly in plain words (Grade-6 reading level).
Rules:
- Base your advice ONLY on the approved knowledge-base entry below.
- Do NOT invent or recommend any chemical, dose, or treatment not listed in the entry.
- For chemicals, name ONLY the active ingredient from the entry; tell the farmer to follow the product label for exact doses.
- Prioritize natural, cultural, and bio-agent methods first.
- If severity is High or Act Now, emphasize immediate referral to an extension worker.
- Output in three short sections:
  1. What I see: (1-2 sentences on condition and severity)
  2. What to do now: (2-3 practical, prioritized actions)
  3. When to get help: (1 sentence on referral)<|eot_id|>"""

old_find = """<|start_header_id|>user<|end_header_id|>
Finding:
- Crop: Tomato
- Problem: Septoria Leaf Spot
- Type: disease
- Severity: MODERATE (18.5% affected leaf area)
- Field spread: patches
- Region: Telangana
- Urgency: Act this week
- Certainty: Sure (92.0%)"""

old_kb = """Knowledge-Base Entry (Approved):
- Sourced: PJTSAU (Telangana)
- Immediate action: Remove lower infected leaves and destroy them. Avoid overhead irrigation.
- Natural/cultural methods: Mulching around base, maintain plant spacing, spray neem oil 5ml/L or Trichoderma viride.
- Chemical options (active ingredient only, follow label for dose): Mancozeb 75% WP or Chlorothalonil 75% WP.
- Referral: Routine Field Monitoring (KVK or Kisan Call Centre 1800-180-1551)."""

old_inst = """Provide practical advice for the farmer based strictly on the above information.<|eot_id|><|start_header_id|>assistant<|end_header_id|>
"""

old_full = old_sys + "\n" + old_find + "\n" + old_kb + "\n" + old_inst

print("--- OLD ADVICE PROMPT BREAKDOWN (EN) ---")
print("System text:       ", len(enc.encode(old_sys, allowed_special='all')))
print("Finding:           ", len(enc.encode(old_find, allowed_special='all')))
print("KB passages:       ", len(enc.encode(old_kb, allowed_special='all')))
print("Instructions:      ", len(enc.encode(old_inst, allowed_special='all')))
print("Total Old EN:      ", len(enc.encode(old_full, allowed_special='all')))

# New Advice Prompt breakdown (EN)
new_sys_en = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Crop Doctor offline assistant.
- Explain in plain words for a smallholder farmer.
- Answer in at most 6 short, complete sentences. End every section with a complete sentence.
- Use only the steps in the knowledge-base entry below.
- Do not give doses (always instruct to follow the product label).
- Do not name any product not in the entry.
- Do not change the severity or referral advice.
Output in three short parts:
1. What I see:
2. What to do now:
3. When to get help:<|eot_id|>"""

new_find = """<|start_header_id|>user<|end_header_id|>
Finding: tomato, Septoria Leaf Spot (disease), MODERATE (25.0% affected leaf area), Sure, Act this week."""

new_kb = """Knowledge base:
- Immediate step: Remove lower infected leaves and destroy them.
- Natural remedy: Mulch around the base of plants to prevent soil splash.
- Chemical: Mancozeb (Dithane M-45)
- Referral: Routine Field Monitoring (KVK or Kisan Call Centre 1800-180-1551)"""

new_inst = """Explain practical steps for the farmer.<|eot_id|><|start_header_id|>assistant<|end_header_id|>
"""

new_full = new_sys_en + "\n" + new_find + "\n" + new_kb + "\n" + new_inst

print("\n--- NEW ADVICE PROMPT BREAKDOWN (EN) ---")
print("System text:       ", len(enc.encode(new_sys_en, allowed_special='all')))
print("Finding:           ", len(enc.encode(new_find, allowed_special='all')))
print("KB passages:       ", len(enc.encode(new_kb, allowed_special='all')))
print("Instructions:      ", len(enc.encode(new_inst, allowed_special='all')))
print("Total New EN:      ", len(enc.encode(new_full, allowed_special='all')))

# New Advice Prompt breakdown (HI)
new_sys_hi = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Crop Doctor offline assistant.
- Explain in plain words for a smallholder farmer in simple Hindi (सरल हिंदी में).
- Answer in at most 6 short, complete sentences. End every section with a complete sentence.
- Use only the steps in the knowledge-base entry below.
- Do not give doses (always instruct to follow the product label).
- Do not name any product not in the entry.
- Do not change the severity or referral advice.
Output in three short parts:
1. What I see:
2. What to do now:
3. When to get help:<|eot_id|>"""

new_full_hi = new_sys_hi + "\n" + new_find + "\n" + new_kb + "\n" + new_inst
print("\n--- NEW ADVICE PROMPT BREAKDOWN (HI) ---")
print("System text:       ", len(enc.encode(new_sys_hi, allowed_special='all')))
print("Finding:           ", len(enc.encode(new_find, allowed_special='all')))
print("KB passages:       ", len(enc.encode(new_kb, allowed_special='all')))
print("Instructions:      ", len(enc.encode(new_inst, allowed_special='all')))
print("Total New HI:      ", len(enc.encode(new_full_hi, allowed_special='all')))

# Chat Prompt breakdown
chat_sys = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Crop Doctor offline chat assistant.
- Answer in 1 or 2 short sentences (at most 35 words).
- End with a complete sentence.
- Use ONLY the Passage below. Do NOT add new chemicals, doses, or numbers.
- If the Passage does not answer the question, state: "This is not covered in the verified guide. Please consult an extension officer."<|eot_id|>"""

chat_find = """<|start_header_id|>user<|end_header_id|>
Finding: Tomato, Septoria Leaf Spot, MODERATE (Sure)"""

chat_passage = """Passage: Cultural control: Remove and destroy infected lower leaves. Avoid overhead sprinkler irrigation to keep foliage dry. Mulch the soil surface around plants to prevent spores from splashing from the soil onto lower leaves."""

chat_query = """Question: How should I water the plants?<|eot_id|><|start_header_id|>assistant<|end_header_id|>"""

chat_full = chat_sys + "\n" + chat_find + "\n" + chat_passage + "\n" + chat_query

print("\n--- CHAT PROMPT BREAKDOWN ---")
print("System text:       ", len(enc.encode(chat_sys, allowed_special='all')))
print("Finding:           ", len(enc.encode(chat_find, allowed_special='all')))
print("Passage:           ", len(enc.encode(chat_passage, allowed_special='all')))
print("Question/Inst:     ", len(enc.encode(chat_query, allowed_special='all')))
print("Total Chat:        ", len(enc.encode(chat_full, allowed_special='all')))
