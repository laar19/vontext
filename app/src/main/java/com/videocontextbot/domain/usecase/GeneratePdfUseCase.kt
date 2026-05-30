package com.videocontextbot.domain.usecase

import com.videocontextbot.domain.model.FrameInfo
import com.videocontextbot.processor.PdfGenerator
import com.videocontextbot.processor.whisper.TranscriptionResult
import java.io.File
import javax.inject.Inject

class GeneratePdfUseCase @Inject constructor(
    private val pdfGenerator: PdfGenerator
) {
    suspend operator fun invoke(
        outputDir: File,
        videoFilename: String,
        hasAudio: Boolean,
        duration: String,
        frames: List<FrameInfo>,
        transcription: TranscriptionResult?,
        notes: String?
    ): Result<File> {
        return pdfGenerator.generate(
            outputDir = outputDir,
            videoFilename = videoFilename,
            hasAudio = hasAudio,
            duration = duration,
            frames = frames,
            transcription = transcription,
            notes = notes
        )
    }
}
