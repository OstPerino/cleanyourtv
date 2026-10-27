#!/usr/bin/env bash
# Usage: TV_HOST=127.0.0.1 TV_PORT=15555 custom/backdrop.sh <video file>   # any format ffmpeg reads
#        TV_HOST=... custom/backdrop.sh --remove
set -euo pipefail
cd "$(dirname "$0")/.."
: "${TV_HOST:?set TV_HOST (and TV_PORT when using tools/relay.py)}"
PY=tools/venv/bin/python
URI=content://com.gathin.tvhome.theme/backdrop.mp4
STAGE=/data/local/tmp/cyt-theme-backdrop.mp4
tv() { "$PY" -I tools/tv.py "$@"; }

if [ "${1:-}" = "--remove" ]; then
  tv "content delete --uri $URI; am force-stop com.gathin.tvhome; input keyevent HOME" >/dev/null
  echo "backdrop video removed; Home falls back to the light animation"
  exit 0
fi

SRC="${1:?usage: custom/backdrop.sh <video file> | --remove}"
[ -f "$SRC" ] || { echo "no such file: $SRC" >&2; exit 2; }
OUT="$(mktemp -d)/backdrop.mp4"
ffmpeg -hide_banner -loglevel error -y -i "$SRC" -an -t 60 \
  -vf "scale=1920:1080:force_original_aspect_ratio=increase,crop=1920:1080,fps=30,format=yuv420p" \
  -c:v libx264 -profile:v high -level 4.1 -preset slow -crf 22 -movflags +faststart "$OUT"
echo "encoded $(du -h "$OUT" | cut -f1)"
"$PY" -I tools/b64push.py "$OUT" "$STAGE"
tv "content write --uri $URI < $STAGE; rm -f $STAGE; am force-stop com.gathin.tvhome; input keyevent HOME" >/dev/null
rm -f "$OUT"
echo "backdrop video on the TV; switch it on in Launcher settings > Фон if it is set to animation or off"
