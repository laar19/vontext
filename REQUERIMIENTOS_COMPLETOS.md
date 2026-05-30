# VideoContextBot Android - Especificación Técnica Completa

## Resumen Ejecutivo

VideoContextBot Android es una aplicación nativa 100% local que procesa videos de grabaciones de pantalla para generar contexto enriquecido para agentes de IA. La aplicación procesa todo en el dispositivo sin dependencia de servidores externos, pero ofrece opción opcional de usar APIs remotas de Whisper para mayor velocidad.

**Características principales:**
- ✅ Procesamiento 100% local (offline-first)
- ✅ Modelos STT locales: Whisper tiny/small/medium (ONNX/TFLite)
- ✅ Opción de API remota Whisper (Groq/OpenAI/DeepSeek)
- ✅ Detección de escenas con OpenCV Android
- ✅ Generación de PDF nativa
- ✅ Creación de ZIP
- ✅ Construcción reproducible con Docker

---

## 1. Arquitectura del Sistema

### 1.1 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    VideoContextBot Android                       │
├─────────────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose)                                     │
│  ├── HomeScreen (Video picker, notes, interval slider)          │
│  ├── ProcessingScreen (Progress, logs, cancel)                  │
│  └── ResultsScreen (PDF viewer, ZIP share, history)             │
├─────────────────────────────────────────────────────────────────┤
│  ViewModel Layer (StateFlow + Hilt DI)                          │
│  └── VideoViewModel                                              │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer (Use Cases)                                        │
│  ├── ProcessVideoUseCase                                         │
│  ├── ExtractFramesUseCase                                        │
│  ├── TranscribeAudioUseCase                                      │
│  ├── GeneratePdfUseCase                                          │
│  └── CreateZipUseCase                                            │
├─────────────────────────────────────────────────────────────────┤
│  Data Layer                                                      │
│  ├── Repository (VideoRepository, JobRepository)                │
│  ├── Local (Room Database, DataStore)                           │
│  ├── Remote (Whisper API - opcional)                            │
│  └── Models (Job, FrameInfo, TranscriptionSegment)              │
├─────────────────────────────────────────────────────────────────┤
│  Processing Engine (Local)                                       │
│  ├── AudioDetector (MediaMetadataRetriever)                     │
│  ├── FrameExtractor (OpenCV + MediaMetadataRetriever)           │
│  ├── SceneDetector (OpenCV histogram comparison)                │
│  ├── WhisperLocal (Whisper.cpp / TFLite Whisper)                │
│  ├── PdfGenerator (PdfDocument / iText7)                        │
│  └── ZipCreator (java.util.zip)                                 │
├─────────────────────────────────────────────────────────────────┤
│  Background Processing                                           │
│  ├── WorkManager (VideoProcessingWorker)                        │
│  └── ForegroundService (Progress notifications)                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Flujo de Procesamiento

```
1. Select Video → 2. Configure (notes, interval) → 3. Create Job (Room)
       ↓
4. Queue WorkManager → 5. Foreground Service starts
       ↓
6. Analyze Video (audio?, duration) → 7. Extract Frames (scene/interval)
       ↓
8. Transcribe Audio (Local Whisper OR Remote API)
       ↓
9. Generate PDF → 10. Create ZIP → 11. Update Job (COMPLETED)
       ↓
12. Show Results (PDF viewer, ZIP share button)
```

---

## 2. Stack Tecnológico

### 2.1 Core Android

| Componente | Tecnología | Versión |
|------------|------------|---------|
| Lenguaje | Kotlin | 2.0+ |
| UI | Jetpack Compose | 1.6+ |
| Arquitectura | MVVM + Clean Architecture | - |
| DI | Hilt | 2.51+ |
| Navigation | Navigation Compose | 2.7+ |

### 2.2 Persistencia

| Componente | Tecnología | Versión |
|------------|------------|---------|
| Database | Room | 2.6+ |
| Preferences | DataStore | 1.0+ |

### 2.3 Procesamiento

| Componente | Tecnología | Versión | Nota |
|------------|------------|---------|------|
| Video/Audio | MediaMetadataRetriever | Android SDK | Nativo |
| Procesamiento Imagen | OpenCV Android | 4.9+ | Scene detection |
| STT Local | whisper.cpp (JNI) | 1.7+ | **Recomendado** |
| STT Local Alt | TFLite Whisper | - | Alternativa |
| STT Remoto | Retrofit + OkHttp | 2.9+ | Opcional |
| PDF | PdfDocument | Android SDK | Nativo |
| ZIP | java.util.zip | Android SDK | Nativo |

### 2.4 Background

| Componente | Tecnología | Versión |
|------------|------------|---------|
| Background Work | WorkManager | 2.9+ |
| Foreground Service | Android Service | SDK |
| Notificaciones | AndroidX Core | 1.12+ |

---

## 3. Modelos STT Locales (Offline)

### 3.1 Opción A: whisper.cpp (JNI) - **RECOMENDADO**

**Ventajas:**
- ✅ Rendimiento óptimo (C++ nativo)
- ✅ Soporte oficial para modelos Whisper
- ✅ Quantización disponible (menos memoria)
- ✅ Actualizaciones frecuentes
- ✅ Bajo consumo de batería

**Modelos disponibles:**

| Modelo | Tamaño | RAM Requerida | Velocidad (Android) | Precisión |
|--------|--------|---------------|---------------------|-----------|
| tiny | 75 MB | ~300 MB | ~0.5x realtime | Baja |
| base | 142 MB | ~500 MB | ~0.3x realtime | Media |
| small | 466 MB | ~1.2 GB | ~0.2x realtime | Buena |
| medium | 1.5 GB | ~3.5 GB | ~0.1x realtime | Excelente |
| large-v3 | 3.1 GB | ~7 GB | ~0.05x realtime | Máxima |

**Recomendación por dispositivo:**
- **Gama baja (<4GB RAM):** tiny o base
- **Gama media (4-6GB RAM):** small
- **Gama alta (>6GB RAM):** medium o large-v3

**Implementación:**
```kotlin
// WhisperCppWrapper.kt
class WhisperCppWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        System.loadLibrary("whisper")
    }

    external fun initModel(modelPath: String): Long
    external fun transcribe(audioPath: String, pointer: Long): TranscriptionResult
    external fun freeModel(pointer: Long)
}

data class TranscriptionResult(
    val text: String,
    val language: String,
    val segments: List<Segment>
)

data class Segment(
    val start: Float,    // segundos
    val end: Float,      // segundos
    val text: String
)
```

