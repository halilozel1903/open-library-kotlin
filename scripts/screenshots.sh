#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# The sample opens each screen from the `scene` extra and loads it live from openlibrary.org, with real covers and
# author photos from covers.openlibrary.org. There is no bundled or fallback data in the app. Each scene logs
# SCENE_READY once its data and every cover on screen have loaded, or SCENE_FAILED when a request fails, and a failed
# or unfinished scene fails the run. Every capture is also checked for the expected text and for a blank image.
#
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: search, work detail, subject shelf; light and dark
#   bash scripts/screenshots.sh phone    # pixel_7: search, work detail, reading list; light and dark
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"
# Open Library can be slow under load; give each scene time before failing.
SCENE_TIMEOUT=240

# The text each scene must show; the capture fails without it. None of these appear on the loading or error screens.
expected_text() {
  case "$1" in
    search) echo "results for" ;;
    work) echo "Editions" ;;
    subject) echo "Top authors" ;;
    reading) echo "Want to read" ;;
  esac
}

suffix() {
  if [ "$1" = dark ]; then echo "-dark"; else echo ""; fi
}

install_sample
if [ "$device" = tablet ]; then
  ensure_landscape
  scenes="search work subject"
else
  scenes="search work reading"
fi

for mode in light dark; do
  set_night_mode "$mode"
  for scene in $scenes; do
    fresh_launch --es scene "$scene"
    wait_for_scene "$scene" "$SCENE_TIMEOUT"
    capture "$device-$scene$(suffix "$mode")" "$(expected_text "$scene")"
  done
done
