#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DASHBOARD_DIR="$PROJECT_ROOT/dashboard"
APK_PATH="$PROJECT_ROOT/tv/build/outputs/apk/release/tv-release.apk"
DESKTOP_DIR="$HOME/Desktop"
OUT_APK="$DESKTOP_DIR/dwplayer-release.apk"

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
PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
GRADLE_OPTS="${GRADLE_OPTS:-} --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED"
export ANDROID_HOME ANDROID_SDK_ROOT JAVA_HOME GRADLE_OPTS

echo "Building signed Android TV release APK for dwPlayer..."
echo "Using JAVA_HOME=$JAVA_HOME"

if [ ! -x "$PROJECT_ROOT/gradlew" ]; then
  echo "Error: gradlew not found at $PROJECT_ROOT/gradlew"
  exit 1
fi

if [ ! -f "$PROJECT_ROOT/tv/dwplayer.jks" ]; then
  echo "Generating release keystore at $PROJECT_ROOT/tv/dwplayer.jks..."
  keytool -genkeypair -v -keystore "$PROJECT_ROOT/tv/dwplayer.jks" \
    -alias dwplayer -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass dwplayer123 -keypass dwplayer123 \
    -dname "CN=dwPlayer, OU=Android, O=dwPlayer, L=Tehran, ST=Tehran, C=IR"
fi

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

BUILD_TOOLS_DIR="$(find "$ANDROID_SDK_ROOT/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort -V | tail -1)"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"

if [ ! -x "${APKSIGNER:-}" ]; then
  echo "Error: apksigner not found under $ANDROID_SDK_ROOT/build-tools"
  exit 1
fi

echo "Building release APK..."
cd "$PROJECT_ROOT"
./gradlew --no-daemon :tv:assembleRelease

if [ ! -f "$APK_PATH" ]; then
  APK_PATH="$(find "$PROJECT_ROOT/tv/build/outputs/apk/release" -name "*.apk" -type f | sort | tail -1)"
fi

if [ -z "${APK_PATH:-}" ] || [ ! -f "$APK_PATH" ]; then
  echo "Error: release APK not found."
  exit 1
fi

echo "Verifying APK signature..."
"$APKSIGNER" verify --verbose --print-certs "$APK_PATH" >/tmp/dwplayer-tv-release-verify.txt
cat /tmp/dwplayer-tv-release-verify.txt

cp "$APK_PATH" "$OUT_APK"
echo "Release APK copied to $OUT_APK"

echo "Release build completed successfully!"
