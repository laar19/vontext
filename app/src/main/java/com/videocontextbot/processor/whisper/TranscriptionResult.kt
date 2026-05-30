package com.videocontextbot.processor.whisper

import com.videocontextbot.domain.model.TranscriptionSegment

data class TranscriptionResult(
    val text: String,
    val language: String,
    val segments: List<TranscriptionSegment>
)
