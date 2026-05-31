package com.vontext.processor.whisper

import com.vontext.data.local.preferences.SettingsRepository
import com.vontext.data.remote.api.WhisperApi
import com.vontext.domain.model.TranscriptionSegment
import com.vontext.processor.AudioExtractor
import com.vontext.util.TimestampFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteWhisperTranscriber @Inject constructor(
    private val whisperApi: WhisperApi,
    private val audioExtractor: AudioExtractor,
    private val settingsRepository: SettingsRepository
) {
    suspend fun transcribe(
        videoFile: File,
        outputDir: File,
        progressCallback: (suspend (Int, String) -> Unit)? = null
    ): Result<TranscriptionResult> {
        return withContext(Dispatchers.IO) {
            try {
                progressCallback?.invoke(10, "Extrayendo audio...")
                val audioFile = audioExtractor.extractAudio(videoFile, outputDir)

                progressCallback?.invoke(20, "Subiendo a API remota...")

                val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
                val multipartFile = MultipartBody.Part.createFormData(
                    "file",
                    audioFile.name,
                    requestFile
                )

                val response = whisperApi.transcribe(
                    model = "whisper-1".toRequestBody(),
                    file = multipartFile,
                    format = "verbose_json".toRequestBody(),
                    timestamps = "segment".toRequestBody()
                )

                val result = TranscriptionResult(
                    text = response.text,
                    language = response.language ?: "unknown",
                    segments = response.segments?.map {
                        TranscriptionSegment(start = it.start, end = it.end, text = it.text)
                    } ?: emptyList()
                )

                saveTranscriptionToFile(result, outputDir)
                progressCallback?.invoke(50, "Transcripción completada")

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun saveTranscriptionToFile(result: TranscriptionResult, outputDir: File) {
        val file = File(outputDir, "transcription.txt")
        file.bufferedWriter().use { writer ->
            writer.appendLine("=== TRANSCRIPCIÓN (API Remota) ===")
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
}
