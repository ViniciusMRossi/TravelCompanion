#!/usr/bin/env sh
set -eu

GRADLE_VERSION="8.13"
BOOTSTRAP_ROOT="${GRADLE_USER_HOME:-$HOME/.gradle}/tc-bootstrap"
GRADLE_HOME="$BOOTSTRAP_ROOT/gradle-$GRADLE_VERSION"
ZIP="$BOOTSTRAP_ROOT/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BOOTSTRAP_ROOT"
  echo "Bootstrapping Gradle $GRADLE_VERSION from $URL"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$URL"
  else
    echo "ERROR: curl or wget is required for the first bootstrap." >&2
    exit 1
  fi
  if ! command -v unzip >/dev/null 2>&1; then
    echo "ERROR: unzip is required for the first bootstrap." >&2
    exit 1
  fi
  rm -rf "$GRADLE_HOME"
  unzip -q "$ZIP" -d "$BOOTSTRAP_ROOT"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
