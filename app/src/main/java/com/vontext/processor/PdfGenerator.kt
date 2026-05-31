package com.vontext.processor

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.vontext.domain.model.FrameInfo
import com.vontext.domain.model.TranscriptionSegment
import com.vontext.processor.whisper.TranscriptionResult
import com.vontext.util.TimestampFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor() {

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 50f
        private const val TRANSCRIPTION_WINDOW_SECONDS = 15f
    }

    suspend fun generate(
        outputDir: File,
        videoFilename: String,
        hasAudio: Boolean,
        duration: String,
        frames: List<FrameInfo>,
        transcription: TranscriptionResult?,
        notes: String?
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val pdfFile = File(outputDir, "report.pdf")
                val document = PdfDocument()

                var pageNum = 0

                for (frame in frames) {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        PAGE_WIDTH, PAGE_HEIGHT, ++pageNum
                    ).create()

                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    var y = MARGIN

                    val titlePaint = Paint().apply {
                        textSize = 14f
                        isFakeBoldText = true
                        color = Color.BLACK
                    }
                    val timestamp = TimestampFormatter.format(frame.timestamp)
                    canvas.drawText("[$timestamp] Frame ${frame.frameNum}", MARGIN, y, titlePaint)
                    y += 25f

                    if (transcription != null) {
                        val relevantSegments = getSegmentsForTimestamp(
                            transcription,
                            frame.timestamp,
                            TRANSCRIPTION_WINDOW_SECONDS
                        )

                        if (relevantSegments.isNotEmpty()) {
                            val textPaint = Paint().apply {
                                textSize = 10f
                                color = Color.DKGRAY
                            }

                            for (segment in relevantSegments) {
                                y = drawWrappedText(canvas, segment.text, MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint)
                                y += 5f
                                if (y > PAGE_HEIGHT - MARGIN) break
                            }
                            y += 10f
                        }
                    }

                    if (y < PAGE_HEIGHT - 200f) {
                        val bitmap = BitmapFactory.decodeFile(frame.path)
                        if (bitmap != null) {
                            val scale = Math.min(
                                (PAGE_WIDTH - 2 * MARGIN) / bitmap.width,
                                (PAGE_HEIGHT - y - MARGIN - 20f) / bitmap.height
                            )
                            val scaledW = (bitmap.width * scale).toInt()
                            val scaledH = (bitmap.height * scale).toInt()
                            val rect = RectF(MARGIN, y + 20f, MARGIN + scaledW, y + 20f + scaledH)
                            canvas.drawBitmap(bitmap, null, rect, null)
                            bitmap.recycle()
                        }
                    }

                    document.finishPage(page)
                }

                if (transcription != null && transcription.segments.isNotEmpty()) {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        PAGE_WIDTH, PAGE_HEIGHT, ++pageNum
                    ).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    val titlePaint = Paint().apply {
                        textSize = 16f
                        isFakeBoldText = true
                        color = Color.BLACK
                    }
                    canvas.drawText("TRANSCRIPCIÓN COMPLETA", MARGIN, MARGIN + 10f, titlePaint)

                    val textPaint = Paint().apply {
                        textSize = 11f
                        color = Color.BLACK
                    }

                    var y = MARGIN + 50f
                    for (segment in transcription.segments) {
                        val line = "[${TimestampFormatter.format(segment.start)} - ${TimestampFormatter.format(segment.end)}] ${segment.text}"
                        y = drawWrappedText(canvas, line, MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint)
                        y += 5f

                        if (y > PAGE_HEIGHT - MARGIN - 50f) {
                            document.finishPage(page)
                            val newPage = document.startPage(
                                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, ++pageNum).create()
                            )
                            y = MARGIN
                            drawWrappedText(newPage.canvas, line, MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint)
                            y += 15f
                        }
                    }

                    document.finishPage(page)
                }

                FileOutputStream(pdfFile).use { fos ->
                    document.writeTo(fos)
                }
                document.close()

                Result.success(pdfFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getSegmentsForTimestamp(
        transcription: TranscriptionResult,
        timestamp: Float,
        windowSeconds: Float
    ): List<TranscriptionSegment> {
        val halfWindow = windowSeconds / 2
        return transcription.segments.filter { segment ->
            segment.start <= timestamp + halfWindow && segment.end >= timestamp - halfWindow
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        var y = startY
        val words = text.split(" ")
        var line = ""
        val bounds = Rect()

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            paint.getTextBounds(testLine, 0, testLine.length, bounds)
            if (bounds.width() > maxWidth.toInt()) {
                canvas.drawText(line, x, y, paint)
                y += 15f
                line = word
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
            y += 15f
        }
        return y
    }
}
