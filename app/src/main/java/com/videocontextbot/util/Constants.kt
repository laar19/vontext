package com.videocontextbot.util

object Constants {
    const val MAX_VIDEO_SIZE_MB = 2048
    val SUPPORTED_VIDEO_FORMATS = setOf("mp4", "mkv", "avi", "mov", "webm")

    const val MAX_FRAME_WIDTH = 1280
    const val JPEG_QUALITY = 85
    const val MAX_FRAME_COUNT = 30
    const val MIN_FRAME_COUNT = 5
    const val SCENE_DETECT_THRESHOLD = 15.0f
    const val FRAME_INTERVAL_SECONDS = 10

    const val TRANSCRIPTION_WINDOW_SECONDS = 15f
    const val DEFAULT_WHISPER_MODEL = "ggml-small.bin"

    const val TEMP_CLEANUP_HOURS = 24
    const val OUTPUT_CLEANUP_HOURS = 48

    const val PROCESSING_WORK_TAG = "video_processing"
    const val CLEANUP_WORK_TAG = "cleanup_work"

    const val MODEL_DOWNLOAD_TIMEOUT_SECONDS = 300L
    const val MODEL_DOWNLOAD_RETRIES = 3
}
