package com.vontext.domain.model

data class FrameInfo(
    val frameNum: Int,
    val timestamp: Float,
    val path: String,
    val filename: String,
    val width: Int,
    val height: Int
)
