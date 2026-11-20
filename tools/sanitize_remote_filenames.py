import subprocess

ADB = r"C:\Users\ec026\AppData\Local\Android\Sdk\platform-tools\adb.exe"
DEVICE = "sseqjnucrkeeprtw"

def run_cmd(cmd):
    return subprocess.run([ADB, "-s", DEVICE, "shell"] + cmd, stdout=subprocess.PIPE, text=True, encoding="utf-8").stdout

for folder in ["screen", "gallery", "real_leaves"]:
    out = run_cmd(["run-as", "com.cropdoctor", "ls", f"/data/data/com.cropdoctor/files/realtest/{folder}"])
    files = [f.strip() for f in out.splitlines() if f.strip().endswith(".jpg")]
    for f in files:
        clean = f.replace(" ", "_").replace("(", "_").replace(")", "_").replace("%", "_").replace("+", "_")
        if clean != f:
            print(f"Renaming: {f} -> {clean}")
            # move file inside run-as
            run_cmd(["run-as", "com.cropdoctor", "mv", 
                     f"/data/data/com.cropdoctor/files/realtest/{folder}/{f}",
                     f"/data/data/com.cropdoctor/files/realtest/{folder}/{clean}"])

print("Done sanitizing.")
