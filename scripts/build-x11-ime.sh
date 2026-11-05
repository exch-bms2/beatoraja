#!/usr/bin/env bash
set -euo pipefail

# Separate output; the stock Maven native and Ant libraries are never overwritten.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REVISION=d9d6f0f1f967807ffade6598ea9a631ebaf37a56
OUT="${1:-$ROOT/build-gradle/generated/x11-ime}"
JAVA_HOME="${JAVA_HOME:?Set JAVA_HOME to a Java 17 JDK}"
if [[ "$(uname -s)" != Linux ]]; then
    echo 'X11 IME native build requires Linux and X11 development headers.' >&2
    exit 1
fi
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
WORK="$(mktemp -d "$OUT/source.XXXXXXXX")"
trap 'rm -rf "$WORK"' EXIT
if [[ -n "${GLFW_X11_IME_ARCHIVE:-}" ]]; then
    cp "$GLFW_X11_IME_ARCHIVE" "$WORK/glfw.tar.gz"
else
    curl -fL --connect-timeout 15 --max-time 120 \
        "https://codeload.github.com/glfw/glfw/tar.gz/$REVISION" -o "$WORK/glfw.tar.gz"
fi
# This is the source revision recorded inside lwjgl-glfw-3.4.3-natives-linux.jar.
echo '96722514789b1aa1ad9c4debd874450156ea457aea691e546ffaf5b53892d5a3  '"$WORK/glfw.tar.gz" | sha256sum --check --status
tar -xzf "$WORK/glfw.tar.gz" -C "$WORK"
SOURCE="$WORK/glfw-$REVISION"
python3 "$ROOT/scripts/prepare-x11-ime.py" "$SOURCE" "$ROOT/native/linux/x11-ime.c"
cmake -S "$SOURCE" -B "$WORK/build" -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON \
    -DGLFW_BUILD_X11=ON -DGLFW_BUILD_WAYLAND=OFF -DGLFW_BUILD_EXAMPLES=OFF \
    -DGLFW_BUILD_TESTS=OFF -DGLFW_BUILD_DOCS=OFF \
    "-DCMAKE_C_FLAGS=-I\"$JAVA_HOME/include\" -I\"$JAVA_HOME/include/linux\""
cmake --build "$WORK/build" --parallel "${BUILD_JOBS:-2}"
cp -L "$WORK/build/src/libglfw.so" "$OUT/libglfw-beatoraja-x11-ime.so"
cp "$SOURCE/LICENSE.md" "$OUT/GLFW-LICENSE.md"
printf '%s\n' "$REVISION (altered: opt-in beatoraja X11 IME)" > "$OUT/GLFW-REVISION.txt"
echo "Built $OUT/libglfw-beatoraja-x11-ime.so"
