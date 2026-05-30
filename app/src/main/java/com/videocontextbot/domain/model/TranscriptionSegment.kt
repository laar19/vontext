package com.videocontextbot.domain.model

data class TranscriptionSegment(
    val start: Float,
    val end: Float,
    val text: String
)
