package com.vontext.domain.usecase

import com.vontext.domain.model.FrameInfo
import com.vontext.processor.PdfGenerator
import com.vontext.processor.whisper.TranscriptionResult
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
