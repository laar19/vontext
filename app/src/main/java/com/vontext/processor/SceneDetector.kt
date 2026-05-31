package com.vontext.processor

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneDetector @Inject constructor() {

    suspend fun detect(
        videoFile: File,
        threshold: Float = 15.0f,
        sampleIntervalMs: Long = 1000L
    ): List<Float> {
        return withContext(Dispatchers.Default) {
            val sceneChanges = mutableListOf<Float>()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoFile.absolutePath)

                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: return@withContext emptyList()

                val adjustedInterval = if (durationMs > 600_000L) {
                    (durationMs / 300).coerceIn(2000L, 5000L)
                } else {
                    sampleIntervalMs
                }

                var currentTimeMs = 0L
                var prevHistogram: IntArray? = null
                var prevTimestamp = 0f
                var frameCount = 0
                val maxFrames = 300

                while (currentTimeMs < durationMs && frameCount < maxFrames) {
                    val frame = retriever.getFrameAtTime(
                        currentTimeMs,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    if (frame != null) {
                        val histogram = computeHistogram(frame)

                        if (prevHistogram != null) {
                            val score = compareHistograms(prevHistogram!!, histogram)
                            if (score > threshold) {
                                sceneChanges.add(prevTimestamp)
                            }
                        }

                        prevHistogram = histogram
                        prevTimestamp = currentTimeMs / 1000f
                        frame.recycle()
                    }

                    currentTimeMs += adjustedInterval
                    frameCount++
                }

                sceneChanges
            } finally {
                retriever.release()
            }
        }
    }

    private fun compareHistograms(hist1: IntArray, hist2: IntArray): Float {
        var diff = 0.0
        for (i in 0 until 256) {
            diff += kotlin.math.abs(hist1[i] - hist2[i]).toDouble()
        }
        return (diff / (hist1.size * 256)).toFloat() * 100
    }

    private fun computeHistogram(bitmap: Bitmap): IntArray {
        val histogram = IntArray(256)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val gray = ((r + g + b) / 3).coerceIn(0, 255)
            histogram[gray]++
        }

        val total = pixels.size.toFloat()
        for (i in histogram.indices) {
            histogram[i] = ((histogram[i] / total) * 1000).toInt()
        }

        return histogram
    }
}
