#!/usr/bin/env sh
set -eu
echo "Travel Companion bootstrap"
python3 tools/check_repo.py
if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
.venv/bin/python -m pip install -q -r tools/requirements.txt
.venv/bin/python tools/validate_trip.py app/src/main/assets/trip/trip.json
echo
echo "Next:"
echo "  ./gradlew tasks"
echo "  ./gradlew test"
echo "  ./gradlew assembleDebug"
