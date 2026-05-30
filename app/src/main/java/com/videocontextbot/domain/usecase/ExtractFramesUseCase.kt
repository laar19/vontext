package com.videocontextbot.domain.usecase

import com.videocontextbot.domain.model.FrameInfo
import com.videocontextbot.domain.model.ProcessingConfig
import com.videocontextbot.processor.FrameExtractor
import java.io.File
import javax.inject.Inject

class ExtractFramesUseCase @Inject constructor(
    private val frameExtractor: FrameExtractor
) {
    suspend operator fun invoke(
        videoFile: File,
        outputDir: File,
        config: ProcessingConfig
    ): List<FrameInfo> {
        return frameExtractor.extractFrames(videoFile, outputDir, config)
    }
}
