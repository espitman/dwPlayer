#!/usr/bin/env bash
set -euo pipefail

PHONE_AVD="Medium_Phone_API_36.0"
TV_AVD="Television_4K"

TARGET="${1:-tv}"

case "$TARGET" in
  phone)
    AVD_NAME="$PHONE_AVD"
    ;;
  tv)
    AVD_NAME="$TV_AVD"
    ;;
  *)
    AVD_NAME="$TARGET"
    ;;
esac

EMULATOR_BIN="${ANDROID_HOME:-$HOME/Library/Android/sdk}/emulator/emulator"

echo "Starting emulator for: $AVD_NAME..."
exec "$EMULATOR_BIN" -avd "$AVD_NAME"
