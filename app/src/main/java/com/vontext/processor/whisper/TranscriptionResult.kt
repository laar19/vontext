package com.vontext.processor.whisper

import com.vontext.domain.model.TranscriptionSegment

data class TranscriptionResult(
    val text: String,
    val language: String,
    val segments: List<TranscriptionSegment>
)