**Estructura JNI (C++):**
```cpp
// whisper-jni.cpp
#include "whisper.h"
#include <jni.h>

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_videocontextbot_processor_WhisperCppWrapper_initModel(
        JNIEnv* env, jobject thiz, jstring modelPath
    ) {
        const char* path = env->GetStringUTFChars(modelPath, nullptr);
        whisper_context_params params = whisper_context_default_params();
        whisper_context* ctx = whisper_init_from_file(path, params);
        env->ReleaseStringUTFChars(modelPath, path);
        return reinterpret_cast<jlong>(ctx);
    }

    JNIEXPORT jobject JNICALL
    Java_com_videocontextbot_processor_WhisperCppWrapper_transcribe(
        JNIEnv* env, jobject thiz, jstring audioPath, jlong pointer
    ) {
        whisper_context* ctx = reinterpret_cast<whisper_context*>(pointer);
        // ... implementación de transcripción
        // Retorna Kotlin TranscriptionResult
    }
}
```

### 3.2 Opción B: TFLite Whisper

**Ventajas:**
- ✅ Integración nativa con TensorFlow Lite
- ✅ Delegate GPU disponible
- ✅ Menor curva de aprendizaje

**Desventajas:**
- ⚠️ Modelos limitados
- ⚠️ Menor optimización que whisper.cpp

**Implementación:**
```kotlin
// TFLiteWhisperWrapper.kt
class TFLiteWhisperWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null

    fun loadModel(modelPath: String) {
        val model = File(modelPath)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            // Opcional: GPU delegate
            // addDelegateFactory(GpuDelegateFactory())
        }
        interpreter = Interpreter(FileInputStream(model), options)
    }

    fun transcribe(audioData: FloatArray): TranscriptionResult {
        // ... implementación
    }
}
```

### 3.3 Opción C: API Remota (Opcional)

**Proveedores soportados:**

| Proveedor | Endpoint | Modelos | Precio |
|-----------|----------|---------|--------|
| OpenAI | api.openai.com | whisper-1 | $0.006/min |
| Groq | api.groq.com | whisper-large-v3 | Gratis (limitado) |
| DeepSeek | api.deepseek.com | whisper | Variable |
| Ollama (self-hosted) | Custom | whisper | Gratis |

**Configuración:**
```kotlin
// SettingsScreen / DataStore
data class WhisperConfig(
    val mode: WhisperMode = WhisperMode.LOCAL_SMALL,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val model: String = "whisper-large-v3"
)

enum class WhisperMode {
    LOCAL_TINY,
    LOCAL_BASE,
    LOCAL_SMALL,
    LOCAL_MEDIUM,
    REMOTE_OPENAI,
    REMOTE_GROQ,
    REMOTE_DEEPSEEK,
    REMOTE_OLLAMA
}
```

**Retrofit API:**
```kotlin
interface WhisperApi {
    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribe(
        @Header("Authorization") apiKey: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") format: RequestBody,
        @Part("timestamp_granularities") timestamps: RequestBody
    ): TranscriptionResponse
}
```

---

## 4. Modelos y Datos

### 4.1 Entidades Room

```kotlin
@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey val jobId: String,
    val status: JobStatus,
    val videoPath: String,
    val videoFilename: String,
    val hasAudio: Boolean?,
    val videoDuration: Float?,  // segundos
    val additionalNotes: String?,
    val progress: Int,           // 0-100
    val progressMessage: String,
    val errorMessage: String?,
    val createdAt: Long,         // timestamp
    val startedAt: Long?,
    val completedAt: Long?,
    val outputFolder: String?,
    val pdfPath: String?,
    val zipPath: String?,
    val frameInterval: Int?,     // 0 = auto (scene detection)
    val whisperMode: String?,    // modo STT usado
    val transcriptionSegments: Int?  // cantidad de segmentos
)

enum class JobStatus {
    PENDING,
    ANALYZING,
    EXTRACTING_FRAMES,
    TRANSCRIBING,
    GENERATING_PDF,
    CREATING_ZIP,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

### 4.2 Modelos de Dominio

```kotlin
// FrameInfo.kt
data class FrameInfo(
    val frameNum: Int,           // 1-indexed
    val timestamp: Float,        // segundos desde inicio
    val path: String,            // ruta absoluta
    val filename: String,        // frame_001_00m00s.jpg
    val width: Int,
    val height: Int
)

// TranscriptionSegment.kt
data class TranscriptionSegment(
    val start: Float,            // segundos
    val end: Float,              // segundos
    val text: String
)

// VideoInfo.kt
data class VideoInfo(
    val path: String,
    val filename: String,
    val duration: Float,         // segundos
    val hasAudio: Boolean,
    val width: Int,
    val height: Int,
    val mimeType: String
)

// ProcessingConfig.kt
data class ProcessingConfig(
    val frameInterval: Int = 0,  // 0 = auto
    val whisperMode: WhisperMode = WhisperMode.LOCAL_SMALL,
    val additionalNotes: String? = null,
    val maxFrames: Int = 50,
    val minFrames: Int = 10,
    val sceneDetectThreshold: Float = 15.0f
)
```

### 4.3 DataStore Preferences

```kotlin
// AppSettings.kt
data class AppSettings(
    val defaultWhisperMode: WhisperMode = WhisperMode.LOCAL_SMALL,
    val defaultFrameInterval: Int = 0,
    val openaiApiKey: String? = null,
    val openaiBaseUrl: String? = null,
    val whisperModel: String = "whisper-small",
    val maxVideoSizeMb: Int = 2048,
    val cleanupTempAfterHours: Int = 24,
    val cleanupOutputAfterHours: Int = 48,
    val notificationsEnabled: Boolean = true
)
```

---

## 5. Procesamiento de Video

### 5.1 Detección de Audio

```kotlin
// AudioDetector.kt
class AudioDetector @Inject constructor() {
    
