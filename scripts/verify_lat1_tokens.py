import json
import tiktoken
from tiktoken.load import load_tiktoken_bpe

mergeable_ranks = load_tiktoken_bpe(r'C:\Users\<USERNAME>\Downloads\tokenizer.model')
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

classes_to_test = [
    'tomato_septoria_leaf_spot',
    'chilli_leaf_curl_virus',
    'rice_leaf_blast',
    'cotton_bacterial_blight',
    'maize_northern_leaf_blight'
]

print("=== NEW PROMPT TOKENS ACROSS 10 RUNS (5 EN, 5 HI) ===")
results = []
for lang in ['en', 'hi']:
    for cid in classes_to_test:
        c = next(x for x in kb_data['classes'] if x['class_id'] == cid)
        crop = c['crop']
        dis = c['names']['en']
        ktype = c['type']
        dmg = '25.0% affected leaf area' if ktype != 'pest' else '18.0% pest infestation / plant damage'
        
        lang_inst = " in simple Hindi (सरल हिंदी में)" if lang == 'hi' else ""
        sys_prompt = f"""<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Crop Doctor offline assistant.
- Explain in plain words for a smallholder farmer{lang_inst}.
- Answer in at most 6 short, complete sentences. End every section with a complete sentence.
- Use only the steps in the knowledge-base entry below.
- Do not give doses (always instruct to follow the product label).
- Do not name any product not in the entry.
- Do not change the severity or referral advice.
Output in three short parts:
1. What I see:
2. What to do now:
3. When to get help:<|eot_id|>"""

        finding = f"""<|start_header_id|>user<|end_header_id|>
Finding: {crop}, {dis} ({ktype}), MODERATE ({dmg}), Sure, Act this week."""

        raw_step = c['immediate_step']['en'] or 'Isolate infected plants.'
        step = raw_step.split('.')[0].strip() + '.' if '.' in raw_step else raw_step
        step = step[:80]
        
        raw_nat = c['treatment']['natural'][0]['action']['en'] if c['treatment']['natural'] else 'Field hygiene.'
        nat = raw_nat.split('.')[0].strip() + '.' if '.' in raw_nat else raw_nat
        nat = nat[:75]
        
        chem_obj = c['treatment']['chemical'][0] if c['treatment']['chemical'] else None
        chem = f"{chem_obj['active_ingredient']} ({chem_obj['product_example']})" if chem_obj else 'None recommended for current stage.'
        chem = chem[:50]
        
        ref = 'Routine Field Monitoring (KVK or Kisan Call Centre 1800-180-1551)'
        
        kb_block = f"""
Knowledge base:
- Immediate step: {step}
- Natural remedy: {nat}
- Chemical: {chem}
- Referral: {ref}"""

        instruction = "Explain practical steps for the farmer.<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n"
        full = sys_prompt + finding + kb_block + "\n" + instruction
        
        tot = len(enc.encode(full, allowed_special='all'))
        sys_tok = len(enc.encode(sys_prompt, allowed_special='all'))
        find_tok = len(enc.encode(finding, allowed_special='all'))
        kb_tok = len(enc.encode(kb_block, allowed_special='all'))
        inst_tok = len(enc.encode(instruction, allowed_special='all'))
        results.append({
            'class_id': cid,
            'lang': lang,
            'total': tot,
            'sys': sys_tok,
            'find': find_tok,
            'kb': kb_tok,
            'inst': inst_tok
        })
        print(f"[{cid}] ({lang}): Total={tot} | Sys={sys_tok} | Find={find_tok} | KB={kb_tok} | Inst={inst_tok}")

with open('benchmarks/lat1_token_breakdown.json', 'w') as out_f:
    json.dump(results, out_f, indent=2)
print("Saved benchmarks/lat1_token_breakdown.json")
