#!/usr/bin/env bash
set -euo pipefail

PORTAUDIO_VERSION="${PORTAUDIO_VERSION:-v19.7.0}"
PORTAUDIO_URL="${PORTAUDIO_URL:-https://github.com/PortAudio/portaudio/archive/refs/tags/${PORTAUDIO_VERSION}.tar.gz}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="${BUILD_DIR:-${ROOT_DIR}/build/jportaudio-linux}"
OUTPUT_DIR="${OUTPUT_DIR:-${ROOT_DIR}/natives}"
OUTPUT="${OUTPUT_DIR}/libjportaudio.so"

if [[ -z "${JAVA_HOME:-}" ]]; then
	JAVAC="$(command -v javac || true)"
	if [[ -z "${JAVAC}" ]]; then
		echo "javac is not found. Install JDK and set JAVA_HOME." >&2
		exit 1
	fi
	if command -v readlink >/dev/null 2>&1; then
		JAVAC="$(readlink -f "${JAVAC}")"
	fi
	JAVA_HOME="$(cd "$(dirname "${JAVAC}")/.." && pwd)"
fi

if [[ ! -f "${JAVA_HOME}/include/jni.h" ]]; then
	cat >&2 <<EOF
jni.h is not found: ${JAVA_HOME}/include/jni.h

Install a JDK development package and set JAVA_HOME to the JDK root.
Examples:
  Ubuntu/Debian: sudo apt install openjdk-17-jdk build-essential libasound2-dev
                 export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
  Fedora:        sudo dnf install java-17-openjdk-devel gcc make alsa-lib-devel
                 export JAVA_HOME=/usr/lib/jvm/java-17-openjdk

Current JAVA_HOME: ${JAVA_HOME}
EOF
	exit 1
fi

if [[ ! -f "${JAVA_HOME}/include/linux/jni_md.h" ]]; then
	echo "jni_md.h is not found: ${JAVA_HOME}/include/linux/jni_md.h" >&2
	echo "JAVA_HOME must point to a Linux JDK, not a JRE or a non-Linux JDK." >&2
	exit 1
fi

mkdir -p "${BUILD_DIR}" "${OUTPUT_DIR}"
cd "${BUILD_DIR}"

TARBALL="${BUILD_DIR}/portaudio-${PORTAUDIO_VERSION}.tar.gz"
if [[ -n "${PORTAUDIO_TARBALL:-}" ]]; then
	cp "${PORTAUDIO_TARBALL}" "${TARBALL}"
elif [[ ! -f "${TARBALL}" ]]; then
	if command -v curl >/dev/null 2>&1; then
		curl -L "${PORTAUDIO_URL}" -o "${TARBALL}"
	elif command -v wget >/dev/null 2>&1; then
		wget -O "${TARBALL}" "${PORTAUDIO_URL}"
	else
		echo "curl or wget is required to download PortAudio." >&2
		exit 1
	fi
fi

rm -rf portaudio-src
mkdir portaudio-src
tar -xf "${TARBALL}" -C portaudio-src --strip-components=1

cd portaudio-src
CFLAGS="${CFLAGS:-} -fPIC -O2" ./configure \
	--with-alsa \
	--without-jack \
	--without-oss \
	--without-pulseaudio \
	--enable-static \
	--disable-shared
make -j"$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 2)"

JNI_SRC="${PWD}/bindings/java/c/src"
gcc -shared -fPIC -O2 \
	-I"${JAVA_HOME}/include" \
	-I"${JAVA_HOME}/include/linux" \
	-I"${PWD}/include" \
	"${JNI_SRC}/com_portaudio_PortAudio.c" \
	"${JNI_SRC}/com_portaudio_BlockingStream.c" \
	"${JNI_SRC}/jpa_tools.c" \
	"${PWD}/lib/.libs/libportaudio.a" \
	-lasound -lpthread -lm -lrt \
	-Wl,-soname,libjportaudio.so \
	-o "${OUTPUT}"

if command -v strip >/dev/null 2>&1; then
	strip "${OUTPUT}"
fi

echo "Built ${OUTPUT}"
if command -v ldd >/dev/null 2>&1; then
	ldd "${OUTPUT}" || true
fi
