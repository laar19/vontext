package com.videocontextbot.domain.usecase

import com.videocontextbot.processor.whisper.LocalWhisperTranscriber
import com.videocontextbot.processor.whisper.TranscriptionResult
import java.io.File
import javax.inject.Inject

class TranscribeAudioUseCase @Inject constructor(
    private val localWhisperTranscriber: LocalWhisperTranscriber
) {
    suspend operator fun invoke(
        videoFile: File,
        outputDir: File,
        progressCallback: ((Int, String) -> Unit)? = null
    ): Result<TranscriptionResult> {
        return localWhisperTranscriber.transcribe(videoFile, outputDir, progressCallback)
    }
}
