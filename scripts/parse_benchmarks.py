import subprocess
import json

ADB = r"C:\Users\<USERNAME>\AppData\Local\Android\Sdk\platform-tools\adb.exe"
device = "sseqjnucrkeeprtw"

cmd = [ADB, "-s", device, "shell", "run-as", "com.cropdoctor", "ls", "files/benchmarks"]
res = subprocess.run(cmd, capture_output=True, text=True, errors="replace")
files = [f.strip() for f in res.stdout.splitlines() if f.strip() and not "latest" in f]

print(f"Total benchmark json files found: {len(files)}")
for f in sorted(files)[-10:]:
    c = [ADB, "-s", device, "shell", "run-as", "com.cropdoctor", "cat", f"files/benchmarks/{f}"]
    r = subprocess.run(c, capture_output=True, text=True, errors="replace")
    try:
        data = json.loads(r.stdout)
        m = data["metrics"]
        print(f"{data['disease']} ({data['language'].upper()}): TTFT={m['first_token_time_ms']}ms, Speed={m['tokens_per_second']} tok/s, Tokens={m['tokens_generated']}, GuardrailPassed={m['guardrail_passed']}, PSS={m['peak_ram_pss_mb']}MB")
    except Exception as e:
        print(f, e, r.stdout)
