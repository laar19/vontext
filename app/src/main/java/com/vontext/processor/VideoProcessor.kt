package com.vontext.processor

import com.vontext.domain.model.ProcessingConfig
import com.vontext.processor.whisper.LocalWhisperTranscriber
import com.vontext.processor.whisper.RemoteWhisperTranscriber
import com.vontext.processor.whisper.WhisperMode
import com.vontext.util.TimestampFormatter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoProcessor @Inject constructor(
    private val audioDetector: AudioDetector,
    private val audioExtractor: AudioExtractor,
    private val frameExtractor: FrameExtractor,
    private val localWhisperTranscriber: LocalWhisperTranscriber,
    private val remoteWhisperTranscriber: RemoteWhisperTranscriber,
    private val pdfGenerator: PdfGenerator,
    private val zipCreator: ZipCreator
) {
    suspend fun process(
        videoFile: File,
        jobId: String,
        config: ProcessingConfig,
        outputDir: File,
        progressCallback: (suspend (Int, String) -> Unit)? = null
    ): Result<File> {
        return try {
            progressCallback?.invoke(5, "Analizando video...")
            val videoInfo = audioDetector.getVideoInfo(videoFile)
            val duration = TimestampFormatter.formatLong(videoInfo.duration)

            progressCallback?.invoke(15, "Extrayendo frames...")
            val frames = frameExtractor.extractFrames(videoFile, outputDir, config)

            val transcriptionResult = if (videoInfo.hasAudio) {
                when (config.whisperMode) {
                    WhisperMode.LOCAL_TINY, WhisperMode.LOCAL_BASE,
                    WhisperMode.LOCAL_SMALL, WhisperMode.LOCAL_MEDIUM -> {
                        progressCallback?.invoke(40, "Transcribiendo audio (local)...")
                        localWhisperTranscriber.transcribe(videoFile, outputDir, progressCallback)
                            .getOrNull()
                    }
                    WhisperMode.REMOTE_OPENAI, WhisperMode.REMOTE_GROQ,
                    WhisperMode.REMOTE_DEEPSEEK, WhisperMode.REMOTE_OLLAMA -> {
                        progressCallback?.invoke(40, "Transcribiendo audio (remoto)...")
                        remoteWhisperTranscriber.transcribe(videoFile, outputDir, progressCallback)
                            .getOrNull()
                    }
                }
            } else null

            progressCallback?.invoke(70, "Generando PDF...")
            pdfGenerator.generate(
                outputDir = outputDir,
                videoFilename = videoFile.name,
                hasAudio = videoInfo.hasAudio,
                duration = duration,
                frames = frames,
                transcription = transcriptionResult,
                notes = config.additionalNotes
            ).getOrThrow()

            progressCallback?.invoke(90, "Creando ZIP...")
            val zipResult = zipCreator.create(outputDir).getOrThrow()

            progressCallback?.invoke(100, "Procesamiento completado")
            Result.success(zipResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
