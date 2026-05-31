package com.vontext.domain.model

data class VideoInfo(
    val path: String,
    val filename: String,
    val duration: Float,
    val hasAudio: Boolean,
    val width: Int,
    val height: Int,
    val mimeType: String
)
