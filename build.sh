#!/bin/bash
# Сборка TorrGuard.apk без Gradle: aapt2 + javac + d8 + zipalign + apksigner
set -e
ROOT="/c/Users/Sergey/newMem"
D="$ROOT/downloads"
T="$ROOT/tools"
P="$ROOT/torrguard"
BT=""
for cand in "$T"/build-tools/*/; do BT="${cand%/}"; done
PLATFORM=""
for cand in "$T"/platform-28/*/; do PLATFORM="${cand%/}"; done
JDK_BIN=""
for cand in "$T"/jdk/*/; do JDK_BIN="${cand%/}/bin"; done
echo "build-tools: $BT"
echo "platform:   $PLATFORM"
echo "jdk:        $JDK_BIN"
[ -z "$BT" ] || [ -z "$PLATFORM" ] || [ -z "$JDK_BIN" ] && { echo "TOOLS NOT EXTRACTED"; exit 1; }
mkdir -p "$P/build"
JDK_HOME="${JDK_BIN%/bin}"
export JAVA_HOME="$(cygpath -w "$JDK_HOME")"
export PATH="$JDK_BIN:$PATH"

"$BT/aapt2.exe" link \
  -I "$PLATFORM/android.jar" \
  --manifest "$P/AndroidManifest.xml" \
  --min-sdk-version 26 --target-sdk-version 28 \
  --version-code 4 --version-name 1.3 \
  -o "$P/build/unsigned.apk"

rm -rf "$P/build/classes" "$P/build/dexdir"
mkdir -p "$P/build/classes" "$P/build/dexdir"
"$JDK_BIN/javac.exe" -source 1.8 -target 1.8 \
  -bootclasspath "$PLATFORM/android.jar" \
  -d "$P/build/classes" \
  "$P"/src/ru/sergey/torrguard/*.java 2>&1 | grep -v "^warning" || true

ls "$P/build/classes/ru/sergey/torrguard/" >/dev/null
cmd //c "$(cygpath -w "$BT")\\d8.bat" --release \
  --lib "$(cygpath -w "$PLATFORM/android.jar")" \
  --min-api 26 \
  --output "$(cygpath -w "$P/build/dexdir")" \
  "$(cygpath -w "$P")/build/classes/ru/sergey/torrguard/"*.class
[ -f "$P/build/dexdir/classes.dex" ] || { echo "D8 FAILED"; exit 1; }

# вшить classes.dex в apk (PowerShell ZipArchive)
powershell -NoProfile -Command '
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apk = [System.IO.Compression.ZipFile]::Open("C:\Users\Sergey\newMem\torrguard\build\unsigned.apk", "Update")
$entry = $apk.GetEntry("classes.dex"); if ($entry) { $entry.Delete() }
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($apk, "C:\Users\Sergey\newMem\torrguard\build\dexdir\classes.dex", "classes.dex", "Optimal") | Out-Null
$apk.Dispose()
Write-Output "dex added"'

"$BT/zipalign.exe" -f 4 "$P/build/unsigned.apk" "$P/build/aligned.apk"

# keystore создается один раз и хранится — иначе подпись меняется между сборками
if [ ! -f "$P/torrguard.keystore" ]; then
"$JDK_BIN/keytool.exe" -genkeypair -keystore "$P/torrguard.keystore" -alias torrguard \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass torrguard123 -keypass torrguard123 \
  -dname "CN=TorrGuard, O=Sergey" >/dev/null 2>&1
fi

cmd //c "$(cygpath -w "$BT")\\apksigner.bat" sign \
  --ks "$(cygpath -w "$P/torrguard.keystore")" \
  --ks-key-alias torrguard \
  --ks-pass pass:torrguard123 --key-pass pass:torrguard123 \
  --out "$(cygpath -w "$P/TorrGuard.apk")" \
  "$(cygpath -w "$P/build/aligned.apk")"

ls -la "$P/TorrGuard.apk"
echo "BUILD OK"
