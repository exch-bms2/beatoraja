#!/usr/bin/env bash
# Record a JFR profile of the running beatoraja game process (not the launcher).
# Usage: scripts/lua-profile-jfr.sh [seconds=60] [output=luadiag/profile-<time>.jfr]
# Start it once the screen to measure (select/play/result) is showing and warmed up.
set -euo pipefail
seconds="${1:-60}"
out="${2:-luadiag/profile-$(date +%Y%m%d-%H%M%S).jfr}"
jcmd="${JAVA_HOME:+$JAVA_HOME/bin/}jcmd"
pid=$("$jcmd" -l | awk '/beatoraja/ && /gameProcess=true|DesktopLauncher/ {print $1}' | while read -r p; do
  if "$jcmd" "$p" VM.system_properties 2>/dev/null | grep -q '^beatoraja.gameProcess=true'; then echo "$p"; fi
done | head -n1)
if [ -z "$pid" ]; then echo "beatoraja game process not found (jcmd -l)" >&2; exit 1; fi
mkdir -p "$(dirname "$out")"
abs="$(cd "$(dirname "$out")" && pwd)/$(basename "$out")"
"$jcmd" "$pid" JFR.start name=luaprofile settings=profile duration="${seconds}s" filename="$abs"
echo "recording pid $pid for ${seconds}s -> $abs"
sleep "$seconds"; sleep 2
echo "done: python3 scripts/jfr-hot.py \"$abs\""
