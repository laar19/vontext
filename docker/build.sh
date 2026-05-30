#!/bin/bash
# Script de build para VideoContextBot Android
# Se ejecuta dentro del contenedor Docker

set -e

echo "========================================="
echo "  VideoContextBot Android Build"
echo "========================================="
echo ""
echo "Android SDK: $ANDROID_HOME"
echo "Android NDK: $ANDROID_NDK_HOME"
echo "Gradle: $(gradle --version 2>&1 | grep 'Gradle' | head -1)"
echo "CMake: $(cmake --version | head -1)"
echo ""

# Verificar que existe el proyecto
if [ ! -f "build.gradle.kts" ]; then
    echo "ERROR: build.gradle.kts no encontrado"
    echo "Asegúrate de montar el volumen correctamente"
    exit 1
fi

# Limpiar build anterior
echo "[1/5] Cleaning previous build..."
gradle clean || {
    echo "Warning: gradle clean failed, continuing..."
}

# Configurar local.properties
echo "[2/5] Configuring local.properties..."
cat > local.properties << EOF
sdk.dir=$ANDROID_HOME
ndk.dir=$ANDROID_NDK_HOME
cmake.dir=$ANDROID_HOME/cmake/3.22.1
EOF

# Build de librerías nativas (whisper.cpp)
echo "[3/5] Building native libraries (whisper.cpp)..."
if [ -d "app/src/main/cpp/whisper" ]; then
    cd app/src/main/cpp/whisper
    git pull || true
    make clean || true
    make -j$(nproc) || {
        echo "Warning: whisper.cpp build failed, continuing..."
    }
    cd ../../../..
else
    echo "whisper.cpp submodule not found, skipping native build"
    echo "Run: git submodule update --init --recursive"
fi

# Build debug APK
echo "[4/5] Building Debug APK..."
gradle assembleDebug --no-daemon --stacktrace || {
    echo "ERROR: Debug build failed"
    exit 1
}

# Build release APK (sin signing)
echo "[5/5] Building Release APK..."
gradle assembleRelease --no-daemon --stacktrace || {
    echo "Warning: Release build failed (may need signing config)"
}

# Crear directorio de output
echo ""
echo "Copying APKs to output directory..."
mkdir -p /output

# Copiar APKs
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    cp app/build/outputs/apk/debug/app-debug.apk /output/VideoContextBot-debug.apk
    echo "✓ Debug APK: /output/VideoContextBot-debug.apk"
fi

if [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    cp app/build/outputs/apk/release/app-release-unsigned.apk /output/VideoContextBot-release-unsigned.apk
    echo "✓ Release APK: /output/VideoContextBot-release-unsigned.apk"
elif [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    cp app/build/outputs/apk/release/app-release.apk /output/VideoContextBot-release.apk
    echo "✓ Release APK: /output/VideoContextBot-release.apk"
fi

# Mostrar información de los APKs
echo ""
echo "========================================="
echo "  Build Complete!"
echo "========================================="
echo ""
echo "APKs disponibles en /output/:"
ls -lh /output/*.apk 2>/dev/null || echo "No APKs found"
echo ""
echo "Para instalar en dispositivo:"
echo "  adb install /output/VideoContextBot-debug.apk"
echo ""
