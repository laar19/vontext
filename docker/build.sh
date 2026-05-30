#!/bin/bash
# Script de build para VideoContextBot Android
# Se ejecuta dentro del contenedor Docker

set -e

PROJECT_DIR="/project"
OUTPUT_DIR="/output"
WHISPER_DIR="$PROJECT_DIR/app/src/main/cpp/whisper"

echo "========================================="
echo "  VideoContextBot Android Build"
echo "========================================="
echo ""
echo "Android SDK: $ANDROID_HOME"
echo "Android NDK: $ANDROID_NDK_HOME"
echo "Gradle: $(gradle --version 2>&1 | grep 'Gradle' | head -1)"
echo "CMake: $(cmake --version | head -1)"
echo "Project: $PROJECT_DIR"
echo ""

# Verificar que existe el proyecto
if [ ! -f "$PROJECT_DIR/build.gradle.kts" ]; then
    echo "ERROR: build.gradle.kts no encontrado en $PROJECT_DIR"
    ls -la "$PROJECT_DIR" || true
    exit 1
fi

# Siempre trabajar desde el directorio del proyecto
cd "$PROJECT_DIR"
pwd

# Limpiar build anterior
echo "[1/5] Cleaning previous build..."
gradle clean --no-build-cache || {
    echo "Warning: gradle clean failed, continuing..."
}

# Configurar local.properties
echo "[2/5] Configuring local.properties..."
cat > "$PROJECT_DIR/local.properties" << EOF
sdk.dir=$ANDROID_HOME
ndk.dir=$ANDROID_NDK_HOME
cmake.dir=$ANDROID_HOME/cmake/3.22.1
EOF

# Build de librerías nativas (whisper.cpp)
echo "[3/5] Building native libraries (whisper.cpp)..."
if [ -d "$WHISPER_DIR" ]; then
    echo "Building whisper.cpp in $WHISPER_DIR"
    git config --global --add safe.directory "$WHISPER_DIR" 2>/dev/null || true
    cd "$WHISPER_DIR"
    git checkout v1.7.5 || true
    make clean || true
    make -j$(nproc) || {
        echo "Warning: whisper.cpp build failed, continuing..."
    }
    # Volver al directorio del proyecto
    cd "$PROJECT_DIR"
    echo "Back to project dir: $(pwd)"
else
    echo "whisper.cpp submodule not found, skipping native build"
    echo "Run: git submodule update --init --recursive"
fi

# Build debug APK
echo "[4/5] Building Debug APK..."
cd "$PROJECT_DIR"
echo "Current dir: $(pwd)"
gradle assembleDebug --no-daemon --stacktrace || {
    echo "ERROR: Debug build failed"
    exit 1
}

# Build release APK (sin signing)
echo "[5/5] Building Release APK..."
cd "$PROJECT_DIR"
gradle assembleRelease --no-daemon --stacktrace || {
    echo "Warning: Release build failed (may need signing config)"
}

# Crear directorio de output
echo ""
echo "Copying APKs to output directory..."
mkdir -p "$OUTPUT_DIR"

# Copiar APKs
if [ -f "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" ]; then
    cp "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" "$OUTPUT_DIR/VideoContextBot-debug.apk"
    echo "✓ Debug APK: $OUTPUT_DIR/VideoContextBot-debug.apk"
fi

if [ -f "$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    cp "$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk" "$OUTPUT_DIR/VideoContextBot-release-unsigned.apk"
    echo "✓ Release APK: $OUTPUT_DIR/VideoContextBot-release-unsigned.apk"
elif [ -f "$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk" ]; then
    cp "$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk" "$OUTPUT_DIR/VideoContextBot-release.apk"
    echo "✓ Release APK: $OUTPUT_DIR/VideoContextBot-release.apk"
fi

# Mostrar información de los APKs
echo ""
echo "========================================="
echo "  Build Complete!"
echo "========================================="
echo ""
echo "APKs disponibles en $OUTPUT_DIR:"
ls -lh "$OUTPUT_DIR"/*.apk 2>/dev/null || echo "No APKs found"
echo ""
echo "Para instalar en dispositivo:"
echo "  adb install $OUTPUT_DIR/VideoContextBot-debug.apk"
echo ""
