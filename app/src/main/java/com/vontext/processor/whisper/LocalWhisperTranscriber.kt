package com.vontext.processor.whisper

import android.content.Context
import com.vontext.processor.AudioExtractor
import com.vontext.util.TimestampFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalWhisperTranscriber @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioExtractor: AudioExtractor
) {
    private var whisperContext: Long = 0

    suspend fun loadModel(model: WhisperModel): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = getModelFile(model)
                whisperContext = WhisperCppWrapper().initModel(modelFile.absolutePath)
                if (whisperContext == 0L) {
                    Result.failure(Exception("No se pudo inicializar el modelo Whisper"))
                } else {
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun transcribe(
        videoFile: File,
        outputDir: File,
        progressCallback: (suspend (Int, String) -> Unit)? = null
    ): Result<TranscriptionResult> {
        return withContext(Dispatchers.IO) {
            try {
                progressCallback?.invoke(10, "Extrayendo audio...")
                val audioFile = audioExtractor.extractAudio(videoFile, outputDir)

                progressCallback?.invoke(30, "Transcribiendo con Whisper local...")
                if (whisperContext == 0L) {
                    return@withContext Result.failure(Exception("Modelo Whisper no cargado"))
                }

                val result = WhisperCppWrapper().transcribe(
                    audioFile.absolutePath,
                    whisperContext
                )

                saveTranscriptionToFile(result, outputDir)
                progressCallback?.invoke(50, "Transcripción completada")

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getModelFile(model: WhisperModel): File {
        val modelDir = File(context.filesDir, "whisper_models").apply { mkdirs() }
        val modelFile = File(modelDir, "${model.filename}.bin")
        if (!modelFile.exists()) {
            copyModelFromAssets(model, modelFile)
        }
        return modelFile
    }

    private fun copyModelFromAssets(model: WhisperModel, destination: File) {
        try {
            context.assets.open("models/${model.filename}.bin").use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun saveTranscriptionToFile(result: TranscriptionResult, outputDir: File) {
        val file = File(outputDir, "transcription.txt")
        file.bufferedWriter().use { writer ->
            writer.appendLine("=== TRANSCRIPCIÓN ===")
            writer.appendLine("Idioma detectado: ${result.language}")
            writer.appendLine()
            result.segments.forEach { segment ->
                val startStr = TimestampFormatter.format(segment.start)
                val endStr = TimestampFormatter.format(segment.end)
                writer.appendLine("[$startStr - $endStr] ${segment.text}")
            }
            writer.appendLine()
            writer.appendLine("=== TEXTO COMPLETO ===")
            writer.appendLine(result.text)
        }
    }

    fun release() {
        if (whisperContext != 0L) {
            try {
                WhisperCppWrapper().freeModel(whisperContext)
            } catch (_: Exception) {
            }
            whisperContext = 0
        }
    }
}
