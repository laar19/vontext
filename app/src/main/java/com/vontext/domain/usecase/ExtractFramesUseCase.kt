package com.vontext.domain.usecase

import com.vontext.domain.model.FrameInfo
import com.vontext.domain.model.ProcessingConfig
import com.vontext.processor.FrameExtractor
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