    fun hasAudio(videoPath: String): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            val hasAudio = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
            )
            hasAudio == "yes"
        } finally {
            retriever.release()
        }
    }

    fun getDuration(videoPath: String): Float {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            durationMs / 1000f
        } finally {
            retriever.release()
        }
    }

    fun getVideoInfo(videoPath: String): VideoInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            VideoInfo(
                path = videoPath,
                filename = File(videoPath).name,
                duration = getDuration(videoPath),
                hasAudio = hasAudio(videoPath),
                width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull() ?: 0,
                mimeType = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                ) ?: ""
            )
        } finally {
            retriever.release()
        }
    }
}
```

### 5.2 Extracción de Frames

```kotlin
// FrameExtractor.kt
class FrameExtractor @Inject constructor(
    private val sceneDetector: SceneDetector
) {
    companion object {
        private const val MAX_FRAME_WIDTH = 1920
        private const val JPEG_QUALITY = 85
    }

    suspend fun extractFrames(
        videoPath: String,
        outputDir: File,
        config: ProcessingConfig
    ): List<FrameInfo> {
        return if (config.frameInterval > 0) {
            extractByInterval(videoPath, outputDir, config.frameInterval)
        } else {
            extractBySceneDetection(videoPath, outputDir, config)
        }
    }

    private suspend fun extractByInterval(
        videoPath: String,
        outputDir: File,
        intervalSeconds: Int
    ): List<FrameInfo> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: return emptyList()

            val frames = mutableListOf<FrameInfo>()
            var currentTimeMs = 0L
            var frameNum = 1

            while (currentTimeMs < durationMs) {
                val bitmap = retriever.getFrameAtTime(
                    currentTimeMs,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (bitmap != null) {
                    val framePath = saveFrame(bitmap, outputDir, frameNum, currentTimeMs)
                    frames.add(
                        FrameInfo(
                            frameNum = frameNum,
                            timestamp = currentTimeMs / 1000f,
                            path = framePath,
                            filename = "frame_${frameNum.toString().padStart(3, '0')}_${formatTimestamp(currentTimeMs)}.jpg",
                            width = bitmap.width,
                            height = bitmap.height
                        )
                    )
                    bitmap.recycle()
                    frameNum++
                }

                currentTimeMs += intervalSeconds * 1000L
            }

            applyFrameLimits(frames)
        } finally {
            retriever.release()
        }
    }

    private suspend fun extractBySceneDetection(
        videoPath: String,
        outputDir: File,
        config: ProcessingConfig
    ): List<FrameInfo> {
        // Usar OpenCV para detección de escenas
        val sceneChanges = sceneDetector.detect(
            videoPath = videoPath,
            threshold = config.sceneDetectThreshold
        )

        // Si muy pocas escenas, fallback a intervalo
        if (sceneChanges.size < config.minFrames) {
            return extractByInterval(videoPath, outputDir, 10)
        }

        return extractFramesAtTimestamps(videoPath, outputDir, sceneChanges)
    }

    private fun saveFrame(
        bitmap: Bitmap,
        outputDir: File,
        frameNum: Int,
        timestampMs: Long
    ): String {
        // Redimensionar si excede 1920px
        val resized = if (bitmap.width > MAX_FRAME_WIDTH) {
            val scale = MAX_FRAME_WIDTH.toFloat() / bitmap.width
            Bitmap.createScaledBitmap(
                bitmap,
                MAX_FRAME_WIDTH,
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val filename = "frame_${frameNum.toString().padStart(3, '0')}_${formatTimestamp(timestampMs)}.jpg"
        val file = File(outputDir, filename)

        FileOutputStream(file).use { fos ->
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
        }

        if (resized != bitmap) resized.recycle()

        return file.absolutePath
    }

    private fun applyFrameLimits(frames: MutableList<FrameInfo>): List<FrameInfo> {
        if (frames.size <= 50) return frames
        
        // Subsamplear equidistantemente
        val result = mutableListOf<FrameInfo>()
        val step = frames.size.toFloat() / 50
        
        for (i in frames.indices step step.toInt()) {
            result.add(frames[i])
            if (result.size >= 50) break
        }

        // Asegurar último frame
        if (result.last() != frames.last()) {
            result[result.size - 1] = frames.last()
        }

        return result
    }

    private fun formatTimestamp(timestampMs: Long): String {
        val seconds = timestampMs / 1000
        val mins = seconds / 60
        val secs = seconds % 60
        return "${mins}m${secs}s"
    }
}
```

### 5.3 Detección de Escenas (OpenCV)

```kotlin
// SceneDetector.kt
class SceneDetector @Inject constructor() {

    /**
     * Detecta cambios de escena usando comparación de histogramas
     * Basado en el algoritmo de PySceneDetect ContentDetector
     */
    suspend fun detect(videoPath: String, threshold: Float = 15.0f): List<Float> {
        return withContext(Dispatchers.Default) {
            val sceneChanges = mutableListOf<Float>()
            
            // Extraer frames clave para análisis
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: return@withContext emptyList()

            // Muestrear cada 1 segundo para detección
            val sampleIntervalMs = 1000L
            var currentTimeMs = 0L
            
            var prevHistogram: Mat? = null
            var prevTimestamp = 0f

            while (currentTimeMs < durationMs) {
                val frame = retriever.getFrameAtTime(currentTimeMs)
                
                if (frame != null) {
                    val mat = Mat()
                    Utils.bitmapToMat(frame, mat)
                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

                    val histogram = Mat()
                    Imgproc.calcHist(
                        listOf(mat),
                        intArrayOf(0),
                        null,
                        histogram,
                        intArrayOf(256),
                        floatArrayOf(0f, 256f)
                    )
                    Imgproc.normalize(histogram, histogram, 0.0, 1.0, Core.NORM_MINMAX)

                    if (prevHistogram != null) {
                        val score = compareHistograms(prevHistogram!!, histogram)
                        if (score > threshold) {
                            sceneChanges.add(prevTimestamp)
                        }
                    }

                    prevHistogram = histogram
                    prevTimestamp = currentTimeMs / 1000f
                    frame.recycle()
                    mat.release()
                }

                currentTimeMs += sampleIntervalMs
            }

            retriever.release()
            prevHistogram?.release()

            sceneChanges
        }
    }

    private fun compareHistograms(hist1: Mat, hist2: Mat): Float {
        val correlation = Imgproc.compareHist(hist1, hist2, Imgproc.COMPARE_CORREL)
        // Convertir a score de diferencia (0 = igual, alto = diferente)
        return (1.0 - correlation).toFloat() * 100
    }
}
```

---

## 6. Transcripción de Audio

### 6.1 Extractor de Audio

```kotlin
// AudioExtractor.kt
class AudioExtractor @Inject constructor() {

    /**
     * Extrae audio del video a formato WAV para Whisper
     */
    suspend fun extractAudio(videoPath: String, outputDir: File): File {
        return withContext(Dispatchers.IO) {
            val audioFile = File(outputDir, "audio.wav")
            
            // Usar MediaCodec para decodificar audio
            val extractor = MediaExtractor()
            extractor.setDataSource(videoPath)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1) {
                throw IllegalStateException("No audio track found")
            }

            extractor.selectTrack(audioTrackIndex)

            // Configificar codec
            val codecName = audioFormat!!.getString(MediaFormat.KEY_MIME)
                ?.let { "audio/mp4a-latm" } // Asumir AAC, ajustar según necesite
                ?: "audio/mp4a-latm"

            val codec = MediaCodec.createDecoderByType(codecName)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            // Procesar buffers y escribir WAV
            // ... implementación de decoding

            codec.stop()
            codec.release()
            extractor.release()

            audioFile
        }
    }
}
```

### 6.2 Transcriptor Local (whisper.cpp)

```kotlin
// LocalWhisperTranscriber.kt
class LocalWhisperTranscriber @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioExtractor: AudioExtractor
) {
    private var whisperContext: Long = 0

    suspend fun loadModel(model: WhisperModel): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = getModelFile(model)
                whisperContext = WhisperCppWrapper(context).initModel(modelFile.absolutePath)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun transcribe(
        videoPath: String,
        outputDir: File,
        progressCallback: ((Int, String) -> Unit)? = null
    ): Result<TranscriptionResult> {
        return withContext(Dispatchers.IO) {
            try {
                progressCallback?.invoke(10, "Extrayendo audio...")
                val audioFile = audioExtractor.extractAudio(videoPath, outputDir)
                
                progressCallback?.invoke(20, "Transcribiendo con Whisper local...")
                val result = WhisperCppWrapper(context).transcribe(
                    audioFile.absolutePath,
                    whisperContext
                )

                // Guardar transcripción en archivo
                saveTranscriptionToFile(result, outputDir)

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getModelFile(model: WhisperModel): File {
        // Los modelos están en assets/models/ o descargados
        val modelDir = File(context.filesDir, "whisper_models")
        modelDir.mkdirs()

        val modelFile = File(modelDir, "${model.filename}.bin")
        
        if (!modelFile.exists()) {
            // Copiar desde assets o descargar
            copyModelFromAssets(model, modelFile)
        }

        return modelFile
    }

    private fun saveTranscriptionToFile(
        result: TranscriptionResult,
        outputDir: File
    ) {
        val file = File(outputDir, "transcription.txt")
        file.bufferedWriter().use { writer ->
            writer.writeLine("=== TRANSCRIPCIÓN ===")
            writer.writeLine("Idioma detectado: ${result.language}")
            writer.writeLine()

            result.segments.forEach { segment ->
                val startStr = formatTimestamp(segment.start)
                val endStr = formatTimestamp(segment.end)
                writer.writeLine("[$startStr - $endStr] ${segment.text}")
            }

            writer.writeLine()
            writer.writeLine("=== TEXTO COMPLETO ===")
            writer.writeLine(result.text)
        }
    }

    fun release() {
        if (whisperContext != 0L) {
            WhisperCppWrapper(context).freeModel(whisperContext)
            whisperContext = 0
        }
    }
}

enum class WhisperModel(val filename: String, val sizeMb: Int) {
    TINY("ggml-tiny", 75),
    BASE("ggml-base", 142),
    SMALL("ggml-small", 466),
    MEDIUM("ggml-medium", 1537),
    LARGE_V3("ggml-large-v3", 3093)
}
```

### 6.3 Transcriptor Remoto (API)

```kotlin
// RemoteWhisperTranscriber.kt
class RemoteWhisperTranscriber @Inject constructor(
    private val whisperApi: WhisperApi,
    private val audioExtractor: AudioExtractor,
    private val settingsRepository: SettingsRepository
) {
    suspend fun transcribe(
        videoPath: String,
        outputDir: File,
        progressCallback: ((Int, String) -> Unit)? = null
    ): Result<TranscriptionResult> {
        return withContext(Dispatchers.IO) {
            try {
                val config = settingsRepository.getWhisperConfig()
                
                progressCallback?.invoke(10, "Extrayendo audio...")
                val audioFile = audioExtractor.extractAudio(videoPath, outputDir)

                progressCallback?.invoke(20, "Subiendo a API remota...")
                
                val requestFile = audioFile.asRequestBody("audio/wav".toMediaType())
                val multipartFile = MultipartBody.Part.createFormData(
                    "file",
                    audioFile.name,
                    requestFile
                )

                val response = whisperApi.transcribe(
                    apiKey = "Bearer ${config.apiKey}",
                    file = multipartFile,
                    model = config.model.toRequestBody(),
                    format = "verbose_json".toRequestBody(),
                    timestamps = "segment".toRequestBody()
                )

                val result = response.toDomainModel()
                saveTranscriptionToFile(result, outputDir)

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

---

## 7. Generación de PDF

```kotlin
// PdfGenerator.kt
class PdfGenerator @Inject constructor() {

    companion object {
        private const val MAX_IMAGE_WIDTH_MM = 190f
        private const val TRANSCRIPTION_WINDOW_SECONDS = 15f
    }

    suspend fun generate(
        outputDir: File,
        videoFilename: String,
        hasAudio: Boolean,
        duration: String,
        frames: List<FrameInfo>,
        transcription: TranscriptionResult?,
        notes: String?
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val pdfFile = File(outputDir, "report.pdf")
                
                PdfDocument().use { document ->
                    var pageInfo = 0

                    // Página por frame
                    frames.forEach { frame ->
                        val pageInfo = PdfDocument.PageInfo.Builder(
                            595,  // A4 width en puntos
                            842,  // A4 height en puntos
                            ++pageInfo
                        ).create()

                        val page = document.startPage(pageInfo)
                        val canvas = page.canvas

                        // Header con timestamp
                        val paint = Paint().apply {
                            textSize = 14f
                            isFakeBoldText = true
                            color = Color.BLACK
                        }
                        val timestamp = formatTimestamp(frame.timestamp)
                        canvas.drawText("[$timestamp] Frame ${frame.frameNum}", 50f, 50f, paint)

                        // Transcripción relevante
                        if (transcription != null) {
                            val relevantSegments = getSegmentsForTimestamp(
                                transcription,
                                frame.timestamp,
                                TRANSCRIPTION_WINDOW_SECONDS
                            )

                            if (relevantSegments.isNotEmpty()) {
                                val textPaint = Paint().apply {
                                    textSize = 10f
                                    color = Color.DKGRAY
                                }

                                val textBounds = Rect()
                                var y = 80f

                                relevantSegments.forEach { segment ->
                                    textPaint.getTextBounds(segment.text, 0, segment.text.length, textBounds)
                                    
                                    // Word wrap simple
                                    val words = segment.text.split(" ")
                                    var line = ""
                                    words.forEach { word ->
                                        val testLine = if (line.isEmpty()) word else "$line $word"
                                        textPaint.getTextBounds(testLine, 0, testLine.length, textBounds)
                                        
                                        if (textBounds.width() > 500) {
                                            canvas.drawText(line, 50f, y, textPaint)
                                            y += 15f
                                            line = word
                                        } else {
                                            line = testLine
                                        }
                                    }
                                    
                                    if (line.isNotEmpty()) {
                                        canvas.drawText(line, 50f, y, textPaint)
                                        y += 15f
                                    }
                                    y += 10f
                                }
                            }
                        }

                        // Cargar y dibujar imagen
                        val bitmap = BitmapFactory.decodeFile(frame.path)
                        if (bitmap != null) {
                            val scale = Math.min(
                                500f / bitmap.width,
                                400f / bitmap.height
                            )
                            val scaledWidth = (bitmap.width * scale).toInt()
                            val scaledHeight = (bitmap.height * scale).toInt()

                            val rect = RectF(50f, y + 20f, 50f + scaledWidth, y + 20f + scaledHeight)
                            canvas.drawBitmap(bitmap, null, rect, null)
                            bitmap.recycle()
                        }

                        document.finishPage(page)
                    }

                    // Página final con transcripción completa
                    if (transcription != null) {
                        val page = document.startPage(
                            PdfDocument.PageInfo.Builder(595, 842, ++pageInfo).create()
                        )

                        val canvas = page.canvas
                        val paint = Paint().apply {
                            textSize = 16f
                            isFakeBoldText = true
                            color = Color.BLACK
                        }
                        canvas.drawText("TRANSCRIPCIÓN COMPLETA", 50f, 50f, paint)

                        val textPaint = Paint().apply {
                            textSize = 11f
                            color = Color.BLACK
                        }

                        var y = 90f
                        transcription.segments.forEach { segment ->
                            val line = "[${formatTimestamp(segment.start)} - ${formatTimestamp(segment.end)}] ${segment.text}"
                            
                            // Word wrap
                            val words = line.split(" ")
                            var currentLine = ""
                            words.forEach { word ->
                                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                                val bounds = Rect()
                                textPaint.getTextBounds(testLine, 0, testLine.length, bounds)
                                
                                if (bounds.width() > 500) {
                                    canvas.drawText(currentLine, 50f, y, textPaint)
                                    y += 15f
                                    currentLine = word
                                } else {
                                    currentLine = testLine
                                }
                            }
                            
                            if (currentLine.isNotEmpty()) {
                                canvas.drawText(currentLine, 50f, y, textPaint)
                                y += 15f
                            }
                            y += 5f

                            // Nueva página si necesario
                            if (y > 800) {
                                document.finishPage(page)
                                val newPage = document.startPage(
                                    PdfDocument.PageInfo.Builder(595, 842, ++pageInfo).create()
                                )
                                // Continuar en nueva página
                            }
                        }

                        document.finishPage(page)
                    }
                }

                // Escribir a archivo
                FileOutputStream(pdfFile).use { fos ->
                    // PdfDocument.writeTo() requiere API 30+
                    // Para versiones anteriores, usar iText7
                }

                Result.success(pdfFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getSegmentsForTimestamp(
        transcription: TranscriptionResult,
        timestamp: Float,
        windowSeconds: Float
    ): List<TranscriptionSegment> {
        val halfWindow = windowSeconds / 2
        return transcription.segments.filter { segment ->
            segment.start <= timestamp + halfWindow && segment.end >= timestamp - halfWindow
        }
    }
}
```

---

## 8. Creación de ZIP

```kotlin
// ZipCreator.kt
class ZipCreator @Inject constructor() {

    suspend fun create(outputDir: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val parentDir = outputDir.parentFile 
                    ?: throw IllegalStateException("Output dir has no parent")
                val zipFile = File(parentDir, "${outputDir.name}.zip")

                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    outputDir.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val entryName = file.relativeTo(outputDir).path
                            val entry = ZipEntry(entryName)
                            zos.putNextEntry(entry)

                            file.inputStream().use { fis ->
                                fis.copyTo(zos)
                            }

                            zos.closeEntry()
                        }
                }

                Result.success(zipFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

---

## 9. Background Processing

### 9.1 WorkManager

```kotlin
// VideoProcessingWorker.kt
@HiltWorker
class VideoProcessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val videoProcessor: VideoProcessor,
    private val jobRepository: JobRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString("job_id") ?: return Result.failure()
        val videoPath = inputData.getString("video_path") ?: return Result.failure()

        // Actualizar estado a PROCESSING
        jobRepository.updateStatus(jobId, JobStatus.PROCESSING)

        return try {
            videoProcessor.process(
                videoPath = videoPath,
                jobId = jobId,
                progressCallback = { progress, message ->
                    jobRepository.updateProgress(jobId, progress, message)
                    setProgress(workDataOf(
                        "progress" to progress,
                        "message" to message
                    ))
                }
            )

            jobRepository.updateStatus(jobId, JobStatus.COMPLETED)
            Result.success()
        } catch (e: CancellationException) {
            jobRepository.updateStatus(jobId, JobStatus.CANCELLED)
            Result.failure()
        } catch (e: Exception) {
            jobRepository.updateStatus(jobId, JobStatus.FAILED, e.message)
            Result.failure()
        }
    }
}
```

### 9.2 Foreground Service para Notificaciones

```kotlin
// ProcessingNotificationService.kt
@HiltAndroidApp
class ProcessingNotificationService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "processing_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId = intent?.getStringExtra("job_id") ?: return START_NOT_STICKY

        val notification = createNotification(jobId, 0, "Iniciando...")
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    fun updateProgress(jobId: String, progress: Int, message: String) {
        val notification = createNotification(jobId, progress, message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(jobId: String, progress: Int, message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("job_id", jobId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Procesando video")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_processing)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Procesamiento de Video",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Muestra el progreso del procesamiento de video"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

## 10. Interfaz de Usuario (Jetpack Compose)

### 10.1 HomeScreen

```kotlin
// HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: VideoViewModel = hiltViewModel(),
    onNavigateToProcessing: (String) -> Unit
) {
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var notes by remember { mutableStateOf("") }
    var frameInterval by remember { mutableStateOf(0) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedVideoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Video Picker
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            onClick = { videoPicker.launch("video/*") }
        ) {
            selectedVideoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Video seleccionado",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Seleccionar video")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notas
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas adicionales (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Intervalo de frames
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Intervalo de frames:")
            Text(
                text = if (frameInterval == 0) "Auto (detección de escenas)" else "$frameInterval segundos",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Slider(
            value = frameInterval.toFloat(),
            onValueChange = { frameInterval = it.toInt() },
            valueRange = 0f..30f,
            steps = 29
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón procesar
        Button(
            onClick = {
                selectedVideoUri?.let { uri ->
                    viewModel.startProcessing(uri, notes, frameInterval)
                    onNavigateToProcessing(viewModel.currentJobId!!)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedVideoUri != null
        ) {
            Text("Procesar Video")
        }
    }
}
```

### 10.2 ProcessingScreen

```kotlin
// ProcessingScreen.kt
@Composable
fun ProcessingScreen(
    jobId: String,
    viewModel: VideoViewModel = hiltViewModel(),
    onNavigateToResults: () -> Unit
) {
    val uiState by viewModel.processingState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Procesando...",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Barra de progreso
        LinearProgressIndicator(
            progress = uiState.progress / 100f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "${uiState.progress}%",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = uiState.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Logs
        Text(
            text = "Logs:",
            style = MaterialTheme.typography.labelLarge
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            LazyColumn {
                items(uiState.logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón cancelar
        if (uiState.status == JobStatus.PROCESSING) {
            OutlinedButton(
                onClick = { viewModel.cancelProcessing(jobId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }

        // Navegar a resultados cuando completa
        LaunchedEffect(uiState.status) {
            if (uiState.status == JobStatus.COMPLETED) {
                onNavigateToResults()
            }
        }
    }
}
```

### 10.3 ResultsScreen

```kotlin
// ResultsScreen.kt
@Composable
fun ResultsScreen(
    jobId: String,
    viewModel: VideoViewModel = hiltViewModel()
) {
    val job by viewModel.getJob(jobId).collectAsState(initial = null)

    if (job == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Estado
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (job!!.status) {
                    JobStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                    JobStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = job!!.status.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = job!!.progressMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Acciones
        job?.pdfPath?.let { pdfPath ->
            Button(
                onClick = { /* Abrir PDF */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver PDF")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        job?.zipPath?.let { zipPath ->
            Button(
                onClick = { /* Compartir ZIP */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir ZIP")
            }
        }
    }
}
```

---

## 11. Permisos Android

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application
    android:name=".VideoContextApplication"
    ...>
    
    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <service
        android:name=".service.ProcessingNotificationService"
        android:foregroundServiceType="dataSync"
        android:exported="false" />

    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths" />
    </provider>
</application>
```

---

## 12. Estructura del Proyecto

```
VideoContextBotAndroid/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/videocontextbot/
│   │       │   ├── VideoContextApplication.kt
│   │       │   ├── MainActivity.kt
│   │       │   ├── ui/
│   │       │   │   ├── theme/
│   │       │   │   │   ├── Theme.kt
│   │       │   │   │   ├── Color.kt
│   │       │   │   │   └── Type.kt
│   │       │   │   ├── navigation/
│   │       │   │   │   └── AppNavigation.kt
│   │       │   │   ├── screens/
│   │       │   │   │   ├── HomeScreen.kt
│   │       │   │   │   ├── ProcessingScreen.kt
│   │       │   │   │   ├── ResultsScreen.kt
│   │       │   │   │   ├── SettingsScreen.kt
│   │       │   │   │   └── HistoryScreen.kt
│   │       │   │   └── components/
│   │       │   │       ├── VideoPicker.kt
│   │       │   │       ├── NotesInput.kt
│   │       │   │       ├── IntervalSlider.kt
│   │       │   │       ├── ProgressIndicator.kt
│   │       │   │       ├── LogViewer.kt
│   │       │   │       └── JobCard.kt
│   │       │   ├── viewmodel/
│   │       │   │   └── VideoViewModel.kt
│   │       │   ├── data/
│   │       │   │   ├── repository/
│   │       │   │   │   ├── VideoRepository.kt
│   │       │   │   │   ├── JobRepository.kt
│   │       │   │   │   └── SettingsRepository.kt
│   │       │   │   ├── local/
│   │       │   │   │   ├── dao/
│   │       │   │   │   │   └── JobDao.kt
│   │       │   │   │   ├── database/
│   │       │   │   │   │   └── AppDatabase.kt
│   │       │   │   │   └── preferences/
│   │       │   │   │       └── AppSettings.kt
│   │       │   │   └── remote/
│   │       │   │       ├── api/
│   │       │   │       │   └── WhisperApi.kt
│   │       │   │       └── dto/
│   │       │   │           └── TranscriptionDto.kt
│   │       │   ├── domain/
│   │       │   │   ├── model/
│   │       │   │   │   ├── Job.kt
│   │       │   │   │   ├── FrameInfo.kt
│   │       │   │   │   ├── TranscriptionSegment.kt
│   │       │   │   │   ├── VideoInfo.kt
│   │       │   │   │   └── ProcessingConfig.kt
│   │       │   │   └── usecase/
│   │       │   │       ├── ProcessVideoUseCase.kt
│   │       │   │       ├── ExtractFramesUseCase.kt
│   │       │   │       ├── TranscribeAudioUseCase.kt
│   │       │   │       ├── GeneratePdfUseCase.kt
│   │       │   │       └── CreateZipUseCase.kt
│   │       │   ├── processor/
│   │       │   │   ├── VideoProcessor.kt
│   │       │   │   ├── AudioDetector.kt
│   │       │   │   ├── AudioExtractor.kt
│   │       │   │   ├── FrameExtractor.kt
│   │       │   │   ├── SceneDetector.kt
│   │       │   │   ├── transcriber/
│   │       │   │   │   ├── LocalWhisperTranscriber.kt
│   │       │   │   │   └── RemoteWhisperTranscriber.kt
│   │       │   │   ├── whisper/
│   │       │   │   │   ├── WhisperCppWrapper.kt
│   │       │   │   │   └── WhisperModel.kt
│   │       │   │   ├── PdfGenerator.kt
│   │       │   │   └── ZipCreator.kt
│   │       │   ├── worker/
│   │       │   │   └── VideoProcessingWorker.kt
│   │       │   ├── service/
│   │       │   │   └── ProcessingNotificationService.kt
│   │       │   ├── di/
│   │       │   │   ├── AppModule.kt
│   │       │   │   ├── DatabaseModule.kt
│   │       │   │   └── RepositoryModule.kt
│   │       │   └── util/
│   │       │       ├── Constants.kt
│   │       │       ├── Extensions.kt
│   │       │       └── TimestampFormatter.kt
│   │       ├── cpp/
│   │       │   ├── CMakeLists.txt
│   │       │   ├── whisper-jni.cpp
│   │       │   └── whisper/
│   │       │       └── (submodule whisper.cpp)
│   │       ├── res/
│   │       │   ├── values/
│   │       │   │   ├── strings.xml
│   │       │   │   ├── colors.xml
│   │       │   │   └── themes.xml
│   │       │   ├── drawable/
│   │       │   └── xml/
│   │       │       └── file_paths.xml
│   │       ├── assets/
│   │       │   └── models/
│   │       │       └── (modelos Whisper opcionales)
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
├── docker/
│   └── Dockerfile.build
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── local.properties
```

---

## 13. Dependencias Gradle

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.cmake)
}

android {
    namespace = "com.videocontextbot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.videocontextbot"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
            }
        }

        // ABI filters para reducir tamaño del APK
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // OpenCV
    implementation(libs.opencv)

    // Retrofit (API remota)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Coil (imágenes)
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Logging
    implementation(libs.timber)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
```

---

## 14. Docker Build Container

### 14.1 Dockerfile para Construir APK

```dockerfile
# docker/Dockerfile.build
FROM ubuntu:22.04

# Evitar prompts interactivos
ENV DEBIAN_FRONTEND=noninteractive

# Instalar dependencias del sistema
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    git \
    cmake \
    ninja-build \
    build-essential \
    clang \
    lld \
    && rm -rf /var/lib/apt/lists/*

# Configurar Java
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Crear usuario no-root
RUN useradd -m -s /bin/bash builder
USER builder
WORKDIR /home/builder

# Instalar Android SDK command-line tools
ENV ANDROID_HOME=/home/builder/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    cd $ANDROID_HOME/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip cmdline-tools.zip && \
    mv cmdline-tools latest && \
    rm cmdline-tools.zip

# Aceptar licencias Android
RUN yes | sdkmanager --licenses || true

# Instalar componentes SDK requeridos
RUN sdkmanager --install \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0" \
    "ndk;26.1.10909125" \
    "cmake;3.22.1"

# Instalar Gradle
ENV GRADLE_VERSION=8.5
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip && \
    unzip /tmp/gradle.zip -d /opt/ && \
    rm /tmp/gradle.zip && \
    ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle

# Variables de entorno para build
ENV ANDROID_NDK_HOME=$ANDROID_HOME/ndk/26.1.10909125
ENV ANDROID_NDK=$ANDROID_NDK_HOME

# Directorio de trabajo
WORKDIR /home/builder/project

# Script de build
COPY --chown=builder:builder build.sh /home/builder/build.sh
RUN chmod +x /home/builder/build.sh

# Comando por defecto
CMD ["/home/builder/build.sh"]
```

### 14.2 Script de Build

```bash
#!/bin/bash
# build.sh

set -e

echo "=== VideoContextBot Android Build ==="
echo "Android SDK: $ANDROID_HOME"
echo "Android NDK: $ANDROID_NDK_HOME"
echo "Gradle: $(gradle --version | grep 'Gradle')"

# Limpiar
echo "Cleaning..."
gradle clean

# Build de Native (whisper.cpp)
echo "Building native libraries..."
cd src/main/cpp
if [ ! -d "whisper" ]; then
    echo "Cloning whisper.cpp..."
    git clone https://github.com/ggerganov/whisper.cpp.git whisper
fi
cd whisper
git pull
make clean
make -j4

# Regresar y build del APK
cd ../..

echo "Building debug APK..."
gradle assembleDebug

echo "Building release APK..."
gradle assembleRelease

# Copiar APKs al volumen compartido
echo "Copying APKs to output..."
mkdir -p /output
cp build/outputs/apk/debug/*.apk /output/
cp build/outputs/apk/release/*.apk /output/

echo "=== Build Complete ==="
echo "APKs disponibles en /output/"
ls -la /output/
```

### 14.3 docker-compose.yml para Build

```yaml
# docker-compose.build.yml
version: '3.8'

services:
  builder:
    build:
      context: .
      dockerfile: docker/Dockerfile.build
    volumes:
      - ..:/home/builder/project:cached
      - ./output:/output
    environment:
      - GRADLE_USER_HOME=/home/builder/.gradle
    working_dir: /home/builder/project
```

### 14.4 Comandos de Build

```bash
# Construir imagen Docker
docker-compose -f docker-compose.build.yml build

# Construir APKs
docker-compose -f docker-compose.build.yml up

# Los APKs estarán en docker/output/
```

---

## 15. Modelos Whisper en el Repositorio

### 15.1 Estrategia de Modelos

**Opción A: Git LFS (Recomendado para modelos pequeños)**

```bash
# Instalar Git LFS
git lfs install

# Trackear modelos
git lfs track "app/src/main/assets/models/*.bin"

# Agregar archivo .gitattributes
echo "*.bin filter=lfs diff=lfs merge=lfs -text" >> .gitattributes

# Agregar modelos (solo tiny y base en el repo)
git add app/src/main/assets/models/ggml-tiny.bin
git add app/src/main/assets/models/ggml-base.bin
git commit -m "Add Whisper tiny and base models"
```

**Opción B: Descarga bajo demanda (Recomendado para modelos grandes)**

```kotlin
// ModelDownloader.kt
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun downloadModel(model: WhisperModel): Result<File> {
        val modelDir = File(context.filesDir, "whisper_models").apply { mkdirs() }
        val modelFile = File(modelDir, "${model.filename}.bin")

        if (modelFile.exists()) {
            return Result.success(modelFile)
        }

        // Descargar desde Hugging Face o GitHub Releases
        val url = when (model) {
            WhisperModel.TINY -> "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
            WhisperModel.BASE -> "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
            WhisperModel.SMALL -> "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
            WhisperModel.MEDIUM -> "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin"
            WhisperModel.LARGE_V3 -> "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3.bin"
        }

        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Download failed: ${response.code}"))
                }

                response.body?.byteStream()?.use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            Result.success(modelFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 15.2 .gitattributes para Git LFS

```
# .gitattributes
*.bin filter=lfs diff=lfs merge=lfs -text
*.pt filter=lfs diff=lfs merge=lfs -text
*.onnx filter=lfs diff=lfs merge=lfs -text
*.tflite filter=lfs diff=lfs merge=lfs -text
```

---

## 16. Configuración y Variables

### 16.1 BuildConfig

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "${versionCode}")
        buildConfigField("int", "MAX_VIDEO_SIZE_MB", "2048")
        buildConfigField("int", "MAX_FRAME_COUNT", "50")
        buildConfigField("int", "MIN_FRAME_COUNT", "10")
        buildConfigField("float", "SCENE_DETECT_THRESHOLD", "15.0f")
        buildConfigField("int", "FRAME_INTERVAL_SECONDS", "10")
    }
}
```

### 16.2 Constants.kt

```kotlin
// util/Constants.kt
object Constants {
    // Video
    const val MAX_VIDEO_SIZE_MB = 2048
    const val SUPPORTED_VIDEO_FORMATS = setOf("mp4", "mkv", "avi", "mov", "webm")

    // Frames
    const val MAX_FRAME_WIDTH = 1920
    const val JPEG_QUALITY = 85
    const val MAX_FRAME_COUNT = 50
    const val MIN_FRAME_COUNT = 10
    const val SCENE_DETECT_THRESHOLD = 15.0f
    const val FRAME_INTERVAL_SECONDS = 10

    // Transcription
    const val TRANSCRIPTION_WINDOW_SECONDS = 15f
    const val DEFAULT_WHISPER_MODEL = "ggml-small.bin"

    // Cleanup
    const val TEMP_CLEANUP_HOURS = 24
    const val OUTPUT_CLEANUP_HOURS = 48

    // WorkManager
    const val PROCESSING_WORK_TAG = "video_processing"
    const val CLEANUP_WORK_TAG = "cleanup_work"
}
```

---

## 17. Testing

### 17.1 Tests Unitarios

```kotlin
// AudioDetectorTest.kt
@Test
fun `hasAudio returns true for video with audio track`() {
    // Given
    val videoPath = createTestVideoWithAudio()

    // When
    val result = audioDetector.hasAudio(videoPath)

    // Then
    assertTrue(result)
}

@Test
fun `extractFramesByInterval returns correct frame count`() {
    // Given
    val videoPath = createTestVideo(durationSeconds = 60f)
    val intervalSeconds = 10

    // When
    val frames = runBlocking {
        frameExtractor.extractByInterval(videoPath, outputDir, intervalSeconds)
    }

    // Then
    assertTrue(frames.size in 10..50)
    assertEquals(6, frames.size) // 60 segundos / 10 = 6 frames
}
```

### 17.2 Tests de UI (Compose)

```kotlin
// HomeScreenTest.kt
@Test
fun homeScreen_showsVideoPicker() {
    composeTestRule.setContent {
        HomeScreen(onNavigateToProcessing = {})
    }

    composeTestRule
        .onNodeWithContentDescription("Seleccionar video")
        .assertIsDisplayed()
}

@Test
fun homeScreen_processButtonDisabledWithoutVideo() {
    composeTestRule.setContent {
        HomeScreen(onNavigateToProcessing = {})
    }

    composeTestRule
        .onNodeWithText("Procesar Video")
        .assertIsNotEnabled()
}
```

---

## 18. Checklist de Implementación

### Fase 1: Setup del Proyecto
- [ ] Configurar proyecto Android con Compose
- [ ] Configurar Hilt DI
- [ ] Configurar Room Database
- [ ] Configurar WorkManager
- [ ] Configurar CMake para whisper.cpp
- [ ] Implementar modelo de datos Job

### Fase 2: UI Básica
- [ ] HomeScreen con selector de video
- [ ] Input de notas
- [ ] Slider de intervalo
- [ ] Botón de procesar
- [ ] Navigation entre screens

### Fase 3: Procesamiento de Video
- [ ] AudioDetector (detección de audio)
- [ ] FrameExtractor por intervalo
- [ ] SceneDetector con OpenCV
- [ ] AudioExtractor (extraer audio para Whisper)

### Fase 4: Transcripción
- [ ] WhisperCppWrapper (JNI)
- [ ] LocalWhisperTranscriber
- [ ] RemoteWhisperTranscriber (API)
- [ ] ModelDownloader (descarga bajo demanda)

### Fase 5: PDF y ZIP
- [ ] PdfGenerator (PdfDocument nativo)
- [ ] ZipCreator (java.util.zip)

### Fase 6: Background Processing
- [ ] VideoProcessingWorker (WorkManager)
- [ ] ProcessingNotificationService (Foreground)
- [ ] Actualizar progreso en UI

### Fase 7: Resultados
- [ ] ResultsScreen
- [ ] PDF viewer (intent externo)
- [ ] ZIP share (intent externo)
- [ ] Historial de jobs

### Fase 8: Docker Build
- [ ] Dockerfile.build
- [ ] build.sh script
- [ ] docker-compose.build.yml
- [ ] Probar build en contenedor

### Fase 9: Pulido
- [ ] Manejo de errores
- [ ] Tests unitarios
- [ ] Tests de UI
- [ ] Optimización de memoria
- [ ] Probar en dispositivos reales

---

## 19. Comparación con Proyecto Original Python/Docker

| Característica | Proyecto Original | Android App |
|----------------|-------------------|-------------|
| **Procesamiento** | Servidor Docker | Local (dispositivo) |
| **STT** | Whisper API (remoto) | Whisper local + API opcional |
| **Scene Detection** | PySceneDetect | OpenCV Android |
| **PDF** | fpdf2 | PdfDocument nativo |
| **Background** | Celery | WorkManager |
| **Notificaciones** | Telegram | Notificaciones nativas |
| **UI** | Gradio Web | Jetpack Compose |
| **Database** | SQLite (volumen) | Room (local) |
| **Construcción** | docker-compose | Docker build container |

---

## 20. Recursos y Referencias

### 20.1 Documentación Oficial
- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [OpenCV Android](https://docs.opencv.org/4.x/d8/d63/group__java__android__service.html)
- [whisper.cpp](https://github.com/ggerganov/whisper.cpp)

### 20.2 Bibliotecas
- [Hilt](https://dagger.dev/hilt/)
- [Retrofit](https://square.github.io/retrofit/)
- [Coil](https://coil-kt.github.io/coil/)

### 20.3 APIs de Transcripción
- [OpenAI Whisper](https://platform.openai.com/docs/guides/speech-to-text)
- [Groq API](https://console.groq.com/docs/quickstart)
- [DeepSeek API](https://platform.deepseek.com/api-docs/)

---

**Documento generado:** 2026-05-30  
**Propósito:** Desarrollo de Android App 100% local con opción de API remota  
**Proyecto GitHub:** https://github.com/usuario/VideoContextBotAndroid
