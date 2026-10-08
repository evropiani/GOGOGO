#!/usr/bin/env bash
# Builds a signed release APK without Gradle/AGP, using plain SDK tools:
#   javac, d8 (or dx), aapt2, zipalign, apksigner.
# Tools are looked up in $ANDROID_HOME/build-tools/* first, then on PATH (Debian's
# android-sdk packages work: aapt, dalvik-exchange, zipalign, apksigner, android-sdk-platform-23).
#
# Usage: tools/build-apk.sh [output.apk]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/app/src/main"
OUT_APK="${1:-$ROOT/build/GOGOGO-release.apk}"
B="$ROOT/build/apk"
PKG=com.gogogo.game
VERSION_CODE="${VERSION_CODE:-$(grep -E '^versionCode=' "$ROOT/version.properties" | cut -d= -f2)}"
VERSION_NAME="${VERSION_NAME:-$(grep -E '^versionName=' "$ROOT/version.properties" | cut -d= -f2)}"
MIN_SDK=24
TARGET_SDK=35

find_tool() {
  local name="$1"
  if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/build-tools" ]; then
    local t
    t="$(ls -d "$ANDROID_HOME"/build-tools/*/ 2>/dev/null | sort -V | tail -1)"
    if [ -n "$t" ] && [ -x "$t/$name" ]; then echo "$t/$name"; return; fi
  fi
  command -v "$name" || true
}

ANDROID_JAR="${ANDROID_JAR:-}"
if [ -z "$ANDROID_JAR" ]; then
  if [ -n "${ANDROID_HOME:-}" ] && ls "$ANDROID_HOME"/platforms/android-*/android.jar >/dev/null 2>&1; then
    ANDROID_JAR="$(ls "$ANDROID_HOME"/platforms/android-*/android.jar | sort -V | tail -1)"
  else
    ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
  fi
fi
AAPT2="$(find_tool aapt2)"
ZIPALIGN="$(find_tool zipalign)"
APKSIGNER="$(find_tool apksigner)"
D8="$(find_tool d8)"
DX="$(command -v dalvik-exchange || find_tool dx)"
for t in "$ANDROID_JAR" "$AAPT2" "$ZIPALIGN" "$APKSIGNER"; do
  [ -n "$t" ] && [ -e "$t" ] || { echo "missing build tool ($t)"; exit 1; }
done
echo "android.jar: $ANDROID_JAR"

rm -rf "$B" && mkdir -p "$B/res" "$B/gen" "$B/classes" "$B/dex"

# 1. manifest with package + sdk levels injected (the source manifest is AGP-style)
sed -e "s#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"$PKG\" android:versionCode=\"$VERSION_CODE\" android:versionName=\"$VERSION_NAME\">\n    <uses-sdk android:minSdkVersion=\"$MIN_SDK\" android:targetSdkVersion=\"$TARGET_SDK\" />#" \
  "$APP/AndroidManifest.xml" > "$B/AndroidManifest.xml"

# 2. resources + assets
"$AAPT2" compile --dir "$APP/res" -o "$B/res/compiled.zip"
"$AAPT2" link -I "$ANDROID_JAR" --manifest "$B/AndroidManifest.xml" -A "$APP/assets" \
  --java "$B/gen" -0 ogg -0 png -o "$B/base.apk" "$B/res/compiled.zip" \
  --min-sdk-version $MIN_SDK --target-sdk-version $TARGET_SDK

# 3. compile Java (Java 8 bytecode, no lambdas so dx can also be used)
find "$APP/java" "$B/gen" -name '*.java' > "$B/sources.txt"
javac -nowarn -encoding UTF-8 -source 8 -target 8 -Xlint:-options -bootclasspath "$ANDROID_JAR" \
  -d "$B/classes" @"$B/sources.txt" 2>&1 | grep -v "Picked up" || true
[ -f "$B/classes/com/gogogo/game/android/GoActivity.class" ] || { echo "javac failed"; exit 1; }

# 4. dex
if [ -n "$D8" ]; then
  "$D8" --release --min-api $MIN_SDK --lib "$ANDROID_JAR" --output "$B/dex" $(find "$B/classes" -name '*.class')
else
  "$DX" --dex --min-sdk-version=$MIN_SDK --output="$B/dex/classes.dex" "$B/classes" 2>&1 | grep -v "Picked up" || true
fi
[ -f "$B/dex/classes.dex" ] || { echo "dexing failed"; exit 1; }

# 5. package, align, sign
cp "$B/base.apk" "$B/unsigned.apk"
(cd "$B/dex" && zip -q -X "$B/unsigned.apk" classes.dex)
"$ZIPALIGN" -f -p 4 "$B/unsigned.apk" "$B/aligned.apk"
KS_PROPS="$ROOT/keystore/keystore.properties"
prop() { grep -E "^$1=" "$KS_PROPS" | cut -d= -f2-; }
mkdir -p "$(dirname "$OUT_APK")"
"$APKSIGNER" sign --ks "$ROOT/keystore/$(basename "$(prop storeFile)")" --ks-pass "pass:$(prop storePassword)" \
  --ks-key-alias "$(prop keyAlias)" --key-pass "pass:$(prop keyPassword)" --out "$OUT_APK" "$B/aligned.apk" 2>&1 | grep -v "Picked up" || true
"$APKSIGNER" verify "$OUT_APK" 2>&1 | grep -v "Picked up" || true
echo "built $OUT_APK ($(du -h "$OUT_APK" | cut -f1))"
