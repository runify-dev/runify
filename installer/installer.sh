#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

ELECTRON_DIR="$SCRIPT_DIR/electron"
JRE_DIR="$ELECTRON_DIR/jre"
JAR_NAME="runify.jar"
JAVA_VERSION="25"

# Parse arguments
JRE_PATH=""
PLATFORM="auto"

while [[ $# -gt 0 ]]; do
  case $1 in
    --jre-path)
      JRE_PATH="$2"
      shift 2
      ;;
    mac|win)
      PLATFORM="$1"
      shift
      ;;
    *)
      echo "Usage: ./installer/installer.sh [--jre-path /path/to/jdk] [mac|win]"
      exit 1
      ;;
  esac
done

if [ "$PLATFORM" = "auto" ]; then
  case "$(uname -s)" in
    Darwin*) PLATFORM="mac" ;;
    MINGW*|MSYS*|CYGWIN*) PLATFORM="win" ;;
    *) echo "Unsupported platform"; exit 1 ;;
  esac
fi

echo "========================================="
echo "  Run Desktop Installer Builder"
echo "  Platform: $PLATFORM"
echo "========================================="

# Step 1: Maven build (frontend + backend)
echo ""
echo "[1/5] Building project..."
mvn clean package -DskipTests
cp "$PROJECT_DIR/backend/target/$JAR_NAME" "$ELECTRON_DIR/$JAR_NAME"
echo "  -> JAR copied to electron/$JAR_NAME"

# Step 2: Prepare JRE
echo ""
echo "[2/5] Preparing JDK runtime..."

if [ -n "$JRE_PATH" ]; then
  # Use provided JRE path
  if [ ! -d "$JRE_PATH" ]; then
    echo "  Error: JRE path not found: $JRE_PATH"
    exit 1
  fi
  rm -rf "$JRE_DIR"
  cp -R "$JRE_PATH" "$JRE_DIR"
  echo "  -> JRE copied from $JRE_PATH"

elif [ -d "$JRE_DIR" ] && [ -n "$(ls -A "$JRE_DIR" 2>/dev/null)" ]; then
  # Reuse existing JRE
  echo "  -> JDK already exists at $JRE_DIR, skipping download"

else
  hash="bd75d5f9689641da8e1daabeccb5528b"
  base="https://download.java.net/java/GA/jdk${JAVA_VERSION}/${hash}/36/GPL"
  tmp_file=""
  url=""

  case "$PLATFORM" in
    mac)
      arch=$(uname -m)
      if [ "$arch" = "arm64" ]; then
        url="${base}/openjdk-${JAVA_VERSION}_macos-aarch64_bin.tar.gz"
      else
        url="${base}/openjdk-${JAVA_VERSION}_macos-x64_bin.tar.gz"
      fi
      tmp_file="$SCRIPT_DIR/.jdk-mac.tar.gz"

      echo "  Downloading OpenJDK ${JAVA_VERSION} for mac (${arch})..."
      echo "  URL: $url"
      curl -L --connect-timeout 10 --max-time 600 -o "$tmp_file" "$url"

      echo "  Extracting..."
      rm -rf "$JRE_DIR"
      mkdir -p "$JRE_DIR"
      tar -xzf "$tmp_file" -C "$JRE_DIR"
      # Flatten nested JDK directory (e.g. jre/jdk-25.jdk/Contents -> jre/Contents)
      nested=$(find "$JRE_DIR" -maxdepth 1 -type d -name "jdk-*" | head -1)
      if [ -n "$nested" ]; then
        mv "$nested"/* "$JRE_DIR"/
        rmdir "$nested"
      fi
      rm -f "$tmp_file"
      ;;
    win)
      url="${base}/openjdk-${JAVA_VERSION}_windows-x64_bin.zip"
      tmp_file="$SCRIPT_DIR/.jdk-win.zip"

      echo "  Downloading OpenJDK ${JAVA_VERSION} for windows..."
      echo "  URL: $url"
      curl -L --connect-timeout 10 --max-time 600 -o "$tmp_file" "$url"

      echo "  Extracting..."
      rm -rf "$JRE_DIR"
      mkdir -p "$JRE_DIR"
      unzip -q "$tmp_file" -d "$JRE_DIR"
      inner=$(ls -d "$JRE_DIR"/*/ 2>/dev/null | head -1)
      if [ -n "$inner" ]; then
        mv "$inner"/* "$JRE_DIR"/
        rmdir "$inner"
      fi
      rm -f "$tmp_file"
      ;;
  esac
fi

echo "  -> JDK runtime ready at $JRE_DIR"

# Step 3: Install Electron dependencies
echo ""
echo "[3/6] Installing Electron dependencies..."
cd "$ELECTRON_DIR"
npm install

# Step 4: Convert icon for Windows
if [ "$PLATFORM" = "win" ]; then
  echo ""
  echo "[4/6] Converting icon to ICO..."
  pip install Pillow -q
  python3 -c "
from PIL import Image
img = Image.open('$PROJECT_DIR/frontend/public/favicon.ico')
sizes = [(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)]
img.save('$ELECTRON_DIR/icon.ico', format='ICO', sizes=sizes)
"
  echo "  -> icon.ico created"
fi

# Step 5: Build Electron app
echo ""
echo "[5/6] Building Electron app..."
case "$PLATFORM" in
  mac)
    npm run build:mac
    echo "  Signing app..."
    find dist -name "*.app" -exec codesign --force --deep --sign - {} \;
    ;;
  win)
    npm run build:win
    ;;
esac

# Step 6: Copy jar to release directory
echo ""
echo "[6/6] Copying jar to release..."
mkdir -p "$PROJECT_DIR/release"
cp "$ELECTRON_DIR/$JAR_NAME" "$PROJECT_DIR/release/$JAR_NAME"

echo ""
echo "========================================="
echo "  Build complete!"
echo "  Desktop: installer/electron/dist/"
echo "  JAR: release/$JAR_NAME"
echo "========================================="
