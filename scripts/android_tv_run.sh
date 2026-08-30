#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DASHBOARD_DIR="$PROJECT_ROOT/dashboard"
APK_PATH="$PROJECT_ROOT/tv/build/outputs/apk/debug/tv-debug.apk"
APP_ID="com.dwplayer"
MAIN_ACTIVITY="com.dwplayer.MainActivity"
APP_PORT="8200"
REQUESTED_AVD="${1:-Television_4K}"
AVD_NAME=""

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  if [ -x "/opt/homebrew/opt/openjdk@17/bin/java" ]; then
    JAVA_HOME="/opt/homebrew/opt/openjdk@17"
  elif [ -x "/usr/local/opt/openjdk@17/bin/java" ]; then
    JAVA_HOME="/usr/local/opt/openjdk@17"
  elif [ -x "/Users/espitman/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]; then
    JAVA_HOME="/Users/espitman/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  else
    echo "Error: no usable Java 17 runtime found. Install OpenJDK 17 or set JAVA_HOME."
    exit 1
  fi
fi
PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"
GRADLE_OPTS="${GRADLE_OPTS:-} --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED"
export ANDROID_HOME ANDROID_SDK_ROOT JAVA_HOME GRADLE_OPTS

ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"
EMU_BIN="$ANDROID_SDK_ROOT/emulator/emulator"

echo "Starting Android TV debug build and launch for dwPlayer..."
echo "Using JAVA_HOME=$JAVA_HOME"

if [ ! -x "$ADB_BIN" ]; then
  echo "Error: adb not found at $ADB_BIN"
  exit 1
fi

if [ ! -x "$EMU_BIN" ]; then
  echo "Error: emulator not found at $EMU_BIN"
  exit 1
fi

if [ ! -x "$PROJECT_ROOT/gradlew" ]; then
  echo "Error: gradlew not found at $PROJECT_ROOT/gradlew"
  exit 1
fi

SERIAL="$($ADB_BIN devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')"

if [ -z "$SERIAL" ]; then
  AVAILABLE_AVDS="$("$EMU_BIN" -list-avds || true)"
  if [ -z "$AVAILABLE_AVDS" ]; then
    echo "Error: no Android AVD found. Create a TV AVD first."
    exit 1
  fi

  if echo "$AVAILABLE_AVDS" | grep -Fxq "$REQUESTED_AVD"; then
    AVD_NAME="$REQUESTED_AVD"
  else
    AVD_NAME="$(echo "$AVAILABLE_AVDS" | grep -Ei 'google-tv|android tv|tv|television' | head -1 || true)"
    if [ -z "$AVD_NAME" ]; then
      AVD_NAME="$(echo "$AVAILABLE_AVDS" | head -1)"
    fi
    echo "Requested AVD '$REQUESTED_AVD' not found. Using '$AVD_NAME'."
  fi

  echo "No online emulator found. Booting $AVD_NAME..."
  nohup "$EMU_BIN" -avd "$AVD_NAME" -gpu swiftshader_indirect -no-snapshot-load -no-boot-anim >/tmp/dwplayer_android_tv_emulator.log 2>&1 &

  for _ in $(seq 1 180); do
    SERIAL="$($ADB_BIN devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')"
    if [ -n "$SERIAL" ]; then
      break
    fi
    sleep 2
  done
fi

if [ -z "${SERIAL:-}" ]; then
  echo "Error: emulator did not come online in time."
  "$ADB_BIN" devices -l || true
  exit 1
fi

echo "Target device: $SERIAL"
echo "Waiting for boot completion..."
"$ADB_BIN" -s "$SERIAL" wait-for-device
for _ in $(seq 1 120); do
  BOOT="$("$ADB_BIN" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  if [ "$BOOT" = "1" ]; then
    break
  fi
  sleep 2
done

HOST_TIME_MS="$(($(date +%s) * 1000))"
"$ADB_BIN" -s "$SERIAL" shell cmd alarm set-time "$HOST_TIME_MS" >/dev/null 2>&1 || true

if [ -d "$DASHBOARD_DIR" ] && [ -f "$DASHBOARD_DIR/package.json" ]; then
  echo "Checking embedded dashboard..."
  (
    cd "$DASHBOARD_DIR"
    if [ ! -d "$DASHBOARD_DIR/node_modules" ]; then
      npm install --no-audit 2>/dev/null || true
    fi
    npm run build 2>/dev/null || true
  ) || true
fi

echo "Building Android TV debug APK..."
cd "$PROJECT_ROOT"
./gradlew --no-daemon :tv:assembleDebug

if [ ! -f "$APK_PATH" ]; then
  echo "Error: debug APK not found at $APK_PATH"
  exit 1
fi

echo "Installing debug APK..."
"$ADB_BIN" -s "$SERIAL" install -r -t "$APK_PATH"

echo "Launching $APP_ID/$MAIN_ACTIVITY..."
"$ADB_BIN" -s "$SERIAL" shell am start -W -n "$APP_ID/$MAIN_ACTIVITY"

echo "Forwarding host port $APP_PORT to device port $APP_PORT..."
"$ADB_BIN" -s "$SERIAL" forward "tcp:$APP_PORT" "tcp:$APP_PORT"

echo "Android TV debug launch succeeded!"
echo "dwPlayer Web Companion available at http://127.0.0.1:$APP_PORT"
