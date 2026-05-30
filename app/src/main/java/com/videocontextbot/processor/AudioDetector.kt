package com.videocontextbot.processor

import android.media.MediaMetadataRetriever
import com.videocontextbot.domain.model.VideoInfo
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioDetector @Inject constructor() {

    fun hasAudio(videoFile: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val hasAudio = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
            )
            hasAudio == "yes"
        } finally {
            retriever.release()
        }
    }

    fun getDuration(videoFile: File): Float {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            durationMs / 1000f
        } finally {
            retriever.release()
        }
    }

    fun getVideoInfo(videoFile: File): VideoInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            VideoInfo(
                path = videoFile.absolutePath,
                filename = videoFile.name,
                duration = getDuration(videoFile),
                hasAudio = hasAudio(videoFile),
                width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull() ?: 0,
                mimeType = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                ) ?: ""
            )
        } finally {
            retriever.release()
        }
    }
}
