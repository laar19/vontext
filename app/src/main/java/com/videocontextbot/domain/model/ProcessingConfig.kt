package com.videocontextbot.domain.model

import com.videocontextbot.processor.whisper.WhisperMode

data class ProcessingConfig(
    val frameInterval: Int = 0,
    val whisperMode: WhisperMode = WhisperMode.LOCAL_SMALL,
    val additionalNotes: String? = null,
    val maxFrames: Int = 30,
    val minFrames: Int = 5,
    val sceneDetectThreshold: Float = 15.0f
)
