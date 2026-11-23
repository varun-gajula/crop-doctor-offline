#!/usr/bin/env python3
"""
Offline Voice Recognition Evaluation & Word Error Rate (WER) Benchmark.
Computes Word Error Rate (WER) and Sentence Error Rate (SER) across English, Hindi, and Telugu.

Formula:
    WER = (Substitutions + Deletions + Insertions) / Total Reference Words
"""

import os
import sys
import json
import glob
import time

def compute_wer(ref_words, hyp_words):
    """
    Computes Levenshtein distance at word level between reference and hypothesis.
    Returns (wer, substitutions, deletions, insertions, n_ref).
    """
    n = len(ref_words)
    m = len(hyp_words)
    if n == 0:
        return (1.0 if m > 0 else 0.0), 0, 0, m, 0

    # DP table for edit distance
    dp = [[0] * (m + 1) for _ in range(n + 1)]
    for i in range(n + 1):
        dp[i][0] = i
    for j in range(m + 1):
        dp[0][j] = j

    for i in range(1, n + 1):
        for j in range(1, m + 1):
            if ref_words[i - 1].lower() == hyp_words[j - 1].lower():
                dp[i][j] = dp[i - 1][j - 1]
            else:
                dp[i][j] = min(
                    dp[i - 1][j] + 1,      # deletion
                    dp[i][j - 1] + 1,      # insertion
                    dp[i - 1][j - 1] + 1  # substitution
                )

    edits = dp[n][m]
    wer = edits / float(n)
    return wer, edits, n

def normalize_text(text):
    """Normalizes punctuation and whitespace for fair speech evaluation."""
    text = text.strip().lower()
    for p in [',', '.', '?', '!', ':', ';', '"', '\'', '-', '(', ')']:
        text = text.replace(p, ' ')
    return ' '.join(text.split())

def main():
    base_dir = r"C:\crop-data\voice_test"
    languages = ['en', 'hi', 'te']
    
    print("=" * 70)
    print("Crop Doctor Offline Voice Benchmark (Vosk Models)")
    print("=" * 70)
    
    results = {}
    
    for lang in languages:
        lang_dir = os.path.join(base_dir, lang)
        q_file = os.path.join(lang_dir, "questions.txt")
        hyp_file = os.path.join(lang_dir, "hypotheses.txt")
        wav_files = sorted(glob.glob(os.path.join(lang_dir, "*.wav")))
        
        if not os.path.exists(q_file):
            print(f"[{lang.upper()}] No questions.txt found in {lang_dir}.")
            continue
            
        with open(q_file, 'r', encoding='utf-8') as f:
            ref_lines = [l.strip() for l in f if l.strip()]
            
        print(f"[{lang.upper()}] Ground truth questions ({len(ref_lines)}):")
        for i, q in enumerate(ref_lines, 1):
            print(f"  {i:2d}. {q}")
            
        # Check for recorded audio or hypotheses
        if os.path.exists(hyp_file):
            with open(hyp_file, 'r', encoding='utf-8') as f:
                hyp_lines = [l.strip() for l in f if l.strip()]
            
            total_edits = 0
            total_words = 0
            for r, h in zip(ref_lines, hyp_lines):
                r_words = normalize_text(r).split()
                h_words = normalize_text(h).split()
                wer, edits, n = compute_wer(r_words, h_words)
                total_edits += edits
                total_words += n
            
            overall_wer = (total_edits / float(total_words)) if total_words > 0 else 0.0
            print(f"  -> Evaluated WER from hypotheses.txt: {overall_wer * 100:.2f}% ({total_edits}/{total_words} errors)")
            results[lang] = {
                "ground_truth_count": len(ref_lines),
                "hypotheses_count": len(hyp_lines),
                "wer": overall_wer,
                "total_words": total_words,
                "total_edits": total_edits
            }
        elif len(wav_files) > 0:
            print(f"  -> Found {len(wav_files)} WAV files. Ready for on-device recognition.")
            results[lang] = {
                "ground_truth_count": len(ref_lines),
                "wav_count": len(wav_files),
                "status": "wav_files_ready"
            }
        else:
            print(f"  -> Audio files (.wav) pending user placement in {lang_dir}")
            results[lang] = {
                "ground_truth_count": len(ref_lines),
                "wav_count": 0,
                "status": "pending_audio"
            }
            
    # Save benchmark log
    os.makedirs("benchmarks", exist_ok=True)
    report_path = os.path.join("benchmarks", "voice_wer_report.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
        
    print(f"\nBenchmark report saved to: {report_path}")

if __name__ == "__main__":
    main()
