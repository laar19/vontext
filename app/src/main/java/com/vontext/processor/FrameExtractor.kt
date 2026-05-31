package com.vontext.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.vontext.domain.model.FrameInfo
import com.vontext.domain.model.ProcessingConfig
import com.vontext.util.Constants
import com.vontext.util.TimestampFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrameExtractor @Inject constructor(
    private val sceneDetector: SceneDetector
) {
    suspend fun extractFrames(
        videoFile: File,
        outputDir: File,
        config: ProcessingConfig
    ): List<FrameInfo> {
        return if (config.frameInterval > 0) {
            extractByInterval(videoFile, outputDir, config.frameInterval)
        } else {
            extractBySceneDetection(videoFile, outputDir, config)
        }
    }

    private fun extractByInterval(
        videoFile: File,
        outputDir: File,
        intervalSeconds: Int
    ): List<FrameInfo> {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: return emptyList()

            val frames = mutableListOf<FrameInfo>()
            var currentTimeMs = 0L
            var frameNum = 1

            while (currentTimeMs < durationMs && frameNum <= Constants.MAX_FRAME_COUNT) {
                val bitmap = retriever.getFrameAtTime(
                    currentTimeMs,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                if (bitmap != null) {
                    val framePath = saveFrame(bitmap, outputDir, frameNum, currentTimeMs)
                    frames.add(
                        FrameInfo(
                            frameNum = frameNum,
                            timestamp = currentTimeMs / 1000f,
                            path = framePath,
                            filename = "frame_${frameNum.toString().padStart(3, '0')}_${TimestampFormatter.formatTimestamp(currentTimeMs)}.jpg",
                            width = bitmap.width,
                            height = bitmap.height
                        )
                    )
                    bitmap.recycle()
                    frameNum++
                }
                currentTimeMs += intervalSeconds * 1000L
            }

            return limitFrameCount(frames)
        } finally {
            retriever.release()
        }
    }

    private suspend fun extractBySceneDetection(
        videoFile: File,
        outputDir: File,
        config: ProcessingConfig
    ): List<FrameInfo> {
        val sceneChanges = sceneDetector.detect(
            videoFile = videoFile,
            threshold = config.sceneDetectThreshold
        )

        if (sceneChanges.size < config.minFrames) {
            return extractByInterval(videoFile, outputDir, 10)
        }

        return extractFramesAtTimestamps(videoFile, outputDir, sceneChanges)
    }

    private fun extractFramesAtTimestamps(
        videoFile: File,
        outputDir: File,
        timestamps: List<Float>
    ): List<FrameInfo> {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val limited = timestamps.take(Constants.MAX_FRAME_COUNT)
            val frames = mutableListOf<FrameInfo>()

            for ((index, timeSec) in limited.withIndex()) {
                val timeMs = (timeSec * 1000).toLong()
                val bitmap = retriever.getFrameAtTime(
                    timeMs,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                if (bitmap != null) {
                    val frameNum = index + 1
                    val framePath = saveFrame(bitmap, outputDir, frameNum, timeMs)
                    frames.add(
                        FrameInfo(
                            frameNum = frameNum,
                            timestamp = timeSec,
                            path = framePath,
                            filename = "frame_${frameNum.toString().padStart(3, '0')}_${TimestampFormatter.format(timeSec)}.jpg",
                            width = bitmap.width,
                            height = bitmap.height
                        )
                    )
                    bitmap.recycle()
                }
            }

            return frames
        } finally {
            retriever.release()
        }
    }

    private fun saveFrame(
        bitmap: Bitmap,
        outputDir: File,
        frameNum: Int,
        timestampMs: Long
    ): String {
        val resized = if (bitmap.width > Constants.MAX_FRAME_WIDTH) {
            val scale = Constants.MAX_FRAME_WIDTH.toFloat() / bitmap.width
            Bitmap.createScaledBitmap(
                bitmap,
                Constants.MAX_FRAME_WIDTH,
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val filename = "frame_${frameNum.toString().padStart(3, '0')}_${TimestampFormatter.formatTimestamp(timestampMs)}.jpg"
        val file = File(outputDir, filename)

        FileOutputStream(file).use { fos ->
            resized.compress(Bitmap.CompressFormat.JPEG, Constants.JPEG_QUALITY, fos)
        }

        if (resized !== bitmap) resized.recycle()

        return file.absolutePath
    }

    private fun limitFrameCount(frames: MutableList<FrameInfo>): List<FrameInfo> {
        if (frames.size <= Constants.MAX_FRAME_COUNT) return frames

        val result = mutableListOf<FrameInfo>()
        val step = frames.size.toFloat() / Constants.MAX_FRAME_COUNT
        var i = 0
        while (i < frames.size && result.size < Constants.MAX_FRAME_COUNT) {
            result.add(frames[i])
            i += step.toInt().coerceAtLeast(1)
        }
        if (result.isNotEmpty() && result.last() != frames.last()) {
            result[result.size - 1] = frames.last()
        }
        return result
    }

    private fun applyFrameLimits(timestamps: List<Long>): List<Long> {
        if (timestamps.size <= Constants.MAX_FRAME_COUNT) return timestamps
        val result = mutableListOf<Long>()
        val step = timestamps.size.toFloat() / Constants.MAX_FRAME_COUNT
        var i = 0
        while (i < timestamps.size && result.size < Constants.MAX_FRAME_COUNT) {
            result.add(timestamps[i])
            i += step.toInt().coerceAtLeast(1)
        }
        if (result.isNotEmpty() && result.last() != timestamps.last()) {
            result[result.size - 1] = timestamps.last()
        }
        return result
    }
}
