#!/usr/bin/env bash
# Runs the game on desktop (OpenGL ES via Mesa/LWJGL) with a scripted session.
# Usage: tools/desktop/run.sh <width> <height> "<script>"   (see DesktopLauncher for commands)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LIB="${GOGOGO_LWJGL:-$HOME/.cache/gogogo-lwjgl}"
OUT="${GOGOGO_DESKTOP_OUT:-$ROOT/build/desktop}"
V=3.3.4
mkdir -p "$LIB" "$OUT"
for a in lwjgl lwjgl-glfw lwjgl-opengles lwjgl-egl; do
  for c in "" "-natives-linux"; do
    [ "$a$c" = "lwjgl-egl-natives-linux" ] && continue
    f="$LIB/$a-$V$c.jar"
    [ -f "$f" ] || curl -sSfL --retry 5 --retry-delay 3 -o "$f" "https://repo.maven.apache.org/maven2/org/lwjgl/$a/$V/$a-$V$c.jar"
  done
done
CP="$(ls "$LIB"/*.jar | tr '\n' ':')"
rm -rf "$OUT/classes" && mkdir -p "$OUT/classes"
find "$ROOT/app/src/main/java/com/gogogo/game/engine" "$ROOT/app/src/main/java/com/gogogo/game/game" "$ROOT/tools/desktop/src" -name '*.java' > "$OUT/sources.txt"
javac -nowarn -encoding UTF-8 -d "$OUT/classes" -cp "$CP" @"$OUT/sources.txt" 2>&1 | grep -v "Picked up" || true
if [ -z "${DISPLAY:-}" ]; then
  export DISPLAY=:99
  pgrep -f "Xvfb :99" >/dev/null || (Xvfb :99 -screen 0 1600x2000x24 >/dev/null 2>&1 &) && sleep 1
fi
cd "$OUT"
java -cp "$OUT/classes:$CP" com.gogogo.desktop.DesktopLauncher "$ROOT/app/src/main/assets" "$@" 2>&1 | grep -v -E "Picked up|XDG_RUNTIME|DRI3|libEGL warning" || true
