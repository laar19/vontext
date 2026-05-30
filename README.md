# VideoContextBot Android

Aplicación Android nativa que procesa videos de grabaciones de pantalla para generar contexto enriquecido para agentes de IA. **100% local** con opción de API remota.

[![Build Status](https://github.com/usuario/VideoContextBotAndroid/actions/workflows/build.yml/badge.svg)](https://github.com/usuario/VideoContextBotAndroid/actions)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-26+-green.svg)](https://developer.android.com/)

## Características

✅ **Procesamiento 100% local** - Sin dependencia de servidores externos
✅ **Modelos Whisper locales** - Tiny, Base, Small, Medium (offline)
✅ **API remota opcional** - OpenAI, Groq, DeepSeek
✅ **Detección de escenas** - OpenCV Android
✅ **Generación de PDF** - Nativo con PdfDocument
✅ **Exportación ZIP** - Todos los archivos de output
✅ **Background processing** - WorkManager + Foreground Service
✅ **Construcción reproducible** - Docker build container

## Capturas

*(Espacio para capturas de la app)*

## Requisitos

- Android 8.0 (API 26) o superior
- Mínimo 4GB RAM recomendado (para modelos locales)
- Espacio libre: 500MB - 4GB (dependiendo del modelo Whisper)

## Modelos de Transcripción

### Locales (Offline)

| Modelo | Tamaño | RAM | Velocidad | Precisión |
|--------|--------|-----|-----------|-----------|
| Tiny | 75 MB | ~300 MB | ~0.5x realtime | Baja |
| Base | 142 MB | ~500 MB | ~0.3x realtime | Media |
| Small | 466 MB | ~1.2 GB | ~0.2x realtime | Buena |
| Medium | 1.5 GB | ~3.5 GB | ~0.1x realtime | Excelente |

### Remotos (API)

- OpenAI Whisper
- Groq (gratis, limitado)
- DeepSeek
- Ollama (self-hosted)

## Instalación

### Opción A: Descargar APK

```bash
# Ir a Releases y descargar el APK
adb install VideoContextBot-debug.apk
```

### Opción B: Construir con Docker

```bash
# Clonar el repositorio
git clone https://github.com/usuario/VideoContextBotAndroid.git
cd VideoContextBotAndroid

# Inicializar submódulos (whisper.cpp)
git submodule update --init --recursive

# Construir con Docker
cd docker
docker-compose -f docker-compose.build.yml build
docker-compose -f docker-compose.build.yml up

# APKs en docker/output/
```

### Opción C: Construir con Android Studio

1. Abrir proyecto en Android Studio
2. Sync Gradle
3. Build → Build Bundle(s) / APK(s) → Build APK(s)

## Uso

1. **Seleccionar video** - Toca el área de video picker
2. **Configurar** - Añade notas opcionales y ajusta intervalo de frames
3. **Procesar** - Presiona "Procesar Video"
4. **Esperar** - Verás el progreso en tiempo real
5. **Resultados** - Descarga PDF y ZIP

## Configuración

### Modo de Transcripción

Ve a Settings → Whisper Mode:

- **Local**: Modelos offline (recomendado para privacidad)
- **Remoto**: API externa (más rápido, requiere internet)

### API Keys (para modo remoto)

```
Settings → API Configuration
- API Key: tu-api-key
- Base URL: https://api.groq.com/openai/v1 (ejemplo)
```

## Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/videocontextbot/
│   │   ├── ui/           # Jetpack Compose UI
│   │   ├── viewmodel/    # ViewModels
│   │   ├── data/         # Repositorios, Room, API
│   │   ├── domain/       # Modelos y Use Cases
│   │   ├── processor/    # Procesamiento de video
│   │   └── worker/       # WorkManager
│   ├── cpp/              # whisper.cpp (JNI)
│   └── res/              # Recursos Android
├── build.gradle.kts
└── proguard-rules.pro
docker/
├── Dockerfile.build      # Imagen de build
├── build.sh              # Script de build
└── docker-compose.build.yml
```

## Desarrollo

### Prerrequisitos

- Android Studio Hedgehog o superior
- JDK 17
- Android SDK 35
- NDK 26.1
- CMake 3.22

### Comandos útiles

```bash
# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Tests unitarios
./gradlew test

# Tests de UI
./gradlew connectedAndroidTest

# Limpiar
./gradlew clean

# Instalar en dispositivo
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Agregar nuevos modelos

Los modelos se descargan automáticamente la primera vez. Para pre-cargar:

```bash
# Colocar en app/src/main/assets/models/
# Usar Git LFS para archivos grandes
git lfs track "*.bin"
```

## Testing

```kotlin
// Ejemplo de test unitario
@Test
fun `extract frames by interval returns correct count`() {
    val frames = frameExtractor.extractByInterval(videoPath, 10)
    assertTrue(frames.size in 10..50)
}
```

## Licencia

MIT License - ver [LICENSE](LICENSE) para detalles.

## Créditos

- [whisper.cpp](https://github.com/ggerganov/whisper.cpp) - Inferencia local de Whisper
- [OpenCV](https://opencv.org/) - Detección de escenas
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI moderna

## Referencias

- [Especificación Técnica Completa](REQUERIMIENTOS_COMPLETOS.md)
- [Proyecto Original Python/Docker](../VideoContextBot)

## Roadmap

- [ ] Soporte para modelos TFLite
- [ ] Delegate GPU para inferencia más rápida
- [ ] Edición de transcripción manual
- [ ] Export a formatos adicionales (SRT, VTT)
- [ ] Modo batch (múltiples videos)
- [ ] Sync con cloud (opcional)

## Soporte

Issues: https://github.com/usuario/VideoContextBotAndroid/issues
