#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

echo "========================================="
echo "  Build JAR Package"
echo "========================================="

# Step 1: Build frontend
echo ""
echo "[1/3] Building frontend..."
cd "$PROJECT_DIR/frontend"
npm install
npm run build

# Step 2: Build backend with Maven
echo ""
echo "[2/3] Building backend..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests

# Step 3: Copy jar to release directory
echo ""
echo "[3/3] Copying jar to release..."
mkdir -p "$PROJECT_DIR/release"
cp "$PROJECT_DIR/backend/target/backend-1.0.0-jar-with-dependencies.jar" "$PROJECT_DIR/release/backend.jar"

echo ""
echo "========================================="
echo "  Build complete!"
echo "  Output: release/backend.jar"
echo "========================================="
